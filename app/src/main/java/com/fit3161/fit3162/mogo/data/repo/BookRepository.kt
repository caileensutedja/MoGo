package com.fit3161.fit3162.mogo.data.repo;

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

//data class Ride(
//        val id: String,
//        val driverName: String,
//        val carType: String,
//        val totalSeats: Int,
//        val availableSeats: Int,
//        val destination: String,
//        val eta: String,
//        val date: String
//)
//public class BookRepository {
//    //getfutureridesbydate (input: date)
//}

@Serializable
data class RideUser(
        @SerialName("user_id")    val userId: String,
        @SerialName("user_name")  val userName: String,
        @SerialName("user_email") val userEmail: String? = null,
        @SerialName("user_phone") val userPhone: String? = null
        // Continue here
)
@Serializable
data class Vehicle(
        @SerialName("vehicle_id")       val vehicleId: String,
        @SerialName("vehicle_make")     val vehicleMake: String,
        @SerialName("vehicle_model")    val vehicleModel: String? = null,
        @SerialName("vehicle_type")     val vehicleType: String,
        @SerialName("vehicle_colour")   val vehicleColour: String,
        @SerialName("vehicle_capacity") val vehicleCapacity: Int,
        @SerialName("plate_number")     val plateNumber: String
)
@Serializable
data class Ride(
        @SerialName("ride_id")          val id: String,
        @SerialName("driver_id")        val driverId: String,
        @SerialName("vehicle_id")       val vehicleId: String,
        @SerialName("origin")           val origin: String,
        @SerialName("destination")      val destination: String,
        @SerialName("ride_status")      val rideStatus: String,
        @SerialName("available_seats")  val availableSeats: Int,
        @SerialName("departure_time")   val departureTime: String,
        @SerialName("carbon_estimate")  val carbonEstimate: Double? = null,
        @SerialName("is_recurring")     val isRecurring: Boolean,
        @SerialName("time_created")     val timeCreated: String? = null,
        val users: RideUser? = null,    // joined driver info
        val vehicles: Vehicle? = null   // joined vehicle info
)

@Serializable
data class Booking(
        @SerialName("booking_id")       val id: String,
        @SerialName("ride_id")          val rideId: String,
        @SerialName("rider_id")         val riderId: String,
        @SerialName("pickup_location")  val pickupLocation: String,
        @SerialName("dropoff_location") val dropoffLocation: String,
        @SerialName("seats_booked")     val seatsBooked: Int,
        @SerialName("booking_status")   val bookingStatus: String,
        @SerialName("time_created")     val timeCreated: String? = null,
        val rides: Ride? = null         // joined ride data
)

class BookRepository(private val client: SupabaseClient) {

        // BookScreen: current user's confirmed bookings, with ride + driver + vehicle
        suspend fun getBookedRides(userId: String): List<Booking> {
                return client
                        .from("bookings")
                        .select(Columns.raw("*, rides(*, users(*), vehicles(*))")) {
                                filter {
                                        eq("rider_id", userId)
                                        eq("booking_status", "confirmed") // adjust to your enum value
                                }
                        }
                        .decodeList<Booking>()
        }

        // FutureRideScreen: available rides on a given date, with driver + vehicle
        suspend fun getFutureRidesByDate(date: String): List<Ride> {
                return client
                        .from("rides")
                        .select(Columns.raw("*, users(*), vehicles(*)")) {
                                filter {
                                        gte("departure_time", "${date}T00:00:00Z")
                                        lte("departure_time", "${date}T23:59:59Z")
                                        eq("ride_status", "scheduled") // adjust to your enum value
                                        gt("available_seats", 0)
                                }
                        }
                        .decodeList<Ride>()
        }
}