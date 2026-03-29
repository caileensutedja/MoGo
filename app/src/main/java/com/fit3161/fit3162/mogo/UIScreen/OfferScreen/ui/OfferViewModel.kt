package com.fit3161.fit3162.mogo.UIScreen.OfferScreen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.repo.Offer
import com.fit3161.fit3162.mogo.data.repo.OfferRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OfferUiState(
    val offers: List<Offer> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class OfferViewModel (private val repo: OfferRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(OfferUiState())
    val uiState: StateFlow<OfferUiState> = _uiState.asStateFlow()

    private fun loadOffers() {
        viewModelScope.launch {
            try {
                // DELETE, FOR DUMMY
                val allOffers = loadDummyOffers()
                _uiState.value = _uiState.value.copy(
                    offers = allOffers,
                    isLoading = false
                )

                // Uncomment
//                val rides = repo.getOffers()

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
        val dummy = loadDummyOffers()
        _uiState.value = _uiState.value.copy(
            offers = dummy
        )
    }
    private fun loadDummyOffers(): List<Offer> {
//        return  emptyList<Offer>()
        return listOf(
            Offer(
                id = "1",
                title = "Welcome to Mogo ($5 off)",
                store = "MoGo",
                amount = 5,
                tc = "Only Applicable for the first ride",
                date = "2026-05-13"
            ),
            Offer(
                id = "2",
                title = "Graffalis is MoGo's new partner ($5 off)",
                store = "Graffalis",
                amount = 5,
                tc = "Must spend $15 for offer to be valid",
                date = "2026-04-23"
            ),
            Offer(
                id = "3",
                title = "Refer a Friend to Mogo ($10 off)",
                store = "MoGo",
                amount = 5,
                tc = "Offer a friend for $10 off",
                date = "2026-05-13"
            )
        )
    }
}


class OfferViewModelFactory(
    private val repo: OfferRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OfferViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OfferViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
