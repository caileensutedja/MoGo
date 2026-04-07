package com.fit3161.fit3162.mogo.data.repo

import com.fit3161.fit3162.mogo.data.model.RouteResult
import com.fit3161.fit3162.mogo.data.remote.RoutesApiService
import com.fit3161.fit3162.mogo.data.remote.dto.LatLngLiteral
import com.fit3161.fit3162.mogo.data.remote.dto.RoutesRequest
import com.fit3161.fit3162.mogo.data.remote.dto.Waypoint
import com.fit3161.fit3162.mogo.data.remote.dto.WaypointLocation
import com.google.android.gms.maps.model.LatLng


/**
 * Repository for all routing/maps data.
 *
 * Responsibilities:
 * - Makes Routes API calls via RoutesApiService (Retrofit)
 * - Maps raw RoutesResponse DTO → RouteResult domain model
 * - Returns Result<RouteResult> so ViewModel can use .fold() without
 *   handling exceptions directly
 *
 * Follows the same pattern as AuthRepository, BookRepository, OfferRepository.
 */
class MapsRepository(
    private val apiService: RoutesApiService,
    private val apiKey: String
) {

    /**
     * Fetches a driving route between two coordinates.
     *
     * @param origin        Starting LatLng.
     * @param destination   Ending LatLng.
     * @return              Result.success(RouteResult) or Result.failure(Exception).
     *                      runCatching wraps ALL exceptions — network failures,
     *                      empty responses, JSON errors — as Result.failure.
     */
    suspend fun getRoute(
        origin: LatLng,
        destination: LatLng
    ): Result<RouteResult> = runCatching {

        val response = apiService.computeRoutes(
            apiKey  = apiKey,
            request = RoutesRequest(
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
        )

        check(response.routes.isNotEmpty()) {
            "Routes API returned no routes. Check coordinates and API key."
        }

        val route = response.routes.first()
        val leg   = route.legs.first()

        RouteResult(
            polylinePoints  = route.polyline.encodedPolyline,
            distanceText    = leg.localizedValues.distance.text,
            durationText    = leg.localizedValues.duration.text,
            distanceMeters  = route.distanceMeters,
            durationSeconds = route.duration.trimEnd('s').toIntOrNull() ?: 0,
            startLocation   = LatLng(
                leg.startLocation.latLng.latitude,
                leg.startLocation.latLng.longitude
            ),
            endLocation     = LatLng(
                leg.endLocation.latLng.latitude,
                leg.endLocation.latLng.longitude
            ),
            boundsSouthwest = LatLng(
                route.bounds.low.latitude,
                route.bounds.low.longitude
            ),
            boundsNortheast = LatLng(
                route.bounds.high.latitude,
                route.bounds.high.longitude
            )
        )
    }
}
