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

data class OngoingRideDetails(
    val driverName: String? = null,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val estimatedDistanceKm: Double? = null,
    val estimatedDurationMinutes: Int? = null
)

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
    val ongoingRide: OngoingRideDetails? = null,  // <-- critical
    val isLoading: Boolean = true,
    val error: String? = null
)

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
                val driverHistory = allDriverRides.filter { it.rideStatus == "completed" }

                // Calculate carbon
                val riderCarbon = riderHistory.mapNotNull { it.rides?.carbonEstimate }.sum()
                val driverCarbon = driverHistory.mapNotNull { it.carbonEstimate }.sum()
                val totalCarbonSaved = riderCarbon + driverCarbon
                val totalDistanceShared = totalCarbonSaved / 0.21


                val rideStreak = minOf(riderHistory.size + driverHistory.size, 7)
                val treesEquivalent = totalCarbonSaved / 25.0

                // Determine ongoing ride details
                val ongoingRideDetails = if (isDriver) {
                    upcomingDriverRides.firstOrNull()?.let { ride ->
                        OngoingRideDetails(
                            driverName = profile?.user_name,
                            origin = ride.origin,
                            destination = ride.destination,
                            departureTime = ride.departureTime,
                            estimatedDistanceKm = ride.carbonEstimate?.let { carbon ->
                                carbon / 0.21  // petrol factor
                            },
                            estimatedDurationMinutes = ride.carbonEstimate?.let { carbon ->
                                ((carbon / 0.21) / 40 * 60).toInt()
                            }
                        )
                    }
                } else {
                    ongoingBookings.firstOrNull()?.let { booking ->
                        val ride = booking.rides
                        OngoingRideDetails(
                            driverName = ride?.users?.userName,
                            origin = ride?.origin ?: "",
                            destination = ride?.destination ?: "",
                            departureTime = ride?.departureTime ?: "",
                            estimatedDistanceKm = ride?.carbonEstimate?.let { carbon ->
                                carbon / 0.21
                            },
                            estimatedDurationMinutes = ride?.carbonEstimate?.let { carbon ->
                                ((carbon / 0.21) / 40 * 60).toInt()
                            }
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
                    ongoingRide = ongoingRideDetails,   // <-- set here
                    isLoading = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

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