package com.fit3161.fit3162.mogo.UIScreen.BookScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

/**
 * Displays a booked ride as a three-leg route on a map:
 *   - GREEN marker  = driver's starting location
 *   - PINK marker = rider's pickup location
 *   - RED marker    = destination
 *
 * Camera frames all three points when the route loads.
 */
@Composable
fun BookingPreviewScreen(viewModel: BookingPreviewViewModel) {
    val state by viewModel.state.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        // Default Melbourne CBD until route bounds are computed
        position = CameraPosition.fromLatLngZoom(
            LatLng(-37.8136, 144.9631), 12f
        )
    }

    // When route is ready, fit the camera to all three points (with padding)
    LaunchedEffect(state) {
        if (state is PreviewState.Success) {
            val s = state as PreviewState.Success
            val bounds = LatLngBounds.Builder()
                .include(s.driverOrigin)
                .include(s.pickup)
                .include(s.destination)
                .build()
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngBounds(bounds, 150),
                durationMs = 1000
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(zoomControlsEnabled = true)
        ) {
            if (state is PreviewState.Success) {
                val s = state as PreviewState.Success

                // Three-leg route polyline (driver origin -> pickup -> destination)
                Polyline(
                    points = PolyUtil.decode(s.route.polylinePoints),
                    color = Color.Blue,
                    width = 8f
                )

                // Driver origin (green)
                Marker(
                    state = rememberUpdatedMarkerState(position = s.driverOrigin),
                    title = "Driver's start",
                    icon = BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_GREEN
                    )
                )

                // Rider pickup (orange)
                Marker(
                    state = rememberUpdatedMarkerState(position = s.pickup),
                    title = "Your pickup",
                    icon = BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_ROSE
                    )
                )

                // Destination (default red)
                Marker(
                    state = rememberUpdatedMarkerState(position = s.destination),
                    title = "Destination"
                )
            }
        }

        // Loading spinner over the map while route is being computed
        if (state is PreviewState.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Bottom info card with distance + duration
        if (state is PreviewState.Success) {
            val s = state as PreviewState.Success
            val ride = s.booking.rides

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = ride?.destination ?: "Trip preview",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Distance",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                s.route.distanceText,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Duration",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                s.route.durationText,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }

        // Error snack bar
        if (state is PreviewState.Error) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text((state as PreviewState.Error).message)
            }
        }
    }
}