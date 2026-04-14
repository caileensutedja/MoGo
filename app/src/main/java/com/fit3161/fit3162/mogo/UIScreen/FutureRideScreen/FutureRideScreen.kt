package com.fit3161.fit3162.mogo.UIScreen.FutureRideScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.runtime.collectAsState

// Helper function to convert milliseconds to date string
fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
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

        // Row for date and destination selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Date picker button
            Button(onClick = { showDatePicker = true }) {
                Text("Select Date")
            }

            // Destination dropdown button with "All" option
            var expanded by remember { mutableStateOf(false) }
            Box {
                Button(onClick = { expanded = true }) {
                    Text(viewModel.selectedDestination.collectAsState().value.ifEmpty { "Select Destination" })
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    val destinations = listOf("All", "Clayton Campus", "Caulfield Campus", "Peninsula Campus", "Parkville Campus")
                    destinations.forEach { destination ->
                        DropdownMenuItem(
                            text = { Text(destination) },
                            onClick = {
                                if (destination == "All") {
                                    viewModel.onDestinationSelected("")
                                } else {
                                    viewModel.onDestinationSelected(destination)
                                }
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Display selected date text
        Text(
            text = state.selectedDate.ifEmpty { "No Date selected" },
            fontSize = 18.sp,
            color = Color(0xFF808080)
        )

        // Date picker dialog
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

        // Loading state
        if (state.isLoading) {
            CircularProgressIndicator()
        }

        // Error state
        state.error?.let {
            Text("Error: $it", color = Color.Red)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // No rides message
        if (state.rides.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No rides available")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.rides.size) { idx ->
                    FutureRideCard(ride = state.rides[idx])
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ride.driverName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
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
                Text(
                    text = "ETA: ${ride.eta}",
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { /* TODO */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAD7FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Not Interested")
                    }
                    Button(
                        onClick = { /* TODO */ },
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