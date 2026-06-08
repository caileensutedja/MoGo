package com.fit3161.fit3162.mogo.UIScreen.BookScreen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.fit3161.fit3162.mogo.data.repo.Ride
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset


// Common cancellation reasons for riders
private val riderCancellationReasons = listOf(
    "Found another ride",
    "Schedule changed",
    "Personal emergency",
    "Wrong pickup/destination",
    "No longer needed",
    "Prefer not to say"
)

// Carbon savings: compares shared ride vs two separate rides when not doing carpooling.
// See BookScreen carbon comments in previous versions for full formula.
private fun computeCarbonSavedKg(booking: Booking, ride: Ride?): Double? {
    val riderDistanceMeters = booking.estimatedDistanceMeters ?: return null
    val rideCarbonEstimate = ride?.carbonEstimate ?: return null

    if (rideCarbonEstimate <= 0) return null

    val riderSoloKm = riderDistanceMeters / 1000.0
    val sharedVehicleFactor = when (ride.vehicleType.lowercase()) {
        "electric", "ev" -> 0.01; "hybrid" -> 0.12; else -> 0.21
    }

    val driverSoloKm = rideCarbonEstimate / sharedVehicleFactor
    val separateEmissions = (driverSoloKm + riderSoloKm) * 0.21
    val sharedEmissions = driverSoloKm * sharedVehicleFactor
    val saved = separateEmissions - sharedEmissions

    return if (saved > 0) saved else null
}

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

/**
 * BookScreenUI layout.
 */
@Composable
fun BookScreenUI(
    viewModel: BookViewModel,
    modifier: Modifier = Modifier,
    onNavigateToFutureBookRides: () -> Unit,
    onNavigateToBookingPreview: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Cancel success dialog
    if (state.showCancelSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCancelSuccess() },
            title = { Text("Booking Cancelled", fontWeight = FontWeight.Bold) },
            text = { Text("Your booking has been cancelled successfully.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissCancelSuccess() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB57BFF))
                ) { Text("OK", color = Color.White) }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Book", fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(20.dp))

        // Ongoing/upcoming ride section
        val ongoing = state.ongoingRide
        if (ongoing != null) {
            val title = getRideTitle(ongoing.departureTime, ongoing.estimatedDurationMinutes)
            Text(text = title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A2C8A))
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DirectionsCar, null, Modifier.size(18.dp), tint = Color(0xFF4A2C8A))
                            Spacer(Modifier.width(4.dp))
                            Text(ongoing.driverName ?: "Driver", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF4A2C8A))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatDepartureTime(ongoing.departureTime), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFFFE0B2)) {
                                Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.HourglassBottom, null, Modifier.size(14.dp), tint = Color(0xFFE65100))
                                    Spacer(Modifier.width(4.dp))
                                    Text(formatTimeLeft(ongoing.departureTime), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE65100))
                                }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null, Modifier.size(16.dp), tint = Color.DarkGray)
                        Spacer(Modifier.width(4.dp))
                        Text("${ongoing.origin} → ${ongoing.destination}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    if (ongoing.estimatedDistanceKm != null || ongoing.estimatedDurationMinutes != null) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            ongoing.estimatedDistanceKm?.let {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Route, null, Modifier.size(14.dp), tint = Color.Gray)
                                    Spacer(Modifier.width(4.dp))
                                    Text("${"%.1f".format(it)} km", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                            ongoing.estimatedDurationMinutes?.let {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Timer, null, Modifier.size(14.dp), tint = Color.Gray)
                                    Spacer(Modifier.width(4.dp))
                                    Text("$it min trip", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Text("No upcoming rides", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A2C8A))
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth().height(80.dp).background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) { Text("No rides scheduled", fontSize = 14.sp, color = Color.Gray) }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Booked rides list
        Text("Booked Rides", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

        if (state.isLoading) CircularProgressIndicator()
        state.error?.let { Text("Error: $it", color = Color.Red) }
        state.rebookMessage?.let {
            Text(it, color = if (it.contains("Rebooked")) Color(0xFF4CAF50) else Color.Red, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!state.isLoading && state.bookings.isEmpty() && state.error == null) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("No booked rides") }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.bookings.size) { idx ->
                    BookedCard(
                        booking = state.bookings[idx],
                        onRebookNextWeek = { ride, booking -> viewModel.onRebookNextWeek(ride, booking) },
                        onCancelBooking = { bookingId, rideId, reason -> viewModel.cancelBooking(bookingId, rideId, reason) },
                        onDetailsClick = { onNavigateToBookingPreview(state.bookings[idx].id) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Pickup location is selected in FutureRideScreen via PickupDialog when booking
        Button(
            onClick = { onNavigateToFutureBookRides() },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCEA2FD))
        ) { Text("Book Future Ride", fontSize = 18.sp) }

        Spacer(modifier = Modifier.height(15.dp))
    }
}

/**
 * BookedCard UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookedCard(
    booking: Booking,
    onRebookNextWeek: (Ride, Booking) -> Unit = { _, _ -> },
    onCancelBooking: (String, String, String) -> Unit = { _, _, _ -> },
    onDetailsClick: () -> Unit = {}
) {
    val ride = booking.rides
    val driver = ride?.users
    var showCancelDialog by remember { mutableStateOf(false) }

    if (showCancelDialog) {
        var selectedReason by remember { mutableStateOf("") }
        var expanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Booking", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Are you sure you want to cancel this booking?", fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Reason for cancellation:", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(4.dp))
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = selectedReason.ifEmpty { "Select a reason" },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            riderCancellationReasons.forEach { reason ->
                                DropdownMenuItem(
                                    text = { Text(reason) },
                                    onClick = { selectedReason = reason; expanded = false }
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
                        if (ride != null) onCancelBooking(booking.id, ride.id, selectedReason.ifEmpty { "Prefer not to say" })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    enabled = selectedReason.isNotEmpty()
                ) { Text("Confirm Cancellation", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showCancelDialog = false }) { Text("Go Back") } }
        )
    }

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
            if (driver?.avatarUrl != null) {
                AsyncImage(
                    model = driver.avatarUrl,
                    contentDescription = "Driver profile",
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier.size(60.dp).background(Color(0xFFDCCBFF), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(driver?.userName?.take(1) ?: "?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A2C8A))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Driver: ${driver?.userName ?: "Unknown"}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DirectionsCar, null, Modifier.size(14.dp), tint = Color.DarkGray)
                    Spacer(Modifier.width(4.dp))
                    Text(ride?.vehicleType ?: "Unknown", fontSize = 14.sp, color = Color.DarkGray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, Modifier.size(14.dp), tint = Color.DarkGray)
                    Spacer(Modifier.width(4.dp))
                    Text("${booking.pickupLocation} → ${ride?.destination ?: booking.dropoffLocation}", fontSize = 14.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, null, Modifier.size(14.dp), tint = Color.DarkGray)
                    Spacer(Modifier.width(4.dp))
                    Text(formatDepartureTime(ride?.departureTime), fontSize = 14.sp, color = Color.DarkGray)
                }

                // Carbon savings: computed or fallback to ride.carbonEstimate
                val carbonSaved = computeCarbonSavedKg(booking, ride)
                val carbonDisplay = carbonSaved ?: ride?.carbonEstimate
                if (carbonDisplay != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Eco, null, Modifier.size(14.dp), tint = Color(0xFF4CAF50))
                        Spacer(Modifier.width(4.dp))
                        Text("%.2f kg CO₂ saved".format(carbonDisplay), fontSize = 13.sp, color = Color(0xFF4CAF50))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { showCancelDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    Button(
                        onClick = onDetailsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB57BFF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("Details") }
                }

                if (ride?.isRecurring == true && ride.recurringGroupId != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onRebookNextWeek(ride, booking) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB57BFF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Rebook Next Week") }
                }
            }
        }
    }
}

/**
 * Formatter helper function.
 */
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
