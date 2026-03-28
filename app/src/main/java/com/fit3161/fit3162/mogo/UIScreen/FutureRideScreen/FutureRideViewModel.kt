package com.fit3161.fit3162.mogo.UIScreen.FutureRideScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.repo.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FutureRide(
    val id: String,
    val driverName: String,
    val carType: String,
    val totalSeats: Int,
    val availableSeats: Int,
    val eta: String,
    val date: String
)

data class FutureRideUiState(
    val selectedDate: String = "",
    val rides: List<FutureRide> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class FutureRideViewModel (private val repo: BookRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(FutureRideUiState())
    val uiState: StateFlow<FutureRideUiState> = _uiState.asStateFlow()

    fun onDateSelected(date: String) {
        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            isLoading = true,
            error = null
        )

        loadRidesByDate(date)
    }

    private fun loadRidesByDate(date: String) {
        viewModelScope.launch {
            try {
                // DELETE, FOR DUMMY
                val allRides = loadDummyRides()
                _uiState.value = _uiState.value.copy(
                    rides = allRides,
                    isLoading = false
                )

                // Uncomment
//                val rides = repo.getFutureRidesByDate(date)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
//}


    /**
     * TEMPORARY: Dummy data
     * TODO: Replace with repository call when backend is ready
     */
    init {
        val dummy = loadDummyRides()
        _uiState.value = _uiState.value.copy(
            rides = dummy
        )
    }
    private fun loadDummyRides(): List<FutureRide> {
//        return  emptyList<FutureRide>()
        return listOf(
            FutureRide(
                id = "1",
                driverName = "Rice Tan",
                carType = "Electric",
                totalSeats = 4,
                availableSeats = 2,
                eta = "12:00",
                date = "2026-04-13"
            ),
            FutureRide(
                id = "2",
                driverName = "John Lim",
                carType = "Electric",
                totalSeats = 4,
                availableSeats = 1,
                eta = "14:00",
                date = "2026-04-13"
            ),
            FutureRide(
                id = "3",
                driverName = "Sarah Lee",
                carType = "Diesel",
                totalSeats = 6,
                availableSeats = 3,
                eta = "15:00",
                date = "2026-04-13"
            )
        )
    }
}

class FutureRideViewModelFactory(
    private val repo: BookRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FutureRideViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FutureRideViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}