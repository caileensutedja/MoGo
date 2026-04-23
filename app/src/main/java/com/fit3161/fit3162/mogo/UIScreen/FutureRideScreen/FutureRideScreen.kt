package com.fit3161.fit3162.mogo.UIScreen.FutureRideScreen

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fit3161.fit3162.mogo.UIScreen.BookScreen.formatDepartureTime
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

        /**
         * Date & Date Picker
         */
        // Selected Date Text
        Text(
            text = state.selectedDate.ifEmpty { "No Date selected" },
            fontSize = 18.sp,
            color = Color.Gray
        )

        // Button for Date Picker
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

        // DATE PICKER DIALOG
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

        // Loading State
        if (state.isLoading) {
            CircularProgressIndicator()
        }

        // Error State
        state.error?.let {
            Text("Error: $it", color = Color.Red)
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (!state.isLoading && state.error == null){
            if (state.visibleRides.isEmpty()) {
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
                            onUnhide = {}
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Hidden rides toggle
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
    onUnhide: () -> Unit = {}
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
                            onClick = { onUnhide() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB57BFF)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Restore")
                        }
                    } else {
                        Button(
                            onClick = { onHide() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAD7FF)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Not Interested")
                        }
                        Button(
                            onClick = { /* TODO: book the ride */ },
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
fun FutureRideCard_2(ride: Ride) {
    val driver = ride.users
    val vehicle = ride.vehicles

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = driver?.userName ?: "Unknown Driver",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${vehicle?.vehicleMake ?: ""} ${vehicle?.vehicleModel ?: ""} · ${vehicle?.vehicleType ?: "Unknown"}",
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

                // Buttons
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Button(
                        onClick = { /* TODO: hide */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEAD7FF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Not Interested")
                    }

                    Button(
                        onClick = { /* TODO: book the ride */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB57BFF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Book")
                    }
                }
            }
        }
    }
}

fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(Date(millis))
}