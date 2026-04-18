package com.fit3161.fit3162.mogo.UIScreen.BookScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.repo.BookRepository
import com.fit3161.fit3162.mogo.data.repo.Booking
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


data class BookUIState(
    val bookings: List<Booking> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class BookViewModel (
    private val repo: BookRepository,
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
                _uiState.value = _uiState.value.copy(
                    bookings = bookings,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}


class BookViewModelFactory(
    private val client: SupabaseClient,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookViewModel(BookRepository(client), userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
