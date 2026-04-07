package com.fit3161.fit3162.mogo.ui.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fit3161.fit3162.mogo.data.repo.MapsRepository


/**
 * Factory for MapsViewModel.
 * Follows the same pattern as BookViewModelFactory, OfferViewModelFactory, etc.
 */
class MapsViewModelFactory(
    private val repository: MapsRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
