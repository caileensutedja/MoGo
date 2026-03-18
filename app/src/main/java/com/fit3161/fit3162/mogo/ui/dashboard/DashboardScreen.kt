package com.fit3161.fit3162.mogo.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


/**
 * DashboardScreen.kt
 *
 * The main screen shown after a successful login.
 * Currently a placeholder — replace the contents with your actual
 * dashboard UI as the app grows.
 *
 * The Sign Out button calls the logout lambda, which is handled in
 * AppNavigation.kt — it calls AuthRepository.logout() and then
 * navigates back to the Login screen, clearing the back stack so
 * the user cannot press back to return to the Dashboard.
 *
 * @param onLogout Called when the user taps "Sign Out".
 */
@Composable
fun DashboardScreen(
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onLogout) {
            Text("Sign Out")
        }
    }
}
