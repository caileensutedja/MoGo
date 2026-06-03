package com.fit3161.fit3162.mogo.UIScreen.FutureRideScreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.repo.BookRepository
//import com.fit3161.fit3162.mogo.data.repo.CAMPUS_OPTIONS
import com.fit3161.fit3162.mogo.data.repo.MapsRepository
import com.fit3161.fit3162.mogo.data.repo.PlacesRepository
import com.fit3161.fit3162.mogo.data.repo.Ride
import com.google.android.gms.maps.model.LatLng
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FutureRideUiState(
    val selectedDate: String = "",
    val rides: List<Ride> = emptyList(),
    val hiddenRideIds: Set<String> = emptySet(),
    val selectedCampus: String? = null,
    val genderPreference: String? = null,
    val isLoading: Boolean = false,
    val bookingMessage: String? = null,
    val showBookSuccess: Boolean = false,
    val error: String? = null
) {
    val visibleRides get() = rides.filter { it.id !in hiddenRideIds }.filter { selectedCampus == null || it.destination == selectedCampus }
    val hiddenRides get() = rides.filter { it.id in hiddenRideIds }.filter { selectedCampus == null || it.destination == selectedCampus }
}

class FutureRideViewModel(
    private val repo: BookRepository,
    private val mapsRepo: MapsRepository,
    val placesRepo: PlacesRepository,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(FutureRideUiState())
    val uiState: StateFlow<FutureRideUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val pref = repo.getGenderPreference(userId)
                val hiddenIds = repo.getHiddenRideIds(userId)
                _uiState.value = _uiState.value.copy(genderPreference = pref, hiddenRideIds = hiddenIds)
                loadAllFutureRides()
            } catch (e: Exception) {
                Log.e("CRASH", "Init failed: ${e.stackTraceToString()}")
                _uiState.value = _uiState.value.copy(error = "Connection Failed: ${e.message}")
            }
        }
    }

    // Book at explicit pickup. Computes pickup→dest distance for carbon.
    fun bookRideAt(rideId: String, pickupLat: Double, pickupLng: Double, pickupName: String? = null) {
        viewModelScope.launch {
            try {
                val resolvedName = pickupName ?: mapsRepo.reverseGeocode(LatLng(pickupLat, pickupLng))
                val ride = _uiState.value.rides.firstOrNull { it.id == rideId }
                val destLat = ride?.destinationLat; val destLng = ride?.destinationLng
                var distanceMeters: Int? = null; var durationSeconds: Int? = null
                if (destLat != null && destLng != null) {
                    mapsRepo.getRoute(LatLng(pickupLat, pickupLng), LatLng(destLat, destLng)).onSuccess { distanceMeters = it.distanceMeters; durationSeconds = it.durationSeconds }
                }
                repo.bookRide(rideId = rideId, riderId = userId, pickupLocation = resolvedName, pickupLat = pickupLat, pickupLng = pickupLng, estimatedDistanceMeters = distanceMeters, estimatedDurationSeconds = durationSeconds)
                    .onSuccess { _uiState.value = _uiState.value.copy(showBookSuccess = true, rides = _uiState.value.rides.filter { it.id != rideId }) }
                    .onFailure { _uiState.value = _uiState.value.copy(error = "Booking failed: ${it.message}") }
            } catch (e: Exception) { _uiState.value = _uiState.value.copy(error = e.message) }
        }
    }

    // Book using current location. Reverse geocodes first.
    fun bookRideUsingCurrentLocation(rideId: String) {
        viewModelScope.launch {
            try {
                val pickup = mapsRepo.getDeviceLocation().getOrElse { _uiState.value = _uiState.value.copy(error = "Couldn't get location: ${it.message}"); return@launch }
                val pickupName = mapsRepo.reverseGeocode(pickup)
                bookRideAt(rideId, pickup.latitude, pickup.longitude, pickupName)
            } catch (e: Exception) { _uiState.value = _uiState.value.copy(error = e.message) }
        }
    }

    fun onDateSelected(date: String) { _uiState.value = _uiState.value.copy(selectedDate = date, isLoading = true, error = null); loadRidesByDate(date) }
    fun onDateCleared() { _uiState.value = _uiState.value.copy(selectedDate = "", isLoading = true, error = null); loadAllFutureRides() }
    fun onCampusSelected(campus: String?) { _uiState.value = _uiState.value.copy(selectedCampus = campus) }

    private fun loadAllFutureRides() {
        viewModelScope.launch {
            try {
                val rides = repo.getAllFutureRides(userId, _uiState.value.genderPreference)
                _uiState.value = _uiState.value.copy(
                    rides = rides,
                    isLoading = false,
                    showBookSuccess = _uiState.value.showBookSuccess
                )
            }
            catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
        }
    }

    fun loadRidesByDate(date: String) {
        viewModelScope.launch {
            Log.d("DATE_DEBUG", "Loading rides for date: $date")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {

                val rides = repo.getFutureRidesByDate(userId, date, _uiState.value.genderPreference)
                Log.d("DATE_DEBUG", "Got ${rides.size} rides")
                rides.forEach { Log.d("DATE_DEBUG", "Ride: ${it.departureTime}") }
                _uiState.value = _uiState.value.copy(rides = rides, isLoading = false)

            }
            catch (e: Exception) { _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
        }
    }

    fun hideRide(rideId: String) {
        _uiState.value = _uiState.value.copy(hiddenRideIds = _uiState.value.hiddenRideIds + rideId)
        viewModelScope.launch { try { repo.hideRide(userId, rideId) } catch (e: Exception) { _uiState.value = _uiState.value.copy(hiddenRideIds = _uiState.value.hiddenRideIds - rideId) } }
    }

    fun unhideRide(rideId: String) {
        _uiState.value = _uiState.value.copy(hiddenRideIds = _uiState.value.hiddenRideIds - rideId)
        viewModelScope.launch { try { repo.unhideRide(userId, rideId) } catch (e: Exception) { _uiState.value = _uiState.value.copy(hiddenRideIds = _uiState.value.hiddenRideIds + rideId) } }
    }

    // Book with named pickup (from PickupDialog). Computes distance for carbon.
    fun bookRide(ride: Ride, pickupName: String, pickupLat: Double, pickupLng: Double) {
        val dropoffLat = ride.destinationLat ?: 0.0
        val dropoffLng = ride.destinationLng ?: 0.0
        viewModelScope.launch {
            var distanceMeters: Int? = null;
            var durationSeconds: Int? = null
            mapsRepo.getRoute(LatLng(pickupLat, pickupLng), LatLng(dropoffLat, dropoffLng))
                .onSuccess {
                    distanceMeters = it.distanceMeters; durationSeconds = it.durationSeconds
                }
            val result = repo.bookRide(
                riderId = userId,
                rideId = ride.id,
                pickupLocation = pickupName,
                pickupLat = pickupLat,
                pickupLng = pickupLng,
                dropoffLocation = ride.destination,
                dropoffLat = dropoffLat,
                dropoffLng = dropoffLng,
                estimatedDistanceMeters = distanceMeters,
                estimatedDurationSeconds = durationSeconds
            )
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(showBookSuccess = true)
                if (_uiState.value.selectedDate.isNotEmpty()) loadRidesByDate(_uiState.value.selectedDate)
                else loadAllFutureRides()
            } else {
                _uiState.value = _uiState.value.copy(
                    bookingMessage = result.exceptionOrNull()?.message ?: "Booking failed"
                )
            }
        }
    }

    fun clearBookingMessage() { _uiState.value = _uiState.value.copy(bookingMessage = null) }
    fun refresh() {
        if (_uiState.value.selectedDate.isNotEmpty()) loadRidesByDate(_uiState.value.selectedDate)
        else loadAllFutureRides()
    }
    fun dismissBookSuccess() {
        _uiState.value = _uiState.value.copy(showBookSuccess = false)
    }
}

class FutureRideViewModelFactory(
    private val client: SupabaseClient, private val mapsRepo: MapsRepository, private val placesRepo: PlacesRepository, private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FutureRideViewModel::class.java)) { @Suppress("UNCHECKED_CAST") return FutureRideViewModel(BookRepository(client), mapsRepo, placesRepo, userId) as T }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
