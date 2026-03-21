package com.fit3161.fit3162.mogo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fit3161.fit3162.mogo.MogoApplication
import com.fit3161.fit3162.mogo.data.repo.AuthRepository
import com.fit3161.fit3162.mogo.ui.dashboard.DashboardScreen
import com.fit3161.fit3162.mogo.ui.login.LoginScreenTemp
import com.fit3161.fit3162.mogo.ui.login.LoginViewModel
import com.fit3161.fit3162.mogo.ui.login.LoginViewModelFactory
import com.fit3161.fit3162.mogo.ui.register.RegisterScreenTemp
import com.fit3161.fit3162.mogo.ui.register.RegisterViewModel
import com.fit3161.fit3162.mogo.ui.register.RegisterViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Login     : Screen("login")
    object Register  : Screen("register")
    object Dashboard : Screen("dashboard")
}

@Composable
fun AppNavigation(application: MogoApplication) {
    val navController = rememberNavController()
    val supabase = application.supabase
    val authRepository = AuthRepository(supabase)

    NavHost(navController = navController, startDestination = Screen.Login.route) {

        composable(Screen.Login.route) {
            val viewModel: LoginViewModel = viewModel(
                factory = LoginViewModelFactory(authRepository)
            )
            LoginScreenTemp(
                viewModel = viewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            val viewModel: RegisterViewModel = viewModel(
                factory = RegisterViewModelFactory(authRepository)
            )
            RegisterScreenTemp(
                viewModel = viewModel,
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onLogout = {
                    CoroutineScope(Dispatchers.Main).launch {
                        authRepository.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
