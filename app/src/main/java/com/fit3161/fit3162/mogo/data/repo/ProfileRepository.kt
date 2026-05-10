package com.fit3161.fit3162.mogo.data.repo

import com.fit3161.fit3162.mogo.data.model.Location
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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

class ProfileRepository(private val supabase: SupabaseClient) {

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