package com.fit3161.fit3162.mogo.data.repo

import android.annotation.SuppressLint
import android.util.Log // ADDED: for debug logging
import com.fit3161.fit3162.mogo.data.model.RouteResult
import com.fit3161.fit3162.mogo.data.remote.RoutesApiService
import com.fit3161.fit3162.mogo.data.remote.dto.LatLngLiteral
import com.fit3161.fit3162.mogo.data.remote.dto.RoutesRequest
import com.fit3161.fit3162.mogo.data.remote.dto.Waypoint
import com.fit3161.fit3162.mogo.data.remote.dto.WaypointLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority // ADDED: for getCurrentLocation()
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource // ADDED: for getCurrentLocation()
import kotlinx.coroutines.tasks.await // ADDED: requires kotlinx-coroutines-play-services dependency

class MapsRepository(
    private val apiService: RoutesApiService,
    private val apiKey: String,
    private val fusedLocationProviderClient: FusedLocationProviderClient
) {

    // ADDED: log tag constant
    companion object {
        private const val TAG = "MapsRepository"
    }

    // ADDED: entire function — was previously in MapsViewModel using callback-based lastLocation
    // Now uses getCurrentLocation() with coroutine await() for reliability
    // (lastLocation can return null if no app has recently requested location)
    @SuppressLint("MissingPermission")
    suspend fun getDeviceLocation(): Result<LatLng> = runCatching {
        // CHANGED: explicit type annotation to fix "Cannot infer type for T" compiler error
        val location: android.location.Location? = fusedLocationProviderClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).await()

        // CHANGED: was location.let { ... } ?: throw ...
        // Changed to if/else to fix "Cannot infer type parameter" compiler error
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

        // ADDED: debug logging to trace API calls
        Log.d(TAG, "Fetching route: (${origin.latitude}, ${origin.longitude}) → (${destination.latitude}, ${destination.longitude})")

        // CHANGED: extracted request into a variable for readability
        val request = RoutesRequest(
            origin = Waypoint(
                WaypointLocation(
                    LatLngLiteral(origin.latitude, origin.longitude)
                )
            ),
            destination = Waypoint(
                WaypointLocation(
                    LatLngLiteral(destination.latitude, destination.longitude)
                )
            )
        )

        val response = apiService.computeRoutes(
            apiKey = apiKey,
            request = request
        )
        // REMOVED: commented-out fieldMask, packageName, sha1Fingerprint parameters
        // (these are now handled by the OkHttp interceptor in MogoApplication)

        check(response.routes.isNotEmpty()) {
            "Routes API returned no routes. Check coordinates and API key."
        }

        val route = response.routes.first()
        val leg = route.legs.first()

        // ADDED: log successful route response
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
}

//package com.fit3161.fit3162.mogo.data.repo
//
//import android.annotation.SuppressLint
//import com.fit3161.fit3162.mogo.data.model.RouteResult
//import com.fit3161.fit3162.mogo.data.remote.RoutesApiService
//import com.fit3161.fit3162.mogo.data.remote.dto.LatLngLiteral
//import com.fit3161.fit3162.mogo.data.remote.dto.RoutesRequest
//import com.fit3161.fit3162.mogo.data.remote.dto.Waypoint
//import com.fit3161.fit3162.mogo.data.remote.dto.WaypointLocation
//import com.google.android.gms.location.FusedLocationProviderClient
//import com.google.android.gms.location.Priority
//import com.google.android.gms.maps.model.LatLng
//import com.google.android.gms.tasks.CancellationTokenSource
//import kotlinx.coroutines.tasks.await
//
//
///**
// * Repository for all routing/maps data.
// *
// * Responsibilities:
// * - Makes Routes API calls via RoutesApiService (Retrofit)
// * - Maps raw RoutesResponse DTO → RouteResult domain model
// * - Returns Result<RouteResult> so ViewModel can use .fold() without
// *   handling exceptions directly
// *
// * Follows the same pattern as AuthRepository, BookRepository, OfferRepository.
// */
//class MapsRepository(
//    private val apiService: RoutesApiService,
//    private val apiKey: String,
//    private val fusedLocationProviderClient: FusedLocationProviderClient
//) {
//
//    @SuppressLint("MissingPermission")
//    suspend fun getDeviceLocation(): Result<LatLng> = runCatching {
//        val location = fusedLocationProviderClient.getCurrentLocation(
//            Priority.PRIORITY_HIGH_ACCURACY,
//            CancellationTokenSource().token
//        ).await()
//
//        location.let {
//            LatLng(it.latitude, it.longitude)
//        } ?: throw Exception("Unable to get device location. Ensure location services are enabled.")
//
//    }
//
//    /**
//     * Fetches a driving route between two coordinates.
//     *
//     * @param origin        Starting LatLng.
//     * @param destination   Ending LatLng.
//     * @return              Result.success(RouteResult) or Result.failure(Exception).
//     *                      runCatching wraps ALL exceptions — network failures,
//     *                      empty responses, JSON errors — as Result.failure.
//     */
//    suspend fun getRoute(
//        origin: LatLng,
//        destination: LatLng
//    ): Result<RouteResult> = runCatching {
//
//        val response = apiService.computeRoutes(
//            apiKey = apiKey,
//            request = RoutesRequest(
//                origin = Waypoint(
//                    WaypointLocation(
//                        LatLngLiteral(origin.latitude, origin.longitude)
//                    )
//                ),
//                destination = Waypoint(
//                    WaypointLocation(
//                        LatLngLiteral(destination.latitude, destination.longitude)
//                    )
//                )
//            ),
////            fieldMask = TODO(),
////            packageName = TODO(),
////            sha1Fingerprint = TODO()
//        )
//
//        check(response.routes.isNotEmpty()) {
//            "Routes API returned no routes. Check coordinates and API key."
//        }
//
//        val route = response.routes.first()
//        val leg   = route.legs.first()
//
//        RouteResult(
//            polylinePoints = route.polyline.encodedPolyline,
//            distanceText = leg.localizedValues.distance.text,
//            durationText = leg.localizedValues.duration.text,
//            distanceMeters = route.distanceMeters,
//            durationSeconds = route.duration.trimEnd('s').toIntOrNull() ?: 0,
//            startLocation = LatLng(
//                leg.startLocation.latLng.latitude,
//                leg.startLocation.latLng.longitude
//            ),
//            endLocation = LatLng(
//                leg.endLocation.latLng.latitude,
//                leg.endLocation.latLng.longitude
//            ),
//            boundsSouthwest = LatLng(
//                route.bounds.low.latitude,
//                route.bounds.low.longitude
//            ),
//            boundsNortheast = LatLng(
//                route.bounds.high.latitude,
//                route.bounds.high.longitude
//            )
//        )
//    }
//}
