package com.fit3161.fit3162.mogo.UIScreen.BookScreen


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
import com.fit3161.fit3162.mogo.data.repo.Ride

@Composable
fun BookScreenUI(
    viewModel: BookViewModel,
    modifier: Modifier = Modifier,
    onNavigateToFutureBookRides: () -> Unit
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

                    BookedCardSkeleton(
                        ride = ride
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }


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

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun BookedCardSkeleton(ride: Ride) {
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
                    text = ride.driverName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${ride.carType} | ${ride.totalSeats} seats",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "📍 ${ride.destination}",
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

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
                        Text("Cancel")
                    }

                    Button(
                        onClick = { /* TODO */ },
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