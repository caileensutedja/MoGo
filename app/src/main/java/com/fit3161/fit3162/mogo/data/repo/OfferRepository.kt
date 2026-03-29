package com.fit3161.fit3162.mogo.data.repo
data class Offer(
    val id: String,
    val title: String,
    val store: String,
    val amount: Int,
    val tc: String,
    val date: String
)
class OfferRepository {
}