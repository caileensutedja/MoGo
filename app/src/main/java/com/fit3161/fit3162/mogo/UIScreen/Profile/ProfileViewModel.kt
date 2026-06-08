package com.fit3161.fit3162.mogo.UIScreen.Profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.model.Location
import com.fit3161.fit3162.mogo.data.model.PresetDestinations
import com.fit3161.fit3162.mogo.data.repo.AuthRepository
import com.fit3161.fit3162.mogo.data.repo.ImageUploadRepository
import com.fit3161.fit3162.mogo.data.repo.ProfileRepository
import com.fit3161.fit3162.mogo.data.repo.UserProfile
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel and state definitions for the Profile screen.
 *
 * Manages loading, displaying, and updating the authenticated user's profile data,
 * including their avatar, personal details, role, and home campus.
 */



/**
 * UI state for the Profile screen.
 *
 * @property profile The authenticated user's profile data, or null if not yet loaded.
 * @property homeCampus The user's home campus as a [Location], or null if not set.
 * @property isLoading Whether profile data is currently being fetched.
 * @property error Error message to display if loading or updating fails.
 */
data class ProfileUiState(
    val profile: UserProfile? = null,
    val homeCampus: Location? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * ViewModel for the Profile screen.
 *
 * Fetches and exposes the user's profile, and handles updates to individual
 * profile fields and the profile avatar.
 *
 * @param authRepo Provides the current authenticated user's ID and Supabase client.
 * @param profileRepo Fetches and updates profile data in the remote database.
 * @param context Application context required for image upload operations.
 */
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

    /**
     * Uploads a new profile picture and updates the user's avatar URL.
     *
     * Uploads the image at [uri] to remote storage, saves the resulting URL
     * to the user's profile, then reloads the profile to reflect the change.
     *
     * @param uri The local URI of the image selected from the device gallery.
     */
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

    /**
     * Updates a single field on the authenticated user's profile.
     *
     * Writes the new value to the remote database, then reloads the profile.
     * For "home_campus", also updates [ProfileUiState.homeCampus] immediately.
     * For "user_role", updates the role in [ProfileUiState.profile] and invokes [onRoleChanged].
     *
     * @param field The field to update. One of: "name", "mobile", "gender", "user_role", "home_campus".
     * @param value The new value to save.
     * @param onRoleChanged Optional callback invoked after a role change is saved.
     */
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

/** Converts a campus name to a Location, using PresetDestinations as the source. */
fun locationFromCampusName(name: String): Location? {
    val preset = PresetDestinations.byName(name) ?: return null
    return Location(preset.name, preset.latLng)
}
