package com.fit3161.fit3162.mogo.UIScreen.SettingsScreen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fit3161.fit3162.mogo.ui.theme.MoGoTheme

class SettingsScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoGoTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { BottomNavBar() }
                ) { innerPadding ->
                    SettingsScreenUI(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreenUI(modifier: Modifier = Modifier) {

    // Dropdown states
    var driverPref by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var otherSetting by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Back button
        Text(
            text = "Back",
            fontSize = 16.sp,
            modifier = Modifier.clickable { /* TODO */ }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Title
        Text(
            text = "Settings",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Driver Preference dropdown
        SettingsDropdown(
            label = "Driver Preference",
            value = driverPref,
            options = listOf("Male", "Female", "N/A"),
            onValueChange = { driverPref = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        SettingsDropdown(
            label = "Car Preference",
            value = otherSetting,
            options = listOf("Electric", "Hybrid", "Any"),
            onValueChange = { otherSetting = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Role dropdown
        SettingsDropdown(
            label = "Role (Driver/Rider)",
            value = role,
            options = listOf("Rider", "Driver"),
            onValueChange = { role = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Placeholder for additional settings
        SettingsDropdown(
            label = "Placeholder",
            value = otherSetting,
            options = listOf("Option A", "Option B", "Option C"),
            onValueChange = { otherSetting = it }
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Save Changes button
        Button(
            onClick = { /* TODO */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFCEA2FD)
            ),
            shape = RoundedCornerShape(15.dp)
        ) {
            Text("Save Changes", fontSize = 18.sp)
        }
    }
}

@Composable
fun SettingsDropdown(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
                .background(Color(0xFFF3E8FF), RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = if (value.isEmpty()) "Select..." else value,
                fontSize = 16.sp
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
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
            selected = true,
            onClick = { /* TODO */ },
            icon = { Box(modifier = Modifier.size(24.dp).background(Color.Gray)) },
            label = { Text("Profile") }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSettingsScreen() {
    MoGoTheme {
        Scaffold(
            bottomBar = { BottomNavBar() }
        ) { innerPadding ->
            SettingsScreenUI(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}