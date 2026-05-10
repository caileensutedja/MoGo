package com.fit3161.fit3162.mogo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fit3161.fit3162.mogo.data.model.UserRoleViewModel
import com.fit3161.fit3162.mogo.data.model.UserRoleViewModelFactory
import com.fit3161.fit3162.mogo.data.repo.ProfileRepository
import com.fit3161.fit3162.mogo.ui.navigation.AppNavigation
import com.fit3161.fit3162.mogo.ui.navigation.BottomBar
import com.fit3161.fit3162.mogo.ui.navigation.Screen
import com.fit3161.fit3162.mogo.ui.theme.MoGoTheme
import io.github.jan.supabase.auth.auth

/**
 * MainActivity that acts as entry point of app.
 * Calls [AppNavigation] (handles switching between screens).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoGoTheme {
                val application = application as MogoApplication
                val navController: NavHostController = rememberNavController()

                // Current route to remember the stack
                val currentRoute =
                    navController.currentBackStackEntryAsState().value?.destination?.route

                // List of screens where we do not want to show the bottom bar
                val hideBottomBarRoutes = listOf(
                    Screen.Welcome.route,
                    Screen.Login.route,
                    Screen.Register.route
                )

                // Variable to allow bottom bar to be shown
                val showBottomBar = currentRoute != null && currentRoute !in hideBottomBarRoutes

                val supabase = application.supabase
                val roleViewModel: UserRoleViewModel = viewModel<UserRoleViewModel>(
                    factory = UserRoleViewModelFactory(ProfileRepository(supabase), supabase)
                )
                val userRole by roleViewModel.userRole.collectAsStateWithLifecycle()
                var roleTrigger by remember { mutableStateOf(0) }
                LaunchedEffect(Unit) {
                    roleViewModel.fetchRole()
                }

                Scaffold(modifier = Modifier.fillMaxSize(),
                    // Show the bottom bar on approved screens
                    bottomBar = {
                        if (showBottomBar) {
                            BottomBar(navController, userRole)
                        }
                    }
                ){ innerPadding ->

                    Surface(modifier=Modifier.padding(innerPadding)) {
                        AppNavigation(
                            application = application as MogoApplication,
                            navController = navController, // Navigation Controller
                            onRoleChanged = {
                                roleViewModel.fetchRole()
                                roleTrigger++}
                        )
                    }

                }
            }
        }
    }
}

