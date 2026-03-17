package com.fit3161.fit3162.mogo.UIScreen.HomeScreen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

class HomeScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoGoTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { BottomNavBar() }
                ) { innerPadding ->
                    HomeScreenUI(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreenUI(modifier: Modifier = Modifier) {

    Column(
        modifier = modifier
            .fillMaxSize()
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
                    .background(Color(0xFFDCCBFF), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("P")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Title
        Text(
            text = "Home",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Bookings Section Title
        Text(
            text = "Bookings",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Rider + Driver with titles ABOVE boxes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Rider",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .height(60.dp)
                        .fillMaxWidth()
                        .background(Color(0xFFF3E8FF), RoundedCornerShape(15.dp))
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Driver",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .height(60.dp)
                        .fillMaxWidth()
                        .background(Color(0xFFF3E8FF), RoundedCornerShape(15.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Ongoing Ride
        DashboardSection(title = "Ongoing Ride")

        Spacer(modifier = Modifier.height(20.dp))

        // History
        DashboardSection(title = "History")

        Spacer(modifier = Modifier.height(20.dp))

        // Carbon Metrics
        DashboardSection(title = "Carbon Metrics")
    }
}

@Composable
fun DashboardSection(title: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp))
        )
    }
}

@Composable
fun DashboardButton(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(60.dp)
            .background(Color(0xFFF3E8FF), RoundedCornerShape(15.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun BottomNavBar() {
    NavigationBar(
        containerColor = Color.White
    ) {
        NavigationBarItem(
            selected = true,
            onClick = { /* TODO */ },
            icon = { Box(modifier = Modifier.size(24.dp).background(Color.Gray)) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { /* TODO */ },
            icon = { Box(modifier = Modifier.size(24.dp).background(Color.LightGray)) },
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
fun PreviewHomeScreen() {
    MoGoTheme {
        Scaffold(
            bottomBar = { BottomNavBar() }
        ) { innerPadding ->
            HomeScreenUI(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}