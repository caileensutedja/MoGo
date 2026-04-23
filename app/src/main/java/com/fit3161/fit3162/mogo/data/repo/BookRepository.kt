package com.fit3161.fit3162.mogo.data.repo;

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RideUser(
        @SerialName("user_id")            val userId: String,
        @SerialName("user_name")          val userName: String,
        @SerialName("user_phone")         val userPhone: String,
        @SerialName("user_email")         val userEmail: String,
        @SerialName("user_dob")           val userDob: String? = null,
        @SerialName("user_gender")        val userGender: String,
        @SerialName("user_role")          val userRole: String,
        @SerialName("driver_rating")      val driverRating: Double? = null,
        @SerialName("home_campus")        val homeCampus: String? = null,
        @SerialName("time_created")       val timeCreated: String? = null,
        @SerialName("updated_at")         val updatedAt: String? = null,
        @SerialName("gender_preference")  val genderPreference: String? = null
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
        @SerialName("vehicle_id")       val vehicleId: String? = null,  // make nullable for now
        @SerialName("origin")           val origin: String,
        @SerialName("destination")      val destination: String,
        @SerialName("ride_status")      val rideStatus: String,
        @SerialName("available_seats")  val availableSeats: Int,
        @SerialName("departure_time")   val departureTime: String,
        @SerialName("carbon_estimate")  val carbonEstimate: Double? = null,
        @SerialName("is_recurring")     val isRecurring: Boolean,
        @SerialName("time_created")     val timeCreated: String? = null,
        @SerialName("vehicle_type")     val vehicleType: String,
        @SerialName("plate_number")     val plateNumber: String,
        val users: RideUser? = null,
        val vehicles: Vehicle? = null
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
@Serializable
data class HiddenRide(
        @SerialName("ride_id") val rideId: String
)
@Serializable
data class BookedRideId(
        @SerialName("ride_id") val rideId: String
)
class BookRepository(private val client: SupabaseClient) {

        /**
         * UPLOAD RIDE
         */
        suspend fun uploadRide(ride: Ride): Result<Unit> {
                return try {
                        client.from("rides").insert(ride)
                        Result.success(Unit)
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Upload failed", e)
                        Result.failure(e)
                }
        }
        //TODO: Incorporate Vehicles
        suspend fun getUserVehicles(userId: String): List<Vehicle> {
                return client.from("vehicles")
                        .select() {
                                filter { eq("driver_id", userId) }
                        }
                        .decodeList<Vehicle>()
        }

        /**
         * BOOKED RIDES
         */
        // BookScreen: current user's confirmed bookings, with ride + driver + vehicle
        suspend fun getBookedRides(userId: String): List<Booking> {
                return client
                        .from("bookings")
                        .select(Columns.raw("*, rides(*, users(*), vehicles(*))")) {
                                filter {
                                        eq("rider_id", userId)
                                        eq("booking_status", "confirmed")
                                }
                                order("time_created", Order.ASCENDING) // Change it to ride's date
                        }
                        .decodeList<Booking>()
        }

        /**
         * FUTURE RIDES
         */
        suspend fun getAllFutureRides(userId: String, genderPreference: String? = null): List<Ride> {
                val now = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
                        .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)

                val bookedRideIds = client
                        .from("bookings")
                        .select(Columns.raw("ride_id")) {
                                filter { eq("rider_id", userId) }
                        }
                        .decodeList<HiddenRide>() // reuse HiddenRide since it's just a ride_id wrapper
                        .map { it.rideId }
                        .toSet()

                return client
                        .from("rides")
                        .select(Columns.raw("*, users(*), vehicles(*)")) {
                                filter {
                                        and {
                                                gte("departure_time", now)
                                                eq("ride_status", "scheduled")
                                                gt("available_seats", 0)
                                                neq("driver_id",userId)
                                                // Filter by driver gender at DB level if preference is set
                                                if (genderPreference != null) {
                                                        eq("users.user_gender", genderPreference)
                                                }
                                        }
                                }
                                order("departure_time", Order.ASCENDING) // Ascending order from Now
                        }
                        .decodeList<Ride>()
                        .filter { it.id !in bookedRideIds }
        }

        suspend fun getFutureRidesByDate(userId: String, date: String, genderPreference: String? = null): List<Ride> {
                Log.d("DATE", "Date given is: ${date}")
                Log.d("DATE", "Date given converted gta is: ${date}T00:00:00+10:00")
                Log.d("DATE", "Date given converted gta is: ${date}T23:59:59+10:00")
                val bookedRideIds = client
                        .from("bookings")
                        .select(Columns.raw("ride_id")) {
                                filter { eq("rider_id", userId) }
                        }
                        .decodeList<HiddenRide>() // reuse HiddenRide since it's just a ride_id wrapper
                        .map { it.rideId }
                        .toSet()

                return client
                        .from("rides")
                        .select(Columns.raw("*, users(*), vehicles(*)")) {
                                filter {
                                        and {
                                                gte("departure_time", "${date}T00:00:00+00:00")
                                                lte("departure_time", "${date}T23:59:59+00:00")
                                                eq("ride_status", "scheduled")
                                                gt("available_seats", 0)
                                                neq("driver_id",userId)
                                                // Filter by driver gender at DB level if preference is set
                                                if (genderPreference != null) {
                                                        eq("users.user_gender", genderPreference)
                                                }
                                        }
                                }
                                order("departure_time", Order.ASCENDING)
                        }
                        .decodeList<Ride>()
                        .filter { it.id !in bookedRideIds }
        }

        /**
         * USER GENDER PREFERENCE
         */
        @Serializable
        data class UserPreference(
                @SerialName("user_id") val userId: String,
                @SerialName("gender_preference") val genderPreference: String? = null
        )
        suspend fun getGenderPreference(userId: String): String? {
                return client
                        .from("users")
                        .select(Columns.raw("user_id, gender_preference")) {
                                filter { eq("user_id", userId) }
                                limit(1)
                        }
                        .decodeSingleOrNull<UserPreference>()  // ← only change this line
                        ?.genderPreference
        }

        /**
         * MY RIDES
         */
        /**
         * MY RIDES (Driver's uploaded rides)
         */
        suspend fun getMyRides(userId: String): List<Ride> {
                return client
                        .from("rides")
                        .select(Columns.raw("*, vehicles(*)")) {
                                filter { eq("driver_id", userId) }
                                order("departure_time", Order.ASCENDING)
                        }
                        .decodeList<Ride>()
        }

        suspend fun cancelRide(rideId: String): Result<Unit> {
                return try {
                        client.from("rides")
                                .delete {
                                        filter { eq("ride_id", rideId) }
                                }
                        Result.success(Unit)
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Cancel ride failed", e)
                        Result.failure(e)
                }
        }

        /**
         * HIDE USER
         * Below are functions to hide ride options
         */
        suspend fun hideRide(userId: String, rideId: String) {
                try {
                        Log.d("MOGO_DEBUG", "Attempting to hide ride: $rideId for user: $userId")

                        client.from("hidden_rides").insert(mapOf(
                                "user_id" to userId,
                                "ride_id" to rideId
                        ))

                        Log.d("MOGO_DEBUG", "Successfully hidden ride!")
                } catch (e: Exception) {
                        // This will print the exact SQL error (e.g., Foreign Key violation, 403 Forbidden, etc.)
                        Log.e("MOGO_DEBUG", "FAILED to hide ride. Error: ${e.message}")
                        Log.e("MOGO_DEBUG", "Full StackTrace: ${e.stackTraceToString()}")
                }
        }

        suspend fun unhideRide(userId: String, rideId: String) {
                client
                        .from("hidden_rides")
                        .delete {
                                filter {
                                        and {
                                                eq("user_id", userId)
                                                eq("ride_id", rideId)
                                        }
                                }
                        }
        }

        suspend fun getHiddenRideIds(userId: String): Set<String> {
                return client
                        .from("hidden_rides")
                        .select(Columns.raw("ride_id")) {
                                filter { eq("user_id", userId) }
                        }
                        .decodeList<HiddenRide>()
                        .map { it.rideId }
                        .toSet()
        }

        /**
         * RIDER HISTORY - Completed past bookings
         */
        suspend fun getRiderHistory(userId: String): List<Booking> {
                return try {
                        client
                                .from("bookings")
                                .select(Columns.raw("*, rides(*, users(*), vehicles(*))")) {
                                        filter {
                                                and {
                                                        eq("rider_id", userId)
                                                        eq("booking_status", "completed")
                                                }
                                        }
                                        order("time_created", Order.DESCENDING)  // most recent first
                                }
                                .decodeList<Booking>()
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Failed to get rider history", e)
                        emptyList()
                }
        }

        /**
         * DRIVER HISTORY - Completed past rides
         */
        suspend fun getDriverHistory(userId: String): List<Ride> {
                return try {
                        client
                                .from("rides")
                                .select(Columns.raw("*, users(*), vehicles(*)")) {
                                        filter {
                                                and {
                                                        eq("driver_id", userId)
                                                        eq("ride_status", "completed")
                                                }
                                        }
                                        order("departure_time", Order.DESCENDING)  // most recent first
                                }
                                .decodeList<Ride>()
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Failed to get driver history", e)
                        emptyList()
                }
        }

        /**
         * ONGOING/CONFIRMED RIDER BOOKINGS (not yet completed)
         */
        suspend fun getOngoingRiderBookings(userId: String): List<Booking> {
                return try {
                        client
                                .from("bookings")
                                .select(Columns.raw("*, rides(*, users(*), vehicles(*))")) {
                                        filter {
                                                and {
                                                        eq("rider_id", userId)
                                                        eq("booking_status", "confirmed")
                                                }
                                        }
                                        order("time_created", Order.ASCENDING)
                                }
                                .decodeList<Booking>()
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Failed to get ongoing bookings", e)
                        emptyList()
                }
        }

        /**
         * UPCOMING DRIVER RIDES (scheduled, not yet completed)
         */
        suspend fun getUpcomingDriverRides(userId: String): List<Ride> {
                val now = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
                        .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)

                return try {
                        client
                                .from("rides")
                                .select(Columns.raw("*, users(*), vehicles(*)")) {
                                        filter {
                                                and {
                                                        eq("driver_id", userId)
                                                        eq("ride_status", "scheduled")
                                                        gte("departure_time", now)  // only future rides
                                                }
                                        }
                                        order("departure_time", Order.ASCENDING)
                                }
                                .decodeList<Ride>()
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Failed to get upcoming driver rides", e)
                        emptyList()
                }
        }
}

