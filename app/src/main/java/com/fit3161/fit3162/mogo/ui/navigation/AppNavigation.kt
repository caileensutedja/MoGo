package com.fit3161.fit3162.mogo.ui.navigation

import android.util.Log
import androidx.compose.material.icons.Icons
import com.fit3161.fit3162.mogo.UIScreen.Profile.ProfileRoute
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.fit3161.fit3162.mogo.UIScreen.HomeDashboard.HomeScreenUI
import com.fit3161.fit3162.mogo.UIScreen.MyRides.MyRidesScreen
import com.fit3161.fit3162.mogo.UIScreen.MyRides.MyRidesViewModel
import com.fit3161.fit3162.mogo.UIScreen.MyRides.MyRidesViewModelFactory
import com.fit3161.fit3162.mogo.UIScreen.OfferScreen.OfferScreenUI
import com.fit3161.fit3162.mogo.UIScreen.OfferScreen.ui.OfferViewModel
import com.fit3161.fit3162.mogo.UIScreen.OfferScreen.ui.OfferViewModelFactory
import com.fit3161.fit3162.mogo.UIScreen.RegisterScreen.RegisterScreen
import com.fit3161.fit3162.mogo.UIScreen.SignInScreen.SignInScreen
import com.fit3161.fit3162.mogo.UIScreen.WelcomeScreen.WelcomeScreen
import com.fit3161.fit3162.mogo.data.repo.AuthRepository
import com.fit3161.fit3162.mogo.UIScreen.SignInScreen.SignInViewModel
import com.fit3161.fit3162.mogo.UIScreen.RegisterScreen.RegisterViewModel
import com.fit3161.fit3162.mogo.UIScreen.RegisterScreen.RegisterViewModelFactory
import com.fit3161.fit3162.mogo.UIScreen.SignInScreen.SignInViewModelFactory
import com.fit3161.fit3162.mogo.UIScreen.UploadRide.UploadRideScreen
import com.fit3161.fit3162.mogo.UIScreen.UploadRide.UploadRideViewModel
import com.fit3161.fit3162.mogo.UIScreen.UploadRide.UploadRideViewModelFactory
import io.github.jan.supabase.auth.auth
import com.fit3161.fit3162.mogo.ui.maps.MapScreenUI
import com.fit3161.fit3162.mogo.ui.maps.MapsViewModel
import com.fit3161.fit3162.mogo.ui.maps.MapsViewModelFactory
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.fit3161.fit3162.mogo.UIScreen.HomeDashboard.HomeViewModel
import com.fit3161.fit3162.mogo.UIScreen.HomeDashboard.HomeViewModelFactory
import com.fit3161.fit3162.mogo.data.SessionManager
import kotlinx.coroutines.launch

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
    object UploadRide: Screen("uploadRide")
    object MyRides: Screen("myRides")
    object Profile: Screen("profile")
    object Offer: Screen("offer")
    object Map : Screen("map")
}

/**
 * Handles navigation between screens for the entire app.
 * Defines how the screens are connected to each other.
 *
 * @param application [MogoApplication] instance used to access the shared Supabase client.
 */
@Composable
fun AppNavigation(
    application: MogoApplication,
    navController: NavHostController,
    onRoleChanged: () -> Unit = {}) {

    val sessionManager = remember { SessionManager(application) }
    val isLoggedIn by sessionManager.isLoggedIn.collectAsState(initial = false)
    val timestamp by sessionManager.loginTimestamp.collectAsState(initial = 0L)
    Log.d("DEBUG LOG IN/is logged in", "${isLoggedIn}")
    Log.d("DEBUG LOG IN/session manager", "${!sessionManager.isSessionExpired(timestamp)}")


    val startDestination = if (isLoggedIn && !sessionManager.isSessionExpired(timestamp)) {
        Screen.Dashboard.route
    } else {
        Screen.Welcome.route
    }

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
        // TODO: Logic fix if alr logged in or not -> Session Persistence
        startDestination = startDestination // App starts in Welcome Screen when first launched.
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
                factory = SignInViewModelFactory(authRepository, sessionManager)
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

        // TODO: Remove during code cleanup. Dashboard only contains a single button: SignOut to go back to the previous. screen.

        composable(Screen.Dashboard.route) {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(supabase)
            )
            HomeScreenUI(
                viewModel = viewModel,
                onProfileClick = { navController.navigate(Screen.Profile.route) }
            )
        }

        /**
         * FIX SCREENS BELOW
         */
        // Booked UI composable.
        composable(Screen.Booked.route) {
            val userId = supabase.auth.currentUserOrNull()?.id ?: ""
            val viewModel: BookViewModel = viewModel(
                factory = BookViewModelFactory(supabase, userId)
            )
            BookScreenUI(
                viewModel = viewModel,
                onNavigateToFutureBookRides = {
                    navController.navigate(Screen.FutureRides.route)
                }
//                onNavigateToUploadRides = {
//                    navController.navigate(Screen.UploadRide.route)
//                },
//                onNavigateToMyRides = {
//                    navController.navigate(Screen.MyRides.route)
//                }
            )
        }

        // Future Rides UI composable.
        composable(Screen.FutureRides.route) {
            val userId = supabase.auth.currentUserOrNull()?.id ?: ""
            val viewModel: FutureRideViewModel = viewModel(
                factory = FutureRideViewModelFactory(supabase, userId)
            )
            FutureRideScreenUI(
                viewModel = viewModel,
                )
        }

        composable(Screen.UploadRide.route) {
            val userId = supabase.auth.currentUserOrNull()?.id ?: ""
            val viewModel: UploadRideViewModel = viewModel(
                factory = UploadRideViewModelFactory(supabase, userId)
            )
            UploadRideScreen(
                viewModel = viewModel,
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) // Navigate from Welcome Screen to Login Screen.
                })
        }

        composable(Screen.MyRides.route){
            val userId = supabase.auth.currentUserOrNull()?.id ?: ""
            val viewModel: MyRidesViewModel = viewModel(
                factory = MyRidesViewModelFactory(supabase, userId)
            )
            MyRidesScreen(
                viewModel,
                onNavigateToUploadRides = {
                    navController.navigate(Screen.UploadRide.route)
                }
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

        composable(Screen.Profile.route) {
            val scope = rememberCoroutineScope()
            ProfileRoute(
                application = application,
                onLogout = {
                    scope.launch {
                        sessionManager.clearSession()
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(0) { inclusive = true }  // clears the entire back stack
                        }
                    }
                },
                onRoleChanged = onRoleChanged
            )
        }


        // Map View Composable
        composable(Screen.Map.route) {
            val factory = MapsViewModelFactory(application.mapsRepository)
            val viewModel: MapsViewModel = viewModel(factory = factory)
            MapScreenUI(viewModel = viewModel)
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

// val profileRepository = ProfileRepository(supabase)


/**
 * Composable bottom bar for easy navigation.
 */
@Composable
fun BottomBar(navController: NavHostController, userRole: String = "rider") {

    val currentRoute =
        navController.currentBackStackEntryAsState().value?.destination?.route

    val riderOrDriver = if (userRole.lowercase() == "driver") {
        BottomNavItem(Screen.MyRides.route, "My Rides", Icons.Filled.CalendarMonth)
    } else {
        BottomNavItem(Screen.Booked.route, "Booked", Icons.Filled.CalendarMonth)
    }

    // Lists within the bottom bar
    val items = listOf(
        BottomNavItem(Screen.Dashboard.route, "Home", Icons.Filled.Home),
        riderOrDriver,
        BottomNavItem(Screen.Offer.route, "Offer", Icons.Filled.LocalOffer),
        BottomNavItem(Screen.Profile.route, "Profile", Icons.Filled.Person),

        BottomNavItem(Screen.Map.route, "Map View", Icons.Filled.Map)
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
