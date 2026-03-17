package com.fit3161.fit3162.mogo.UIScreen.FutureRideScreen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fit3161.fit3162.mogo.ui.theme.MoGoTheme

class FutureRideScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoGoTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { BottomNavBar() }
                ) { innerPadding ->
                    FutureRideScreenUI(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun FutureRideScreenUI(modifier: Modifier = Modifier) {

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

        // Date
        Text(
            text = "Monday, 13 October 2025",
            fontSize = 18.sp,
            color = Color.Gray,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Ride list
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(3) {
                FutureRideCardSkeleton()
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun FutureRideCardSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Car image placeholder
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .background(Color(0xFFDCCBFF), RoundedCornerShape(10.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                // Driver name
                Text(
                    text = "Driver Name",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                // Electric + seats
                Text(
                    text = "Electric | X seats (Y Available)",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )

                // Distance + time
                Text(
                    text = "800m away • 5 mins",
                    fontSize = 14.sp
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

@Composable
fun BottomNavBar() {
    NavigationBar(
        containerColor = Color.White
    ) {
        NavigationBarItem(
            selected = false,
            onClick = { /* TODO */ },
            icon = { Box(modifier = Modifier.size(24.dp).background(Color.LightGray)) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = true,
            onClick = { /* TODO */ },
            icon = { Box(modifier = Modifier.size(24.dp).background(Color.Gray)) },
            label = { Text("Book") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { /* TODO */ },
            icon = { Box(modifier = Modifier.size(24.dp).background(Color.LightGray)) },
            label = { Text("Offer") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { /* TODO */ },
            icon = { Box(modifier = Modifier.size(24.dp).background(Color.LightGray)) },
            label = { Text("Profile") }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewFutureRideScreen() {
    MoGoTheme {
        Scaffold(
            bottomBar = { BottomNavBar() }
        ) { innerPadding ->
            FutureRideScreenUI(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}