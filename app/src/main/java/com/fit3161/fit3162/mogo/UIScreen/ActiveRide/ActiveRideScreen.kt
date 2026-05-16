package com.fit3161.fit3162.mogo.UIScreen.ActiveRide

import android.net.Uri
import android.telephony.SmsManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fit3161.fit3162.mogo.data.repo.Booking
import com.fit3161.fit3162.mogo.data.repo.EmergencyContact
import com.fit3161.fit3162.mogo.utils.readContactFromUri

@Composable
fun ActiveRideScreen(
    booking: Booking,
    riderName: String,
    emergencyContacts: List<EmergencyContact>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showSosDialog by remember { mutableStateOf(false) }

    // Contact picker for share trip
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let {
            val (_, phone) = readContactFromUri(context.contentResolver, it)
            if (phone != null) {
                val ride = booking.rides
                val message = "Hi! $riderName has shared their trip with you. " +
                        "They are heading to ${ride?.destination ?: booking.dropoffLocation} " +
                        "departing at ${ride?.departureTime ?: ""}. " +
                        "Driver: ${ride?.users?.userName ?: "Unknown"}, " +
                        "Vehicle: ${ride?.vehicleType ?: ""} (${ride?.plateNumber ?: ""})."
                sendSms(context, phone, message)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) contactPickerLauncher.launch(null)
    }

    // SOS confirmation dialog
    if (showSosDialog) {
        AlertDialog(
            onDismissRequest = { showSosDialog = false },
            title = { Text("Send SOS Alert?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will send an urgent safety alert to all your emergency contacts " +
                            "with your current ride details."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSosDialog = false
                        val ride = booking.rides
                        val message = "URGENT: $riderName feels unsafe. " +
                                "They are in a ride heading to ${ride?.destination ?: booking.dropoffLocation}. " +
                                "Driver: ${ride?.users?.userName ?: "Unknown"}, " +
                                "Vehicle: ${ride?.vehicleType ?: ""} plate ${ride?.plateNumber ?: ""}. " +
                                "Departure: ${ride?.departureTime ?: ""}. Please check on them immediately."
                        emergencyContacts.forEach { contact ->
                            sendSms(context, contact.contactPhone, message)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Send SOS", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSosDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back
        Text(
            text = "< Back",
            fontSize = 16.sp,
            modifier = Modifier.clickable { onBack() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Active Ride", fontSize = 28.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(24.dp))

        // Ride details card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF3E8FF), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            val ride = booking.rides
            RideDetailRow("To", ride?.destination ?: booking.dropoffLocation)
            RideDetailRow("From", ride?.origin ?: booking.pickupLocation)
            RideDetailRow("Departing", ride?.departureTime ?: "")
            RideDetailRow("Driver", ride?.users?.userName ?: "Unknown")
            RideDetailRow("Vehicle", "${ride?.vehicleType ?: ""} - ${ride?.plateNumber ?: ""}")
            RideDetailRow("Seats booked", booking.seatsBooked.toString())
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Share trip button
        Button(
            onClick = {
                permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
            },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDCCBFF)),
            shape = RoundedCornerShape(15.dp)
        ) {
            Text("Share Trip", fontSize = 18.sp, color = Color.Black)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SOS button
        Button(
            onClick = { showSosDialog = true },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            shape = RoundedCornerShape(15.dp)
        ) {
            Text("🚨 SOS - I Feel Unsafe", fontSize = 18.sp, color = Color.White)
        }

        if (emergencyContacts.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No safety contacts set up. Add them in Settings.",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun RideDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
        Text(text = value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

fun sendSms(context: android.content.Context, phone: String, message: String) {
    try {
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(phone, null, message, null, null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
