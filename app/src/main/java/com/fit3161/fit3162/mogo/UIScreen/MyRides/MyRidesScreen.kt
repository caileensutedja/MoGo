package com.fit3161.fit3162.mogo.UIScreen.MyRides

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fit3161.fit3162.mogo.UIScreen.BookScreen.formatDepartureTime
import com.fit3161.fit3162.mogo.data.repo.Ride
import com.fit3161.fit3162.mogo.data.repo.RideBookingInfo

// Common cancellation reasons for drivers
private val driverCancellationReasons = listOf(
    "Schedule conflict",
    "Vehicle issue",
    "Personal emergency",
    "No riders booked",
    "Weather conditions",
    "Prefer not to say"
)

@Composable
fun MyRidesScreen(
    viewModel: MyRidesViewModel,
    onNavigateToUploadRides: () -> Unit,
    onNavigateToActiveRide: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Cancel success dialog
    if (state.showCancelSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCancelSuccess() },
            title = { Text("Ride Cancelled", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Your ride has been cancelled successfully. " +
                            "All riders have been notified."
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissCancelSuccess() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB57BFF))
                ) {
                    Text("OK", color = Color.White)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "My Future Rides",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "as driver",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (state.isLoading) {
            CircularProgressIndicator()
        }

        state.error?.let {
            Text("Error: $it", color = Color.Red)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!state.isLoading && state.rides.isEmpty() && state.error == null) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No booked rides")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                // Active rides (in_progress + scheduled) - always visible
                val activeRides = state.activeRides
                items(activeRides.size) { idx ->
                    val ride = activeRides[idx]
                    val bookings = state.rideBookings[ride.id] ?: emptyList()
                    MyRideCard(
                        ride = ride,
                        bookings = bookings,
                        onCancelRide = { reason ->
                            viewModel.cancelRide(ride.id, reason)
                        },
                        onStartRide = { viewModel.startRide(ride.id) },
                        onEndRide = { viewModel.endRide(ride.id) },
                        onViewOnMap = { onNavigateToActiveRide() }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Show/hide completed rides toggle
                val completedRides = state.completedRides
                if (completedRides.isNotEmpty()) {
                    item {
                        TextButton(
                            onClick = { viewModel.toggleShowCompleted() }
                        ) {
                            Text(
                                text = if (state.showCompleted)
                                    "Hide completed rides (${completedRides.size})"
                                else
                                    "Show completed rides (${completedRides.size})",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Completed/cancelled rides (only shown when toggled on)
                    if (state.showCompleted) {
                        items(completedRides.size) { idx ->
                            val ride = completedRides[idx]
                            val bookings = state.rideBookings[ride.id] ?: emptyList()
                            MyRideCard(
                                ride = ride,
                                bookings = bookings,
                                onCancelRide = { },
                                onStartRide = { },
                                onEndRide = { },
                                onViewOnMap = { }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        Button(
            onClick = { onNavigateToUploadRides() },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCEA2FD))
        ) {
            Text("Upload Future Ride", fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRideCard(
    ride: Ride,
    bookings: List<RideBookingInfo> = emptyList(),
    onCancelRide: (String) -> Unit,
    onStartRide: () -> Unit = {},
    onEndRide: () -> Unit = {},
    onViewOnMap: () -> Unit = {}
) {
    val vehicle = ride.vehicles
    var showCancelDialog by remember { mutableStateOf(false) }
    var showStartDialog by remember { mutableStateOf(false) }
    var showEndDialog by remember { mutableStateOf(false) }

    // Cancel dialog with reason dropdown
    if (showCancelDialog) {
        var selectedReason by remember { mutableStateOf("") }
        var expanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Ride", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Are you sure? This will cancel all rider bookings too.",
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Reason for cancellation:", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(4.dp))

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedReason.ifEmpty { "Select a reason" },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            driverCancellationReasons.forEach { reason ->
                                DropdownMenuItem(
                                    text = { Text(reason) },
                                    onClick = {
                                        selectedReason = reason
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onCancelRide(selectedReason.ifEmpty { "Prefer not to say" })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    enabled = selectedReason.isNotEmpty()
                ) {
                    Text("Confirm Cancellation", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Go Back") }
            }
        )
    }

    // Start Ride confirmation dialog
    if (showStartDialog) {
        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = { Text("Start Ride") },
            text = {
                Text(
                    "This will mark the ride as in progress and " +
                            "begin sharing your live location with riders."
                )
            },
            confirmButton = {
                Button(
                    onClick = { showStartDialog = false; onStartRide() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Start Ride", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDialog = false }) { Text("Not Yet") }
            }
        )
    }

    // End Ride confirmation dialog
    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("End Ride") },
            text = {
                Text(
                    "This will mark the ride as completed " +
                            "and stop location sharing."
                )
            },
            confirmButton = {
                Button(
                    onClick = { showEndDialog = false; onEndRide() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A2C8A))
                ) {
                    Text("End Ride", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) { Text("Keep Going") }
            }
        )
    }

    // Card background: green for in_progress, grey for completed/cancelled, purple for scheduled
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when (ride.rideStatus) {
                    "in_progress" -> Color(0xFFE8F5E9)
                    "completed" -> Color(0xFFEEEEEE)
                    "cancelled" -> Color(0xFFEEEEEE)
                    else -> Color(0xFFF3E8FF)
                },
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .background(Color(0xFFDCCBFF), RoundedCornerShape(10.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, null, Modifier.size(16.dp), tint = Color.DarkGray)
                    Spacer(Modifier.width(4.dp))
                    Text(formatDepartureTime(ride.departureTime), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DirectionsCar, null, Modifier.size(14.dp), tint = Color.DarkGray)
                    Spacer(Modifier.width(4.dp))
                    Text(ride.vehicleType, fontSize = 14.sp, color = Color.DarkGray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, Modifier.size(14.dp), tint = Color.DarkGray)
                    Spacer(Modifier.width(4.dp))
                    Text("${ride.origin} → ${ride.destination}", fontSize = 14.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EventSeat, null, Modifier.size(14.dp), tint = Color.DarkGray)
                    Spacer(Modifier.width(4.dp))
                    Text("${ride.availableSeats} seats available", fontSize = 14.sp, color = Color.DarkGray)
                }
                Text(
                    text = "Status: ${ride.rideStatus}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when (ride.rideStatus) {
                        "scheduled" -> Color(0xFF4CAF50)
                        "in_progress" -> Color(0xFF2196F3)
                        "completed" -> Color.Gray
                        "cancelled" -> Color.Red
                        else -> Color.Gray
                    }
                )
                vehicle?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DirectionsCar, null, Modifier.size(14.dp), tint = Color.DarkGray)
                        Spacer(Modifier.width(4.dp))
                        Text("${it.vehicleMake} ${it.vehicleModel ?: ""} · ${it.plateNumber}", fontSize = 14.sp, color = Color.DarkGray)
                    }
                }
                ride.carbonEstimate?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Eco, null, Modifier.size(14.dp), tint = Color(0xFF4CAF50))
                        Spacer(Modifier.width(4.dp))
                        Text("%.2f kg CO₂".format(it), fontSize = 13.sp, color = Color(0xFF4CAF50))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Rider bookings section
                if (bookings.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Group, null, Modifier.size(16.dp), tint = Color(0xFF4A2C8A))
                        Spacer(Modifier.width(4.dp))
                        Text("${bookings.size} rider(s) booked:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4A2C8A))
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    bookings.forEach { booking ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, bottom = 4.dp)
                                .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Person, null, Modifier.size(14.dp), tint = Color.DarkGray)
                                Spacer(Modifier.width(4.dp))
                                Text(booking.users?.userName ?: "Unknown Rider", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, null, Modifier.size(14.dp), tint = Color.DarkGray)
                                Spacer(Modifier.width(4.dp))
                                Text("Pickup: ${booking.pickupLocation.ifBlank { "Not specified" }}", fontSize = 12.sp, color = Color.DarkGray)
                            }
                            booking.users?.userPhone?.let { phone ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Phone, null, Modifier.size(14.dp), tint = Color.DarkGray)
                                    Spacer(Modifier.width(4.dp))
                                    Text(phone, fontSize = 12.sp, color = Color.DarkGray)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.EventSeat, null, Modifier.size(14.dp), tint = Color.DarkGray)
                                Spacer(Modifier.width(4.dp))
                                Text("${booking.seatsBooked} seat(s)", fontSize = 12.sp, color = Color.DarkGray)
                            }
                        }
                    }
                } else if (ride.rideStatus == "scheduled" || ride.rideStatus == "in_progress") {
                    Text("No riders booked yet", fontSize = 13.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons based on ride status
                when (ride.rideStatus) {
                    "scheduled" -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { showCancelDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCDD2)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel", color = Color.Red)
                            }
                            Button(
                                onClick = { showStartDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.PlayArrow, null, Modifier.size(16.dp), tint = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Text("Start Ride", color = Color.White)
                            }
                        }
                    }
                    "in_progress" -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onViewOnMap() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB57BFF)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Map, null, Modifier.size(16.dp), tint = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Text("View Map", color = Color.White)
                            }
                            Button(
                                onClick = { showEndDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A2C8A)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Stop, null, Modifier.size(16.dp), tint = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Text("End Ride", color = Color.White)
                            }
                        }
                    }
                    // completed / cancelled: no action buttons
                }
            }
        }
    }
}
