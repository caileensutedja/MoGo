package com.fit3161.fit3162.mogo.UIScreen.MyRides

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

data class MyRidesUiState(
    val rides: List<Ride> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class MyRidesViewModel(
    private val repo: BookRepository,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyRidesUiState())
    val uiState: StateFlow<MyRidesUiState> = _uiState.asStateFlow()

    init {
        loadMyRides()
    }

    private fun loadMyRides() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val rides = repo.getMyRides(userId)
                // Sort: scheduled (active) first, then completed, then others
                val sortedRides = rides.sortedBy { ride ->
                    when (ride.rideStatus) {
                        "scheduled" -> 0
                        "completed" -> 1
                        else -> 2
                    }
                }
                _uiState.value = _uiState.value.copy(rides = sortedRides, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun cancelRide(rideId: String) {
        _uiState.value = _uiState.value.copy(
            rides = _uiState.value.rides.filter { it.id != rideId }
        )
        viewModelScope.launch {
            val result = repo.cancelRide(rideId)
            if (result.isFailure) {
                loadMyRides()
                _uiState.value = _uiState.value.copy(error = "Failed to cancel ride")
            }
        }
    }
}

class MyRidesViewModelFactory(
    private val client: SupabaseClient,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyRidesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MyRidesViewModel(BookRepository(client), userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}