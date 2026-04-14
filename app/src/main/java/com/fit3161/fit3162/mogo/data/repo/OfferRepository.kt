package com.fit3161.fit3162.mogo.data.repo

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable


@Serializable
data class Offer(
    @SerialName("offer_id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("business_id") val businessId: String,
    @SerialName("offer_code") val offerCode: String,
    @SerialName("discount_amount") val amount: Double? = null,
    @SerialName("expiry") val date: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("time_created") val timeCreated: String? = null,
    val businesses: Business? = null
)
@Serializable
data class Business(
    @SerialName("business_name") val name: String
)
class OfferRepository(private val client: SupabaseClient) {
    suspend fun getOffers(): List<Offer> {
        return client
            .from("offers")
            .select(Columns.raw("*, businesses(business_name)")) {
                filter {
                    eq("is_active", true)
                }
            }
            .decodeList<Offer>()
    }

}