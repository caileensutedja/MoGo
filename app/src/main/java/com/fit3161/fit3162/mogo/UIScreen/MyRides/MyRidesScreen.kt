package com.fit3161.fit3162.mogo.UIScreen.MyRides

import androidx.compose.foundation.background
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
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No rides posted")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.rides.size) { idx ->
                    val ride = state.rides[idx]
                    MyRideCard(
                        ride = ride,
                        onCancelRide = { viewModel.cancelRide(ride.id) }
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
                }) { Text("Yes, Cancel", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Go Back") }
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = formatDepartureTime(ride.departureTime),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A2C8A)
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
                    "🚙 ${it.vehicleMake} ${it.vehicleModel ?: ""} · ${it.plateNumber}",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            }
            ride.carbonEstimate?.let {
                Text("🌿 %.2f kg CO₂".format(it), fontSize = 13.sp, color = Color(0xFF4CAF50))
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = { showConfirmDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCDD2)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel Ride", color = Color.Red)
            }
        }
    }
}