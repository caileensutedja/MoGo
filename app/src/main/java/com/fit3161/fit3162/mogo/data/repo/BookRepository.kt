package com.fit3161.fit3162.mogo.data.repo

import android.util.Log
import com.fit3161.fit3162.mogo.data.model.Location
import com.google.android.gms.maps.model.LatLng
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
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
        @SerialName("avatar_url") val avatarUrl: String? = null,   // ← add this
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
        @SerialName("recurring_group_id") val recurringGroupId: String? = null,
        @SerialName("recurring_week_index") val recurringWeekIndex: Int? = null,
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
        @SerialName("pickup_lat") val pickupLat: Double? = null,
        @SerialName("pickup_lng") val pickupLng: Double? = null,
        @SerialName("seats_booked") val seatsBooked: Int,
        @SerialName("booking_status") val bookingStatus: String,
        @SerialName("time_created") val timeCreated: String? = null,
        @SerialName("dropoff_lat") val dropoffLat: Double? = null,
        @SerialName("dropoff_lng") val dropoffLng: Double? = null,
        val rides: Ride? = null
)

/**
 * Write-only DTO for inserting a new booking.
 * Excludes DB-generated fields (booking_id, time_created) and joined nested
 * objects (rides) that don't exist as columns on the bookings table.
 */
@Serializable
data class BookingInsert(
        @SerialName("ride_id") val rideId: String,
        @SerialName("rider_id") val riderId: String,
        @SerialName("pickup_location") val pickupLocation: String,
        @SerialName("dropoff_location") val dropoffLocation: String,
        @SerialName("pickup_lat") val pickupLat: Double,
        @SerialName("pickup_lng") val pickupLng: Double,
        @SerialName("seats_booked") val seatsBooked: Int,
        @SerialName("booking_status") val bookingStatus: String
)

@Serializable
data class HiddenRide(
        @SerialName("ride_id") val rideId: String
)


val CAMPUS_OPTIONS: Map<String, Location> = mapOf(
        "Clayton"    to Location("Clayton",    LatLng(-37.9105, 145.1363)),
        "Caulfield"  to Location("Caulfield",  LatLng(-37.8768, 145.0452)),
        "Peninsula"  to Location("Peninsula",  LatLng(-38.1484, 145.1302)),
        "Parkville"  to Location("Parkville",  LatLng(-37.7963, 144.9614)),
)

fun locationFromCampusName(name: String): Location? = CAMPUS_OPTIONS[name]

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

        /**
         *
         */
        suspend fun getBookingById(bookingId: String): Booking? {
                return try {
                        client.from("bookings")
                                .select(Columns.raw("*, rides(*, users(*), vehicles(*))")) {
                                        filter { eq("booking_id", bookingId) }
                                }
                                .decodeSingleOrNull<Booking>()
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Failed to fetch booking", e)
                        null
                }
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

        /**
         * Creates a new booking row for a rider on a given ride.
         * Captures the rider's pickup coordinates so the driver knows where to stop.
         */
        suspend fun bookRide(
                rideId: String,
                riderId: String,
                pickupLat: Double,
                pickupLng: Double,
                seatsBooked: Int = 1
        ): Result<Unit> {
                return try {
                        client.from("bookings").insert(
                                BookingInsert(
                                        rideId = rideId,
                                        riderId = riderId,
                                        pickupLocation = "Rider's location",
                                        dropoffLocation = "Destination",
                                        pickupLat = pickupLat,
                                        pickupLng = pickupLng,
                                        seatsBooked = seatsBooked,
                                        bookingStatus = "confirmed"
                                )
                        )
                        Result.success(Unit)
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Booking failed", e)
                        Result.failure(e)
                }
        }

        suspend fun getAllFutureRides(userId: String, genderPreference: String? = null): List<Ride> {
                val nowUtc = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
                val thirtyMinsFromNowUtc = nowUtc.plusMinutes(30)
                        .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)

                val bookedRideIds = client
                        .from("bookings")
                        .select(Columns.raw("ride_id")) {
                                filter { eq("rider_id", userId)
                                        neq("booking_status", "cancelled")}

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

        // Bulk insert for recurring rides
        suspend fun uploadRides(rides: List<Ride>): Result<Unit> {
                return try {
                        client.from("rides").insert(rides)
                        Result.success(Unit)
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Bulk upload failed", e)
                        Result.failure(e)
                }
        }

        // For "rebook next week" — find the next ride in the same recurring group
        suspend fun getNextRecurringRide(
                recurringGroupId: String,
                afterDepartureTime: String
        ): Ride? {
                return try {
                        client.from("rides")
                                .select(Columns.raw("*, users(*), vehicles(*)")) {
                                        filter {
                                                and {
                                                        eq("recurring_group_id", recurringGroupId)
                                                        eq("ride_status", "scheduled")
                                                        gt("departure_time", afterDepartureTime)
                                                        gt("available_seats", 0)
                                                }
                                        }
                                        order("departure_time", Order.ASCENDING)
                                        limit(1)
                                }
                                .decodeSingleOrNull<Ride>()
                } catch (e: Exception) {
                        null
                }
        }

        // Book a ride (insert into bookings)
        suspend fun bookRide(
                riderId: String,
                rideId: String,
                pickupLocation: String,
                pickupLat: Double,
                pickupLng: Double,
                dropoffLocation: String,
                dropoffLat: Double,
                dropoffLng: Double,
        ): Result<Unit> {
                return try {
                        // 1. Re-check seats atomically via RPC (returns false if no seats left)
                        val hasSeats = client.postgrest
                                .rpc("decrement_seats", mapOf("p_ride_id" to rideId))
                                .data
                                .trimIndent()
                                .toBooleanStrictOrNull() ?: false

                        if (!hasSeats) return Result.failure(Exception("No seats available"))

                        // 2. Insert booking with all lat/lng fields
                        val booking = Booking(
                                id = UUID.randomUUID().toString(),
                                rideId = rideId,
                                riderId = riderId,
                                pickupLocation = pickupLocation,
                                pickupLat = pickupLat,
                                pickupLng = pickupLng,
                                dropoffLocation = dropoffLocation,
                                dropoffLat = dropoffLat,
                                dropoffLng = dropoffLng,
                                seatsBooked = 1,
                                bookingStatus = "confirmed"
                        )
                        client.from("bookings").insert(booking)
                        Result.success(Unit)
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "bookRide failed", e)
                        Result.failure(e)
                }
        }

//        suspend fun cancelBooking(bookingId: String, rideId: String): Result<Unit> {
//                return try {
//                        // 1. Soft-delete the booking
//                        client.from("bookings").update({
//                                set("booking_status", "cancelled")
//                                set("cancelled_by", "rider")
//                                set("cancellation_time", java.time.LocalDateTime.now().toString())
//                        }) {
//                                filter { eq("booking_id", bookingId) }
//                        }
//                        // 2. Restore the seat atomically via RPC
//                        client.postgrest.rpc("increment_seats", mapOf("p_ride_id" to rideId))
//                        Result.success(Unit)
//                } catch (e: Exception) {
//                        Log.e("REPO_ERROR", "cancelBooking failed", e)
//                        Result.failure(e)
//                }
//        }

        suspend fun cancelBooking(bookingId: String, rideId: String): Result<Unit> {
                return try {
                        client.from("bookings").update({
                                set("booking_status", "cancelled")
                                set("cancelled_by", "rider")
                                set("cancellation_time", java.time.LocalDateTime.now().toString())
                        }) {
                                filter { eq("booking_id", bookingId) }
                        }
                        client.postgrest.rpc("increment_seats", mapOf("p_ride_id" to rideId))  // ← must be increment, not decrement
                        Result.success(Unit)
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "cancelBooking failed", e)
                        Result.failure(e)
                }
        }
//        suspend fun bookRide(
//                riderId: String,
//                rideId: String,
//                pickupLocation: String,
//                pickupLat: Double,
//                pickupLng: Double,
//                dropoffLocation: String,
//                dropoffLat: Double,
//                dropoffLng: Double,
//        ): Result<Unit> {
//                return try {
//                        val booking = Booking(
//                                id = UUID.randomUUID().toString(),
//                                rideId = rideId,
//                                riderId = riderId,
//                                pickupLocation = pickupLocation,
//                                dropoffLocation = dropoffLocation,
//                                seatsBooked = 1,
//                                bookingStatus = "confirmed"
//                        )
//                        client.from("bookings").insert(booking)
//                        // Decrement available_seats
//                        client.from("rides").update({ set("available_seats", /* raw rpc preferred */ 0) }) {
//                                // Ideally use a Postgres function/RPC to do available_seats - 1 atomically
//                                filter { eq("ride_id", rideId) }
//                        }
//                        Result.success(Unit)
//                } catch (e: Exception) {
//                        Result.failure(e)
//                }
//        }
}
