package com.fit3161.fit3162.mogo.UIScreen.MyRides

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRidesScreen(
    viewModel: MyRidesViewModel,
    onNavigateToUploadRides: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "My Rides",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "as driver",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = Color.Gray,
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
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No rides posted")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.rides.size) { idx ->
                    MyRideCard(
                        ride = state.rides[idx],
                        onCancelRide = { viewModel.cancelRide(state.rides[idx].id) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
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

@Composable
fun MyRideCard(ride: Ride, onCancelRide: () -> Unit) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    val isScheduled = ride.rideStatus == "scheduled"

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Cancel Ride") },
            text = { Text("Are you sure you want to cancel this ride? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onCancelRide()
                }) {
                    Text("Yes, Cancel", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Go Back")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row: Time + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDepartureTime(ride.departureTime),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A2C8A)
                )
                // Status badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when (ride.rideStatus) {
                        "scheduled" -> Color(0xFFE8F5E9)
                        "completed" -> Color(0xFFE3F2FD)
                        else -> Color(0xFFFFEBEE)
                    }
                ) {
                    Text(
                        text = ride.rideStatus.replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = when (ride.rideStatus) {
                            "scheduled" -> Color(0xFF2E7D32)
                            "completed" -> Color(0xFF1565C0)
                            else -> Color(0xFFC62828)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // Route
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📍", fontSize = 16.sp)
                Text(
                    text = "${ride.origin} → ${ride.destination}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Vehicle & Seats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🚙", fontSize = 14.sp)
                    Text(
                        text = ride.vehicleType.replaceFirstChar { it.uppercase() },
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("💺", fontSize = 14.sp)
                    Text(
                        text = "${ride.availableSeats} seats left",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                }
            }

            // Carbon estimate + Cancel button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Carbon estimate (if available)
                if (ride.carbonEstimate != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🌿", fontSize = 14.sp)
                        Text(
                            text = "%.2f kg CO₂".format(ride.carbonEstimate),
                            fontSize = 13.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Cancel button (only for scheduled rides) or completion mark
                if (isScheduled) {
                    Button(
                        onClick = { showConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFCDD2),
                            contentColor = Color.Red
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Cancel Ride", fontSize = 13.sp)
                    }
                } else {
                    Text("✓ Completed", fontSize = 13.sp, color = Color(0xFF4CAF50))
                }
            }
        }
    }
}