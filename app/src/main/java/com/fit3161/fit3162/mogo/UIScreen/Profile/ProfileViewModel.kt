package com.fit3161.fit3162.mogo.UIScreen.Profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.model.Location
import com.fit3161.fit3162.mogo.data.repo.AuthRepository
import com.fit3161.fit3162.mogo.data.repo.ImageUploadRepository
import com.fit3161.fit3162.mogo.data.repo.ProfileRepository
import com.fit3161.fit3162.mogo.data.repo.UserProfile
import com.fit3161.fit3162.mogo.data.repo.locationFromCampusName
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: UserProfile? = null,
    val homeCampus: Location? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class ProfileViewModel(
    private val authRepo: AuthRepository,
    private val profileRepo: ProfileRepository,
    private val context: Context // add outside the constructor, within function
) : ViewModel() {

    private val imageUploadRepo = ImageUploadRepository(authRepo.getSupabaseClient(), context)   // pass context

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun updateProfilePicture(uri: Uri) {
        viewModelScope.launch {
            val userId = authRepo.getCurrentUserId() ?: return@launch
            val avatarUrl = imageUploadRepo.uploadProfileImage(userId, uri)
            if (avatarUrl != null) {
                profileRepo.updateAvatarUrl(userId, avatarUrl)
                loadProfile()
            }
        }
    }

    fun updateField(field: String, value: String, onRoleChanged: () -> Unit = {}) {
        viewModelScope.launch {
            val userId = authRepo.getCurrentUserId() ?: return@launch
            when (field) {
                "name" -> profileRepo.updateProfile(userId, name = value)
                "mobile" -> profileRepo.updateProfile(userId, phone = value)
                "gender" -> profileRepo.updateProfile(userId, gender = value)
                "user_role" -> profileRepo.updateProfile(userId, user_role = value)
                "home_campus" -> {
                    val result = profileRepo.updateProfile(userId, home_campus = value)
//                    println("home_campus update result: $result")  // check Logcat
                    _uiState.update { it.copy(homeCampus = locationFromCampusName(value)) }
                }
            }
            loadProfile()
            if (field == "user_role") {
                _uiState.update { it.copy(profile = it.profile?.copy(user_role = value)) }
                onRoleChanged()  // called AFTER save completes
            }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true)
            }
            val userId = authRepo.getCurrentUserId()
            if (userId == null) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Not logged in")
                }
                return@launch
            }
            try {
                val profile = profileRepo.getProfile(userId)
                if (profile != null) {
                    _uiState.update {
//                        it.copy(profile = profile, isLoading = false)
                        it.copy(
                            profile = profile,
                            homeCampus = locationFromCampusName(profile.home_campus ?: ""),  // ← fix
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Profile not found") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
