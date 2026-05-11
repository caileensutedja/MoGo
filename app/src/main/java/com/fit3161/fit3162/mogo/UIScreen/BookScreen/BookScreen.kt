package com.fit3161.fit3162.mogo.UIScreen.BookScreen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fit3161.fit3162.mogo.data.repo.Booking
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

// Helper: format time left until departure
private fun formatTimeLeft(departureTime: String): String {
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val departure = try { OffsetDateTime.parse(departureTime) } catch (e: Exception) { return "??" }
    if (departure.isBefore(now)) return "Departed"
    val minutesLeft = Duration.between(now, departure).toMinutes()
    return when {
        minutesLeft < 60 -> "${minutesLeft} min"
        minutesLeft < 1440 -> "${minutesLeft / 60}h ${minutesLeft % 60}m"
        else -> "${minutesLeft / 1440}d"
    }
}

@Composable
fun BookScreenUI(
    viewModel: BookViewModel,
    modifier: Modifier = Modifier,
    onNavigateToFutureBookRides: () -> Unit
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Title
        Text(
            text = "Book",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ========== ONGOING RIDE (Improved) ==========
        Text("Ongoing Ride", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        val ongoing = state.ongoingRide
        if (ongoing != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Top row: Driver name (left) + Departure time (right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "🚗 ${ongoing.driverName ?: "Driver"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF4A2C8A)
                        )
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = formatDepartureTime(ongoing.departureTime),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray
                            )
                            Text(
                                text = "⏳ ${formatTimeLeft(ongoing.departureTime)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFE65100)
                            )
                        }
                    }

                    // Route
                    Text(
                        text = "📍 ${ongoing.origin} → ${ongoing.destination}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Distance & duration (if available)
                    if (ongoing.estimatedDistanceKm != null || ongoing.estimatedDurationMinutes != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ongoing.estimatedDistanceKm?.let { distance ->
                                Text("📏 ${"%.1f".format(distance)} km", fontSize = 12.sp, color = Color.Gray)
                            }
                            ongoing.estimatedDurationMinutes?.let { duration ->
                                Text("⏱️ $duration min trip", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No ongoing ride", fontSize = 14.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Booked Rides Section
        Text("Booked Rides", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

        // Loading State
        if (state.isLoading) {
            CircularProgressIndicator()
        }

        // Error State
        state.error?.let {
            Text("Error: $it", color = Color.Red)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!state.isLoading && state.bookings.isEmpty() && state.error == null) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No booked rides")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.bookings.size) { idx ->
                    BookedCardSkeleton(booking = state.bookings[idx])
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Future Ride Button
        Button(
            onClick = { onNavigateToFutureBookRides() },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCEA2FD))
        ) {
            Text("Book Future Ride", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun BookedCardSkeleton(booking: Booking) {
    val ride = booking.rides
    val driver = ride?.users
    val vehicle = ride?.vehicles

    Log.d("BOOKING_DEBUG", "booking: $booking")
    Log.d("BOOKING_DEBUG", "ride: $ride")
    Log.d("BOOKING_DEBUG", "driver: $driver")
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
                    text = driver?.userName ?: "Unknown Driver",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${ride?.vehicleType}",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "📍 ${ride?.destination ?: booking.dropoffLocation}",
                    fontSize = 14.sp
                )
                Text(
                    text = "🕐 ${formatDepartureTime(ride?.departureTime)}",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                ride?.carbonEstimate?.let {
                    Text(
                        text = "🌿 %.2f kg CO₂".format(it),
                        fontSize = 13.sp,
                        color = Color(0xFF4CAF50)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { /* TODO: cancel booking */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEAD7FF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { /* TODO: show details */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB57BFF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Details")
                    }
                }
            }
        }
    }
}

fun formatDepartureTime(timestamp: String?): String {
    if (timestamp == null) return "TBA"
    return try {
        val dt = java.time.OffsetDateTime.parse(timestamp)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
        dt.format(formatter)
    } catch (e: Exception) {
        timestamp.take(16)
    }
}