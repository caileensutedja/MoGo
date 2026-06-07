package com.fit3161.fit3162.mogo.UIScreen.ActiveRide

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.fit3161.fit3162.mogo.utils.readContactFromUri
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.*

/**
 * ActiveRide Screen layout.
 *
 * Always shows the Google Map:
 * - When no active ride: displays a "No active rides" card over the map.
 * - When a ride is in progress: shows live tracking with driver/rider markers,
 *   route polyline, ride details, Share Trip, and SOS buttons.
 */
@Composable
fun ActiveRideScreen(
    viewModel: ActiveRideViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Location permission handling
    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        locationPermissionGranted =
            perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
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

    var showSosDialog by remember { mutableStateOf(false) }

    // Contact picker for Share Trip functionality
    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let {
            val (_, phone) = readContactFromUri(context.contentResolver, it)
            if (phone != null) {
                val ride = state.ride
                val msg = "Hi! ${state.riderName} has shared their trip. " +
                        "Heading to ${ride?.destination ?: "destination"}. " +
                        "Driver: ${ride?.users?.userName ?: "Unknown"}, " +
                        "Vehicle: ${ride?.vehicleType ?: ""} (${ride?.plateNumber ?: ""})."
                sendSms(context, phone, msg)
            }
        }
    }
    val contactPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) contactPickerLauncher.launch(null)
    }

    // SOS confirmation dialog
    if (showSosDialog) {
        AlertDialog(
            onDismissRequest = { showSosDialog = false },
            title = {
                Text("Send SOS Alert?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will send an urgent safety alert to all your " +
                            "emergency contacts with your current ride details."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSosDialog = false
                        val ride = state.ride
                        val msg = "URGENT: ${state.riderName} feels unsafe. " +
                                "Ride heading to ${ride?.destination ?: "unknown"}. " +
                                "Driver: ${ride?.users?.userName ?: "Unknown"}, " +
                                "Vehicle: ${ride?.vehicleType ?: ""} plate " +
                                "${ride?.plateNumber ?: ""}. " +
                                "Please check on them immediately."
                        state.emergencyContacts.forEach {
                            sendSms(context, it.contactPhone, msg)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Send SOS", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSosDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Ride completed dialog
    if (state.rideEnded) {
        AlertDialog(
            onDismissRequest = { onBack() },
            title = { Text("Ride Completed") },
            text = { Text("The ride has ended. We hope you had a safe trip!") },
            confirmButton = {
                Button(onClick = { onBack() }) { Text("Done") }
            }
        )
    }

    // Default map position (Melbourne CBD) shown briefly during init
    val defaultPosition = LatLng(-37.8136, 144.9631)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 14f)
    }

    val ride = state.ride
    val hasActiveRide = ride != null

    // Camera animations only when there's an active ride
    if (hasActiveRide) {
        // Fit camera to show all markers (driver, rider, destination)
        LaunchedEffect(state.driverLocation, state.riderLocation) {
            val points = listOfNotNull(state.driverLocation, state.riderLocation)
            val allPoints = if (ride?.destinationLat != null && ride.destinationLng != null) {
                points + LatLng(ride.destinationLat, ride.destinationLng)
            } else {
                points
            }
            if (allPoints.size >= 2) {
                val bounds = LatLngBounds.Builder().apply {
                    allPoints.forEach { include(it) }
                }.build()
                try {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(bounds, 120),
                        800
                    )
                } catch (_: Exception) { }
            } else if (allPoints.isNotEmpty()) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(allPoints.first(), 15f),
                    800
                )
            }
        }

        // Fit camera to route bounds when route loads
        LaunchedEffect(state.routeState) {
            if (state.routeState is ActiveRouteState.Success) {
                val route = (state.routeState as ActiveRouteState.Success).route
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(
                        LatLngBounds(route.boundsSouthwest, route.boundsNortheast),
                        100
                    ),
                    1000
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Map section (always visible)
        Box(modifier = Modifier.weight(1f)) {
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
                // Only draw markers and polyline when there's an active ride
                if (hasActiveRide) {
                    // Route polyline
                    if (state.routeState is ActiveRouteState.Success) {
                        val routePoints = PolyUtil.decode(
                            (state.routeState as ActiveRouteState.Success)
                                .route.polylinePoints
                        )
                        Polyline(
                            points = routePoints,
                            color = Color.Blue,
                            width = 8f
                        )
                    }

                    // Driver marker (blue)
                    state.driverLocation?.let { loc ->
                        Marker(
                            state = rememberUpdatedMarkerState(position = loc),
                            title = ride?.users?.userName ?: "Driver",
                            snippet = "Driver",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_AZURE
                            )
                        )
                    }

                    // Rider marker (pink)
                    state.riderLocation?.let { loc ->
                        Marker(
                            state = rememberUpdatedMarkerState(position = loc),
                            title = "Rider",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_ROSE
                            )
                        )
                    }

                    // Destination marker (default red)
                    if (ride?.destinationLat != null && ride.destinationLng != null) {
                        Marker(
                            state = rememberUpdatedMarkerState(
                                position = LatLng(
                                    ride.destinationLat,
                                    ride.destinationLng
                                )
                            ),
                            title = ride.destination
                        )
                    }
                }
            }

            // Top card: status banner or "no active rides" message
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                        .copy(alpha = 0.95f)
                )
            ) {
                if (!hasActiveRide && !state.isLoading) {
                    // No active ride: show idle message over the map
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.DirectionsCar, null,
                            Modifier.size(32.dp),
                            tint = Color.Gray
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No active rides currently",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.Gray
                        )
                        Text(
                            "When a driver starts a ride you've booked, " +
                                    "it will appear here with live tracking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                } else {
                    // Active ride: show status banner
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when {
                                    state.isLoading -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Loading...",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                    ride?.rideStatus == "in_progress" &&
                                            state.driverLocation != null -> {
                                        Icon(
                                            Icons.Filled.DirectionsCar, null,
                                            Modifier.size(18.dp),
                                            tint = Color(0xFF2196F3)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "${ride.users?.userName ?: "Driver"} " +
                                                    "is on the way",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                    ride?.rideStatus == "in_progress" -> {
                                        Icon(
                                            Icons.Filled.HourglassBottom, null,
                                            Modifier.size(18.dp),
                                            tint = Color(0xFFFF9800)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Waiting for driver location...",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            Icons.Filled.Info, null,
                                            Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Ride: ${ride?.rideStatus ?: "unknown"}",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                }
                            }
                            Text(
                                "To: ${ride?.destination ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = onBack) { Text("Back") }
                    }
                }
            }

            // Loading spinner
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            // Route distance/duration info card (bottom of map)
            if (state.routeState is ActiveRouteState.Success) {
                val route = (state.routeState as ActiveRouteState.Success).route
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Distance",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                route.distanceText,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Duration",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                route.durationText,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }

        // Bottom section: ride details + safety buttons (only when active)
        if (hasActiveRide) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                // Ride details card
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3E8FF), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    RideDetailRow(
                        "Driver",
                        "${ride?.users?.userName ?: "Unknown"} · ${ride?.plateNumber ?: ""}"
                    )
                    RideDetailRow("Vehicle", ride?.vehicleType ?: "")
                    RideDetailRow("From", ride?.origin ?: "")
                    RideDetailRow("To", ride?.destination ?: "")
                }

                Spacer(Modifier.height(10.dp))

                // Safety buttons (rider only)
                if (!state.isDriver) {
                    // Share Trip button
                    Button(
                        onClick = {
                            contactPermLauncher.launch(
                                Manifest.permission.READ_CONTACTS
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDCCBFF)
                        ),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Icon(
                            Icons.Filled.Share, null,
                            Modifier.size(18.dp),
                            tint = Color.Black
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Share Trip", fontSize = 16.sp, color = Color.Black)
                    }

                    Spacer(Modifier.height(8.dp))

                    // SOS button
                    Button(
                        onClick = { showSosDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        ),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Icon(
                            Icons.Filled.Warning, null,
                            Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "SOS - I Feel Unsafe",
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }

                    // Warning if no emergency contacts are configured
                    if (state.emergencyContacts.isEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "No safety contacts set up. Add them in Settings.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

// Displays a label-value row in the ride details card.
@Composable
fun RideDetailRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

// Sends an SMS message to a phone number.
// SMS permission on device must be enabled for this to work.
fun sendSms(context: android.content.Context, phone: String, message: String) {
    try {
        SmsManager.getDefault().sendTextMessage(phone, null, message, null, null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
