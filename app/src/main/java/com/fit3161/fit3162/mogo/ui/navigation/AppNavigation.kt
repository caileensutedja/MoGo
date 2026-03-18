package com.fit3161.fit3162.mogo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fit3161.fit3162.mogo.MogoApplication
import com.fit3161.fit3162.mogo.data.repository.AuthRepository
import com.fit3161.fit3162.mogo.ui.dashboard.DashboardScreen
import com.fit3161.fit3162.mogo.ui.login.LoginScreen
import com.fit3161.fit3162.mogo.ui.login.LoginViewModel
import com.fit3161.fit3162.mogo.ui.login.LoginViewModelFactory
import com.fit3161.fit3162.mogo.ui.register.RegisterScreen
import com.fit3161.fit3162.mogo.ui.register.RegisterViewModel
import com.fit3161.fit3162.mogo.ui.register.RegisterViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Screen.kt (defined here for convenience)
 *
 * A sealed class representing every screen/route in the app.
 * Each object holds a unique string route used by the NavController
 * to identify and navigate between destinations.
 *
 * To add a new screen to the app:
 * 1. Add a new object here (e.g. object Profile : Screen("profile"))
 * 2. Add a composable {} block in AppNavigation's NavHost
 */
sealed class Screen(val route: String) {
    object Login     : Screen("login")
    object Register  : Screen("register") // TODO: Implement Register Screen.
    object Dashboard : Screen("dashboard") // TODO: Implement Register Screen.
}

/**
 * TODO: Update doc.
 *
 * Handles navigation between screens.
 */
@Composable
fun AppNavigation(application: MogoApplication) {
    val navController = rememberNavController()

    val authRepo = AuthRepository(application.supabase)

    NavHost(
        navController=navController,
        startDestination = Screen.Login.route
    ) {

        composable(Screen.Login.route) {
            val viewModel : LoginViewModel = viewModel(factory = LoginViewModelFactory(authRepo))

            // ----- LOGIN -----
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // ----- REGISTER -----

        composable(Screen.Register.route) {
            val viewModel: RegisterViewModel = viewModel(
                factory = RegisterViewModelFactory(authRepo)
            )
            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = {
                    // popBackStack returns to Login without adding Register
                    // to the stack again — avoids building up a deep back stack
                    navController.popBackStack()
                }
            )
        }

        // ----- DASHBOARD -----

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onLogout = {
                    CoroutineScope(Dispatchers.Main).launch {
                        authRepo.logout()
                        navController.navigate(Screen.Login.route) {
                            // Remove Dashboard from the back stack so pressing
                            // back after logout does not return to Dashboard
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                }
            )
        }

    }
}
