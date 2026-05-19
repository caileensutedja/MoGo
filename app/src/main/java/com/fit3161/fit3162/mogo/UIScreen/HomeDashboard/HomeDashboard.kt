package com.fit3161.fit3162.mogo.UIScreen.HomeDashboard

import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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

private fun getRideTitle(departureTime: String, durationMinutes: Int?, isInProgress: Boolean): String {
    if (isInProgress) return "Ride in Progress"
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val departure = try { OffsetDateTime.parse(departureTime) } catch (e: Exception) { return "Unknown" }
    if (departure.isAfter(now)) return "Upcoming Ride"
    if (durationMinutes != null) {
        val endTime = departure.plusMinutes(durationMinutes.toLong())
        if (now.isBefore(endTime)) return "Ongoing Ride"
    }
    return "Past Ride"
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreenUI(
    viewModel: HomeViewModel,
    onProfileClick: () -> Unit = {},
    onBookedClick: () -> Unit = {},
    onMyRidesClick: () -> Unit = {},
    onNavigateToActiveRide: () -> Unit = {},
    onRoleToggle: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDriver = uiState.profile?.user_role?.lowercase() == "driver"
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showNotificationDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val refreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            viewModel.loadData()
        }
    )

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && isRefreshing) isRefreshing = false
    }

    if (uiState.isLoading && !isRefreshing) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (uiState.error != null && !isRefreshing) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = uiState.error ?: "Something went wrong", color = Color.Red)
        }
        return
    }

    Box(Modifier.pullRefresh(refreshState)) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFDCCBFF), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("<") }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
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

                    // Notification icon with badge
                    Box {
                        IconButton(onClick = { showNotificationDialog = true }) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color(0xFF4A2C8A)
                            )
                        }
                        if (uiState.hasUnreadNotification) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color.Red, CircleShape)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }

                    // Profile picture
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
            Text(
                text = "Hello, ${uiState.profile?.user_name ?: ""}",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Role toggle
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
                        .background(if (!isDriver) Color(0xFFB57BFF) else Color.Transparent)
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
                        .background(if (isDriver) Color(0xFFB57BFF) else Color.Transparent)
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

            // Ride Section: shows in_progress rides, upcoming rides, or empty state
            val ongoing = uiState.ongoingRide
            if (ongoing != null) {
                val title = getRideTitle(
                    ongoing.departureTime,
                    ongoing.estimatedDurationMinutes,
                    ongoing.isInProgress
                )
                Text(
                    text = title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (ongoing.isInProgress) Color(0xFF2196F3) else Color(0xFF4A2C8A)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Card is tappable when ride is in_progress (navigates to ActiveRide screen)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (ongoing.isInProgress)
                                Modifier.clickable { onNavigateToActiveRide() }
                            else Modifier
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (ongoing.isInProgress) Color(0xFFE3F2FD) else Color(0xFFF3E8FF)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.DirectionsCar, null,
                                    Modifier.size(18.dp),
                                    tint = Color(0xFF4A2C8A)
                                )
                                Text(
                                    text = " ${ongoing.driverName ?: "Driver"}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF4A2C8A)
                                )
                            }
                            IconButton(onClick = {
                                val shareText = "Ride: ${ongoing.origin} → ${ongoing.destination} at ${formatDepartureTime(ongoing.departureTime)}"
                                clipboardManager.setText(AnnotatedString(shareText))
                                Toast.makeText(context, "Ride details copied", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share ride",
                                    tint = Color(0xFF4A2C8A)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.LocationOn, null,
                                Modifier.size(14.dp),
                                tint = Color.DarkGray
                            )
                            Text(
                                " ${ongoing.origin} → ${ongoing.destination}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Schedule, null,
                                    Modifier.size(14.dp),
                                    tint = Color.DarkGray
                                )
                                Text(
                                    " Departs: ${formatDepartureTime(ongoing.departureTime)}",
                                    fontSize = 13.sp,
                                    color = Color.DarkGray
                                )
                            }

                            // Show "In Progress" badge or countdown timer
                            if (ongoing.isInProgress) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFC8E6C9)
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.DirectionsCar, null,
                                            Modifier.size(14.dp),
                                            tint = Color(0xFF2E7D32)
                                        )
                                        Text(
                                            " Live",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFFFE0B2)
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.HourglassBottom, null,
                                            Modifier.size(14.dp),
                                            tint = Color(0xFFE65100)
                                        )
                                        Text(
                                            " ${formatTimeLeft(ongoing.departureTime)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                }
                            }
                        }

                        if (ongoing.estimatedDistanceKm != null || ongoing.estimatedDurationMinutes != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ongoing.estimatedDistanceKm?.let { distance ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Route, null, Modifier.size(14.dp), tint = Color.Gray)
                                        Text(" ${"%.1f".format(distance)} km", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                                ongoing.estimatedDurationMinutes?.let { duration ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Timer, null, Modifier.size(14.dp), tint = Color.Gray)
                                        Text(" $duration min trip", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        // "Tap to track" hint when ride is in progress
                        if (ongoing.isInProgress) {
                            Text(
                                "Tap to view live tracking",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2196F3),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Notifications, null,
                            Modifier.size(48.dp),
                            tint = Color(0xFFCEA2FD)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("No rides scheduled", fontSize = 14.sp, color = Color.Gray)
                        Text("Tap 'Book Future Ride' to get started", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // History section
            Text("History", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
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

            // Bookings section
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

            // Carbon Metrics
            Text("Carbon Metrics", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.weight(1f).height(100.dp).background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("%.2f".format(uiState.totalCarbonSaved), fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color(0xFF4CAF50))
                        Text("kg CO₂ saved", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                Box(
                    modifier = Modifier.weight(1f).height(100.dp).background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("%.1f".format(uiState.treesEquivalent), fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color(0xFF4CAF50))
                        Text("trees equivalent", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                Box(
                    modifier = Modifier.weight(1f).height(100.dp).background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("%.0f".format(uiState.totalDistanceShared), fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color(0xFF4CAF50))
                        Text("km shared", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        PullRefreshIndicator(isRefreshing, refreshState, Modifier.align(Alignment.TopCenter))
    }

    // Notification Dialog
    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            title = { Text("Notifications") },
            text = { Text("No notifications at this time.") },
            confirmButton = { TextButton(onClick = { showNotificationDialog = false }) { Text("OK") } }
        )
    }
}