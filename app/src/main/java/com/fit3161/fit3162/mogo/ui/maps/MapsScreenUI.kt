package com.fit3161.fit3162.mogo.ui.maps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.PolyUtil

import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.rememberUpdatedMarkerState

/**
 * Map screen composable.
 * Observes MapsViewModel state and renders:
 * - GoogleMap with route polyline
 * - Origin and destination markers
 * - Route info card (distance + duration)
 * - Loading indicator
 * - Error message
 */
@Composable
fun MapScreenUI(viewModel: MapsViewModel) {

    val routeState by viewModel.routeState.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(-37.8136, 144.9631), // Default: Melbourne
            12f
        )
    }

    // Trigger route fetch on first composition
    // Replace with real origin/destination from your app's state
    LaunchedEffect(Unit) {
        viewModel.fetchRoute(
            origin      = LatLng(-37.8136, 144.9631),
            destination = LatLng(-37.8755, 145.0456)
        )
    }

    // Animate camera to fit route once loaded
    LaunchedEffect(routeState) {
        if (routeState is RouteState.Success) {
            val route = (routeState as RouteState.Success).route
            val bounds = LatLngBounds(route.boundsSouthwest, route.boundsNortheast)
            cameraPositionState.animate(
                update  = CameraUpdateFactory.newLatLngBounds(bounds, 100),
                durationMs = 1000
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = true
            )
        ) {
            if (routeState is RouteState.Success) {
                val route = (routeState as RouteState.Success).route
                val points = PolyUtil.decode(route.polylinePoints)

                // Route polyline
                Polyline(
                    points = points,
                    color  = Color.Blue,
                    width  = 8f
                )

                // Origin marker
                Marker(
                    state = rememberUpdatedMarkerState(position = route.startLocation),
                    title = "Origin"
                )

//                MarkerState(position = route.endLocation)
                // Destination marker
                Marker(
                    state = rememberUpdatedMarkerState(position = route.endLocation),
                    title = "Destination"
                )
            }
        }

        // Loading indicator
        if (routeState is RouteState.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Route info card
        if (routeState is RouteState.Success) {
            val route = (routeState as RouteState.Success).route
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text  = "Distance",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text  = route.distanceText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text  = "Duration",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text  = route.durationText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        // Error message
        if (routeState is RouteState.Error) {
            val message = (routeState as RouteState.Error).message
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(message)
            }
        }
    }
}
