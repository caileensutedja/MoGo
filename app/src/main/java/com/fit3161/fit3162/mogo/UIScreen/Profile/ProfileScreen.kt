package com.fit3161.fit3162.mogo.UIScreen.ProfileScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fit3161.fit3162.mogo.UIScreen.Profile.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenUI(
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }

    // Dialog states
    var showNameDialog by remember { mutableStateOf(false) }
    var showMobileDialog by remember { mutableStateOf(false) }
    var showGenderDialog by remember { mutableStateOf(false) }

    // Temporary values for editing
    var tempName by remember { mutableStateOf("") }
    var tempMobile by remember { mutableStateOf("") }
    var tempGender by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        uiState.profile?.let {
            name = it.user_name
            email = it.user_email
            mobile = it.user_phone
            gender = it.user_gender
        }
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (uiState.error != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: ${uiState.error}")
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Settings icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.End)
                .background(Color(0xFFDCCBFF), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) { Text("⚙") }

        Spacer(modifier = Modifier.height(10.dp))

        // Profile picture
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color(0xFFDCCBFF), CircleShape),
            contentAlignment = Alignment.Center
        ) { Text("P", fontSize = 40.sp, fontWeight = FontWeight.Bold) }

        Spacer(modifier = Modifier.height(20.dp))

        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = {},
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            trailingIcon = {
                IconButton(onClick = {
                    tempName = name
                    showNameDialog = true
                }) {
                    Text("✎", fontSize = 20.sp)
                }
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Email (read-only, no edit icon)
        OutlinedTextField(
            value = email,
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            onValueChange = {},
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Mobile field
        OutlinedTextField(
            value = mobile,
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            onValueChange = {},
            label = { Text("Mobile Number") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            trailingIcon = {
                IconButton(onClick = {
                    tempMobile = mobile
                    showMobileDialog = true
                }) {
                    Text("✎", fontSize = 20.sp)
                }
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Gender field
        OutlinedTextField(
            value = gender,
            onValueChange = {},
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            label = { Text("Gender") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            trailingIcon = {
                IconButton(onClick = {
                    tempGender = gender
                    showGenderDialog = true
                }) {
                    Text("✎", fontSize = 20.sp)
                }
            }
        )
        Spacer(modifier = Modifier.height(100.dp))

        Button(
            onClick = { /* TODO: change password */ },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Change Password", color = Color.White)
        }
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { /* Save all changes (optional) */ },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCEA2FD)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Save Changes")
        }
    }

    // Edit Name Dialog
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Edit Full Name") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Full Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            viewModel.updateField("name", tempName)
                        }
                        showNameDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Edit Mobile Dialog
    if (showMobileDialog) {
        AlertDialog(
            onDismissRequest = { showMobileDialog = false },
            title = { Text("Edit Mobile Number") },
            text = {
                OutlinedTextField(
                    value = tempMobile,
                    onValueChange = { tempMobile = it },
                    label = { Text("Mobile Number") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempMobile.isNotBlank()) {
                            viewModel.updateField("mobile", tempMobile)
                        }
                        showMobileDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showMobileDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Edit Gender Dialog with dropdown
    if (showGenderDialog) {
        val genderOptions = listOf("Female", "Male", "Non-binary", "Genderqueer", "Agender", "Transgender", "Prefer not to say", "Other")
        var expanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showGenderDialog = false },
            title = { Text("Edit Gender") },
            text = {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = tempGender,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gender") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        genderOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    tempGender = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempGender.isNotBlank()) {
                            viewModel.updateField("gender", tempGender)
                        }
                        showGenderDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showGenderDialog = false }) { Text("Cancel") }
            }
        )
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