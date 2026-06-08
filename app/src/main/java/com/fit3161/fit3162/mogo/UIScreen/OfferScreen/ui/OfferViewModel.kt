package com.fit3161.fit3162.mogo.UIScreen.OfferScreen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.repo.OfferRepository
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel and state definitions for the Offers screen.
 *
 * Manages fetching and exposing the list of available offers
 * for the current user.
 */


data class OfferUiState(
    val offers: com.fit3161.fit3162.mogo.data.repo.Offer? = null,
    val offersList: List<com.fit3161.fit3162.mogo.data.repo.Offer> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for the Offers screen.
 *
 * Fetches the list of available offers from [OfferRepository]
 * and exposes them via [uiState].
 *
 * @param repo Repository used to retrieve offer data.
 */
class OfferViewModel(private val repo: OfferRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(OfferUiState())
    val uiState: StateFlow<OfferUiState> = _uiState.asStateFlow()

    init {
        loadOffers()
    }

    private fun loadOffers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val offers = repo.getOffers()
                _uiState.value = _uiState.value.copy(
                    offersList = offers,
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

/**
 * Factory for creating [OfferViewModel] instances.
 *
 * Constructs an [OfferRepository] from the provided [SupabaseClient]
 * and injects it into the ViewModel.
 *
 * @param client The Supabase client used to initialise the repository.
 */
class OfferViewModelFactory(
    private val client: SupabaseClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OfferViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OfferViewModel(OfferRepository(client)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
