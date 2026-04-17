package com.fit3161.fit3162.mogo.UIScreen.UploadRide

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fit3161.fit3162.mogo.UIScreen.FutureRideScreen.convertMillisToDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadRideScreen(
    viewModel: UploadRideViewModel
) {
    val form by viewModel.form.collectAsState()
    val status by viewModel.status.collectAsState()

    // Date Picker State
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()


    // Time Picker State
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()

//    // Vehicle Option
//    var showVehiclePopup by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Route Details",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = form.origin,
            onValueChange = { viewModel.onOriginChange(it) },
            label = { Text("Starting Location/Address") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.LocationOn, null) }
        )

        OutlinedTextField(
            value = form.destination,
            onValueChange = { viewModel.onDestinationChange(it) },
            label = { Text("Destination") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Flag, null) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Schedule",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )

        // DATE SELECTION (Following your FutureRideScreen style)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = form.departureDate.ifEmpty { "Select Date" },
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.weight(1f),
                label = { Text("Date") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                    }
                }
            )

            OutlinedTextField(
                value = form.departureTime.ifEmpty { "Select Time" },
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.weight(1f),
                label = { Text("Time") },
                leadingIcon = { Icon(Icons.Default.Schedule, null) },
                trailingIcon = {
                    IconButton(onClick = { showTimePicker = true }) {
                        Icon(Icons.Default.Schedule, contentDescription = "Select Time")
                    }
                }
            )
        }

        // DATE PICKER DIALOG
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.onDateChange(convertMillisToDate(millis))
                        }
                        showDatePicker = false
                    }) { Text("Confirm") }
                }
            ) { DatePicker(state = datePickerState) }
        }

        // TIME PICKER DIALOG
        if (showTimePicker) {
            DatePickerDialog( // Reusing dialog container for consistency
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val formattedTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                        viewModel.onTimeChange(formattedTime)
                        showTimePicker = false
                    }) { Text("Confirm") }
                }
            ) {
                Box(Modifier.padding(24.dp)) {
                    TimePicker(state = timePickerState)
                }
            }
        }

        OutlinedTextField(
            value = form.availableSeats,
            onValueChange = { viewModel.onSeatsChange(it) },
            label = { Text("Seats Available") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Groups, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Recurring Ride?")
            }
            Switch(
                checked = form.isRecurring,
                onCheckedChange = { viewModel.onRecurringChange(it) }
            )
        }

        OutlinedTextField(
            value = form.vehicleType,
            onValueChange = { viewModel.onVehicleTypeChange(it) },
            label = { Text("Vehicle Type (e.g. Electric, Petrol)") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.DirectionsCar, null) }
        )

        OutlinedTextField(
            value = form.plateNumber,
            onValueChange = { viewModel.onPlateNumberChange(it) },
            label = { Text("Plate Number") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.DirectionsCar, null) }
        )
//        Text("Vehicle Selection", style = MaterialTheme.typography.titleMedium)
//
//        OutlinedTextField(
//            value = form.selectedVehicle?.let { "${it.vehicleMake} (${it.plateNumber})" } ?: "No Vehicle Selected",
//            onValueChange = {},
//            readOnly = true,
//            label = { Text("Vehicle") },
//            modifier = Modifier.fillMaxWidth().clickable { showVehiclePopup = true },
//            enabled = false,
//            leadingIcon = { Icon(Icons.Default.DirectionsCar, null) },
//            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
//            colors = OutlinedTextFieldDefaults.colors(
//                disabledTextColor = Color.Black,
//                disabledBorderColor = MaterialTheme.colorScheme.outline
//            )
//        )
//
//        if (showVehiclePopup) {
//            AlertDialog(
//                onDismissRequest = { showVehiclePopup = false },
//                title = { Text("Select Vehicle") },
//                text = {
//                    Column {
//                        form.availableVehicles.forEach { vehicle ->
//                            TextButton(
//                                onClick = {
//                                    viewModel.onVehicleSelected(vehicle)
//                                    showVehiclePopup = false
//                                },
//                                modifier = Modifier.fillMaxWidth()
//                            ) {
//                                Text("${vehicle.vehicleMake} ${vehicle.vehicleModel} - ${vehicle.plateNumber}")
//                            }
//                        }
//                        HorizontalDivider()
//                        TextButton(
//                            onClick = { /* TODO: Navigate to Add Vehicle Screen */ },
//                            modifier = Modifier.fillMaxWidth()
//                        ) {
//                            Icon(Icons.Default.Add, contentDescription = null)
//                            Spacer(Modifier.width(8.dp))
//                            Text("Add New Vehicle")
//                        }
//                    }
//                },
//                confirmButton = {
//                    TextButton(onClick = { showVehiclePopup = false }) { Text("Close") }
//                }
//            )
//        }
        // Vehicle
//        Text("Vehicle Selection", style = MaterialTheme.typography.titleMedium)
//
//        OutlinedTextField(
//            value = form.selectedVehicle?.let { "${it.vehicleMake} (${it.plateNumber})" } ?: "No Vehicle Selected",
//            onValueChange = {},
//            readOnly = true,
//            label = { Text("Vehicle") },
//            modifier = Modifier.fillMaxWidth().clickable { showVehiclePopup = true },
//            enabled = false,
//            leadingIcon = { Icon(Icons.Default.DirectionsCar, null) },
//            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
//            colors = OutlinedTextFieldDefaults.colors(
//                disabledTextColor = Color.Black,
//                disabledBorderColor = MaterialTheme.colorScheme.outline
//            )
//        )
//
//        if (showVehiclePopup) {
//            AlertDialog(
//                onDismissRequest = { showVehiclePopup = false },
//                title = { Text("Select Vehicle") },
//                text = {
//                    Column {
//                        form.availableVehicles.forEach { vehicle ->
//                            TextButton(
//                                onClick = {
//                                    viewModel.onVehicleSelected(vehicle)
//                                    showVehiclePopup = false
//                                },
//                                modifier = Modifier.fillMaxWidth()
//                            ) {
//                                Text("${vehicle.vehicleMake} ${vehicle.vehicleModel} - ${vehicle.plateNumber}")
//                            }
//                        }
//                        HorizontalDivider()
//                        TextButton(
//                            onClick = { /* TODO: Navigate to Add Vehicle Screen */ },
//                            modifier = Modifier.fillMaxWidth()
//                        ) {
//                            Icon(Icons.Default.Add, contentDescription = null)
//                            Spacer(Modifier.width(8.dp))
//                            Text("Add New Vehicle")
//                        }
//                    }
//                },
//                confirmButton = {
//                    TextButton(onClick = { showVehiclePopup = false }) { Text("Close") }
//                }
//            )
//        }

        if (status is UploadStatus.Error) {
            Text((status as UploadStatus.Error).message, color = Color.Red)
        }

        Button(
            onClick = { viewModel.submitRide() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = status !is UploadStatus.Loading
        ) {
            if (status is UploadStatus.Loading) CircularProgressIndicator(color = Color.White)
            else Text("Post Ride Offer")
        }
    }
}

