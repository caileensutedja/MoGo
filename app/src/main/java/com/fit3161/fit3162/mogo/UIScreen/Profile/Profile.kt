package com.fit3161.fit3162.mogo.UIScreen.ProfileScreen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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

@Composable
fun ProfileScreenUI(modifier: Modifier = Modifier) {

    // Editable state variables
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Settings icon placeholder
        Box(
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.End)
                .background(Color(0xFFDCCBFF), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("⚙")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Profile picture placeholder
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color(0xFFDCCBFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("P", fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = {},
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            trailingIcon = {
                IconButton(onClick = { /* TODO: implement edit */ }) {
                    Text("✎", fontSize = 20.sp)
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            trailingIcon = {
                IconButton(onClick = { /* TODO: implement edit */ }) {
                    Text("✎", fontSize = 20.sp)
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Mobile field
        OutlinedTextField(
            value = mobile,
            onValueChange = { },
            label = { Text("Mobile Number") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            trailingIcon = {
                IconButton(onClick = { /* TODO: implement edit */ }) {
                    Text("✎", fontSize = 20.sp)
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Gender field
        OutlinedTextField(
            value = gender,
            onValueChange = {},
            label = { Text("Gender (Male/Female/Other)") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            trailingIcon = {
                IconButton(onClick = { /* TODO: implement edit */ }) {
                    Text("✎", fontSize = 20.sp)
                }
            }
        )

        Spacer(modifier = Modifier.height(100.dp))

        // Change Password button
        Button(
            onClick = { /* TODO */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Change Password", color = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Save Changes button
        Button(
            onClick = { /* TODO */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFCEA2FD)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Save Changes")
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
fun PreviewProfileScreen() {
    MoGoTheme {
        Scaffold(
            bottomBar = { BottomNavBar() }
        ) { innerPadding ->
            ProfileScreenUI(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}