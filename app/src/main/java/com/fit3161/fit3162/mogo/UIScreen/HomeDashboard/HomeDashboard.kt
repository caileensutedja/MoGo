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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@Composable
fun HomeScreenUI(
    viewModel: HomeViewModel,
    onProfileClick: () -> Unit = {},
    onBookedClick: () -> Unit = {},
    onMyRidesClick: () -> Unit = {},
    onRoleToggle: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDriver = uiState.profile?.user_role?.lowercase() == "driver"

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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFDCCBFF), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("<")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

        // Greeting (name only)
        Text(
            text = "Hello, ${uiState.profile?.user_name ?: ""}",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Role toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF3E8FF), RoundedCornerShape(50.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(if (!isDriver) Color(0xFFDCCBFF) else Color.Transparent)
                    .clickable { if (isDriver) onRoleToggle("rider") },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Rider",
                    fontWeight = if (!isDriver) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp,
                    color = if (!isDriver) Color(0xFF4A2C8A) else Color.Gray
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(if (isDriver) Color(0xFFDCCBFF) else Color.Transparent)
                    .clickable { if (!isDriver) onRoleToggle("driver") },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Driver",
                    fontWeight = if (isDriver) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp,
                    color = if (isDriver) Color(0xFF4A2C8A) else Color.Gray
                )
            }
        }

        Text(
            text = "Your Current Role: ${uiState.profile?.user_role ?: ""}",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        // ========== HISTORY SECTION ==========
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
                    Text("Rider History", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.riderHistory.isNotEmpty()) {
                        Text("${uiState.riderHistory.size}", fontWeight = FontWeight.Bold, fontSize = 40.sp, color = Color(0xFF4A2B7A))
                        Text("completed rides", fontSize = 11.sp, color = Color.Gray)
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
                    Text("Driver History", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.driverHistory.isNotEmpty()) {
                        Text("${uiState.driverHistory.size}", fontWeight = FontWeight.Bold, fontSize = 40.sp, color = Color(0xFF4A2B7A))
                        Text("completed drives", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        Text("No past drives", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Bookings Section
        Text("Bookings", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        Spacer(modifier = Modifier.height(16.dp))

        // ========== BOOKINGS SECTION ==========
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val confirmedBookings = uiState.bookings.filter { it.bookingStatus == "confirmed" }

            // Rider Bookings Box (clickable)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp))
                    .clickable { onBookedClick() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Rider Bookings", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${confirmedBookings.size}", fontWeight = FontWeight.Bold, fontSize = 40.sp, color = Color(0xFF4A2B7A))
                    Text("booked", fontSize = 11.sp, color = Color.Gray)
                }
            }

            // Driver Bookings Box (clickable)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp))
                    .clickable { onMyRidesClick() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Driver Bookings", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${uiState.driverRides.size}", fontWeight = FontWeight.Bold, fontSize = 40.sp, color = Color(0xFF4A2B7A))
                    Text("offered", fontSize = 11.sp, color = Color.Gray)
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
                    Text("To: ${ongoingRide.rides?.destination ?: ongoingRide.dropoffLocation}", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("Departs: ${ongoingRide.rides?.departureTime ?: ""}", fontSize = 13.sp, color = Color.Gray)
                }
            } else {
                Text("No ongoing ride", fontSize = 14.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // carbon metrics
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
                    Text("%.2f".format(uiState.totalCarbonSaved), fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color(0xFF4CAF50))
                    Text("kg CO₂ saved", fontSize = 11.sp, color = Color.Gray)
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
                    Text("↟ ${uiState.treesEquivalent}", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color(0xFF4CAF50))
                    Text("trees equivalent", fontSize = 11.sp, color = Color.Gray)
                }
            }

            // Box 3: Empty placeholder
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("?", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color(0xFFCEA2FD))
                    Text("coming soon", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}