package com.fit3161.fit3162.mogo.UIScreen.FutureRideScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fit3161.fit3162.mogo.UIScreen.BookScreen.formatDepartureTime
import com.fit3161.fit3162.mogo.data.repo.CAMPUS_OPTIONS
import com.fit3161.fit3162.mogo.data.repo.Ride
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    // Booking dialog state
    var rideToBook by remember { mutableStateOf<Ride?>(null) }

    // Show pickup dialog when a ride is selected
    rideToBook?.let { ride ->
        PickupLocationDialog(
            ride = ride,
            onConfirm = { pickupName, pickupLat, pickupLng ->
                viewModel.bookRide(ride, pickupName, pickupLat, pickupLng)
                rideToBook = null
            },
            onDismiss = { rideToBook = null }
        )
    }

//    // Booking result snackbar
//    val snackbarHostState = remember { SnackbarHostState() }
//    LaunchedEffect(state.bookingMessage) {
//        state.bookingMessage?.let {
//            snackbarHostState.showSnackbar(it)
//            viewModel.clearBookingMessage()
//        }
//    }
//
//    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = "Book Future Ride",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
    // When non-null, the pickup dialog is open for this ride id.
    var bookingRideId by remember { mutableStateOf<String?>(null) }

    // Pickup dialog
    bookingRideId?.let { rideId ->
        PickupDialog(
            placesRepo = viewModel.placesRepo,
            onConfirm = { useCurrentLocation, lat, lng, _ ->
                if (useCurrentLocation) {
                    viewModel.bookRideUsingCurrentLocation(rideId)
                } else if (lat != null && lng != null) {
                    viewModel.bookRideAt(rideId, lat, lng)
                }
                bookingRideId = null
            },
            onDismiss = { bookingRideId = null }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = "Book Future Ride",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

            // Show active gender preference if set
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

            // Selected Date Text
            Text(
                text = state.selectedDate.ifEmpty { "No Date selected" },
                fontSize = 18.sp,
                color = Color.Gray
            )

            // Buttons for Date Picker and Clear
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

            // Date Picker Dialog
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showDatePicker = false
                            datePickerState.selectedDateMillis?.let { millis ->
                                val formattedDate = convertMillisToDate(millis)
                                viewModel.onDateSelected(formattedDate)
                            }
                        }) {
                            Text("Confirm")
                        }
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

            // Selected Date Text
            Text(
                text = "Select your Monash campus destination: ",
                fontSize = 14.sp,
                color = Color.Gray
            )
            val campusNames = listOf("All") + CAMPUS_OPTIONS.keys.toList()

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
                            viewModel.onCampusSelected(if (campus == "All") null else campus)
                        },
                        label = { Text(campus) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFB57BFF),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }


            // Loading State
            if (state.isLoading) {
                CircularProgressIndicator()
            }

            // Error State
            state.error?.let {
                Text("Error: $it", color = Color.Red)
            }

            Spacer(modifier = Modifier.height(20.dp))

        // Main content: rides list
        if (!state.isLoading && state.error == null) {
            // Case: no visible rides and no hidden rides
            if (state.visibleRides.isEmpty() && state.hiddenRides.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                            onBook = { bookingRideId = state.visibleRides[idx].id }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
            // Main content: rides list
            if (!state.isLoading && state.error == null) {
                // Case: no visible rides and no hidden rides
                if (state.visibleRides.isEmpty() && state.hiddenRides.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                                onBook = { rideToBook = state.visibleRides[idx] }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Hidden rides section (always shown if there are hidden rides)
                        if (state.hiddenRides.isNotEmpty()) {
                            item {
                                TextButton(onClick = { showHidden = !showHidden }) {
                                    Text(
                                        text = if (showHidden)
                                            "Hide ignored rides ▲"
                                        else
                                            "Show ignored rides (${state.hiddenRides.size}) ▼",
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
    val vehicle = ride.vehicles

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isHidden) Color(0xFFE0E0E0) else Color(0xFFF3E8FF),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = driver?.userName ?: "Unknown Driver",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHidden) Color.Gray else Color.Black
                )
                Text(
                    text= "Vehicle Type: ${ride?.vehicleType}",
//                    text = "${vehicle?.vehicleMake ?: ""} ${vehicle?.vehicleModel ?: ""} · ${vehicle?.vehicleType ?: "Unknown"}",
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "📍 ${ride.origin} → ${ride.destination}",
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "🕐 ${formatDepartureTime(ride.departureTime)}",
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "💺 ${ride.availableSeats} seats available",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                ride.carbonEstimate?.let {
                    Text(
                        text = "🌿 %.2f kg CO₂".format(it),
                        fontSize = 14.sp,
                        color = Color(0xFF4CAF50)
                    )
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
                        ) {
                            Text("Restore")
                        }
                    } else {
                        Button(
                            onClick = onHide,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAD7FF)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Not Interested")
                        }
                        Button(
                            onClick = onBook,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB57BFF)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Book")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PickupLocationDialog(
    ride: Ride,
    onConfirm: (name: String, lat: Double, lng: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var pickupName by remember { mutableStateOf("") }
    var pickupLat by remember { mutableStateOf("") }
    var pickupLng by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Pickup Location") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Destination: ${ride.destination}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Departure: ${formatDepartureTime(ride.departureTime)}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = pickupName,
                    onValueChange = { pickupName = it },
                    label = { Text("Pickup address / name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = pickupLat,
                    onValueChange = { pickupLat = it },
                    label = { Text("Pickup latitude") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = pickupLng,
                    onValueChange = { pickupLng = it },
                    label = { Text("Pickup longitude") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                error?.let { Text(it, color = Color.Red, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = pickupLat.toDoubleOrNull()
                    val lng = pickupLng.toDoubleOrNull()
                    when {
                        pickupName.isBlank() -> error = "Please enter a pickup address"
                        lat == null -> error = "Latitude must be a number"
                        lng == null -> error = "Longitude must be a number"
                        else -> onConfirm(pickupName, lat, lng)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB57BFF))
            ) { Text("Confirm Booking") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(Date(millis))
}