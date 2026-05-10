package com.fit3161.fit3162.mogo.UIScreen.Settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fit3161.fit3162.mogo.data.repo.EmergencyContact
import com.fit3161.fit3162.mogo.utils.readContactFromUri

@Composable
fun SettingsScreenUI(
    viewModel: SettingsViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var driverPref by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var carPref by remember { mutableStateOf("") }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let {
            val (name, phone) = readContactFromUri(context.contentResolver, it)
            if (name != null && phone != null) {
                viewModel.addContact(name, phone)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) contactPickerLauncher.launch(null)
    }

    // LaunchedEffect inside the composable
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            snackbarHostState.showSnackbar(uiState.successMessage!!)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "< Back",
                fontSize = 16.sp,
                color = Color(0xFF6200EE),
                modifier = Modifier.clickable { onBack() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Settings",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(30.dp))

            SettingsDropdown(
                label = "Driver Preference",
                value = driverPref,
                options = listOf("Male", "Female", "N/A"),
                onValueChange = { driverPref = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            SettingsDropdown(
                label = "Car Preference",
                value = carPref,
                options = listOf("Electric", "Hybrid", "Any"),
                onValueChange = { carPref = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            SettingsDropdown(
                label = "Role (Driver/Rider)",
                value = role,
                options = listOf("Rider", "Driver"),
                onValueChange = { role = it }
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "Safety Contacts",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "These contacts will receive an SOS alert if you feel unsafe during a ride.",
                fontSize = 13.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            uiState.error?.let {
                Text(text = it, color = Color.Red, fontSize = 13.sp)
                LaunchedEffect(it) { viewModel.clearMessages() }
            }

            OutlinedButton(
                onClick = {
                    permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Person, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Safety Contact from Phone")
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (uiState.contacts.isEmpty()) {
                Text(
                    text = "No safety contacts added yet.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                uiState.contacts.forEach { contact ->
                    SafetyContactRow(
                        contact = contact,
                        onDelete = { contact.contactId?.let { viewModel.deleteContact(it) } }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = { viewModel.showSaved() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCEA2FD)),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text("Save Changes", fontSize = 18.sp)
            }
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
        Text(text = label, fontSize = 18.sp, fontWeight = FontWeight.Medium)
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
            Text(text = if (value.isEmpty()) "Select..." else value, fontSize = 16.sp)
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
fun SafetyContactRow(
    contact: EmergencyContact,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF3E8FF), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = contact.contactName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(text = contact.contactPhone, fontSize = 13.sp, color = Color.Gray)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = Color.Red)
        }
    }
}
