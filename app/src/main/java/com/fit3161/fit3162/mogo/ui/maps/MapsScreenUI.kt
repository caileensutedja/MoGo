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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

@Composable
fun MapScreenUI(viewModel: MapsViewModel) {
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

    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            viewModel.loadDeviceLocation()
        }
    }

    // ── Observe state ────────────────────────────────────────────────
    val locationState by viewModel.userLocation.collectAsState()
    val routeState by viewModel.routeState.collectAsState()
    val selectedDestination by viewModel.selectedDestination.collectAsState()
    val nearbyDrivers by viewModel.nearbyDrivers.collectAsState()
    val searchRadius by viewModel.searchRadiusMeters.collectAsState()

    // ── Camera ───────────────────────────────────────────────────────
    val defaultPosition = LatLng(-37.8136, 144.9631)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 14f)
    }

    LaunchedEffect(locationState) {
        if (locationState is LocationState.Located) {
            val userLatLng = (locationState as LocationState.Located).latLng
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(userLatLng, 15f),
                durationMs = 1000
            )
        }
    }

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

    LaunchedEffect(routeState, locationState) {
        if (routeState is RouteState.Idle && locationState is LocationState.Located) {
            val userLatLng = (locationState as LocationState.Located).latLng
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(userLatLng, 15f),
                durationMs = 600
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
            // Search radius circle + driver markers
            // Only shown when user is located AND no route is active
            if (locationState is LocationState.Located && routeState !is RouteState.Success) {
                val userLatLng = (locationState as LocationState.Located).latLng

                Circle(
                    center      = userLatLng,
                    radius      = searchRadius,
                    strokeColor = Color.Blue,
                    strokeWidth = 3f,
                    fillColor   = Color.Blue.copy(alpha = 0.12f)
                )

                nearbyDrivers.forEach { driver ->
                    Marker(
                        state   = rememberUpdatedMarkerState(position = driver.latLng),
                        title   = driver.name,
                        snippet = "Driver ${driver.driverId}",
                        icon    = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_GREEN
                        )
                    )
                }
            }

            // Route polyline + start/end markers
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
                    title = selectedDestination?.name ?: "Destination"
                )
            }
        }

        // ── Destination picker card (top) ───────────────────────────
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedDestination != null)
                            "Route to ${selectedDestination!!.name}"
                        else
                            "Select destination",
                        style = MaterialTheme.typography.titleSmall
                    )

                    if (selectedDestination != null) {
                        IconButton(
                            onClick = { viewModel.clearRoute() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear route",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.presetDestinations.forEach { destination ->
                        val isSelected = selectedDestination == destination

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    viewModel.selectDestination(destination)
                                }
                            },
                            label = {
                                Text(
                                    destination.name,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }
                }

                // Driver count + radius slider (debug/dev controls)
                if (selectedDestination == null) {
                    Text(
                        text = "${nearbyDrivers.size} drivers within ${formatKm(searchRadius)} km",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )

                    Slider(
                        value = searchRadius.toFloat(),
                        onValueChange = { viewModel.setSearchRadius(it.toDouble()) },
                        valueRange = 500f..10000f,
                        steps = 18
                    )
                }
            }
        }

        // ── Loading indicator ───────────────────────────────────────
        if (locationState is LocationState.Loading || routeState is RouteState.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // ── Route info card (bottom) ────────────────────────────────
        if (routeState is RouteState.Success) {
            val route = (routeState as RouteState.Success).route
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
                    selectedDestination?.let { dest ->
                        Text(
                            text = dest.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (dest.description.isNotBlank()) {
                            Text(
                                text = dest.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
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
        }

        // ── Error snackbar ──────────────────────────────────────────
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

private fun formatKm(meters: Double): String = "%.1f".format(meters / 1000.0)
