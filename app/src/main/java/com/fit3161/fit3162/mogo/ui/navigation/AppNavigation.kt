package com.fit3161.fit3162.mogo.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.fit3161.fit3162.mogo.MogoApplication
import com.fit3161.fit3162.mogo.UIScreen.BookScreen.BookScreenUI
import com.fit3161.fit3162.mogo.UIScreen.BookScreen.BookViewModel
import com.fit3161.fit3162.mogo.UIScreen.BookScreen.BookViewModelFactory
import com.fit3161.fit3162.mogo.UIScreen.FutureRideScreen.FutureRideScreenUI
import com.fit3161.fit3162.mogo.UIScreen.FutureRideScreen.FutureRideViewModel
import com.fit3161.fit3162.mogo.UIScreen.FutureRideScreen.FutureRideViewModelFactory
import com.fit3161.fit3162.mogo.UIScreen.HomeScreen.HomeScreenUI
import com.fit3161.fit3162.mogo.UIScreen.OfferScreen.OfferScreenUI
import com.fit3161.fit3162.mogo.UIScreen.OfferScreen.ui.OfferViewModel
import com.fit3161.fit3162.mogo.UIScreen.OfferScreen.ui.OfferViewModelFactory
import com.fit3161.fit3162.mogo.UIScreen.ProfileScreen.ProfileScreenUI
import com.fit3161.fit3162.mogo.UIScreen.RegisterScreen.RegisterScreen
import com.fit3161.fit3162.mogo.UIScreen.SignInScreen.SignInScreen
import com.fit3161.fit3162.mogo.UIScreen.WelcomeScreen.WelcomeScreen
import com.fit3161.fit3162.mogo.data.repo.AuthRepository
import com.fit3161.fit3162.mogo.UIScreen.SignInScreen.SignInViewModel
import com.fit3161.fit3162.mogo.UIScreen.RegisterScreen.RegisterViewModel
import com.fit3161.fit3162.mogo.UIScreen.RegisterScreen.RegisterViewModelFactory
import com.fit3161.fit3162.mogo.UIScreen.SignInScreen.SignInViewModelFactory
import com.fit3161.fit3162.mogo.data.repo.BookRepository
import com.fit3161.fit3162.mogo.data.repo.OfferRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth

/**
 * Defines every screen route in the app.
 *
 * @param route the name of the route (typically named after the screen names).
 */
sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard") // TODO: This is temporary. Remove during clean up/when done.
    object Booked : Screen("booked")
    object FutureRides : Screen("futureRides")
    object Profile: Screen("profile")
    object Offer: Screen("offer")
}

/**
 * Handles navigation between screens for the entire app.
 * Defines how the screens are connected to each other.
 *
 * @param application [MogoApplication] instance used to access the shared Supabase client.
 */
@Composable
fun AppNavigation(application: MogoApplication, navController: NavHostController) {

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
        // TODO: Logic fix if alr logged in or not
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
                    navController.navigate(Screen.Dashboard.route) { // Go to HomeScreen after Login
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Register Screen composable.
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
        /**
         * FIX SCREENS BELOW
         */
        // Home Dashboard Screen composable.
        composable(Screen.Dashboard.route) {
            HomeScreenUI()
        }

        // Booked UI composable.
        composable(Screen.Booked.route) {
//            val bookRepository = remember { BookRepository(SupabaseClient) }
//            val factory = BookViewModelFactory(bookRepository)
//            val viewModel: BookViewModel = viewModel(factory = factory)
            val userId = supabase.auth.currentUserOrNull()?.id ?: ""
            val viewModel: BookViewModel = viewModel(
                factory = BookViewModelFactory(supabase, userId)
            )
            BookScreenUI(
                viewModel = viewModel,
                onNavigateToFutureBookRides = {
                    navController.navigate(Screen.FutureRides.route) // Navigate from Welcome Screen to Login Screen.
                }
            )
        }

        // Future Rides UI composable.
        composable(Screen.FutureRides.route) {
//                val bookRepository = remember { BookRepository(SupabaseClient) }
//                val factory = FutureRideViewModelFactory(bookRepository)
//                val viewModel: FutureRideViewModel = viewModel(factory = factory)
            val viewModel: FutureRideViewModel = viewModel(
                factory = FutureRideViewModelFactory(supabase)
            )
                FutureRideScreenUI(
                    viewModel = viewModel
                )
        }

        // Offer UI composable.
        composable(Screen.Offer.route) {
            val viewModel: OfferViewModel = viewModel(
                factory = OfferViewModelFactory(supabase)
            )
            OfferScreenUI(
                viewModel = viewModel
            )
        }

        // Profile UI composable.
        composable(Screen.Profile.route) {
            ProfileScreenUI()
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

    // Lists within the bottom bar
    val items = listOf(
        BottomNavItem(Screen.Dashboard.route, "Home", Icons.Filled.Home),
        BottomNavItem(Screen.Booked.route, "booked", Icons.Filled.CalendarMonth),
        BottomNavItem(Screen.Offer.route, "Offer", Icons.Filled.LocalOffer),
        BottomNavItem(Screen.Profile.route, "Profile", Icons.Filled.Person)
    )

    NavigationBar {
        items.forEach { item ->
            // When the icon is clicked, it will be highlighted and the user will be redirected to the intended screen
            NavigationBarItem(
                icon = {
                    Icon(item.icon, contentDescription = item.label)
                },
                label = {
                    Text(item.label)
                },
                selected = currentRoute == item.route,
                // TODO: Check if it stores previous screen states
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )

        }
    }
}
