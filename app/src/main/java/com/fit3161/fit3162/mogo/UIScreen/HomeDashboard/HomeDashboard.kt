package com.fit3161.fit3162.mogo.UIScreen.HomeDashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import com.fit3161.fit3162.mogo.UIScreen.BookScreen.formatDepartureTime
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications

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

// Helper: determine ride status title based on current time and duration
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
fun HomeScreenUI(
    viewModel: HomeViewModel,
    onProfileClick: () -> Unit = {},
    onBookedClick: () -> Unit = {},
    onMyRidesClick: () -> Unit = {},
    onRoleToggle: (String) -> Unit = {},
    onNotificationClick: () -> Unit = {}, // new callback
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDriver = uiState.profile?.user_role?.lowercase() == "driver"

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (uiState.error != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = uiState.error ?: "Something went wrong", color = Color.Red)
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // Top Row: Back + (fire streak + notification + profile)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Back button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFDCCBFF), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("<")
            }

            // Right: Fire streak, notification icon, profile icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.rideStreak > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🔥", fontSize = 18.sp)
                        Text(
                            text = "${uiState.rideStreak}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                    }
                }

                // Notification icon button
                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color(0xFF4A2C8A)
                    )
                }

                // Profile image / placeholder
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFDCCBFF), RoundedCornerShape(10.dp))
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.profile?.avatar_url != null) {
                        AsyncImage(
                            model = uiState.profile!!.avatar_url,
                            contentDescription = "Profile picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("P", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Greeting
        Text(
            text = "Hello, ${uiState.profile?.user_name ?: ""}",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Role toggle (improved active contrast)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF3E8FF), RoundedCornerShape(50.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        if (!isDriver) Color(0xFFB57BFF) else Color.Transparent,
                        RoundedCornerShape(50.dp)
                    )
                    .clickable { if (isDriver) onRoleToggle("rider") },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Rider",
                    fontWeight = if (!isDriver) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp,
                    color = if (!isDriver) Color.White else Color.Gray
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        if (isDriver) Color(0xFFB57BFF) else Color.Transparent,
                        RoundedCornerShape(50.dp)
                    )
                    .clickable { if (!isDriver) onRoleToggle("driver") },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Driver",
                    fontWeight = if (isDriver) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp,
                    color = if (isDriver) Color.White else Color.Gray
                )
            }
        }

        Text(
            text = "Your Current Role: ${uiState.profile?.user_role ?: ""}",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        // ========== RIDE SECTION (Upcoming / Ongoing) ==========
        val ongoing = uiState.ongoingRide
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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🚗 ${ongoing.driverName ?: "Driver"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF4A2C8A)
                        )
                    }

                    Text(
                        text = "📍 ${ongoing.origin} → ${ongoing.destination}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🕐 Departs: ${formatDepartureTime(ongoing.departureTime)}",
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFFE0B2),
                            modifier = Modifier.clickable(enabled = false) { }
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

        // ========== HISTORY HEADING ==========
        Text("History", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        // ========== HISTORY SECTION (Role‑specific) ==========
        if (isDriver) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Driver History", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.driverHistory.isNotEmpty()) {
                        Text("${uiState.driverHistory.size}", fontWeight = FontWeight.Bold, fontSize = 40.sp, color = Color(0xFF4A2B7A))
                        Text("completed drives", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        Text("No past drives", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Rider History", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.riderHistory.isNotEmpty()) {
                        Text("${uiState.riderHistory.size}", fontWeight = FontWeight.Bold, fontSize = 40.sp, color = Color(0xFF4A2B7A))
                        Text("completed rides", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        Text("No past rides", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ========== BOOKINGS SECTION ==========
        Text("Bookings", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val confirmedBookings = uiState.bookings.filter { it.bookingStatus == "confirmed" }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp))
                    .clickable { onBookedClick() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Rider Bookings", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${confirmedBookings.size}", fontWeight = FontWeight.Bold, fontSize = 40.sp, color = Color(0xFF4A2B7A))
                    Text("booked", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp))
                    .clickable { onMyRidesClick() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Driver Bookings", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${uiState.driverRides.size}", fontWeight = FontWeight.Bold, fontSize = 40.sp, color = Color(0xFF4A2B7A))
                    Text("offered", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // ========== CARBON METRICS ==========
        Text("Carbon Metrics", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%.2f".format(uiState.totalCarbonSaved), fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color(0xFF4CAF50))
                    Text("kg CO₂ saved", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%.1f".format(uiState.treesEquivalent), fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color(0xFF4CAF50))
                    Text("trees equivalent", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%.0f".format(uiState.totalDistanceShared),
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "km shared",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}