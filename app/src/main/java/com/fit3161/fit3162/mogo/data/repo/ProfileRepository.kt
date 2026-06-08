package com.fit3161.fit3162.mogo.data.repo

import com.fit3161.fit3162.mogo.data.model.Location
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Repository for reading and writing user profile data in the MoGo application.
 *
 * Provides access to the "users" table in Supabase for profile retrieval and updates.
 */


/**
 * Represents a user's profile as stored in the remote database.
 *
 * @property user_id Unique identifier for the user.
 * @property user_email The user's Monash email address.
 * @property user_name The user's display name.
 * @property user_phone The user's mobile phone number.
 * @property user_gender The user's gender.
 * @property user_role The user's current role, either "rider" or "driver".
 * @property avatar_url URL of the user's profile picture, or null if not set.
 * @property home_campus The user's preferred Monash campus, or null if not set.
 */
@Serializable
data class UserProfile(
    val user_id: String,
    val user_email: String,
    val user_name: String,
    val user_phone: String,
    val user_gender: String,
    val user_role: String,
    val avatar_url: String? = null,
    val home_campus: String?
)

/**
 * Repository for accessing and updating user profile data.
 *
 * @param supabase The Supabase client used to query the "users" table.
 */
class ProfileRepository(private val supabase: SupabaseClient) {

    /**
     * Fetches the profile for the given user.
     *
     * @param userId The ID of the user to fetch.
     * @return The matching [UserProfile], or null if not found or an error occurs.
     */
    suspend fun getProfile(userId: String): UserProfile? {
        return try {
            supabase.from("users")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeSingleOrNull<UserProfile>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    /**
     * Updates the avatar URL for the given user.
     *
     * @param userId The ID of the user to update.
     * @param avatarUrl The new avatar URL to save.
     * @return [Result.success] on success, or [Result.failure] with the exception on error.
     */
    suspend fun updateAvatarUrl(userId: String, avatarUrl: String): Result<Unit> {
        return try {
            supabase.from("users")
                .update(buildJsonObject { put("avatar_url", avatarUrl) }) {
                    filter { eq("user_id", userId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    /**
     * Updates one or more profile fields for the given user.
     *
     * Only non-null parameters are included in the update. All fields are optional.
     *
     * @param userId The ID of the user to update.
     * @param name New display name, if changing.
     * @param phone New mobile number, if changing.
     * @param gender New gender value, if changing.
     * @param user_role New role ("rider" or "driver"), if changing.
     * @param home_campus New home campus name, if changing.
     * @return [Result.success] on success, or [Result.failure] with the exception on error.
     */
    suspend fun updateProfile(
        userId: String,
        name: String? = null,
        phone: String? = null,
        gender: String? = null,
        user_role: String? = null,
        home_campus: String? = null
    ): Result<Unit> {
        return try {
            val updates = buildJsonObject {
                name?.let { put("user_name", it) }
                phone?.let { put("user_phone", it) }
                gender?.let { put("user_gender", it) }
                user_role?.let {put("user_role", it)}
                home_campus?.let {put("home_campus", it)}
            }
            supabase.from("users")
                .update(updates) {
                    filter { eq("user_id", userId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}