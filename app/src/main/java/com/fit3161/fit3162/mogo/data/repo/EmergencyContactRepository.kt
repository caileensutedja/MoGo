package com.fit3161.fit3162.mogo.data.repo

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Emergency Contact data class storing an individual's contact information.
 */
@Serializable
data class EmergencyContact(
    @SerialName("contact_id") val contactId: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("contact_name") val contactName: String,
    @SerialName("contact_phone") val contactPhone: String,
    @SerialName("notify_on_ride") val notifyOnRide: Boolean = true
)

class EmergencyContactRepository(private val supabase: SupabaseClient) {

    suspend fun getContacts(userId: String): List<EmergencyContact> {
        return try {
            supabase.from("emergency_contacts")
                .select { filter { eq("user_id", userId) } }
                .decodeList<EmergencyContact>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun addContact(contact: EmergencyContact): Result<Unit> {
        return try {
            supabase.from("emergency_contacts").insert(contact)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteContact(contactId: String): Result<Unit> {
        return try {
            supabase.from("emergency_contacts")
                .delete { filter { eq("contact_id", contactId) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}