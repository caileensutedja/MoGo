package com.fit3161.fit3162.mogo.data.repo

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Serializable
data class RideUser(
        @SerialName("user_id") val userId: String,
        @SerialName("user_name") val userName: String? = null,
        @SerialName("user_phone") val userPhone: String? = null,
        @SerialName("user_email") val userEmail: String? = null,
        @SerialName("user_dob") val userDob: String? = null,
        @SerialName("user_gender") val userGender: String? = null,
        @SerialName("user_role") val userRole: String? = null,
        @SerialName("driver_rating") val driverRating: Double? = null,
        @SerialName("home_campus") val homeCampus: String? = null,
        @SerialName("time_created") val timeCreated: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null,
        @SerialName("gender_preference") val genderPreference: String? = null
)

@Serializable
data class BookedRideId(
        @SerialName("ride_id") val rideId: String
)

@Serializable
data class Vehicle(
        @SerialName("vehicle_id") val vehicleId: String,
        @SerialName("vehicle_make") val vehicleMake: String,
        @SerialName("vehicle_model") val vehicleModel: String? = null,
        @SerialName("vehicle_type") val vehicleType: String,
        @SerialName("vehicle_colour") val vehicleColour: String,
        @SerialName("vehicle_capacity") val vehicleCapacity: Int,
        @SerialName("plate_number") val plateNumber: String
)

@Serializable
data class Ride(
        @SerialName("ride_id") val id: String,
        @SerialName("driver_id") val driverId: String,
        @SerialName("vehicle_id") val vehicleId: String? = null,
        @SerialName("origin") val origin: String,
        @SerialName("destination") val destination: String,
        @SerialName("origin_lat") val originLat: Double? = null,
        @SerialName("origin_lng") val originLng: Double? = null,
        @SerialName("destination_lat") val destinationLat: Double? = null,
        @SerialName("destination_lng") val destinationLng: Double? = null,
        @SerialName("ride_status") val rideStatus: String,
        @SerialName("available_seats") val availableSeats: Int,
        @SerialName("departure_time") val departureTime: String,
        @SerialName("carbon_estimate") val carbonEstimate: Double? = null,
        @SerialName("is_recurring") val isRecurring: Boolean,
        @SerialName("time_created") val timeCreated: String? = null,
        @SerialName("vehicle_type") val vehicleType: String,
        @SerialName("plate_number") val plateNumber: String,
        val users: RideUser? = null,
        val vehicles: Vehicle? = null
)

@Serializable
data class Booking(
        @SerialName("booking_id") val id: String,
        @SerialName("ride_id") val rideId: String,
        @SerialName("rider_id") val riderId: String,
        @SerialName("pickup_location") val pickupLocation: String,
        @SerialName("dropoff_location") val dropoffLocation: String,
        @SerialName("seats_booked") val seatsBooked: Int,
        @SerialName("booking_status") val bookingStatus: String,
        @SerialName("time_created") val timeCreated: String? = null,
        val rides: Ride? = null
)

@Serializable
data class HiddenRide(
        @SerialName("ride_id") val rideId: String
)

class BookRepository(private val client: SupabaseClient) {

        suspend fun uploadRide(ride: Ride): Result<Unit> {
                return try {
                        client.from("rides").insert(ride)
                        Result.success(Unit)
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Upload failed", e)
                        Result.failure(e)
                }
        }

        suspend fun getUserVehicles(userId: String): List<Vehicle> {
                return client.from("vehicles")
                        .select() { filter { eq("driver_id", userId) } }
                        .decodeList<Vehicle>()
        }

        suspend fun getBookedRideIds(userId: String): Set<String> {
                return client
                        .from("bookings")
                        .select(Columns.raw("ride_id")) {
                                filter {
                                        eq("rider_id", userId)
                                        neq("booking_status", "cancelled")
                                }
                        }
                        .decodeList<BookedRideId>()
                        .map { it.rideId }
                        .toSet()
        }

        suspend fun cancelBooking(bookingId: String, userId: String, role: String): Result<Unit> {
                return try {
                        // 1. Get the ride_id from the booking
                        val booking = client.from("bookings")
                                .select(Columns.raw("ride_id")) {
                                        filter { eq("booking_id", bookingId) }
                                }
                                .decodeSingleOrNull<Booking>()
                                ?: return Result.failure(Exception("Booking not found"))

                        val rideId = booking.rideId

                        // 2. Update booking status
                        client.from("bookings")
                                .update(
                                        buildJsonObject {
                                                put("booking_status", "cancelled")
                                                put("cancelled_by", role)
                                                put("cancellation_time", "now()")
                                        }
                                ) {
                                        filter { eq("booking_id", bookingId) }
                                }

                        // 3. Increment available seats safely (fetch then update)
                        val currentRide = client.from("rides")
                                .select(Columns.raw("available_seats")) {
                                        filter { eq("ride_id", rideId) }
                                }
                                .decodeSingleOrNull<Ride>()
                                ?: return Result.failure(Exception("Ride not found"))

                        client.from("rides")
                                .update(
                                        buildJsonObject {
                                                put("available_seats", currentRide.availableSeats + 1)
                                        }
                                ) {
                                        filter { eq("ride_id", rideId) }
                                }

                        Result.success(Unit)
                } catch (e: Exception) {
                        Result.failure(e)
                }
        }

        suspend fun getBookedRides(userId: String): List<Booking> {
                return client
                        .from("bookings")
                        .select(Columns.raw("*, rides(*, users(*), vehicles(*))")) {
                                filter {
                                        eq("rider_id", userId)
                                        eq("booking_status", "confirmed")
                                }
                                order("time_created", Order.ASCENDING)
                        }
                        .decodeList<Booking>()
        }

        suspend fun getAllFutureRides(userId: String, genderPreference: String? = null): List<Ride> {
                val nowUtc = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
                val thirtyMinsFromNowUtc = nowUtc.plusMinutes(30)
                        .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)

                val bookedRideIds = client
                        .from("bookings")
                        .select(Columns.raw("ride_id")) {
                                filter { eq("rider_id", userId) }
                        }
                        .decodeList<BookedRideId>()
                        .map { it.rideId }
                        .toSet()

                return client
                        .from("rides")
                        .select(Columns.raw("*, users!inner(*), vehicles!left(*)")) {
                                filter {
                                        and {
                                                gte("departure_time", thirtyMinsFromNowUtc)
                                                eq("ride_status", "scheduled")
                                                gt("available_seats", 0)
                                                neq("driver_id", userId)
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

        suspend fun getFutureRidesByDate(
                userId: String,
                date: String,
                genderPreference: String? = null
        ): List<Ride> {
                val zoneOffset = java.time.ZoneOffset.ofHours(10) // AEST
                val localDate = java.time.LocalDate.parse(date)

                val startOfDayUtc = localDate.atStartOfDay()
                        .atOffset(zoneOffset)
                        .withOffsetSameInstant(java.time.ZoneOffset.UTC)

                val endOfDayUtc = localDate.plusDays(1).atStartOfDay()
                        .atOffset(zoneOffset)
                        .withOffsetSameInstant(java.time.ZoneOffset.UTC)
                        .minusNanos(1)

                val bookedRideIds = client
                        .from("bookings")
                        .select(Columns.raw("ride_id")) {
                                filter { eq("rider_id", userId) }
                        }
                        .decodeList<BookedRideId>()
                        .map { it.rideId }
                        .toSet()

                return client
                        .from("rides")
                        .select(Columns.raw("*, users!inner(*), vehicles!left(*)")) {
                                filter {
                                        and {
                                                gte("departure_time", startOfDayUtc.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                                                lte("departure_time", endOfDayUtc.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                                                eq("ride_status", "scheduled")
                                                gt("available_seats", 0)
                                                neq("driver_id", userId)
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

        suspend fun getGenderPreference(userId: String): String? {
                return client
                        .from("users")
                        .select(Columns.raw("user_id, gender_preference")) {
                                filter { eq("user_id", userId) }
                                limit(1)
                        }
                        .decodeSingleOrNull<RideUser>()
                        ?.genderPreference
        }

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
                        client.from("rides").delete { filter { eq("ride_id", rideId) } }
                        Result.success(Unit)
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Cancel ride failed", e)
                        Result.failure(e)
                }
        }

        suspend fun hideRide(userId: String, rideId: String) {
                try {
                        client.from("hidden_rides").insert(mapOf(
                                "user_id" to userId,
                                "ride_id" to rideId
                        ))
                } catch (e: Exception) {
                        Log.e("MOGO_DEBUG", "FAILED to hide ride. Error: ${e.message}")
                }
        }

        suspend fun unhideRide(userId: String, rideId: String) {
                client.from("hidden_rides").delete {
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
                                        order("time_created", Order.DESCENDING)
                                }
                                .decodeList<Booking>()
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Failed to get rider history", e)
                        emptyList()
                }
        }

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
                                        order("departure_time", Order.DESCENDING)
                                }
                                .decodeList<Ride>()
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Failed to get driver history", e)
                        emptyList()
                }
        }

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

        suspend fun getUpcomingDriverRides(userId: String): List<Ride> {
                val nowUtc = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
                        .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                return try {
                        client
                                .from("rides")
                                .select(Columns.raw("*, users(*), vehicles(*)")) {
                                        filter {
                                                and {
                                                        eq("driver_id", userId)
                                                        eq("ride_status", "scheduled")
                                                        gte("departure_time", nowUtc)
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

        fun passesHardMemoryFilters(
                ride: Ride,
                riderId: String,
                rider: RideUser,
                alreadyBookedIds: Set<String>,
                blockedByRider: Set<String>,
                blockedByDriver: Set<String>
        ): Boolean {
                val nowUtc = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
                val departure = runCatching {
                        java.time.OffsetDateTime.parse(ride.departureTime)
                }.getOrNull() ?: return false

                return ride.id !in alreadyBookedIds
                        && departure.isAfter(nowUtc.plusMinutes(30))
                        && ride.driverId !in blockedByRider
                        && riderId !in blockedByDriver
        }

        fun passesSoftFilters(ride: Ride, riderId: String, rider: RideUser): Boolean {
                val riderPref = rider.genderPreference == null
                        || rider.genderPreference == "ANY"
                        || rider.genderPreference == ride.users?.userGender
                val driverPref = ride.users?.genderPreference == null
                        || ride.users.genderPreference == "ANY"
                        || ride.users.genderPreference == rider.userGender
                return riderPref && driverPref
        }

        fun isWithinRadiusKm(
                pickupLat: Double, pickupLng: Double,
                centerLat: Double, centerLng: Double,
                radiusKm: Double
        ): Boolean {
                val R = 6371.0
                val dLat = Math.toRadians(pickupLat - centerLat)
                val dLng = Math.toRadians(pickupLng - centerLng)
                val a = sin(dLat / 2) * sin(dLat / 2) +
                        cos(Math.toRadians(centerLat)) *
                        cos(Math.toRadians(pickupLat)) *
                        sin(dLng / 2) * sin(dLng / 2)
                val c = 2 * atan2(sqrt(a), sqrt(1 - a))
                return R * c <= radiusKm
        }

        fun sortRides(rides: List<MapsRepository.RideWithDetour>): List<MapsRepository.RideWithDetour> {
                val nowUtc = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
                return rides.sortedWith(compareBy(
                        { ride ->
                                val dep = runCatching {
                                        java.time.OffsetDateTime.parse(ride.ride.departureTime)
                                }.getOrNull()
                                if (dep != null && dep.isBefore(nowUtc.plusMinutes(15))) 0 else 1
                        },
                        { ride -> when (ride.ride.vehicleType.uppercase()) {
                                "EV" -> 0; "HYBRID" -> 1; else -> 2
                        } },
                        { ride -> -(ride.ride.users?.driverRating ?: 0.0) },
                        { ride -> ride.addedMinutes }
                ))
        }
}