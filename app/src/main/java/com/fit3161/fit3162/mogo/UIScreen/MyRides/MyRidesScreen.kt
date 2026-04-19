package com.fit3161.fit3162.mogo.UIScreen.MyRides

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
    viewModel: MyRidesViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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

        // Loading State
        if (state.isLoading) {
            CircularProgressIndicator()
        }

        // Error State
        state.error?.let {
            Text("Error: $it", color = Color.Red)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!state.isLoading && state.rides.isEmpty() && state.error == null) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No booked rides")
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
    }
}



@Composable
fun MyRideCard(ride: Ride, onCancelRide: () -> Unit) {
    val vehicle = ride.vehicles
    var showConfirmDialog by remember { mutableStateOf(false) }

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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder box for car image
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .background(Color(0xFFDCCBFF), RoundedCornerShape(10.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "🕐 ${formatDepartureTime(ride.departureTime)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "🚙 ${ride.vehicleType}",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "📍 ${ride.origin} → ${ride.destination}",
                    fontSize = 14.sp
                )
                Text(
                    text = "💺 ${ride.availableSeats} seats available left",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "Status: ${ride.rideStatus}",
                    fontSize = 14.sp,
                    color = when (ride.rideStatus) {
                        "scheduled" -> Color(0xFF4CAF50)
                        "cancelled" -> Color.Red
                        else -> Color.Gray
                    }
                )
                vehicle?.let {
                    Text(
                        text = "🚙 ${it.vehicleMake} ${it.vehicleModel ?: ""} · ${it.plateNumber}",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                }
                ride.carbonEstimate?.let {
                    Text(
                        text = "🌿 %.2f kg CO₂".format(it),
                        fontSize = 13.sp,
                        color = Color(0xFF4CAF50)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { showConfirmDialog = true  },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFCDD2)  // light red
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel Ride", color = Color.Red)
                    }
                }
            }
        }
    }
}