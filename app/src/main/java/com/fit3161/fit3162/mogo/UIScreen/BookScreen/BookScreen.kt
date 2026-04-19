package com.fit3161.fit3162.mogo.UIScreen.BookScreen


import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fit3161.fit3162.mogo.data.repo.Booking
import com.fit3161.fit3162.mogo.data.repo.Ride

@Composable
fun BookScreenUI(
    viewModel: BookViewModel,
    modifier: Modifier = Modifier,
    onNavigateToFutureBookRides: () -> Unit,
    onNavigateToUploadRides: () -> Unit,
    onNavigateToMyRides: () -> Unit
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Title
        Text(
            text = "Book",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Ongoing Ride Section
        Text(
            text = "Ongoing Ride",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        /**
         * To Implement: A link to refer to ongoing ride progress
         */
        Text(
            text = "None",
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Booked Rides Section
        Text(
            text = "Booked Rides",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        // Loading State
        if (state.isLoading) {
            CircularProgressIndicator()
        }

        // Error State
        state.error?.let {
            Text("Error: $it", color = Color.Red)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!state.isLoading && state.bookings.isEmpty() && state.error == null) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No booked rides")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.bookings.size) { idx ->
                    BookedCardSkeleton(booking = state.bookings[idx])
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        //  Future Ride Button
        Button(
            onClick = { onNavigateToFutureBookRides() },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFCEA2FD)
            )
        ) {
            Text("Book Future Ride", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(15.dp))

        //  Upload Ride Button
        Button(
            onClick = { onNavigateToUploadRides() },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFCEA2FD)
            )
        ) {
            Text("Upload Future Ride", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(15.dp))

        //  My Ride Button
        Button(
            onClick = { onNavigateToMyRides() },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFCEA2FD)
            )
        ) {
            Text("My Future Ride (Driver)", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}


@Composable
fun BookedCardSkeleton(booking: Booking) {
    val ride = booking.rides
    val driver = ride?.users
    val vehicle = ride?.vehicles

    Log.d("BOOKING_DEBUG", "booking: $booking")
    Log.d("BOOKING_DEBUG", "ride: $ride")
    Log.d("BOOKING_DEBUG", "driver: $driver")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Placeholder box for car image
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .background(Color(0xFFDCCBFF), RoundedCornerShape(10.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = driver?.userName ?: "Unknown Driver",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text= "${ride?.vehicleType}",
//                        text = "${vehicle?.vehicleMake ?: ""} ${vehicle?.vehicleModel ?: ""} · ${vehicle?.vehicleType ?: "Unknown"}",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "📍 ${ride?.destination ?: booking.dropoffLocation}",
                    fontSize = 14.sp
                )
                Text(
                    text = "🕐 ${formatDepartureTime(ride?.departureTime)}",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                ride?.carbonEstimate?.let {
                    Text(
                        text = "🌿 %.2f kg CO₂".format(it),
                        fontSize = 13.sp,
                        color = Color(0xFF4CAF50)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { /* TODO: cancel booking */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEAD7FF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { /* TODO: show details */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB57BFF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Details")
                    }
                }
            }
        }
    }
}


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