package com.fit3161.fit3162.mogo.data.repo

import android.annotation.SuppressLint
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
import kotlinx.coroutines.tasks.await

class MapsRepository(
    private val apiService: RoutesApiService,
    private val apiKey: String,
    private val fusedLocationProviderClient: FusedLocationProviderClient
) {

    companion object {
        private const val TAG = "MapsRepository"
    }

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

    suspend fun getRoute(
        origin: LatLng,
        destination: LatLng
    ): Result<RouteResult> = runCatching {
        Log.d(TAG, "Fetching route: (${origin.latitude}, ${origin.longitude}) → (${destination.latitude}, ${destination.longitude})")

        val request = RoutesRequest(
            origin = Waypoint(WaypointLocation(LatLngLiteral(origin.latitude, origin.longitude))),
            destination = Waypoint(WaypointLocation(LatLngLiteral(destination.latitude, destination.longitude)))
        )

        val response = apiService.computeRoutes(apiKey = apiKey, request = request)

        Log.d(TAG, "Raw response: $response")
        Log.d(TAG, "Routes null? ${response.routes == null}")
        Log.d(TAG, "Routes empty? ${response.routes?.isEmpty()}")

        check(!response.routes.isNullOrEmpty()) {
            "Routes API returned no routes. Check coordinates and API key."
        }

        val route = response.routes.first()
        val leg = route.legs.first()

        Log.d(TAG, "Route received: ${leg.localizedValues.distance.text}, ${leg.localizedValues.duration.text}")

        RouteResult(
            polylinePoints = route.polyline.encodedPolyline,
            distanceText = leg.localizedValues.distance.text,
            durationText = leg.localizedValues.duration.text,
            distanceMeters = route.distanceMeters,
            durationSeconds = route.duration.trimEnd('s').toIntOrNull() ?: 0,
            startLocation = LatLng(
                leg.startLocation.latLng.latitude,
                leg.startLocation.latLng.longitude
            ),
            endLocation = LatLng(
                leg.endLocation.latLng.latitude,
                leg.endLocation.latLng.longitude
            ),
            boundsSouthwest = LatLng(
                route.viewport.low.latitude,
                route.viewport.low.longitude
            ),
            boundsNortheast = LatLng(
                route.viewport.high.latitude,
                route.viewport.high.longitude
            )
        )
    }

    suspend fun computeDetour(
        ride: Ride,
        pickupLat: Double,
        pickupLng: Double
    ): DetourResult? {
        val originLat = ride.originLat ?: return null
        val originLng = ride.originLng ?: return null
        val destinationLat = ride.destinationLat ?: return null
        val destinationLng = ride.destinationLng ?: return null

        val originalResult = getRoute(
            origin = LatLng(originLat, originLng),
            destination = LatLng(destinationLat, destinationLng)
        ).getOrNull() ?: return null

        val detourResult = getRouteWithStop(
            origin = LatLng(originLat, originLng),
            stop = LatLng(pickupLat, pickupLng),
            destination = LatLng(destinationLat, destinationLng)
        ).getOrNull() ?: return null

        return DetourResult(
            addedKm = (detourResult.distanceMeters - originalResult.distanceMeters) / 1000.0,
            addedMinutes = (detourResult.durationSeconds - originalResult.durationSeconds) / 60L
        )
    }

    suspend fun getRouteWithStop(
        origin: LatLng,
        stop: LatLng,
        destination: LatLng
    ): Result<RouteResult> = runCatching {
        val request = RoutesRequest(
            origin = Waypoint(WaypointLocation(LatLngLiteral(origin.latitude, origin.longitude))),
            destination = Waypoint(WaypointLocation(LatLngLiteral(destination.latitude, destination.longitude))),
            intermediates = listOf(
                Waypoint(WaypointLocation(LatLngLiteral(stop.latitude, stop.longitude)))
            )
        )
        val response = apiService.computeRoutes(apiKey = apiKey, request = request)

        // Fixed: use !response.routes.isNullOrEmpty() as condition
        check(!response.routes.isNullOrEmpty()) {
            "Routes API returned no routes. Check coordinates and API key."
        }

        val route = response.routes.first()
        val leg = route.legs.first()

        Log.d(TAG, "Route received: ${leg.localizedValues.distance.text}, ${leg.localizedValues.duration.text}")

        RouteResult(
            polylinePoints = route.polyline.encodedPolyline,
            distanceText = leg.localizedValues.distance.text,
            durationText = leg.localizedValues.duration.text,
            distanceMeters = route.distanceMeters,
            durationSeconds = route.duration.trimEnd('s').toIntOrNull() ?: 0,
            startLocation = LatLng(
                leg.startLocation.latLng.latitude,
                leg.startLocation.latLng.longitude
            ),
            endLocation = LatLng(
                leg.endLocation.latLng.latitude,
                leg.endLocation.latLng.longitude
            ),
            boundsSouthwest = LatLng(
                route.viewport.low.latitude,
                route.viewport.low.longitude
            ),
            boundsNortheast = LatLng(
                route.viewport.high.latitude,
                route.viewport.high.longitude
            )
        )
    }

    data class DetourResult(val addedKm: Double, val addedMinutes: Long)
    data class RideWithDetour(val ride: Ride, val addedKm: Double, val addedMinutes: Long)
}