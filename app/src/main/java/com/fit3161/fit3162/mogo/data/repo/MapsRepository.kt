package com.fit3161.fit3162.mogo.data.repo

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.fit3161.fit3162.mogo.data.model.RouteResult
import com.fit3161.fit3162.mogo.data.remote.RoutesApiService
import com.fit3161.fit3162.mogo.data.remote.dto.LatLngLiteral
import com.fit3161.fit3162.mogo.data.remote.dto.RoutesRequest
import com.fit3161.fit3162.mogo.data.remote.dto.Waypoint
import com.fit3161.fit3162.mogo.data.remote.dto.WaypointLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Repository for mapping and routing operations.
 * Google Maps API, device location, and reverse geocoding.
 *
 * @param context Android Context
 * @param apiService Routes API
 * @param apiKey Google Map's API Key
 * @param fusedLocationProviderClient current Location
 */
class MapsRepository(
    private val context: Context,
    private val apiService: RoutesApiService,
    private val apiKey: String,
    private val fusedLocationProviderClient: FusedLocationProviderClient
) {
    companion object {
        private const val TAG = "MapsRepository"
    }

    /**
     * Get device location (location permissions for the app on the device must be enabled, else it won't work).
     */
    @SuppressLint("MissingPermission")
    suspend fun getDeviceLocation(): Result<LatLng> = runCatching {
        val location: android.location.Location? = fusedLocationProviderClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).await()
        if (location != null) {
            Log.d(TAG, "Device location: ${location.latitude}, ${location.longitude}")
            LatLng(location.latitude, location.longitude)
        } else {
            throw Exception("Unable to get device location. Ensure location services are enabled.")
        }
    }

    /**
     * Converts lat/lng to a address string.
     */
    suspend fun reverseGeocode(latLng: LatLng): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val results = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

            if (!results.isNullOrEmpty()) {

                val address = results[0]
                val parts = listOfNotNull(
                    address.subThoroughfare,
                    address.thoroughfare,
                    address.locality
                )

                val formatted = if (parts.isNotEmpty()) parts.joinToString(" ")

                else address.getAddressLine(0) ?: ""

                Log.d(TAG, "Reverse geocoded: $formatted")
                formatted.ifBlank { formatLatLng(latLng) }
            } else {
                formatLatLng(latLng)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reverse geocode failed: ${e.message}")
            formatLatLng(latLng)
        }
    }

    /**
     * Format latitude and longitude coordinates.
     */
    private fun formatLatLng(latLng: LatLng): String =
        "(%.4f, %.4f)".format(latLng.latitude, latLng.longitude)

    /**
     * Returns a route based on the origin and destination of the route.
     * Route origin and destination are set by drivers (rider only adds a pickup point).
     */
    suspend fun getRoute(origin: LatLng, destination: LatLng): Result<RouteResult> = runCatching {
        val request = RoutesRequest(
            origin = Waypoint(WaypointLocation(LatLngLiteral(origin.latitude, origin.longitude))),
            destination = Waypoint(WaypointLocation(LatLngLiteral(destination.latitude, destination.longitude)))
        )

        val response = apiService.computeRoutes(apiKey = apiKey, request = request)
        check(!response.routes.isNullOrEmpty()) { "Routes API returned no routes." }

        val route = response.routes.first()
        val leg = route.legs.first()

        RouteResult(
            polylinePoints = route.polyline.encodedPolyline,
            distanceText = leg.localizedValues.distance.text,
            durationText = leg.localizedValues.duration.text,
            distanceMeters = route.distanceMeters,
            durationSeconds = route.duration.trimEnd('s').toIntOrNull() ?: 0,
            startLocation = LatLng(leg.startLocation.latLng.latitude, leg.startLocation.latLng.longitude),
            endLocation = LatLng(leg.endLocation.latLng.latitude, leg.endLocation.latLng.longitude),
            boundsSouthwest = LatLng(route.viewport.low.latitude, route.viewport.low.longitude),
            boundsNortheast = LatLng(route.viewport.high.latitude, route.viewport.high.longitude)
        )
    }

    /**
     * Computes detour/route distance and duration after adding Rider pickup location.
     */
    suspend fun computeDetour(ride: Ride, pickupLat: Double, pickupLng: Double): DetourResult? {
        val oLat = ride.originLat ?: return null
        val oLng = ride.originLng ?: return null

        val dLat = ride.destinationLat ?: return null
        val dLng = ride.destinationLng ?: return null

        val original = getRoute(LatLng(oLat, oLng), LatLng(dLat, dLng)).getOrNull() ?: return null
        val detour = getRouteWithStop(LatLng(oLat, oLng), LatLng(pickupLat, pickupLng), LatLng(dLat, dLng)).getOrNull() ?: return null

        return DetourResult(
            addedKm = (detour.distanceMeters - original.distanceMeters) / 1000.0,
            addedMinutes = (detour.durationSeconds - original.durationSeconds) / 60L
        )
    }

    /**
     * Computes/gets route after adding Rider pickup location.
     */
    suspend fun getRouteWithStop(origin: LatLng, stop: LatLng, destination: LatLng): Result<RouteResult> = runCatching {
        val request = RoutesRequest(
            origin = Waypoint(WaypointLocation(LatLngLiteral(origin.latitude, origin.longitude))),
            destination = Waypoint(WaypointLocation(LatLngLiteral(destination.latitude, destination.longitude))),
            intermediates = listOf(Waypoint(WaypointLocation(LatLngLiteral(stop.latitude, stop.longitude))))
        )

        val response = apiService.computeRoutes(apiKey = apiKey, request = request)
        check(!response.routes.isNullOrEmpty()) { "Routes API returned no routes." }

        val route = response.routes.first()
        val leg = route.legs.first()

        RouteResult(
            polylinePoints = route.polyline.encodedPolyline,
            distanceText = leg.localizedValues.distance.text,
            durationText = leg.localizedValues.duration.text,
            distanceMeters = route.distanceMeters,
            durationSeconds = route.duration.trimEnd('s').toIntOrNull() ?: 0,
            startLocation = LatLng(leg.startLocation.latLng.latitude, leg.startLocation.latLng.longitude),
            endLocation = LatLng(leg.endLocation.latLng.latitude, leg.endLocation.latLng.longitude),
            boundsSouthwest = LatLng(route.viewport.low.latitude, route.viewport.low.longitude),
            boundsNortheast = LatLng(route.viewport.high.latitude, route.viewport.high.longitude)
        )
    }

    data class DetourResult(val addedKm: Double, val addedMinutes: Long)

    data class RideWithDetour(val ride: Ride, val addedKm: Double, val addedMinutes: Long)
}
