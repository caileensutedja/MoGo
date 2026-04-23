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

        // Top Row: Back + Profile + Fire Streak
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFDCCBFF), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("<")
            }

            // Profile + Fire Streak
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Fire streak indicator
                if (uiState.rideStreak > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🔥", fontSize = 18.sp)
                        Text(
                            text = "${uiState.rideStreak}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                    }
                }

                // Profile picture
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
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Greeting
        Text(
            text = "Hello, ${uiState.profile?.user_name ?: ""}",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // History Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rider History Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Rider History",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.riderHistory.isNotEmpty()) {
                        Text(
                            text = "${uiState.riderHistory.size}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 40.sp,
                            color = Color(0xFF4A2B7A)
                        )
                        Text(
                            text = "completed rides",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    } else {
                        Text("No past rides", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            // Driver History Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Driver History",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.driverHistory.isNotEmpty()) {
                        Text(
                            text = "${uiState.driverHistory.size}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 40.sp,
                            color = Color(0xFF4A2B7A)
                        )
                        Text(
                            text = "completed drives",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    } else {
                        Text("No past drives", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ========== BOOKINGS SECTION ==========
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val confirmedBookings = uiState.bookings.filter { it.bookingStatus == "confirmed" }

            // Rider Bookings Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Rider Bookings",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${confirmedBookings.size}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp,
                        color = Color(0xFF4A2B7A)
                    )
                    Text(
                        text = "booked",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            // Driver Bookings Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Driver Bookings",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${uiState.driverRides.size}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp,
                        color = Color(0xFF4A2B7A)
                    )
                    Text(
                        text = "offered",
                        fontSize = 11.sp,
                        color = Color.Gray
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
                .height(100.dp)
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
                Text("No ongoing ride", fontSize = 14.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Carbon metrics
        Text("Carbon Metrics", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Box 1: CO₂ saved
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%.2f".format(uiState.totalCarbonSaved),
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "kg CO₂ saved",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            // Box 2: Trees equivalent
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "↟ ${uiState.treesEquivalent}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "trees equivalent",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            // Box 3: Empty (placeholder for future metric)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = Color(0xFFCEA2FD)
                    )
                    Text(
                        text = "coming soon",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}