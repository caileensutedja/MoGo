package com.fit3161.fit3162.mogo.data.remote

import com.fit3161.fit3162.mogo.data.remote.dto.RoutesRequest
import com.fit3161.fit3162.mogo.data.remote.dto.RoutesResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for Google Routes API.
 * Base URL: https://routes.googleapis.com/
 */
interface RoutesApiService {

    @POST("directions/v2:computeRoutes")
    suspend fun computeRoutes(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String =
            "routes.duration," +
                    "routes.distanceMeters," +
                    "routes.polyline," +
                    "routes.viewport," +
                    "routes.legs.startLocation," +
                    "routes.legs.endLocation," +
                    "routes.legs.localizedValues",
        // Headers to support restricted API keys
//        @Header("X-Android-Package") packageName: String,
//        @Header("X-Android-Cert") sha1Fingerprint: String,
        @Body request: RoutesRequest
    ): RoutesResponse
}
