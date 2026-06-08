package com.fit3161.fit3162.mogo.UIScreen.Profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.fit3161.fit3162.mogo.MogoApplication
import com.fit3161.fit3162.mogo.UIScreen.ProfileScreen.ProfileScreenUI
import com.fit3161.fit3162.mogo.data.repo.AuthRepository
import com.fit3161.fit3162.mogo.data.repo.ProfileRepository

/**
 * Navigation Route for the Profile Screen.
 * Provides Key Dependencies (AuthRepo, ProfileRepo, ProfileViewModel)
 */
@Composable
fun ProfileRoute(
    application: MogoApplication,
    onLogout: () -> Unit,
    onRoleChanged: () -> Unit = {},
    onNavigateToSettings: () -> Unit
    ) {
    // Obtain Supabase Client
    val supabase = application.supabase

    // Remembered to avoid recreation
    val authRepo = remember { AuthRepository(supabase) }
    val profileRepo = remember { ProfileRepository(supabase) }
    val viewModel = remember {
        ProfileViewModel(authRepo, profileRepo, application.applicationContext)
    }
    // Display Profile UI Screen with Viewmodel/callbacks.
    ProfileScreenUI(viewModel = viewModel, onLogout = onLogout, onRoleChanged = onRoleChanged, onNavigateToSettings = onNavigateToSettings)
}