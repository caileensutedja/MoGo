package com.fit3161.fit3162.mogo.data.remote.dto

/**
 * Request body sent to Google Routes API POST endpoint.
 * https://routes.googleapis.com/directions/v2:computeRoutes
 */
data class RoutesRequest(
    val origin: Waypoint,
    val destination: Waypoint,
    val travelMode: String = "DRIVE",
    val routingPreference: String = "TRAFFIC_AWARE",
    val computeAlternativeRoutes: Boolean = false
)

data class Waypoint(
    val location: WaypointLocation
)

data class WaypointLocation(
    val latLng: LatLngLiteral
)

data class LatLngLiteral(
    val latitude: Double,
    val longitude: Double
)
