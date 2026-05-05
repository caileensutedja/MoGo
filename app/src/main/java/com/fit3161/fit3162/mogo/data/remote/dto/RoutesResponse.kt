package com.fit3161.fit3162.mogo.data.remote.dto

/**
 * Raw DTO mirroring the Google Routes API JSON response.
 * Only fields declared in the X-Goog-FieldMask header are populated.
 * This is mapped → RouteResult domain model in MapsRepository.
 */
data class RoutesResponse(
    val routes: List<Route>? = null
) {
    data class Route(
        val distanceMeters: Int,
        val duration: String,           // returned as "1234s"
        val polyline: Polyline,
        val viewport: Bounds,
        val legs: List<Leg>
    )

    data class Polyline(
        val encodedPolyline: String
    )

    data class Bounds(
        val low: LatLngLiteral,         // southwest corner
        val high: LatLngLiteral         // northeast corner
    )

    data class Leg(
        val startLocation: LegLocation,
        val endLocation: LegLocation,
        val localizedValues: LocalizedValues
    )

    data class LegLocation(
        val latLng: LatLngLiteral
    )

    data class LocalizedValues(
        val distance: TextValue,        // e.g. "12.3 km"
        val duration: TextValue         // e.g. "18 mins"
    )

    data class TextValue(
        val text: String
    )
}
