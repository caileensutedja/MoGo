package com.fit3161.fit3162.mogo.ui.maps

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

/**
 * Map screen composable.
 *
 * Flow:
 * 1. Check/request location permission
 * 2. Once granted → ViewModel fetches device location
 * 3. Camera centers on user location
 * 4. When a destination is provided → ViewModel fetches route
 * 5. Route polyline, markers, and info card are displayed
 *
 * @param viewModel     The MapsViewModel instance.
 * @param destination   Optional destination LatLng. When non-null, a route is
 *                      fetched from the user's current location to this point.
 *                      In production, pass this from your navigation args or
 *                      a search/selection screen.
 */
@Composable
fun MapScreenUI(
    viewModel: MapsViewModel,
    destination: LatLng? = null  // Pass from nav args / parent screen
) {
    val context = LocalContext.current

    // ── Permission handling ──────────────────────────────────────────
    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Request permission on first composition if not already granted
    LaunchedEffect(Unit) {
        if (!locationPermissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // ── Once permission is granted, fetch the device location ────────
    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            viewModel.loadDeviceLocation()
        }
    }

    // ── Observe state ────────────────────────────────────────────────
    val locationState by viewModel.userLocation.collectAsState()
    val routeState by viewModel.routeState.collectAsState()

    // ── When we have both location and a destination, fetch route ────
    LaunchedEffect(locationState, destination) {
        if (locationState is LocationState.Located && destination != null) {
            viewModel.fetchRoute(destination = destination)
        }
    }

    // ── Camera ───────────────────────────────────────────────────────
    val defaultPosition = LatLng(-37.8136, 144.9631) // Melbourne fallback
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 14f)
    }

    // Center camera on user location when it becomes available
    LaunchedEffect(locationState) {
        if (locationState is LocationState.Located) {
            val userLatLng = (locationState as LocationState.Located).latLng
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(userLatLng, 15f),
                durationMs = 1000
            )
        }
    }

    // Fit camera to route bounds when route loads
    LaunchedEffect(routeState) {
        if (routeState is RouteState.Success) {
            val route = (routeState as RouteState.Success).route
            val bounds = LatLngBounds(route.boundsSouthwest, route.boundsNortheast)
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngBounds(bounds, 100),
                durationMs = 1000
            )
        }
    }

    // ── UI ───────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = locationPermissionGranted
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = locationPermissionGranted
            )
        ) {
            // Draw route polyline + markers when available
            if (routeState is RouteState.Success) {
                val route = (routeState as RouteState.Success).route
                val points = PolyUtil.decode(route.polylinePoints)

                Polyline(
                    points = points,
                    color  = Color.Blue,
                    width  = 8f
                )

                Marker(
                    state = rememberUpdatedMarkerState(position = route.startLocation),
                    title = "Your Location"
                )

                Marker(
                    state = rememberUpdatedMarkerState(position = route.endLocation),
                    title = "Destination"
                )
            }
        }

        // Loading indicator (for either location or route loading)
        if (locationState is LocationState.Loading || routeState is RouteState.Loading) {
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
                        Text("Distance", style = MaterialTheme.typography.labelSmall)
                        Text(route.distanceText, style = MaterialTheme.typography.bodyLarge)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Duration", style = MaterialTheme.typography.labelSmall)
                        Text(route.durationText, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        // Error messages (location or route)
        val errorMessage = when {
            locationState is LocationState.Error ->
                (locationState as LocationState.Error).message
            routeState is RouteState.Error ->
                (routeState as RouteState.Error).message
            else -> null
        }

        if (errorMessage != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(errorMessage)
            }
        }
    }
}
