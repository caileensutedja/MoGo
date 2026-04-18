package com.fit3161.fit3162.mogo.ui.maps

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons              // ADDED
import androidx.compose.material.icons.filled.Close       // ADDED: for clear route button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip               // ADDED: for destination chips
import androidx.compose.material3.Icon                     // ADDED
import androidx.compose.material3.IconButton               // ADDED
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
import com.google.maps.android.compose.MapProperties       // ADDED: for isMyLocationEnabled
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
// REMOVED: unused imports — MarkerState, mutableStateListOf, rememberMarkerState, rememberUpdatedState

@Composable
fun MapScreenUI(viewModel: MapsViewModel) {
    // REMOVED: destination parameter — selection now happens via chips inside this screen
    val context = LocalContext.current

    // ── Permission handling (unchanged) ──────────────────────────────
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

    // ADDED: fetch device location once permission is granted
    // (was not here before — old code used hardcoded origin coordinates)
    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            viewModel.loadDeviceLocation()
        }
    }

    // ── Observe state ────────────────────────────────────────────────
    // ADDED: locationState and selectedDestination observers
    val locationState by viewModel.userLocation.collectAsState()
    val routeState by viewModel.routeState.collectAsState()
    val selectedDestination by viewModel.selectedDestination.collectAsState()

    // REMOVED: old LaunchedEffect(Unit) that called viewModel.fetchRoute() with hardcoded coords
    // Routes are now triggered by chip selection instead

    // ── Camera ───────────────────────────────────────────────────────
    val defaultPosition = LatLng(-37.8136, 144.9631)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 14f)
    }

    // ADDED: center camera on user location when it becomes available
    LaunchedEffect(locationState) {
        if (locationState is LocationState.Located) {
            val userLatLng = (locationState as LocationState.Located).latLng
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(userLatLng, 15f),
                durationMs = 1000
            )
        }
    }

    // Fit camera to route bounds when route loads (unchanged logic)
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

    // ADDED: re-center on user location when route is cleared
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
            // ADDED: MapProperties to enable blue dot for user location
            properties = MapProperties(
                isMyLocationEnabled = locationPermissionGranted
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = locationPermissionGranted
            )
        ) {
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
                    // CHANGED: was "Origin"
                    title = "Your Location"
                )

                Marker(
                    state = rememberUpdatedMarkerState(position = route.endLocation),
                    // CHANGED: was "Destination" — now shows selected preset name
                    title = selectedDestination?.name ?: "Destination"
                )
            }
        }

        // ── ADDED: Destination picker card (top of screen) ──────────
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
                // Header row with title and clear button
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

                    // Clear button — only visible when a route is active
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

                // Destination chips — tapping one triggers route fetch
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
            }
        }
        // ── END: Destination picker card ─────────────────────────────

        // CHANGED: loading indicator now also shows during location loading
        if (locationState is LocationState.Loading || routeState is RouteState.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // CHANGED: route info card now includes destination name + description
        if (routeState is RouteState.Success) {
            val route = (routeState as RouteState.Success).route
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                // CHANGED: was Row only — now Column wrapping destination info + distance/duration
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // ADDED: destination name and description
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

                    // Distance + duration row (unchanged logic, added top padding)
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

        // CHANGED: error handling now covers both location errors and route errors
        // (was only checking routeState before)
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

//package com.fit3161.fit3162.mogo.ui.maps
//
//import android.Manifest
//import android.content.pm.PackageManager
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Close
//import androidx.compose.material3.AssistChip
//import androidx.compose.material3.AssistChipDefaults
//import androidx.compose.material3.Card
//import androidx.compose.material3.CardDefaults
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.FilterChip
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Snackbar
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.core.content.ContextCompat
//import com.google.android.gms.maps.CameraUpdateFactory
//import com.google.android.gms.maps.model.CameraPosition
//import com.google.android.gms.maps.model.LatLng
//import com.google.android.gms.maps.model.LatLngBounds
//import com.google.maps.android.PolyUtil
//import com.google.maps.android.compose.GoogleMap
//import com.google.maps.android.compose.MapProperties
//import com.google.maps.android.compose.MapUiSettings
//import com.google.maps.android.compose.Marker
//import com.google.maps.android.compose.Polyline
//import com.google.maps.android.compose.rememberCameraPositionState
//import com.google.maps.android.compose.rememberUpdatedMarkerState
//
///**
// * Map screen composable.
// *
// * Flow:
// * 1. Check/request location permission
// * 2. Once granted → ViewModel fetches device location
// * 3. Camera centers on user location
// * 4. User selects a preset destination → route is fetched and displayed
// * 5. User can clear the route and pick a different destination
// */
//@Composable
//fun MapScreenUI(viewModel: MapsViewModel) {
//    val context = LocalContext.current
//
//    // ── Permission handling ──────────────────────────────────────────
//    var locationPermissionGranted by remember {
//        mutableStateOf(
//            ContextCompat.checkSelfPermission(
//                context, Manifest.permission.ACCESS_FINE_LOCATION
//            ) == PackageManager.PERMISSION_GRANTED
//        )
//    }
//
//    val permissionLauncher = rememberLauncherForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { permissions ->
//        locationPermissionGranted =
//            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
//                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
//    }
//
//    LaunchedEffect(Unit) {
//        if (!locationPermissionGranted) {
//            permissionLauncher.launch(
//                arrayOf(
//                    Manifest.permission.ACCESS_FINE_LOCATION,
//                    Manifest.permission.ACCESS_COARSE_LOCATION
//                )
//            )
//        }
//    }
//
//    // ── Fetch device location once permission is granted ─────────────
//    LaunchedEffect(locationPermissionGranted) {
//        if (locationPermissionGranted) {
//            viewModel.loadDeviceLocation()
//        }
//    }
//
//    // ── Observe state ────────────────────────────────────────────────
//    val locationState by viewModel.userLocation.collectAsState()
//    val routeState by viewModel.routeState.collectAsState()
//    val selectedDestination by viewModel.selectedDestination.collectAsState()
//
//    // ── Camera ───────────────────────────────────────────────────────
//    val defaultPosition = LatLng(-37.8136, 144.9631) // Melbourne fallback
//    val cameraPositionState = rememberCameraPositionState {
//        position = CameraPosition.fromLatLngZoom(defaultPosition, 14f)
//    }
//
//    // Center camera on user location when available
//    LaunchedEffect(locationState) {
//        if (locationState is LocationState.Located) {
//            val userLatLng = (locationState as LocationState.Located).latLng
//            cameraPositionState.animate(
//                update = CameraUpdateFactory.newLatLngZoom(userLatLng, 15f),
//                durationMs = 1000
//            )
//        }
//    }
//
//    // Fit camera to route bounds when route loads
//    LaunchedEffect(routeState) {
//        if (routeState is RouteState.Success) {
//            val route = (routeState as RouteState.Success).route
//            val bounds = LatLngBounds(route.boundsSouthwest, route.boundsNortheast)
//            cameraPositionState.animate(
//                update = CameraUpdateFactory.newLatLngBounds(bounds, 100),
//                durationMs = 1000
//            )
//        }
//    }
//
//    // Re-center on user location when route is cleared
//    LaunchedEffect(routeState, locationState) {
//        if (routeState is RouteState.Idle && locationState is LocationState.Located) {
//            val userLatLng = (locationState as LocationState.Located).latLng
//            cameraPositionState.animate(
//                update = CameraUpdateFactory.newLatLngZoom(userLatLng, 15f),
//                durationMs = 600
//            )
//        }
//    }
//
//    // ── UI ───────────────────────────────────────────────────────────
//    Box(modifier = Modifier.fillMaxSize()) {
//
//        // Map
//        GoogleMap(
//            modifier = Modifier.fillMaxSize(),
//            cameraPositionState = cameraPositionState,
//            properties = MapProperties(
//                isMyLocationEnabled = locationPermissionGranted
//            ),
//            uiSettings = MapUiSettings(
//                zoomControlsEnabled = true,
//                myLocationButtonEnabled = locationPermissionGranted
//            )
//        ) {
//            // Draw route polyline + markers when a route is loaded
//            if (routeState is RouteState.Success) {
//                val route = (routeState as RouteState.Success).route
//                val points = PolyUtil.decode(route.polylinePoints)
//
//                Polyline(
//                    points = points,
//                    color  = Color.Blue,
//                    width  = 8f
//                )
//
//                Marker(
//                    state = rememberUpdatedMarkerState(position = route.startLocation),
//                    title = "Your Location"
//                )
//
//                Marker(
//                    state = rememberUpdatedMarkerState(position = route.endLocation),
//                    title = selectedDestination?.name ?: "Destination"
//                )
//            }
//        }
//
//        // ── Destination picker (top of screen) ──────────────────────
//        Card(
//            modifier = Modifier
//                .align(Alignment.TopCenter)
//                .fillMaxWidth()
//                .padding(12.dp),
//            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
//            colors = CardDefaults.cardColors(
//                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
//            )
//        ) {
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(12.dp)
//            ) {
//                // Header row with title and optional clear button
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        text = if (selectedDestination != null)
//                            "Route to ${selectedDestination!!.name}"
//                        else
//                            "Select destination",
//                        style = MaterialTheme.typography.titleSmall
//                    )
//
//                    // Show clear button only when a route is active
//                    if (selectedDestination != null) {
//                        IconButton(
//                            onClick = { viewModel.clearRoute() },
//                            modifier = Modifier.size(32.dp)
//                        ) {
//                            Icon(
//                                imageVector = Icons.Default.Close,
//                                contentDescription = "Clear route",
//                                tint = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                        }
//                    }
//                }
//
//                // Destination chips
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(top = 8.dp),
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    viewModel.presetDestinations.forEach { destination ->
//                        val isSelected = selectedDestination == destination
//
//                        FilterChip(
//                            selected = isSelected,
//                            onClick = {
//                                if (!isSelected) {
//                                    viewModel.selectDestination(destination)
//                                }
//                            },
//                            label = { Text(destination.name, style = MaterialTheme.typography.labelMedium) }
//                        )
//                    }
//                }
//            }
//        }
//
//        // ── Loading indicator ────────────────────────────────────────
//        if (locationState is LocationState.Loading || routeState is RouteState.Loading) {
//            CircularProgressIndicator(
//                modifier = Modifier.align(Alignment.Center)
//            )
//        }
//
//        // ── Route info card (bottom of screen) ──────────────────────
//        if (routeState is RouteState.Success) {
//            val route = (routeState as RouteState.Success).route
//            Card(
//                modifier = Modifier
//                    .align(Alignment.BottomCenter)
//                    .fillMaxWidth()
//                    .padding(16.dp),
//                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
//            ) {
//                Column(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(16.dp)
//                ) {
//                    // Destination name + description
//                    selectedDestination?.let { dest ->
//                        Text(
//                            text = dest.name,
//                            style = MaterialTheme.typography.titleMedium
//                        )
//                        if (dest.description.isNotBlank()) {
//                            Text(
//                                text = dest.description,
//                                style = MaterialTheme.typography.bodySmall,
//                                color = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                        }
//                    }
//
//                    // Distance + duration row
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(top = 12.dp),
//                        horizontalArrangement = Arrangement.SpaceEvenly
//                    ) {
//                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                            Text("Distance", style = MaterialTheme.typography.labelSmall)
//                            Text(route.distanceText, style = MaterialTheme.typography.bodyLarge)
//                        }
//                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                            Text("Duration", style = MaterialTheme.typography.labelSmall)
//                            Text(route.durationText, style = MaterialTheme.typography.bodyLarge)
//                        }
//                    }
//                }
//            }
//        }
//
//        // ── Error message ────────────────────────────────────────────
//        val errorMessage = when {
//            locationState is LocationState.Error ->
//                (locationState as LocationState.Error).message
//            routeState is RouteState.Error ->
//                (routeState as RouteState.Error).message
//            else -> null
//        }
//
//        if (errorMessage != null) {
//            Snackbar(
//                modifier = Modifier
//                    .align(Alignment.BottomCenter)
//                    .padding(16.dp)
//            ) {
//                Text(errorMessage)
//            }
//        }
//    }
//}
