package com.fit3161.fit3162.mogo.data.remote

import com.fit3161.fit3162.mogo.data.remote.dto.RoutesRequest
import com.fit3161.fit3162.mogo.data.remote.dto.RoutesResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for Google Routes API.
 * Base URL: https://routes.googleapis.com/
 *
 * X-Goog-FieldMask is required — you are only billed for fields you request.
 */
interface RoutesApiService {

    @POST("directions/v2:computeRoutes")
    suspend fun computeRoutes(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String =
            "routes.duration," +
                    "routes.distanceMeters," +
                    "routes.polyline," +
                    "routes.bounds," +
                    "routes.legs.startLocation," +
                    "routes.legs.endLocation," +
                    "routes.legs.localizedValues",
        @Body request: RoutesRequest
    ): RoutesResponse
}
