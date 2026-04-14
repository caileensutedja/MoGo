package com.fit3161.fit3162.mogo.UIScreen.FutureRideScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.repo.BookRepository
import com.fit3161.fit3162.mogo.data.repo.Ride
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FutureRideUiState(
    val rides: List<Ride> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedDate: String = ""
)

class FutureRideViewModel() : ViewModel() {   // ← no parameters


    private val _uiState = MutableStateFlow(FutureRideUiState())
    val uiState: StateFlow<FutureRideUiState> = _uiState.asStateFlow()

    private var allRides: List<Ride> = emptyList()

    private val _selectedDate = MutableStateFlow("")
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedDestination = MutableStateFlow("")
    val selectedDestination: StateFlow<String> = _selectedDestination.asStateFlow()

    init {
        loadRides()
    }

    private fun loadRides() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Dummy rides – replace with repository.getFutureRides() later
                allRides = listOf(
                    Ride(
                        id = "1",
                        driverName = "Rice Tan",
                        carType = "Electric",
                        destination = "Clayton Campus",
                        eta = "12:00",
                        date = "15-04-2026",
                        totalSeats = 4,
                        availableSeats = 2
                    ),
                    Ride(
                        id = "2",
                        driverName = "John Lim",
                        carType = "Electric",
                        destination = "Caulfield Campus",
                        eta = "14:00",
                        date = "15-04-2026",
                        totalSeats = 4,
                        availableSeats = 3
                    ),
                    Ride(
                        id = "3",
                        driverName = "Sarah Lee",
                        carType = "Diesel",
                        destination = "Clayton Campus",
                        eta = "15:00",
                        date = "15-04-2026",
                        totalSeats = 5,
                        availableSeats = 1
                    )
                )
                filterRides()
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onDateSelected(date: String) {
        _selectedDate.value = date
        filterRides()
    }

    fun onDestinationSelected(destination: String) {
        _selectedDestination.value = destination
        filterRides()
    }

    private fun filterRides() {
        val filtered = allRides.filter { ride ->
            (selectedDate.value.isEmpty() || ride.date == selectedDate.value) &&
                    (selectedDestination.value.isEmpty() || ride.destination.contains(selectedDestination.value, ignoreCase = true))
        }
        _uiState.update {
            it.copy(rides = filtered, selectedDate = selectedDate.value)
        }
    }
}