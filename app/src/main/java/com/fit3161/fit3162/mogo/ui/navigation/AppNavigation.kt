package com.fit3161.fit3162.mogo.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fit3161.fit3162.mogo.MogoApplication
import com.fit3161.fit3162.mogo.UIScreen.HomeScreen.HomeScreenUI
import com.fit3161.fit3162.mogo.UIScreen.RegisterScreen.RegisterScreen
import com.fit3161.fit3162.mogo.UIScreen.SignInScreen.SignInScreen
import com.fit3161.fit3162.mogo.UIScreen.WelcomeScreen.WelcomeScreen
import com.fit3161.fit3162.mogo.data.repo.AuthRepository
import com.fit3161.fit3162.mogo.ui.dashboard.DashboardScreen
import com.fit3161.fit3162.mogo.UIScreen.SignInScreen.SignInViewModel
import com.fit3161.fit3162.mogo.UIScreen.RegisterScreen.RegisterViewModel
import com.fit3161.fit3162.mogo.UIScreen.RegisterScreen.RegisterViewModelFactory
import com.fit3161.fit3162.mogo.UIScreen.SignInScreen.SignInViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Defines every screen route in the app.
 *
 * @param route the name of the route (typically named after the screen names)..
 */
sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard") // TODO: This is temporary. Remove during clean up/when done.
    object HomeDashboard : Screen("homedashboard")

    // TODO: Add the rest of the screens here.
}

/**
 * Handles navigation between screens for the entire app.
 * Defines how the screens are connected to each other.
 *
 * @param application [MogoApplication] instance used to access the shared Supabase client.
 */
@Composable
fun AppNavigation(application: MogoApplication) {

    val navController = rememberNavController()

    // Get Supabase client instance.
    val supabase = application.supabase

    // Single AuthRepository shared across all auth screens.
    val authRepository = AuthRepository(supabase)

    /**
     * Defines full navigation graph.
     *
     * From official documentation:
     * "Provides a place in the Compose hierarchy for self-contained navigation to occur.
     * Once this is called, any Composable within the given NavGraphBuilder can be navigated to
     * from the provided navController."
     *
     */
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route // App starts in Welcome Screen when first launched.
    ) {

        // Welcome Screen composable.
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToLogin = {
                navController.navigate(Screen.Login.route) // Navigate from Welcome Screen to Login Screen.
            })
        }

        // Login Screen composable.
        composable(Screen.Login.route) {
            val viewModel: SignInViewModel = viewModel(
                factory = SignInViewModelFactory(authRepository)
            )
            SignInScreen(
                viewModel = viewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.HomeDashboard.route) { // Go to HomeScreen after Login
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // TODO: Remove during code cleanup. Use existing RegisterScreen UI composables.
        // TEMPORARY Register Screen composable (uses ViewModel for business logic handling).
        composable(Screen.Register.route) {
            val viewModel: RegisterViewModel = viewModel(
                factory = RegisterViewModelFactory(authRepository)
            )
            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // TODO: Remove during code cleanup. Dashboard only contains a single button: SignOut to go back to prev. screen.
        // Dashboard/HomeScreen/Screen after Login composable.
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

        // HomeScreen UI composable.
        composable(Screen.HomeDashboard.route) {
            HomeScreenUI()
        }

    }
}


/**
 * Data class for the bottom bar consisting of route, label, and icon.
 */
data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

/**
 * Composable bottom bar for easy navigation.
 */
@Composable
fun BottomBar(navController: NavHostController) {

    val currentRoute =
        navController.currentBackStackEntryAsState().value?.destination?.route

    val items = listOf(
        BottomNavItem("home", "Home", Icons.Filled.Home),
        BottomNavItem("book", "Book", Icons.Filled.Home),
        BottomNavItem("offer", "Offer", Icons.Filled.Home),
        BottomNavItem("profile", "Profile", Icons.Filled.Home)
    )

    NavigationBar {

        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(item.icon, contentDescription = item.label)
                },
                label = {
                    Text(item.label)
                },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )

        }
    }
}
