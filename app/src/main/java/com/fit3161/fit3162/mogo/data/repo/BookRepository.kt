package com.fit3161.fit3162.mogo.data.repo;

data class Ride(
        val id: String,
        val driverName: String,
        val carType: String,
        val totalSeats: Int,
        val availableSeats: Int,
        val destination: String,
        val eta: String,
        val date: String
)
public class BookRepository {
    //getfutureridesbydate (input: date)
}
