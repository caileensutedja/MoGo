package com.fit3161.fit3162.mogo.UIScreen.BookScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.repo.BookRepository
import com.fit3161.fit3162.mogo.data.repo.Booking
import com.fit3161.fit3162.mogo.data.repo.MapsRepository
import com.fit3161.fit3162.mogo.data.repo.Ride
import com.fit3161.fit3162.mogo.data.repo.RideUser
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BookUIState(
    val bookings: List<Booking> = emptyList(),
    val rides: List<MapsRepository.RideWithDetour> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class BookViewModel(
    private val repo: BookRepository,
    private val mapsRepo: MapsRepository,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookUIState())
    val uiState: StateFlow<BookUIState> = _uiState.asStateFlow()

    init {
        loadBookedRides()
    }

    private fun loadBookedRides() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val bookings = repo.getBookedRides(userId)
                _uiState.value = _uiState.value.copy(bookings = bookings, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadRides(rider: RideUser, pickupLat: Double, pickupLng: Double, date: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val genderPref = repo.getGenderPreference(userId)
                val alreadyBooked = repo.getBookedRideIds(userId)
                val blockedByRider = emptySet<String>()
                val blockedByDriver = emptySet<String>()

                // 1. DB query
                val candidates = if (date != null)
                    repo.getFutureRidesByDate(userId, date, genderPref ?: "")   // ✅ Added userId
                else
                    repo.getAllFutureRides(userId, genderPref ?: "")

                // 2. Hard memory filters
                val hardFiltered = candidates.filter { ride ->
                    repo.passesHardMemoryFilters(ride, userId, rider, alreadyBooked, blockedByRider, blockedByDriver)
                }

                // 3. Soft filters
                val softFiltered = hardFiltered.filter { ride ->
                    repo.passesSoftFilters(ride, userId, rider)
                }

                // 4. Radius pre-filter
                val inRadius = softFiltered.filter { ride ->
                    val oLat = ride.originLat ?: return@filter false
                    val oLng = ride.originLng ?: return@filter false
                    repo.isWithinRadiusKm(pickupLat, pickupLng, oLat, oLng, 5.0)
                }

                // 5. Detour check — strict first, relax if empty
                var withDetour = checkDetours(inRadius, pickupLat, pickupLng, maxDetourKm = 5.0)
                if (withDetour.isEmpty()) {
                    withDetour = checkDetours(inRadius, pickupLat, pickupLng, maxDetourKm = 10.0)
                }

                // 6. Sort
                val sorted = repo.sortRides(withDetour)
                _uiState.value = _uiState.value.copy(rides = sorted, isLoading = false)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private suspend fun checkDetours(
        rides: List<Ride>,
        pickupLat: Double,
        pickupLng: Double,
        maxDetourKm: Double
    ): List<MapsRepository.RideWithDetour> {
        return rides.mapNotNull { ride ->
            val detour = mapsRepo.computeDetour(ride, pickupLat, pickupLng) ?: return@mapNotNull null
            if (detour.addedKm <= maxDetourKm && detour.addedMinutes <= 20) {
                MapsRepository.RideWithDetour(ride, detour.addedKm, detour.addedMinutes)
            } else null
        }
    }
}

class BookViewModelFactory(
    private val client: SupabaseClient,
    private val mapsRepo: MapsRepository,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookViewModel(BookRepository(client), mapsRepo, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}