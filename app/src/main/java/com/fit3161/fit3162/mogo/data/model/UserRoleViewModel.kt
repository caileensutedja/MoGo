package com.fit3161.fit3162.mogo.data.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.repo.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel that provides the current user's role (rider/driver)
 * to the bottom bar so it can show the correct tabs.
 *
 * Lives in MainActivity so it persists across screen navigations.
 */
class UserRoleViewModel(
    private val profileRepo: ProfileRepository,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _userRole = MutableStateFlow("")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    // Fetches the user's role from the database.
    fun fetchRole() {
        viewModelScope.launch {
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
            _userRole.value = profileRepo.getProfile(userId)?.user_role ?: ""
        }
    }
}

class UserRoleViewModelFactory(
    private val profileRepo: ProfileRepository,
    private val supabase: SupabaseClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return UserRoleViewModel(profileRepo, supabase) as T
    }
}