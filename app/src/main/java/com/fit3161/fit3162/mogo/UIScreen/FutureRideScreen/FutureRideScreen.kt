package com.fit3161.fit3162.mogo.UIScreen.FutureRideScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fit3161.fit3162.mogo.UIScreen.BookScreen.formatDepartureTime
import com.fit3161.fit3162.mogo.data.model.PresetDestinations
import com.fit3161.fit3162.mogo.data.repo.PlacesRepository
import com.fit3161.fit3162.mogo.data.repo.Ride
import com.fit3161.fit3162.mogo.ui.components.AddressAutocompleteField
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dialog that lets the rider choose their pickup location before confirming a booking.
 * Options:
 *   1. Type an address (with Places autocomplete suggestions)
 *   2. Tap "Use My Current Location" (uses GPS + reverse geocode)
 *
 * @param ride        The ride being booked (for showing destination and departure time).
 * @param placesRepo  PlacesRepository for autocomplete suggestions.
 * @param onBookWithLocation  Called when user picks an address. Params: (name, lat, lng).
 * @param onBookWithCurrentLocation  Called when user taps "Use My Current Location".
 * @param onDismiss   Called when user taps Cancel.
 */
@Composable
fun PickupDialog(
    ride: Ride,
    placesRepo: PlacesRepository,
    onBookWithLocation: (name: String, lat: Double, lng: Double) -> Unit,
    onBookWithCurrentLocation: () -> Unit,
    onDismiss: () -> Unit
) {
    // State for the typed/selected pickup address
    var pickupName by remember { mutableStateOf("") }
    var pickupLat by remember { mutableStateOf<Double?>(null) }
    var pickupLng by remember { mutableStateOf<Double?>(null) }

    // Whether a valid place has been selected (from autocomplete)
    val hasValidPickup = pickupLat != null && pickupLng != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Confirm Booking", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Ride details
                Text(
                    "Destination: ${ride.destination}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Departure: ${formatDepartureTime(ride.departureTime)}",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )

                HorizontalDivider()

                Text(
                    "Where should the driver pick you up?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                // Address autocomplete field for typing a pickup location
                AddressAutocompleteField(
                    label = "Enter pickup address",
                    currentValue = pickupName,
                    placesRepo = placesRepo,
                    onCurrentLocation = {
                        onBookWithCurrentLocation()
                        onDismiss()
                    },
                    onPlacePicked = { resolved ->
                        pickupName = resolved.name
                        pickupLat = resolved.latLng.latitude
                        pickupLng = resolved.latLng.longitude
                    },
                    setValue = { value ->
                        pickupName = value
                        // Clear coordinates when user types freely
                        pickupLat = null
                        pickupLng = null
                    }
                )

            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (hasValidPickup) {
                        onBookWithLocation(pickupName, pickupLat!!, pickupLng!!)
                        onDismiss()
                    }
                },
                enabled = hasValidPickup,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB57BFF)
                )
            ) {
                Text("Confirm Booking", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FutureRideScreenUI(
    viewModel: FutureRideViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var showHidden by remember { mutableStateOf(false) }

    // The ride the user tapped "Book" on (null = no dialog shown)
    var bookingRide by remember { mutableStateOf<Ride?>(null) }

    // Pickup dialog: shown when bookingRide is set
    bookingRide?.let { ride ->
        PickupDialog(
            ride = ride,
            placesRepo = viewModel.placesRepo,
            onBookWithLocation = { name, lat, lng ->
                viewModel.bookRide(ride, name, lat, lng)
            },
            onBookWithCurrentLocation = {
                viewModel.bookRideUsingCurrentLocation(ride.id)
            },
            onDismiss = { bookingRide = null }
        )
    }

    // Booking success/error message
    state.bookingMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearBookingMessage() },
            title = {
                Text(
                    if (message.contains("success", true)) "Booked!"
                    else "Booking Issue",
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(message) },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearBookingMessage() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB57BFF)
                    )
                ) { Text("OK", color = Color.White) }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Book Future Ride",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        // Gender preference indicator
        state.genderPreference?.let {
            Text(
                text = "Showing: $it drivers only",
                fontSize = 14.sp,
                color = Color(0xFFB57BFF),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected date text
        Text(
            text = state.selectedDate.ifEmpty { "No Date selected" },
            fontSize = 18.sp,
            color = Color.Gray
        )

        // Date picker and clear buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { showDatePicker = true }) {
                Text("Select Date")
            }
            if (state.selectedDate.isNotEmpty()) {
                OutlinedButton(onClick = { viewModel.onDateCleared() }) {
                    Text("Clear")
                }
            }
        }

        // Date picker dialog
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        showDatePicker = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.onDateSelected(convertMillisToDate(millis))
                        }
                    }) { Text("Confirm") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Campus filter chips
        Text(
            text = "Select your Monash campus destination: ",
            fontSize = 14.sp,
            color = Color.Gray
        )
        val campusNames = listOf("All") + PresetDestinations.all.map { it.name }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            campusNames.forEach { campus ->
                val isSelected = if (campus == "All")
                    state.selectedCampus == null
                else
                    state.selectedCampus == campus

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        viewModel.onCampusSelected(
                            if (campus == "All") null else campus
                        )
                    },
                    label = { Text(campus) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFB57BFF),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (state.isLoading) {
            CircularProgressIndicator()
        }
        state.error?.let {
            Text("Error: $it", color = Color.Red)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Rides list
        if (!state.isLoading && state.error == null) {
            if (state.visibleRides.isEmpty() && state.hiddenRides.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.selectedDate.isNotEmpty())
                            "No rides available on this date"
                        else
                            "No upcoming rides available",
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    // Visible rides
                    items(state.visibleRides.size) { idx ->
                        FutureRideCard(
                            ride = state.visibleRides[idx],
                            isHidden = false,
                            onHide = { viewModel.hideRide(state.visibleRides[idx].id) },
                            onUnhide = {},
                            onBook = { bookingRide = state.visibleRides[idx] }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Hidden rides toggle
                    if (state.hiddenRides.isNotEmpty()) {
                        item {
                            TextButton(onClick = { showHidden = !showHidden }) {
                                Text(
                                    text = if (showHidden)
                                        "Hide ignored rides"
                                    else
                                        "Show ignored rides (${state.hiddenRides.size})",
                                    color = Color.Gray
                                )
                            }
                        }

                        if (showHidden) {
                            items(state.hiddenRides.size) { idx ->
                                FutureRideCard(
                                    ride = state.hiddenRides[idx],
                                    isHidden = true,
                                    onHide = {},
                                    onUnhide = { viewModel.unhideRide(state.hiddenRides[idx].id) }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FutureRideCard(
    ride: Ride,
    isHidden: Boolean = false,
    onHide: () -> Unit,
    onUnhide: () -> Unit = {},
    onBook: () -> Unit = {}
) {
    val driver = ride.users

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isHidden) Color(0xFFE0E0E0) else Color(0xFFF3E8FF),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = driver?.userName ?: "Unknown Driver",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (isHidden) Color.Gray else Color.Black
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DirectionsCar, null, Modifier.size(14.dp), tint = Color.DarkGray)
                Spacer(Modifier.width(4.dp))
                Text("Vehicle Type: ${ride.vehicleType}", fontSize = 16.sp, color = Color.DarkGray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, Modifier.size(14.dp), tint = Color.DarkGray)
                Spacer(Modifier.width(4.dp))
                Text("${ride.origin} → ${ride.destination}", fontSize = 16.sp, color = Color.DarkGray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, null, Modifier.size(14.dp), tint = Color.DarkGray)
                Spacer(Modifier.width(4.dp))
                Text(formatDepartureTime(ride.departureTime), fontSize = 16.sp, color = Color.DarkGray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.EventSeat, null, Modifier.size(14.dp), tint = Color.DarkGray)
                Spacer(Modifier.width(4.dp))
                Text("${ride.availableSeats} seats available", fontSize = 14.sp, color = Color.DarkGray)
            }
            ride.carbonEstimate?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Eco, null, Modifier.size(14.dp), tint = Color(0xFF4CAF50))
                    Spacer(Modifier.width(4.dp))
                    Text("%.2f kg CO₂".format(it), fontSize = 14.sp, color = Color(0xFF4CAF50))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isHidden) {
                    Button(
                        onClick = onUnhide,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB57BFF)),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Restore") }
                } else {
                    Button(
                        onClick = onHide,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAD7FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Not Interested") }
                    Button(
                        onClick = onBook,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB57BFF)),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Book") }
                }
            }
        }
    }
}

fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(Date(millis))
}