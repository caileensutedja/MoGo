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
        @SerialName("gender_preference") val genderPreference: String? = null,
        @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class BookedRideId(@SerialName("ride_id") val rideId: String)

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
data class HiddenRide(@SerialName("ride_id") val rideId: String)


class BookRepository(private val client: SupabaseClient) {

        suspend fun uploadRide(ride: Ride): Result<Unit> = try { client.from("rides").insert(ride); Result.success(Unit) } catch (e: Exception) { Log.e("REPO_ERROR", "Upload failed", e); Result.failure(e) }

        suspend fun uploadRides(rides: List<Ride>): Result<Unit> = try { client.from("rides").insert(rides); Result.success(Unit) } catch (e: Exception) { Log.e("REPO_ERROR", "Batch upload failed", e); Result.failure(e) }

        suspend fun getUserVehicles(userId: String): List<Vehicle> = client.from("vehicles").select() { filter { eq("driver_id", userId) } }.decodeList<Vehicle>()

        suspend fun getBookedRideIds(userId: String): Set<String> {
                return client.from("bookings")
                        .select(Columns.raw("ride_id")) { filter { eq("rider_id", userId); neq("booking_status", "cancelled") } }
                        .decodeList<BookedRideId>().map { it.rideId }.toSet()
        }

        suspend fun getBookedRides(userId: String): List<Booking> {
                return client.from("bookings")
                        .select(Columns.raw("*, rides(*, users(*), vehicles(*))")) {
                                filter { eq("rider_id", userId); eq("booking_status", "confirmed") }
                                order("time_created", Order.ASCENDING)
                        }.decodeList<Booking>()
        }

        suspend fun getBookingById(bookingId: String): Booking? {
                return try {
                        client.from("bookings")
                                .select(Columns.raw("*, rides(*, users(*), vehicles(*))")) { filter { eq("booking_id", bookingId) } }
                                .decodeSingleOrNull<Booking>()
                } catch (e: Exception) { Log.e("REPO_ERROR", "Get booking by ID failed", e); null }
        }

        suspend fun getBookingsForRide(rideId: String): List<RideBookingInfo> {
                return try {
                        client.from("bookings")
                                .select(Columns.raw("booking_id, rider_id, pickup_location, seats_booked, booking_status, users(*)")) {
                                        filter { eq("ride_id", rideId); neq("booking_status", "cancelled") }
                                }.decodeList<RideBookingInfo>()
                } catch (e: Exception) { Log.e("REPO_ERROR", "Get bookings for ride failed", e); emptyList() }
        }

        suspend fun getAllFutureRides(userId: String, genderPreference: String? = null): List<Ride> {
                val thirtyMinsFromNow = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).plusMinutes(30)
                        .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                val bookedIds = client.from("bookings").select(Columns.raw("ride_id")) { filter { eq("rider_id", userId) } }
                        .decodeList<BookedRideId>().map { it.rideId }.toSet()
                return client.from("rides")
                        .select(Columns.raw("*, users!inner(*), vehicles!left(*)")) {
                                filter { and { gte("departure_time", thirtyMinsFromNow); eq("ride_status", "scheduled"); gt("available_seats", 0); neq("driver_id", userId); if (genderPreference != null) eq("users.user_gender", genderPreference) } }
                                order("departure_time", Order.ASCENDING)
                        }.decodeList<Ride>().filter { it.id !in bookedIds }
        }

        suspend fun getFutureRidesByDate(userId: String, date: String, genderPreference: String? = null): List<Ride> {
                val zoneOffset = java.time.ZoneOffset.ofHours(10)
                val localDate = java.time.LocalDate.parse(date)
                val startUtc = localDate.atStartOfDay().atOffset(zoneOffset).withOffsetSameInstant(java.time.ZoneOffset.UTC)
                val endUtc = localDate.plusDays(1).atStartOfDay().atOffset(zoneOffset).withOffsetSameInstant(java.time.ZoneOffset.UTC).minusNanos(1)
                val bookedIds = client.from("bookings").select(Columns.raw("ride_id")) { filter { eq("rider_id", userId) } }
                        .decodeList<BookedRideId>().map { it.rideId }.toSet()
                return client.from("rides")
                        .select(Columns.raw("*, users!inner(*), vehicles!left(*)")) {
                                filter { and { gte("departure_time", startUtc.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)); lte("departure_time", endUtc.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)); eq("ride_status", "scheduled"); gt("available_seats", 0); neq("driver_id", userId); if (genderPreference != null) eq("users.user_gender", genderPreference) } }
                                order("departure_time", Order.ASCENDING)
                        }.decodeList<Ride>().filter { it.id !in bookedIds }
        }

        suspend fun getGenderPreference(userId: String): String? {
                return client.from("users")
                        .select(Columns.raw("user_id, gender_preference")) { filter { eq("user_id", userId) }; limit(1) }
                        .decodeSingleOrNull<RideUser>()?.genderPreference
        }

        // FIX: joins users(*) so ride.users.userName is populated
        suspend fun getMyRides(userId: String): List<Ride> {
                return client.from("rides")
                        .select(Columns.raw("*, users(*), vehicles(*)")) {
                                filter { eq("driver_id", userId) }
                                order("departure_time", Order.ASCENDING)
                        }.decodeList<Ride>()
        }

        // Soft-delete: sets ride_status to "cancelled" and cancels all bookings
        suspend fun cancelRide(rideId: String, reason: String = ""): Result<Unit> {
                return try {
                        client.from("rides").update(buildJsonObject { put("ride_status", "cancelled"); if (reason.isNotBlank()) put("cancellation_reason", reason) }) { filter { eq("ride_id", rideId) } }
                        client.from("bookings").update(buildJsonObject { put("booking_status", "cancelled"); put("cancellation_reason", "Ride cancelled by driver") }) { filter { eq("ride_id", rideId); neq("booking_status", "cancelled") } }
                        Result.success(Unit)
                } catch (e: Exception) { Log.e("REPO_ERROR", "Cancel ride failed", e); Result.failure(e) }
        }

        // Cancels booking with reason, restores seats on the ride
        suspend fun cancelBooking(bookingId: String, rideId: String, reason: String = ""): Result<Unit> {
                return try {
                        val booking = client.from("bookings").select { filter { eq("booking_id", bookingId) } }.decodeSingleOrNull<Booking>()
                        val seatsToRestore = booking?.seatsBooked ?: 1
                        client.from("bookings").update(buildJsonObject { put("booking_status", "cancelled"); if (reason.isNotBlank()) put("cancellation_reason", reason) }) { filter { eq("booking_id", bookingId) } }
                        val ride = client.from("rides").select { filter { eq("ride_id", rideId) } }.decodeSingleOrNull<Ride>()
                        if (ride != null) { client.from("rides").update(buildJsonObject { put("available_seats", ride.availableSeats + seatsToRestore) }) { filter { eq("ride_id", rideId) } } }
                        Result.success(Unit)
                } catch (e: Exception) { Log.e("REPO_ERROR", "Cancel booking failed", e); Result.failure(e) }
        }

        suspend fun hideRide(userId: String, rideId: String) {
                try { client.from("hidden_rides").insert(mapOf("user_id" to userId, "ride_id" to rideId)) }
                catch (e: Exception) { Log.e("MOGO_DEBUG", "FAILED to hide ride: ${e.message}") }
        }

        suspend fun unhideRide(userId: String, rideId: String) {
                client.from("hidden_rides").delete { filter { and { eq("user_id", userId); eq("ride_id", rideId) } } }
        }

        suspend fun getHiddenRideIds(userId: String): Set<String> {
                return client.from("hidden_rides").select(Columns.raw("ride_id")) { filter { eq("user_id", userId) } }
                        .decodeList<HiddenRide>().map { it.rideId }.toSet()
        }

        suspend fun getRiderHistory(userId: String): List<Booking> {
                return try {
                        client.from("bookings").select(Columns.raw("*, rides(*, users(*), vehicles(*))")) {
                                filter { and { eq("rider_id", userId); eq("booking_status", "completed") } }; order("time_created", Order.DESCENDING)
                        }.decodeList<Booking>()
                } catch (e: Exception) { Log.e("REPO_ERROR", "Failed to get rider history", e); emptyList() }
        }

        suspend fun getDriverHistory(userId: String): List<Ride> {
                return try {
                        client.from("rides").select(Columns.raw("*, users(*), vehicles(*)")) {
                                filter { and { eq("driver_id", userId); eq("ride_status", "completed") } }; order("departure_time", Order.DESCENDING)
                        }.decodeList<Ride>()
                } catch (e: Exception) { Log.e("REPO_ERROR", "Failed to get driver history", e); emptyList() }
        }

        suspend fun getOngoingRiderBookings(userId: String): List<Booking> {
                return try {
                        client.from("bookings").select(Columns.raw("*, rides(*, users(*), vehicles(*))")) {
                                filter { and { eq("rider_id", userId); eq("booking_status", "confirmed") } }; order("time_created", Order.ASCENDING)
                        }.decodeList<Booking>()
                } catch (e: Exception) { Log.e("REPO_ERROR", "Failed to get ongoing bookings", e); emptyList() }
        }

        suspend fun getUpcomingDriverRides(userId: String): List<Ride> {
                val nowUtc = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                return try {
                        client.from("rides").select(Columns.raw("*, users(*), vehicles(*)")) {
                                filter { and { eq("driver_id", userId); eq("ride_status", "scheduled"); gte("departure_time", nowUtc) } }; order("departure_time", Order.ASCENDING)
                        }.decodeList<Ride>()
                } catch (e: Exception) { Log.e("REPO_ERROR", "Failed to get upcoming driver rides", e); emptyList() }
        }

        suspend fun startRide(rideId: String): Result<Unit> {
                return try {
                        client.from("rides").update(buildJsonObject { put("ride_status", "in_progress") }) { filter { eq("ride_id", rideId) } }
                        Result.success(Unit)
                } catch (e: Exception) { Log.e("REPO_ERROR", "Start ride failed", e); Result.failure(e) }
        }

        suspend fun completeRide(rideId: String): Result<Unit> {
                return try {
                        client.from("rides").update(buildJsonObject {
                                put("ride_status", "completed"); put("driver_live_lat", null as Double?); put("driver_live_lng", null as Double?); put("rider_live_lat", null as Double?); put("rider_live_lng", null as Double?)
                        }) { filter { eq("ride_id", rideId) } }
                        client.from("bookings").update(buildJsonObject { put("booking_status", "completed") }) { filter { eq("ride_id", rideId) } }
                        Result.success(Unit)
                } catch (e: Exception) { Log.e("REPO_ERROR", "Complete ride failed", e); Result.failure(e) }
        }

        suspend fun updateDriverLocation(rideId: String, lat: Double, lng: Double) {
                try { client.from("rides").update(buildJsonObject { put("driver_live_lat", lat); put("driver_live_lng", lng) }) { filter { eq("ride_id", rideId) } } }
                catch (e: Exception) { Log.e("REPO_ERROR", "Update driver location failed", e) }
        }

        suspend fun updateRiderLocation(rideId: String, lat: Double, lng: Double) {
                try { client.from("rides").update(buildJsonObject { put("rider_live_lat", lat); put("rider_live_lng", lng) }) { filter { eq("ride_id", rideId) } } }
                catch (e: Exception) { Log.e("REPO_ERROR", "Update rider location failed", e) }
        }

        suspend fun getRideLiveLocations(rideId: String): Ride? {
                return try { client.from("rides").select { filter { eq("ride_id", rideId) } }.decodeSingleOrNull<Ride>() }
                catch (e: Exception) { Log.e("REPO_ERROR", "Get live locations failed", e); null }
        }

        suspend fun getNextRecurringRide(recurringGroupId: String, afterDepartureTime: String): Ride? {
                return try {
                        client.from("rides").select(Columns.raw("*, users(*), vehicles(*)")) {
                                filter { and { eq("recurring_group_id", recurringGroupId); gt("departure_time", afterDepartureTime); eq("ride_status", "scheduled") } }
                                order("departure_time", Order.ASCENDING); limit(1)
                        }.decodeSingleOrNull<Ride>()
                } catch (e: Exception) { Log.e("REPO_ERROR", "Get next recurring ride failed", e); null }
        }

        suspend fun bookRide(
                riderId: String, rideId: String, pickupLocation: String = "", pickupLat: Double = 0.0, pickupLng: Double = 0.0,
                dropoffLocation: String = "", dropoffLat: Double = 0.0, dropoffLng: Double = 0.0,
                estimatedDistanceMeters: Int? = null, estimatedDurationSeconds: Int? = null
        ): Result<Unit> {
                return try {
                        client.from("bookings").insert(buildJsonObject {
                                put("rider_id", riderId); put("ride_id", rideId); put("pickup_location", pickupLocation)
                                put("pickup_lat", pickupLat); put("pickup_lng", pickupLng); put("dropoff_location", dropoffLocation)
                                put("dropoff_lat", dropoffLat); put("dropoff_lng", dropoffLng); put("seats_booked", 1); put("booking_status", "confirmed")
                                if (estimatedDistanceMeters != null) put("estimated_distance_meters", estimatedDistanceMeters)
                                if (estimatedDurationSeconds != null) put("estimated_duration_seconds", estimatedDurationSeconds)
                        })
                        Result.success(Unit)
                } catch (e: Exception) { Log.e("REPO_ERROR", "Book ride failed", e); Result.failure(e) }
        }

        fun passesHardMemoryFilters(ride: Ride, riderId: String, rider: RideUser, alreadyBookedIds: Set<String>, blockedByRider: Set<String>, blockedByDriver: Set<String>): Boolean {
                val nowUtc = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
                val departure = runCatching { java.time.OffsetDateTime.parse(ride.departureTime) }.getOrNull() ?: return false
                return ride.id !in alreadyBookedIds && departure.isAfter(nowUtc.plusMinutes(30)) && ride.driverId !in blockedByRider && riderId !in blockedByDriver
        }

        fun passesSoftFilters(ride: Ride, riderId: String, rider: RideUser): Boolean {
                val riderPref = rider.genderPreference == null || rider.genderPreference == "ANY" || rider.genderPreference == ride.users?.userGender
                val driverPref = ride.users?.genderPreference == null || ride.users.genderPreference == "ANY" || ride.users.genderPreference == rider.userGender
                return riderPref && driverPref
        }

        fun isWithinRadiusKm(pickupLat: Double, pickupLng: Double, centerLat: Double, centerLng: Double, radiusKm: Double): Boolean {
                val R = 6371.0; val dLat = Math.toRadians(pickupLat - centerLat); val dLng = Math.toRadians(pickupLng - centerLng)
                val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(centerLat)) * cos(Math.toRadians(pickupLat)) * sin(dLng / 2) * sin(dLng / 2)
                return R * 2 * atan2(sqrt(a), sqrt(1 - a)) <= radiusKm
        }

        fun sortRides(rides: List<MapsRepository.RideWithDetour>): List<MapsRepository.RideWithDetour> {
                val nowUtc = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
                return rides.sortedWith(compareBy(
                        { val dep = runCatching { java.time.OffsetDateTime.parse(it.ride.departureTime) }.getOrNull(); if (dep != null && dep.isBefore(nowUtc.plusMinutes(15))) 0 else 1 },
                        { when (it.ride.vehicleType.uppercase()) { "EV" -> 0; "HYBRID" -> 1; else -> 2 } },
                        { -(it.ride.users?.driverRating ?: 0.0) },
                        { it.addedMinutes }
                ))
        }
}