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
import com.fit3161.fit3162.mogo.data.repo.Ride
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FutureRideScreenUI(
    viewModel: FutureRideViewModel,
    modifier: Modifier = Modifier) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

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
        Button(onClick = { showDatePicker = true }) {
            Text("Select Date")
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

        // No Rides
        if (state.rides.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No rides available")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(state.rides.size) { idx ->
                    val ride = state.rides[idx]

                    FutureRideCard(
                        ride = ride
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun FutureRideCard(ride: Ride) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Driver name
                Text(
                    text = ride.driverName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                // Electric + seats
                Text(
                    text = ride.carType,
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )

                Text(
                    text = "Destination: 📍${ride.destination}",
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )

                // Distance + time
                Text(
                    text = "ETA: ${ride.eta}",
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Buttons
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Button(
                        onClick = { /* TODO */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEAD7FF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Not Interested")
                    }

                    Button(
                        onClick = { /* TODO */ },
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
    val formatter = SimpleDateFormat("dd-mm-yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}