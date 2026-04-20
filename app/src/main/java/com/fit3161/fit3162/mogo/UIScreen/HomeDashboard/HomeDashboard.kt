package com.fit3161.fit3162.mogo.UIScreen.HomeDashboard

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun HomeScreenUI(
    viewModel: HomeViewModel,
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (uiState.error != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = uiState.error ?: "Something went wrong", color = Color.Red)
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // Top Row: Back + Profile
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
            ) {
                Text("<")
            }

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

        Spacer(modifier = Modifier.height(20.dp))

        // Greeting (name only)
        Text(
            text = "Hello, ${uiState.profile?.user_name ?: ""}",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Bookings Section
        Text("Bookings", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val confirmedBookings = uiState.bookings.filter { it.bookingStatus == "confirmed" }

            Column(modifier = Modifier.weight(1f)) {
                Text("Rider", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .height(60.dp)
                        .fillMaxWidth()
                        .background(Color(0xFFF3E8FF), RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${confirmedBookings.size} booked",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Driver", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .height(60.dp)
                        .fillMaxWidth()
                        .background(Color(0xFFF3E8FF), RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${uiState.driverRides.size} offered",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Ongoing Ride
        Text("Ongoing Ride", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        val ongoingRide = uiState.bookings.firstOrNull { it.bookingStatus == "confirmed" }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (ongoingRide != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "To: ${ongoingRide.rides?.destination ?: ongoingRide.dropoffLocation}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Departs: ${ongoingRide.rides?.departureTime ?: ""}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            } else {
                Text("No ongoing ride", color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // History
        Text("History", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${uiState.bookings.size} total booking(s)",
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Carbon Metrics
        Text("Carbon Metrics", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%.2f kg CO₂".format(uiState.totalCarbonSaved),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF4CAF50)
                )
                Text(
                    text = "saved by carpooling",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    }
}