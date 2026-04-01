package com.fit3161.fit3162.mogo.UIScreen.SettingsScreen

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fit3161.fit3162.mogo.ui.theme.MoGoTheme

@Composable
fun SettingsScreenUI(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            // TODO: show snackbar "Saved!"
            viewModel.clearSaveResult()
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(text = "Back", fontSize = 16.sp, modifier = Modifier.clickable { /* TODO */ })
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Settings",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(30.dp))

        SettingsDropdown(
            label = "Driver Preference",
            value = uiState.driverPreference,
            options = listOf("Male", "Female", "N/A"),
            onValueChange = viewModel::onDriverPreferenceChange
        )
        Spacer(modifier = Modifier.height(20.dp))

        SettingsDropdown(
            label = "Car Preference",
            value = uiState.carPreference,
            options = listOf("Electric", "Hybrid", "Any"),
            onValueChange = viewModel::onCarPreferenceChange
        )
        Spacer(modifier = Modifier.height(20.dp))

        SettingsDropdown(
            label = "Role (Driver/Rider)",
            value = uiState.role,
            options = listOf("Rider", "Driver"),
            onValueChange = viewModel::onRoleChange
        )
        Spacer(modifier = Modifier.height(20.dp))

        SettingsDropdown(
            label = "Placeholder",
            value = uiState.placeholder,
            options = listOf("Option A", "Option B", "Option C"),
            onValueChange = viewModel::onPlaceholderChange
        )
        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = viewModel::saveSettings,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCEA2FD)),
            shape = RoundedCornerShape(15.dp)
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
            } else {
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
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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