package com.fit3161.fit3162.mogo.UIScreen.Profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.fit3161.fit3162.mogo.MogoApplication
import com.fit3161.fit3162.mogo.UIScreen.ProfileScreen.ProfileScreenUI
import com.fit3161.fit3162.mogo.data.repo.AuthRepository
import com.fit3161.fit3162.mogo.data.repo.ProfileRepository

/**
 * Route composable for the Profile screen.
 *
 * Constructs the required repositories and [ProfileViewModel] from the application context,
 * then renders [ProfileScreenUI].
 *
 * @param application The [MogoApplication] instance used to access the Supabase client and context.
 * @param onLogout Callback invoked when the user logs out.
 * @param onRoleChanged Callback invoked after the user's role is successfully updated.
 * @param onNavigateToSettings Callback invoked when the user navigates to the settings screen.
 */
@Composable
fun ProfileRoute(
    application: MogoApplication,
    onLogout: () -> Unit,
    onRoleChanged: () -> Unit = {},
    onNavigateToSettings: () -> Unit
    ) {
    val supabase = application.supabase
    val authRepo = remember { AuthRepository(supabase) }
    val profileRepo = remember { ProfileRepository(supabase) }
    val viewModel = remember {
        ProfileViewModel(authRepo, profileRepo, application.applicationContext)
    }
    ProfileScreenUI(viewModel = viewModel, onLogout = onLogout, onRoleChanged = onRoleChanged, onNavigateToSettings = onNavigateToSettings)
}
