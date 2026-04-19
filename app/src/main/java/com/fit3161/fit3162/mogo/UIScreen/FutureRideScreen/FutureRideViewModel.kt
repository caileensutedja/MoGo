package com.fit3161.fit3162.mogo.UIScreen.FutureRideScreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.repo.BookRepository
import com.fit3161.fit3162.mogo.data.repo.Ride
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FutureRideUiState(
    val selectedDate: String = "",
    val rides: List<Ride> = emptyList(),
    val hiddenRideIds: Set<String> = emptySet(),
    val genderPreference: String? = null, // Filters gender preference
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val visibleRides: List<Ride>
        get() = rides.filter { it.id !in hiddenRideIds }

    val hiddenRides: List<Ride>
        get() = rides.filter { it.id in hiddenRideIds }
}


class FutureRideViewModel (
    private val repo: BookRepository,
    private val userId: String) : ViewModel() {

    private val _uiState = MutableStateFlow(FutureRideUiState())
    val uiState: StateFlow<FutureRideUiState> = _uiState.asStateFlow()

    init {
//        viewModelScope.launch {
//            Log.d("PASS", "init: $userId")
//            // Load preference and hidden ride IDs first, then fetch rides
//            val pref = repo.getGenderPreference(userId)
//            Log.d("PASS", "Preference is: ${pref}")
//            val hiddenIds = repo.getHiddenRideIds(userId)
//            Log.d("PASS", "Hidden ID is: ${hiddenIds}")
//            _uiState.value = _uiState.value.copy(
//                genderPreference = pref,
//                hiddenRideIds = hiddenIds
//            )
//            loadAllFutureRides()
//        }
        viewModelScope.launch {
            try {
                val pref = repo.getGenderPreference(userId)
                val hiddenIds = repo.getHiddenRideIds(userId)
                _uiState.value = _uiState.value.copy(
                    genderPreference = pref,
                    hiddenRideIds = hiddenIds
                )
                loadAllFutureRides()
            } catch (e: Exception) {
                Log.e("CRASH", "Init failed. Full Error: ${e.stackTraceToString()}") // This gives the full story
                _uiState.value = _uiState.value.copy(error = "Connection Failed: ${e.message}")
            }
        }
    }


    fun onDateSelected(date: String) {
        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            isLoading = true,
            error = null
        )
        loadRidesByDate(date)
    }

    fun onDateCleared() {
        _uiState.value = _uiState.value.copy(
            selectedDate = "",
            isLoading = true,
            error = null
        )
        loadAllFutureRides()
    }

    private fun loadAllFutureRides() {
        viewModelScope.launch {
            try {
                val rides = repo.getAllFutureRides(
                    userId,
                    genderPreference = _uiState.value.genderPreference
                )
                _uiState.value = _uiState.value.copy(rides = rides, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private fun loadRidesByDate(date: String) {
        viewModelScope.launch {
            try {
                val rides = repo.getFutureRidesByDate(
                    userId,
                    date,
                    genderPreference = _uiState.value.genderPreference)
                _uiState.value = _uiState.value.copy(rides = rides, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun hideRide(rideId: String) {
        // Optimistically update UI immediately
        _uiState.value = _uiState.value.copy(
            hiddenRideIds = _uiState.value.hiddenRideIds + rideId
        )
        viewModelScope.launch {
            try {
                repo.hideRide(userId, rideId)
                Log.d("HIDE", "Success to hide ride: $rideId")
            } catch (e: Exception) {
                // Revert if DB call fails
                _uiState.value = _uiState.value.copy(
                    hiddenRideIds = _uiState.value.hiddenRideIds - rideId
                )
                Log.d("HIDE", "Failed to hide ride: ${e.message}")
            }
        }
    }

    fun unhideRide(rideId: String) {
        // Optimistically update UI immediately
        _uiState.value = _uiState.value.copy(
            hiddenRideIds = _uiState.value.hiddenRideIds - rideId
        )
        viewModelScope.launch {
            try {
                repo.unhideRide(userId, rideId)
            } catch (e: Exception) {
                // Revert if DB call fails
                _uiState.value = _uiState.value.copy(
                    hiddenRideIds = _uiState.value.hiddenRideIds + rideId
                )
                Log.d("HIDE", "Failed to unhide ride: ${e.message}")
            }
        }
    }

}

class FutureRideViewModelFactory(
    private val client: SupabaseClient,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FutureRideViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FutureRideViewModel(BookRepository(client), userId) as T  // ← pass it
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}