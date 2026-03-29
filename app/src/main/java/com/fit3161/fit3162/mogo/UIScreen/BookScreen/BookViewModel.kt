package com.fit3161.fit3162.mogo.UIScreen.BookScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.repo.BookRepository
import com.fit3161.fit3162.mogo.data.repo.Ride
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BookUIState(
    val rides: List<Ride> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class BookViewModel (private val repo: BookRepository) : ViewModel(){

    private val _uiState = MutableStateFlow(BookUIState())
    val uiState: StateFlow<BookUIState> = _uiState.asStateFlow()

    private fun loadBookedByDate() {
        viewModelScope.launch {
            try {
                // DELETE, FOR DUMMY
                val allRides = loadDummyRides()
                _uiState.value = _uiState.value.copy(
                    rides = allRides,
                    isLoading = false
                )

                // Uncomment
//                val rides = repo.getBookedRidesByDate()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

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
    private fun loadDummyRides(): List<Ride> {
//        return  emptyList<FutureRide>()
        return listOf(
            Ride(
                id = "1",
                driverName = "Janice Tan",
                carType = "Electric",
                totalSeats = 4,
                availableSeats = 2,
                destination = "LTB, Clayton Campus",
                eta = "12:00",
                date = "2026-04-13"
            ),
            Ride(
                id = "2",
                driverName = "John Doe",
                carType = "Electric",
                totalSeats = 4,
                availableSeats = 1,
                destination = "Building H, Caulfield Campus",
                eta = "14:00",
                date = "2026-04-13"
            ),
            Ride(
                id = "3",
                driverName = "Bob Harryson",
                carType = "Diesel",
                totalSeats = 6,
                availableSeats = 3,
                destination = "Sports Center, Clayton Campus",
                eta = "15:00",
                date = "2026-04-13"
            )
        )
    }
}


class BookViewModelFactory(
    private val repo: BookRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
