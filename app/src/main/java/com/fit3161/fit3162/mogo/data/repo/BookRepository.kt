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

// Represents a user profile joined from the users table.
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
        @SerialName("gender_preference") val genderPreference: String? = null,
        @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class BookedRideId(
        @SerialName("ride_id") val rideId: String
)

// Represents a vehicle linked to a driver.
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

// Represents a ride posted by a driver.
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
        @SerialName("recurring_group_id") val recurringGroupId: String? = null,
        @SerialName("recurring_week_index") val recurringWeekIndex: Int? = null,
        @SerialName("time_created") val timeCreated: String? = null,
        @SerialName("vehicle_type") val vehicleType: String,
        @SerialName("plate_number") val plateNumber: String,
        @SerialName("cancellation_reason") val cancellationReason: String? = null,
        @SerialName("driver_live_lat") val driverLiveLat: Double? = null,
        @SerialName("driver_live_lng") val driverLiveLng: Double? = null,
        @SerialName("rider_live_lat") val riderLiveLat: Double? = null,
        @SerialName("rider_live_lng") val riderLiveLng: Double? = null,
        val users: RideUser? = null,
        val vehicles: Vehicle? = null
)

// Represents a booking made by a rider for a specific ride.
@Serializable
data class Booking(
        @SerialName("booking_id") val id: String,
        @SerialName("ride_id") val rideId: String,
        @SerialName("rider_id") val riderId: String,
        @SerialName("pickup_location") val pickupLocation: String,
        @SerialName("dropoff_location") val dropoffLocation: String,
        @SerialName("seats_booked") val seatsBooked: Int,
        @SerialName("booking_status") val bookingStatus: String,
        @SerialName("pickup_lat") val pickupLat: Double? = null,
        @SerialName("pickup_lng") val pickupLng: Double? = null,
        @SerialName("dropoff_lat") val dropoffLat: Double? = null,
        @SerialName("dropoff_lng") val dropoffLng: Double? = null,
        @SerialName("estimated_distance_meters") val estimatedDistanceMeters: Int? = null,
        @SerialName("estimated_duration_seconds") val estimatedDurationSeconds: Int? = null,
        @SerialName("cancellation_reason") val cancellationReason: String? = null,
        @SerialName("time_created") val timeCreated: String? = null,
        val rides: Ride? = null
)

// Lightweight booking info for the driver's "My Rides" view.
// Joins with users table on rider_id to get the rider's name/phone.
@Serializable
data class RideBookingInfo(
        @SerialName("booking_id") val bookingId: String,
        @SerialName("rider_id") val riderId: String,
        @SerialName("pickup_location") val pickupLocation: String,
        @SerialName("seats_booked") val seatsBooked: Int,
        @SerialName("booking_status") val bookingStatus: String,
        val users: RideUser? = null
)

@Serializable
data class HiddenRide(
        @SerialName("ride_id") val rideId: String
)


class BookRepository(private val client: SupabaseClient) {

        // Upload a single ride to the database.
        suspend fun uploadRide(ride: Ride): Result<Unit> {
                return try {
                        client.from("rides").insert(ride)
                        Result.success(Unit)
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Upload failed", e)
                        Result.failure(e)
                }
        }

        // Upload multiple rides at once (used for recurring rides).
        suspend fun uploadRides(rides: List<Ride>): Result<Unit> {
                return try {
                        client.from("rides").insert(rides)
                        Result.success(Unit)
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Batch upload failed", e)
                        Result.failure(e)
                }
        }

        // Get all vehicles registered to a driver.
        suspend fun getUserVehicles(userId: String): List<Vehicle> {
                return client.from("vehicles")
                        .select() { filter { eq("driver_id", userId) } }
                        .decodeList<Vehicle>()
        }

        // Get IDs of rides the user has already booked (excludes cancelled).
        suspend fun getBookedRideIds(userId: String): Set<String> {
                return client.from("bookings")
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

        // Get all confirmed bookings for a rider, joined with ride + driver + vehicle.
        suspend fun getBookedRides(userId: String): List<Booking> {
                return client.from("bookings")
                        .select(Columns.raw("*, rides(*, users(*), vehicles(*))")) {
                                filter {
                                        eq("rider_id", userId)
                                        eq("booking_status", "confirmed")
                                }
                                order("time_created", Order.ASCENDING)
                        }
                        .decodeList<Booking>()
        }

        // Get a single booking by ID, joined with ride + driver + vehicle.
        suspend fun getBookingById(bookingId: String): Booking? {
                return try {
                        client.from("bookings")
                                .select(Columns.raw("*, rides(*, users(*), vehicles(*))")) {
                                        filter { eq("booking_id", bookingId) }
                                }
                                .decodeSingleOrNull<Booking>()
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Get booking by ID failed", e)
                        null
                }
        }

        // Get all active bookings for a specific ride (for driver's My Rides view).
        // Joins with users table to get the rider's name and phone.
        suspend fun getBookingsForRide(rideId: String): List<RideBookingInfo> {
                return try {
                        client.from("bookings")
                                .select(
                                        Columns.raw(
                                                "booking_id, rider_id, pickup_location, " +
                                                        "seats_booked, booking_status, users(*)"
                                        )
                                ) {
                                        filter {
                                                eq("ride_id", rideId)
                                                neq("booking_status", "cancelled")
                                        }
                                }
                                .decodeList<RideBookingInfo>()
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Get bookings for ride failed", e)
                        emptyList()
                }
        }

        // Get all future rides available for booking.
        suspend fun getAllFutureRides(
                userId: String,
                genderPreference: String? = null
        ): List<Ride> {
                val thirtyMinsFromNow = java.time.OffsetDateTime // Filter rides 30 mins from that time
                        .now(java.time.ZoneOffset.UTC)
                        .plusMinutes(30)
                        .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                val bookedIds = client.from("bookings")
                        .select(Columns.raw("ride_id")) {filter { eq("rider_id", userId) }}
                        .decodeList<BookedRideId>()
                        .map { it.rideId }
                        .toSet()
                return client.from("rides") // Return the list of rides
                        .select(Columns.raw("*, users!inner(*), vehicles!left(*)")) {
                                filter {and {gte("departure_time", thirtyMinsFromNow) // Filter with optional date
                                        eq("ride_status", "scheduled") // Filter upcoming rides only
                                        gt("available_seats", 0) // Filter rides with available seats > 0
                                        neq("driver_id", userId) // Filter that its not its own ride
                                        // Apply gender preference filtering
                                        if (genderPreference != null) { eq("users.user_gender", genderPreference)}}}
                                order("departure_time", Order.ASCENDING)} // Order ascendingly
                        .decodeList<Ride>()
                        .filter { it.id !in bookedIds }} // Removed already booked rides (avoid duplicates)

        // Get future rides filtered by a specific date (AEST timezone).
        suspend fun getFutureRidesByDate(
                userId: String,
                date: String,
                genderPreference: String? = null
        ): List<Ride> {
                val localDate = java.time.LocalDate.parse(date)
                val startUtc = localDate
                        .atStartOfDay(java.time.ZoneId.of("Australia/Melbourne"))
                        .toInstant()
                        .toString()
                val endUtc = localDate
                        .plusDays(1)
                        .atStartOfDay(java.time.ZoneId.of("Australia/Melbourne"))
                        .toInstant()
                        .toString()

                Log.d("DATE_DEBUG", "Querying from $startUtc to $endUtc")

                val bookedIds = client.from("bookings")
                        .select(Columns.raw("ride_id")) {
                                filter { eq("rider_id", userId) }
                        }
                        .decodeList<BookedRideId>()
                        .map { it.rideId }
                        .toSet()

                val formatter = java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME

                val allRides = client.from("rides")
                        .select(Columns.raw("*, users!inner(*), vehicles!left(*)")) {
                                filter {
                                        and {
                                                gte("departure_time", startUtc.format(formatter))
                                                lte("departure_time", endUtc.format(formatter))
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

                Log.d("DATE_DEBUG", "Raw rides before booked filter: ${allRides.size}")
                allRides.forEach {
                        Log.d("DATE_DEBUG", "Raw ride departure: ${it.departureTime}")
                }

                return allRides.filter { it.id !in bookedIds }
        }

        // Get a user's gender preference for ride matching.
        suspend fun getGenderPreference(userId: String): String? {
                return client.from("users")
                        .select(Columns.raw("user_id, gender_preference")) {
                                filter { eq("user_id", userId) }
                                limit(1)
                        }
                        .decodeSingleOrNull<RideUser>()
                        ?.genderPreference
        }

        // Get all rides posted by a driver (any status).
        // Joins users(*) so ride.users.userName is populated.
        suspend fun getMyRides(userId: String): List<Ride> {
                return client.from("rides")
                        .select(Columns.raw("*, users(*), vehicles(*)")) {
                                filter { eq("driver_id", userId) }
                                order("departure_time", Order.ASCENDING)
                        }
                        .decodeList<Ride>()
        }

        // Soft-delete a ride: sets ride_status to "cancelled" with a reason.
        // Also cancels all associated bookings so riders see the cancellation.
        suspend fun cancelRide(
                rideId: String,
                reason: String = ""
        ): Result<Unit> {
                return try {
                        // Soft-delete the ride itself
                        client.from("rides").update(
                                buildJsonObject {
                                        put("ride_status", "cancelled")
                                        if (reason.isNotBlank()) {
                                                put("cancellation_reason", reason)
                                        }
                                }
                        ) {
                                filter { eq("ride_id", rideId) }
                        }

                        // Cancel all active bookings for this ride
                        client.from("bookings").update(
                                buildJsonObject {
                                        put("booking_status", "cancelled")
                                        put("cancellation_reason", "Ride cancelled by driver")
                                }
                        ) {
                                filter {
                                        eq("ride_id", rideId)
                                        neq("booking_status", "cancelled")
                                }
                        }

                        Result.success(Unit)
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Cancel ride failed", e)
                        Result.failure(e)
                }
        }

        // Cancel a booking: sets booking_status to "cancelled" with a reason.
        // Restores the seat(s) on the ride so other riders can book.
        suspend fun cancelBooking(
                bookingId: String,
                rideId: String,
                reason: String = ""
        ): Result<Unit> {
                return try {
                        // Get seats booked before cancelling (to restore them)
                        val booking = client.from("bookings")
                                .select { filter { eq("booking_id", bookingId) } }
                                .decodeSingleOrNull<Booking>()
                        val seatsToRestore = booking?.seatsBooked ?: 1

                        // Cancel the booking with reason
                        client.from("bookings").update(
                                buildJsonObject {
                                        put("booking_status", "cancelled")
                                        if (reason.isNotBlank()) {
                                                put("cancellation_reason", reason)
                                        }
                                }
                        ) {
                                filter { eq("booking_id", bookingId) }
                        }

                        // Restore seats on the ride
                        val ride = client.from("rides")
                                .select { filter { eq("ride_id", rideId) } }
                                .decodeSingleOrNull<Ride>()
                        if (ride != null) {
                                client.from("rides").update(
                                        buildJsonObject {
                                                put("available_seats", ride.availableSeats + seatsToRestore)
                                        }
                                ) {
                                        filter { eq("ride_id", rideId) }
                                }
                        }

                        Result.success(Unit)
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Cancel booking failed", e)
                        Result.failure(e)
                }
        }

        // Hide a ride from the rider's future rides list.
        suspend fun hideRide(userId: String, rideId: String) {
                try {
                        client.from("hidden_rides").insert(
                                mapOf("user_id" to userId, "ride_id" to rideId)
                        )
                } catch (e: Exception) {
                        Log.e("MOGO_DEBUG", "FAILED to hide ride: ${e.message}")
                }
        }

        // Unhide a previously hidden ride.
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

        // Get the set of ride IDs the user has hidden.
        suspend fun getHiddenRideIds(userId: String): Set<String> {
                return client.from("hidden_rides")
                        .select(Columns.raw("ride_id")) {
                                filter { eq("user_id", userId) }
                        }
                        .decodeList<HiddenRide>()
                        .map { it.rideId }
                        .toSet()
        }

        // Get completed bookings for the rider (ride history).
        suspend fun getRiderHistory(userId: String): List<Booking> {
                return try {
                        client.from("bookings")
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

        // Get completed rides for the driver (ride history).
        suspend fun getDriverHistory(userId: String): List<Ride> {
                return try {
                        client.from("rides")
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

        // Get all ongoing (confirmed) bookings for a rider.
        suspend fun getOngoingRiderBookings(userId: String): List<Booking> {
                return try {
                        client.from("bookings")
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

        // Get upcoming scheduled rides for a driver.
        suspend fun getUpcomingDriverRides(userId: String): List<Ride> {
                val nowUtc = java.time.OffsetDateTime
                        .now(java.time.ZoneOffset.UTC)
                        .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                return try {
                        client.from("rides")
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

        // Set ride_status to "in_progress" when the driver starts the ride.
        suspend fun startRide(rideId: String): Result<Unit> {
                return try {
                        client.from("rides").update(
                                buildJsonObject { put("ride_status", "in_progress") }
                        ) {
                                filter { eq("ride_id", rideId) }
                        }
                        Result.success(Unit)
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Start ride failed", e)
                        Result.failure(e)
                }
        }

        // Set ride_status to "completed" when the driver ends the ride.
        // Clears all live location columns and marks associated bookings as completed.
        suspend fun completeRide(rideId: String): Result<Unit> {
                return try {
                        // Mark ride as completed and clear live locations
                        client.from("rides").update(
                                buildJsonObject {
                                        put("ride_status", "completed")
                                        put("driver_live_lat", null as Double?)
                                        put("driver_live_lng", null as Double?)
                                        put("rider_live_lat", null as Double?)
                                        put("rider_live_lng", null as Double?)
                                }
                        ) {
                                filter { eq("ride_id", rideId) }
                        }

                        // Mark all confirmed bookings for this ride as completed
                        client.from("bookings").update(
                                buildJsonObject { put("booking_status", "completed") }
                        ) {
                                filter { eq("ride_id", rideId) }
                        }

                        Result.success(Unit)
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Complete ride failed", e)
                        Result.failure(e)
                }
        }

        // Update the driver's live GPS on the ride row (called every 20s).
        suspend fun updateDriverLocation(
                rideId: String,
                lat: Double,
                lng: Double
        ) {
                try {
                        client.from("rides").update(
                                buildJsonObject {
                                        put("driver_live_lat", lat)
                                        put("driver_live_lng", lng)
                                }
                        ) {
                                filter { eq("ride_id", rideId) }
                        }
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Update driver location failed", e)
                }
        }

        // Update the rider's live GPS on the ride row (called every 20s).
        suspend fun updateRiderLocation(
                rideId: String,
                lat: Double,
                lng: Double
        ) {
                try {
                        client.from("rides").update(
                                buildJsonObject {
                                        put("rider_live_lat", lat)
                                        put("rider_live_lng", lng)
                                }
                        ) {
                                filter { eq("ride_id", rideId) }
                        }
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Update rider location failed", e)
                }
        }

        // Read live locations for a ride (returns the ride row with live lat/lng).
        suspend fun getRideLiveLocations(rideId: String): Ride? {
                return try {
                        client.from("rides")
                                .select { filter { eq("ride_id", rideId) } }
                                .decodeSingleOrNull<Ride>()
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Get live locations failed", e)
                        null
                }
        }

        // Find the next ride in a recurring group (for rebook next week).
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
                                                        gt("departure_time", afterDepartureTime)
                                                        eq("ride_status", "scheduled")
                                                }
                                        }
                                        order("departure_time", Order.ASCENDING)
                                        limit(1)
                                }
                                .decodeSingleOrNull<Ride>()
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Get next recurring ride failed", e)
                        null
                }
        }

        /**
         * Book a ride: inserts a new booking row.
         * Before inserting, deletes any existing cancelled booking for the same
         * rider+ride combo so the unique constraint doesn't block rebooking.
         */
        suspend fun bookRide(
                riderId: String,
                rideId: String,
                pickupLocation: String = "",
                pickupLat: Double = 0.0,
                pickupLng: Double = 0.0,
                dropoffLocation: String = "",
                dropoffLat: Double = 0.0,
                dropoffLng: Double = 0.0,
                estimatedDistanceMeters: Int? = null,
                estimatedDurationSeconds: Int? = null
        ): Result<Unit> {
                return try {
                        // Remove any existing cancelled booking for this rider+ride so the unique constraint doesn't block rebooking
                        client.from("bookings").delete {
                                filter {
                                        eq("rider_id", riderId)
                                        eq("ride_id", rideId)
                                        eq("booking_status", "cancelled")
                                }
                        }

                        // Insert the new booking
                        client.from("bookings").insert(
                                buildJsonObject {
                                        put("rider_id", riderId)
                                        put("ride_id", rideId)
                                        put("pickup_location", pickupLocation)
                                        put("pickup_lat", pickupLat)
                                        put("pickup_lng", pickupLng)
                                        put("dropoff_location", dropoffLocation)
                                        put("dropoff_lat", dropoffLat)
                                        put("dropoff_lng", dropoffLng)
                                        put("seats_booked", 1)
                                        put("booking_status", "confirmed")
                                        if (estimatedDistanceMeters != null) {
                                                put("estimated_distance_meters", estimatedDistanceMeters)
                                        }
                                        if (estimatedDurationSeconds != null) {
                                                put("estimated_duration_seconds", estimatedDurationSeconds)
                                        }
                                }
                        )
                        Result.success(Unit)
                } catch (e: Exception) {
                        Log.e("REPO_ERROR", "Book ride failed", e)
                        Result.failure(e)
                }
        }

        // Hard memory filters for ride matching (time, duplicates, blocked users).
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

        // Soft filters for ride matching (gender preference).
        fun passesSoftFilters(
                ride: Ride,
                riderId: String,
                rider: RideUser
        ): Boolean {
                val riderPref = rider.genderPreference == null
                        || rider.genderPreference == "ANY"
                        || rider.genderPreference == ride.users?.userGender

                val driverPref = ride.users?.genderPreference == null
                        || ride.users.genderPreference == "ANY"
                        || ride.users.genderPreference == rider.userGender

                return riderPref && driverPref
        }

        // Haversine distance check for radius-based filtering.
        fun isWithinRadiusKm(
                pickupLat: Double,
                pickupLng: Double,
                centerLat: Double,
                centerLng: Double,
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

        // Sort rides by urgency, vehicle type (EV first), driver rating, and detour time.
        fun sortRides(
                rides: List<MapsRepository.RideWithDetour>
        ): List<MapsRepository.RideWithDetour> {
                val nowUtc = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
                return rides.sortedWith(
                        compareBy(
                                { ride ->
                                        val dep = runCatching {
                                                java.time.OffsetDateTime.parse(ride.ride.departureTime)
                                        }.getOrNull()
                                        if (dep != null && dep.isBefore(nowUtc.plusMinutes(15))) 0 else 1
                                },
                                { ride ->
                                        when (ride.ride.vehicleType.uppercase()) {
                                                "EV" -> 0
                                                "HYBRID" -> 1
                                                else -> 2
                                        }
                                },
                                { ride -> -(ride.ride.users?.driverRating ?: 0.0) },
                                { ride -> ride.addedMinutes }
                        )
                )
        }
}
