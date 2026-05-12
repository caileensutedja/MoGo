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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
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

// Helper: determine ride title based on current time and estimated duration
private fun getRideTitle(departureTime: String, durationMinutes: Int?): String {
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val departure = try { OffsetDateTime.parse(departureTime) } catch (e: Exception) { return "Unknown" }
    if (departure.isAfter(now)) return "Upcoming Ride"
    if (durationMinutes != null) {
        val endTime = departure.plusMinutes(durationMinutes.toLong())
        if (now.isBefore(endTime)) return "Ongoing Ride"
    }
    return "Past Ride"
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
        Text(
            text = "Book",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ========== RIDE SECTION (Upcoming / Ongoing) ==========
        val ongoing = state.ongoingRide
        if (ongoing != null) {
            val title = getRideTitle(ongoing.departureTime, ongoing.estimatedDurationMinutes)
            Text(
                text = title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A2C8A)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatDepartureTime(ongoing.departureTime),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray
                            )
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFFFE0B2)
                            ) {
                                Text(
                                    text = "⏳ ${formatTimeLeft(ongoing.departureTime)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "📍 ${ongoing.origin} → ${ongoing.destination}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

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
            Text(
                text = "No upcoming rides",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A2C8A)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No rides scheduled", fontSize = 14.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // ========== BOOKED RIDES SECTION ==========
        Text("Booked Rides", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

        if (state.isLoading) {
            CircularProgressIndicator()
        }

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
                    BookedCard(booking = state.bookings[idx])
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

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
fun BookedCard(booking: Booking) {
    val ride = booking.rides
    val driver = ride?.users

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Driver profile picture (from avatar_url)
            if (driver?.avatarUrl != null) {
                AsyncImage(
                    model = driver.avatarUrl,
                    contentDescription = "Driver profile",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFFDCCBFF), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = driver?.userName?.take(1) ?: "?",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A2C8A)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = driver?.userName ?: "Unknown Driver",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = ride?.vehicleType ?: "Unknown",
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

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { /* TODO: cancel booking */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAD7FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { /* TODO: show details */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB57BFF)),
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