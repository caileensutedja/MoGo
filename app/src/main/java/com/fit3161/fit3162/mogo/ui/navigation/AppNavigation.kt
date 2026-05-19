package com.fit3161.fit3162.mogo.ui.navigation

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.fit3161.fit3162.mogo.MogoApplication
import com.fit3161.fit3162.mogo.UIScreen.ActiveRide.ActiveRideScreen
import com.fit3161.fit3162.mogo.UIScreen.ActiveRide.ActiveRideViewModel
import com.fit3161.fit3162.mogo.UIScreen.ActiveRide.ActiveRideViewModelFactory
import com.fit3161.fit3162.mogo.UIScreen.BookScreen.BookScreenUI
import com.fit3161.fit3162.mogo.UIScreen.BookScreen.BookViewModel
import com.fit3161.fit3162.mogo.UIScreen.BookScreen.BookViewModelFactory
import com.fit3161.fit3162.mogo.UIScreen.BookScreen.BookingPreviewScreen
import com.fit3161.fit3162.mogo.UIScreen.BookScreen.BookingPreviewViewModel
import com.fit3161.fit3162.mogo.UIScreen.BookScreen.BookingPreviewViewModelFactory
import com.fit3161.fit3162.mogo.UIScreen.FutureRideScreen.FutureRideScreenUI
import com.fit3161.fit3162.mogo.UIScreen.FutureRideScreen.FutureRideViewModel
import com.fit3161.fit3162.mogo.UIScreen.FutureRideScreen.FutureRideViewModelFactory
import com.fit3161.fit3162.mogo.UIScreen.HomeDashboard.HomeScreenUI
import com.fit3161.fit3162.mogo.UIScreen.HomeDashboard.HomeViewModel
import com.fit3161.fit3162.mogo.UIScreen.HomeDashboard.HomeViewModelFactory
import com.fit3161.fit3162.mogo.UIScreen.MyRides.MyRidesScreen
import com.fit3161.fit3162.mogo.UIScreen.MyRides.MyRidesViewModel
import com.fit3161.fit3162.mogo.UIScreen.MyRides.MyRidesViewModelFactory
import com.fit3161.fit3162.mogo.UIScreen.OfferScreen.OfferScreenUI
import com.fit3161.fit3162.mogo.UIScreen.OfferScreen.ui.OfferViewModel
import com.fit3161.fit3162.mogo.UIScreen.OfferScreen.ui.OfferViewModelFactory
import com.fit3161.fit3162.mogo.UIScreen.RegisterScreen.RegisterScreen
import com.fit3161.fit3162.mogo.UIScreen.RegisterScreen.RegisterViewModel
import com.fit3161.fit3162.mogo.UIScreen.RegisterScreen.RegisterViewModelFactory
import com.fit3161.fit3162.mogo.UIScreen.Settings.SettingsScreenUI
import com.fit3161.fit3162.mogo.UIScreen.Settings.SettingsViewModel
import com.fit3161.fit3162.mogo.UIScreen.Settings.SettingsViewModelFactory
import com.fit3161.fit3162.mogo.UIScreen.SignInScreen.SignInScreen
import com.fit3161.fit3162.mogo.UIScreen.SignInScreen.SignInViewModel
import com.fit3161.fit3162.mogo.UIScreen.SignInScreen.SignInViewModelFactory
import com.fit3161.fit3162.mogo.UIScreen.UploadRide.UploadRideScreen
import com.fit3161.fit3162.mogo.UIScreen.UploadRide.UploadRideViewModel
import com.fit3161.fit3162.mogo.UIScreen.UploadRide.UploadRideViewModelFactory
import com.fit3161.fit3162.mogo.UIScreen.WelcomeScreen.WelcomeScreen
import com.fit3161.fit3162.mogo.data.SessionManager
import com.fit3161.fit3162.mogo.data.repo.AuthRepository
import com.fit3161.fit3162.mogo.ui.maps.MapScreenUI
import com.fit3161.fit3162.mogo.ui.maps.MapsViewModel
import com.fit3161.fit3162.mogo.ui.maps.MapsViewModelFactory
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.github.jan.supabase.auth.auth
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
    object Dashboard : Screen("dashboard")
    object Booked : Screen("booked")
    object FutureRides : Screen("futureRides")
    object UploadRide : Screen("uploadRide")
    object MyRides : Screen("myRides")
    object Profile : Screen("profile")
    object Offer : Screen("offer")
    object Map : Screen("map")
    object ActiveRide : Screen("activeRide")
    object Settings : Screen("settings")
    object BookingPreview : Screen("bookingPreview/{bookingId}") {
        fun createRoute(bookingId: String) = "bookingPreview/$bookingId"
    }
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
    onRoleChanged: () -> Unit = {},
    roleTrigger: Int = 0
) {
    val sessionManager = remember { SessionManager(application) }
    val isLoggedIn by sessionManager.isLoggedIn.collectAsState(initial = false)
    val timestamp by sessionManager.loginTimestamp.collectAsState(initial = 0L)

    val startDestination = if (isLoggedIn && !sessionManager.isSessionExpired(timestamp)) {
        Screen.Dashboard.route
    } else {
        Screen.Welcome.route
    }

    // Get Supabase client instance
    val supabase = application.supabase

    // Single AuthRepository shared across all auth screens
    val authRepository = AuthRepository(supabase)

    /**
     * Defines full navigation graph.
     */
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        // Welcome Screen composable
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }

        // Login Screen composable
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
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Register Screen composable
        composable(Screen.Register.route) {
            val viewModel: RegisterViewModel = viewModel(
                factory = RegisterViewModelFactory(authRepository)
            )
            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // Dashboard / Home Screen composable
        composable(Screen.Dashboard.route) {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(supabase)
            )
            LaunchedEffect(roleTrigger) {
                viewModel.loadData()
            }
            HomeScreenUI(
                viewModel = viewModel,
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                },
                onBookedClick = {
                    navController.navigate(Screen.Booked.route)
                },
                onMyRidesClick = {
                    navController.navigate(Screen.MyRides.route)
                },
                onNavigateToActiveRide = {
                    navController.navigate(Screen.ActiveRide.route)
                },
                onRoleToggle = { newRole ->
                    viewModel.switchRole(newRole, onRoleChanged)
                }
            )
        }

        // Booked Rides Screen composable
        composable(Screen.Booked.route) {
            val userId = supabase.auth.currentUserOrNull()?.id ?: ""
            val viewModel: BookViewModel = viewModel(
                factory = BookViewModelFactory(
                    client = supabase,
                    mapsRepo = application.mapsRepository,
                    userId = userId
                )
            )
            BookScreenUI(
                viewModel = viewModel,
                onNavigateToFutureBookRides = {
                    navController.navigate(Screen.FutureRides.route)
                },
                onNavigateToBookingPreview = { bookingId ->
                    navController.navigate(
                        Screen.BookingPreview.createRoute(bookingId)
                    )
                }
            )
        }

        // Future Rides Screen composable
        composable(Screen.FutureRides.route) {
            val userId = supabase.auth.currentUserOrNull()?.id ?: ""
            val viewModel: FutureRideViewModel = viewModel(
                factory = FutureRideViewModelFactory(
                    client = supabase,
                    mapsRepo = application.mapsRepository,
                    placesRepo = application.placesRepository,
                    userId = userId
                )
            )
            FutureRideScreenUI(viewModel = viewModel)
        }

        // Upload Ride Screen composable
        composable(Screen.UploadRide.route) {
            val userId = supabase.auth.currentUserOrNull()?.id ?: ""
            val viewModel: UploadRideViewModel = viewModel(
                factory = UploadRideViewModelFactory(
                    client = supabase,
                    mapsRepo = application.mapsRepository,
                    placesRepo = application.placesRepository,
                    userId = userId
                )
            )
            UploadRideScreen(
                viewModel = viewModel,
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route)
                }
            )
        }

        // My Rides Screen composable (driver)
        composable(Screen.MyRides.route) {
            val userId = supabase.auth.currentUserOrNull()?.id ?: ""
            val viewModel: MyRidesViewModel = viewModel(
                factory = MyRidesViewModelFactory(supabase, userId)
            )
            MyRidesScreen(
                viewModel = viewModel,
                onNavigateToUploadRides = {
                    navController.navigate(Screen.UploadRide.route)
                },
                onNavigateToActiveRide = {
                    navController.navigate(Screen.ActiveRide.route)
                }
            )
        }

        // Active Ride Screen composable
        // Shows live map + 20s polling + SOS + Share Trip when ride is active.
        // Shows map with "no active rides" message when idle.
        composable(Screen.ActiveRide.route) {
            val viewModel: ActiveRideViewModel = viewModel(
                factory = ActiveRideViewModelFactory(
                    client = supabase,
                    mapsRepo = application.mapsRepository
                )
            )
            ActiveRideScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // Offer Screen composable
        composable(Screen.Offer.route) {
            val viewModel: OfferViewModel = viewModel(
                factory = OfferViewModelFactory(supabase)
            )
            OfferScreenUI(viewModel = viewModel)
        }

        // Profile Screen composable
        composable(Screen.Profile.route) {
            val scope = rememberCoroutineScope()
            ProfileRoute(
                application = application,
                onLogout = {
                    scope.launch {
                        sessionManager.clearSession()
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onRoleChanged = onRoleChanged,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // Settings Screen composable
        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(supabase)
            )
            SettingsScreenUI(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // Map View Screen composable (still accessible via code, not in bottom bar)
        composable(Screen.Map.route) {
            val factory = MapsViewModelFactory(application.mapsRepository)
            val viewModel: MapsViewModel = viewModel(factory = factory)
            MapScreenUI(viewModel = viewModel)
        }

        // Booking Preview Screen composable (shows route on map for a booked ride)
        composable(
            route = Screen.BookingPreview.route,
            arguments = listOf(
                navArgument("bookingId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId")
                ?: return@composable
            val viewModel: BookingPreviewViewModel = viewModel(
                factory = BookingPreviewViewModelFactory(
                    client = supabase,
                    mapsRepo = application.mapsRepository,
                    bookingId = bookingId
                )
            )
            BookingPreviewScreen(viewModel = viewModel)
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
 * Active Ride tab is always visible. When tapped with no active ride,
 * it shows the map with a "No active rides" message.
 */
@Composable
fun BottomBar(
    navController: NavHostController,
    userRole: String = "rider"
) {
    val currentRoute =
        navController.currentBackStackEntryAsState().value?.destination?.route

    // Show "My Rides" for drivers, "Booked" for riders
    val riderOrDriver = if (userRole.lowercase() == "driver") {
        BottomNavItem(Screen.MyRides.route, "My Rides", Icons.Filled.CalendarMonth)
    } else {
        BottomNavItem(Screen.Booked.route, "Booked", Icons.Filled.CalendarMonth)
    }

    // Bottom bar items (Active Ride always shown)
    val items = listOf(
        BottomNavItem(Screen.Dashboard.route, "Home", Icons.Filled.Home),
        riderOrDriver,
        BottomNavItem(Screen.Offer.route, "Offer", Icons.Filled.LocalOffer),
        BottomNavItem(Screen.Profile.route, "Profile", Icons.Filled.Person),
        BottomNavItem(Screen.ActiveRide.route, "Active Ride", Icons.Filled.Map)
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
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}