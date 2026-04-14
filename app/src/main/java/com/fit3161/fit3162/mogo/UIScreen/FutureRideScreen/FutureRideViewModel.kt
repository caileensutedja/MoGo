package com.fit3161.fit3162.mogo.UIScreen.FutureRideScreen

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
    val isLoading: Boolean = false,
    val error: String? = null
)

class FutureRideViewModel (private val repo: BookRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(FutureRideUiState())
    val uiState: StateFlow<FutureRideUiState> = _uiState.asStateFlow()

    init {
        loadAllFutureRides()
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
                val rides = repo.getAllFutureRides()
                _uiState.value = _uiState.value.copy(rides = rides, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private fun loadRidesByDate(date: String) {
        viewModelScope.launch {
            try {
                val rides = repo.getFutureRidesByDate(date)
                _uiState.value = _uiState.value.copy(rides = rides, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}

class FutureRideViewModelFactory(
    private val client: SupabaseClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FutureRideViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FutureRideViewModel(BookRepository(client)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}