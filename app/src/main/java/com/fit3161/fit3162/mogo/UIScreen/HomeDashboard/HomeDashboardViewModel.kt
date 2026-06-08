package com.fit3161.fit3162.mogo.UIScreen.HomeDashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.repo.AuthRepository
import com.fit3161.fit3162.mogo.data.repo.Booking
import com.fit3161.fit3162.mogo.data.repo.BookRepository
import com.fit3161.fit3162.mogo.data.repo.ProfileRepository
import com.fit3161.fit3162.mogo.data.repo.Ride
import com.fit3161.fit3162.mogo.data.repo.UserProfile
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * ViewModel and state definitions for the Home Dashboard screen.
 *
 * Manages loading and exposing ride, booking, history, and carbon metrics data
 * for the currently authenticated user.
 */



/**
 * Ongoing/active ride details for the dashboard card.
 * @param isInProgress True if the ride has started (ride_status = "in_progress").
 *                     False if it's upcoming but not yet started.
 */
data class OngoingRideDetails(
    val driverName: String? = null,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val estimatedDistanceKm: Double? = null,
    val estimatedDurationMinutes: Int? = null,
    val isInProgress: Boolean = false
)

/**
 * UI state for the Home Dashboard screen.
 *
 * @property profile The currently authenticated user's profile.
 * @property bookings Confirmed bookings for the user as a rider.
 * @property riderHistory Past completed bookings for the user as a rider.
 * @property driverHistory Past completed rides offered by the user as a driver.
 * @property driverRides Upcoming scheduled rides offered by the user as a driver.
 * @property totalCarbonSaved Total CO₂ saved in kg across all rider and driver history.
 * @property rideStreak Current ride streak count, capped at 7.
 * @property totalDistanceShared Total kilometres shared across all rides.
 * @property treesEquivalent Carbon savings expressed as a tree-planting equivalent.
 * @property ongoingRide Details of the current in-progress or next upcoming ride, if any.
 * @property hasUnreadNotification Whether the user has unread notifications.
 * @property isLoading Whether data is currently being fetched.
 * @property error Error message to display if data loading fails.
 */
data class HomeUiState(
    val profile: UserProfile? = null,
    val bookings: List<Booking> = emptyList(),
    val riderHistory: List<Booking> = emptyList(),
    val driverHistory: List<Ride> = emptyList(),
    val driverRides: List<Ride> = emptyList(),
    val totalCarbonSaved: Double = 0.0,
    val rideStreak: Int = 0,
    val totalDistanceShared: Double = 0.0,
    val treesEquivalent: Double = 0.0,
    val ongoingRide: OngoingRideDetails? = null,
    val hasUnreadNotification: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)


/**
 * ViewModel for the Home Dashboard screen.
 *
 * Fetches and exposes ride, booking, history, and carbon metrics for the authenticated user.
 * Handles role-switching between rider and driver.
 *
 * @param authRepo Provides the current authenticated user's ID.
 * @param profileRepo Fetches and updates the user's profile.
 * @param bookRepo Fetches ride and booking data for the user.
 */
class HomeViewModel(
    private val authRepo: AuthRepository,
    private val profileRepo: ProfileRepository,
    private val bookRepo: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val userId = authRepo.getCurrentUserId()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Not logged in")
                return@launch
            }
            try {
                val profile = profileRepo.getProfile(userId)
                val isDriver = profile?.user_role?.lowercase() == "driver"

                // Rider data
                val ongoingBookings = bookRepo.getBookedRides(userId)
                val riderHistory = bookRepo.getRiderHistory(userId)

                // Driver data
                val allDriverRides = bookRepo.getMyRides(userId)
                val upcomingDriverRides = allDriverRides.filter { it.rideStatus == "scheduled" }
                val inProgressDriverRides = allDriverRides.filter { it.rideStatus == "in_progress" }
                val driverHistory = allDriverRides.filter { it.rideStatus == "completed" }

                // Calculate carbon
                val riderCarbon = riderHistory.mapNotNull { it.rides?.carbonEstimate }.sum()
                val driverCarbon = driverHistory.mapNotNull { it.carbonEstimate }.sum()
                val totalCarbonSaved = riderCarbon + driverCarbon
                val totalDistanceShared = totalCarbonSaved / 0.21

                val rideStreak = minOf(riderHistory.size + driverHistory.size, 7)
                val treesEquivalent = totalCarbonSaved / 25.0

                val now = OffsetDateTime.now(ZoneOffset.UTC)

                // Determine ongoing ride:
                // Priority 1: in_progress rides (ride has started)
                // Priority 2: upcoming scheduled rides (departure in the future)
                val ongoingRideDetails = if (isDriver) {
                    // Driver: check in_progress first, then upcoming scheduled
                    inProgressDriverRides.firstOrNull()?.let { ride ->
                        OngoingRideDetails(
                            driverName = profile?.user_name,
                            origin = ride.origin,
                            destination = ride.destination,
                            departureTime = ride.departureTime,
                            estimatedDistanceKm = ride.carbonEstimate?.let { it / 0.21 },
                            estimatedDurationMinutes = ride.carbonEstimate?.let { ((it / 0.21) / 40 * 60).toInt() },
                            isInProgress = true
                        )
                    } ?: upcomingDriverRides
                        .filter { ride ->
                            val departure = try { OffsetDateTime.parse(ride.departureTime) } catch (e: Exception) { null }
                            departure != null && departure.isAfter(now)
                        }
                        .firstOrNull()
                        ?.let { ride ->
                            OngoingRideDetails(
                                driverName = profile?.user_name,
                                origin = ride.origin,
                                destination = ride.destination,
                                departureTime = ride.departureTime,
                                estimatedDistanceKm = ride.carbonEstimate?.let { it / 0.21 },
                                estimatedDurationMinutes = ride.carbonEstimate?.let { ((it / 0.21) / 40 * 60).toInt() },
                                isInProgress = false
                            )
                        }
                } else {
                    // Rider: check for in_progress booked ride first, then upcoming
                    ongoingBookings
                        .firstOrNull { it.rides?.rideStatus == "in_progress" }
                        ?.let { booking ->
                            val ride = booking.rides
                            OngoingRideDetails(
                                driverName = ride?.users?.userName,
                                origin = ride?.origin ?: "",
                                destination = ride?.destination ?: "",
                                departureTime = ride?.departureTime ?: "",
                                estimatedDistanceKm = ride?.carbonEstimate?.let { it / 0.21 },
                                estimatedDurationMinutes = ride?.carbonEstimate?.let { ((it / 0.21) / 40 * 60).toInt() },
                                isInProgress = true
                            )
                        }
                        ?: ongoingBookings
                            .filter { booking ->
                                val departure = try { OffsetDateTime.parse(booking.rides?.departureTime) } catch (e: Exception) { null }
                                departure != null && departure.isAfter(now)
                            }
                            .firstOrNull()
                            ?.let { booking ->
                                val ride = booking.rides
                                OngoingRideDetails(
                                    driverName = ride?.users?.userName,
                                    origin = ride?.origin ?: "",
                                    destination = ride?.destination ?: "",
                                    departureTime = ride?.departureTime ?: "",
                                    estimatedDistanceKm = ride?.carbonEstimate?.let { it / 0.21 },
                                    estimatedDurationMinutes = ride?.carbonEstimate?.let { ((it / 0.21) / 40 * 60).toInt() },
                                    isInProgress = false
                                )
                            }
                }

                _uiState.value = _uiState.value.copy(
                    profile = profile,
                    bookings = ongoingBookings,
                    totalDistanceShared = totalDistanceShared,
                    riderHistory = riderHistory,
                    driverHistory = driverHistory,
                    driverRides = upcomingDriverRides,
                    totalCarbonSaved = totalCarbonSaved,
                    rideStreak = rideStreak,
                    treesEquivalent = treesEquivalent,
                    ongoingRide = ongoingRideDetails,
                    hasUnreadNotification = false,
                    isLoading = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    /**
     * Switches the authenticated user's role between rider and driver.
     *
     * Updates the role in the remote profile, reloads dashboard data, and reflects
     * the change immediately in [uiState]. Invokes [onRoleChanged] after the switch completes.
     *
     * @param newRole The role to switch to — either "rider" or "driver".
     * @param onRoleChanged Optional callback invoked after the role has been updated.
     */
    fun switchRole(newRole: String, onRoleChanged: () -> Unit = {}) {
        viewModelScope.launch {
            val userId = authRepo.getCurrentUserId() ?: return@launch
            profileRepo.updateProfile(userId, user_role = newRole)
            loadData()
            _uiState.update { it.copy(profile = it.profile?.copy(user_role = newRole)) }
            onRoleChanged()
        }
    }
}

/**
 * Factory for creating [HomeViewModel] instances.
 *
 * Constructs the required repositories from a [SupabaseClient] and injects them
 * into the ViewModel.
 *
 * @param client The Supabase client used to initialise all repositories.
 */
class HomeViewModelFactory(
    private val client: SupabaseClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                AuthRepository(client),
                ProfileRepository(client),
                BookRepository(client)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
