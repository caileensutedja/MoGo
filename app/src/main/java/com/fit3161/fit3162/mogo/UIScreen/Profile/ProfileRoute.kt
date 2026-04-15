package com.fit3161.fit3162.mogo.UIScreen.Profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.fit3161.fit3162.mogo.MogoApplication
import com.fit3161.fit3162.mogo.UIScreen.ProfileScreen.ProfileScreenUI
import com.fit3161.fit3162.mogo.data.repo.AuthRepository
import com.fit3161.fit3162.mogo.data.repo.ProfileRepository

@Composable
fun ProfileRoute(application: MogoApplication) {
    val supabase = application.supabase
    val authRepo = remember { AuthRepository(supabase) }
    val profileRepo = remember { ProfileRepository(supabase) }
    val viewModel = remember {
        ProfileViewModel(authRepo, profileRepo)
    }
    ProfileScreenUI(viewModel = viewModel)
}