package com.fit3161.fit3162.mogo.UIScreen.ActiveRide

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.model.RouteResult
import com.fit3161.fit3162.mogo.data.repo.AuthRepository
import com.fit3161.fit3162.mogo.data.repo.BookRepository
import com.fit3161.fit3162.mogo.data.repo.Booking
import com.fit3161.fit3162.mogo.data.repo.EmergencyContact
import com.fit3161.fit3162.mogo.data.repo.EmergencyContactRepository
import com.fit3161.fit3162.mogo.data.repo.MapsRepository
import com.fit3161.fit3162.mogo.data.repo.ProfileRepository
import com.fit3161.fit3162.mogo.data.repo.Ride
import com.google.android.gms.maps.model.LatLng
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ActiveRideUiState(
    val booking: Booking? = null,
    val ride: Ride? = null,
    val isDriver: Boolean = false,
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val riderName: String = "",
    val driverLocation: LatLng? = null,
    val riderLocation: LatLng? = null,
    val routeState: ActiveRouteState = ActiveRouteState.Idle,
    val isLoading: Boolean = true,
    val rideEnded: Boolean = false,
    val error: String? = null
)

sealed class ActiveRouteState {
    object Idle : ActiveRouteState()
    object Loading : ActiveRouteState()
    data class Success(val route: RouteResult) : ActiveRouteState()
    data class Error(val message: String) : ActiveRouteState()
}

class ActiveRideViewModel(
    private val authRepo: AuthRepository,
    private val bookRepo: BookRepository,
    private val contactRepo: EmergencyContactRepository,
    private val profileRepo: ProfileRepository,
    private val mapsRepo: MapsRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ActiveRide"
        private const val POLL_INTERVAL_MS = 20_000L
    }

    private val _uiState = MutableStateFlow(ActiveRideUiState())
    val uiState: StateFlow<ActiveRideUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init { loadData() }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val userId = authRepo.getCurrentUserId()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Not logged in")
                return@launch
            }
            try {
                val contacts = contactRepo.getContacts(userId)
                val profile = profileRepo.getProfile(userId)

                // Check driver first, then rider
                val driverRides = bookRepo.getMyRides(userId)
                val activeDriverRide = driverRides.firstOrNull { it.rideStatus == "in_progress" }

                val riderBookings = bookRepo.getOngoingRiderBookings(userId)
                val activeRiderBooking = riderBookings.firstOrNull {
                    it.rides?.rideStatus == "in_progress"
                } ?: riderBookings.firstOrNull()

                val isDriver = activeDriverRide != null
                val ride = activeDriverRide ?: activeRiderBooking?.rides

                _uiState.value = _uiState.value.copy(
                    booking = activeRiderBooking,
                    ride = ride,
                    isDriver = isDriver,
                    emergencyContacts = contacts,
                    riderName = profile?.user_name ?: "",
                    isLoading = false
                )

                if (ride != null) fetchRideRoute(ride)
                if (ride?.rideStatus == "in_progress") startPolling(ride.id, isDriver)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private fun fetchRideRoute(ride: Ride) {
        val oLat = ride.originLat ?: return
        val oLng = ride.originLng ?: return
        val dLat = ride.destinationLat ?: return
        val dLng = ride.destinationLng ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(routeState = ActiveRouteState.Loading)
            mapsRepo.getRoute(LatLng(oLat, oLng), LatLng(dLat, dLng)).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(routeState = ActiveRouteState.Success(it)) },
                onFailure = { _uiState.value = _uiState.value.copy(routeState = ActiveRouteState.Error(it.message ?: "Route failed")) }
            )
        }
    }

    // Every 20s: write my GPS, read the other person's GPS
    private fun startPolling(rideId: String, isDriver: Boolean) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val myLocation = mapsRepo.getDeviceLocation().getOrNull()
                    if (myLocation != null) {
                        if (isDriver) {
                            bookRepo.updateDriverLocation(rideId, myLocation.latitude, myLocation.longitude)
                            _uiState.value = _uiState.value.copy(driverLocation = myLocation)
                        } else {
                            bookRepo.updateRiderLocation(rideId, myLocation.latitude, myLocation.longitude)
                            _uiState.value = _uiState.value.copy(riderLocation = myLocation)
                        }
                    }
                    val liveRide = bookRepo.getRideLiveLocations(rideId)
                    if (liveRide != null) {
                        if (isDriver && liveRide.riderLiveLat != null && liveRide.riderLiveLng != null) {
                            _uiState.value = _uiState.value.copy(riderLocation = LatLng(liveRide.riderLiveLat, liveRide.riderLiveLng))
                        } else if (!isDriver && liveRide.driverLiveLat != null && liveRide.driverLiveLng != null) {
                            _uiState.value = _uiState.value.copy(driverLocation = LatLng(liveRide.driverLiveLat, liveRide.driverLiveLng))
                        }
                        if (liveRide.rideStatus == "completed") {
                            _uiState.value = _uiState.value.copy(rideEnded = true)
                            pollingJob?.cancel()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Polling error: ${e.message}")
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    override fun onCleared() {
        pollingJob?.cancel();
        super.onCleared()
    }
}

class ActiveRideViewModelFactory(
    private val client: SupabaseClient,
    private val mapsRepo: MapsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActiveRideViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ActiveRideViewModel(AuthRepository(client), BookRepository(client), EmergencyContactRepository(client), ProfileRepository(client), mapsRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
