package com.fit3161.fit3162.mogo.data.model

import com.google.android.gms.maps.model.LatLng

/**
 * Domain model representing a successfully calculated route.
 *
 * This is decoupled from the raw Routes API response (RoutesResponse.kt).
 * MapsRepository maps the raw DTO into this model before passing it
 * up to the ViewModel and UI.
 *
 * @param polylinePoints    Encoded polyline string. Decoded via PolyUtil.decode()
 *                          to draw the route line on the map.
 *
 * @param distanceText      Human-readable distance for display in UI. e.g. "12.3 km"
 * @param durationText      Human-readable duration for display in UI. e.g. "18 mins"
 *
 * @param distanceMeters    Raw distance in metres. Use for calculations e.g.
 *                          sorting routes, geofencing, proximity checks.
 * @param durationSeconds   Raw duration in seconds. Use for ETA calculations.
 *
 * @param startLocation     Coordinates of the route's start point. Used to place
 *                          the origin marker on the map.
 * @param endLocation       Coordinates of the route's end point. Used to place
 *                          the destination marker on the map.
 *
 * @param boundsSouthwest   Southwest corner of the bounding box that contains
 * @param boundsNortheast   Northeast corner. Together used to animate the camera
 *                          so the entire route fits on screen.
 *
 * @param startAddress      Human-readable start address. Empty string if not
 * @param endAddress        resolved (Routes API doesn't return addresses —
 *                          use Geocoding API separately if needed).
 */
data class RouteResult(
    val polylinePoints: String,
    val distanceText: String,
    val durationText: String,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val startLocation: LatLng,
    val endLocation: LatLng,
    val boundsSouthwest: LatLng,
    val boundsNortheast: LatLng
)
