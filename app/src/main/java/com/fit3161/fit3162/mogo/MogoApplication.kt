package com.fit3161.fit3162.mogo

import android.app.Application
import com.fit3161.fit3162.mogo.data.remote.RoutesApiService
import com.fit3161.fit3162.mogo.data.repo.MapsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Custom Application class that acts as entry point of the app.
 * This allows for singletons/"single source of truths" objects (e.g. maps navigation client).
 *
 * A custom Application class should be added/registered to AndroidManifest.xml file.
 *
 * Contains:
 * - Supabase client (for database, authentication, realtime db, etc.).
 *
 */
class MogoApplication : Application() {

    /**
     * Supabase HTTP client instance that can be shared across different parts of the app.
     */
    lateinit var supabase: SupabaseClient
        private set


    lateinit var mapsRepository: MapsRepository
        private set


    override fun onCreate() {
        super.onCreate()

        supabase = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL, // Use BuildConfig to access Url and Key from local.properties file.
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) // For login/logout, registering users, managing logged users/sessions management.
            install(Postgrest) // Allows PostgresSQL for DB operations.
            install(Realtime)
        }

        // OkHttpClient — logging in debug only
        val okHttpClient = OkHttpClient.Builder()
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
            }
            .build()

        // Retrofit for Routes API
        val routesApiService = Retrofit.Builder()
            .baseUrl("https://routes.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RoutesApiService::class.java)

        // Maps repository
        mapsRepository = MapsRepository(
            apiService = routesApiService,
            apiKey     = BuildConfig.MAPS_API_KEY
        )

    }

}
