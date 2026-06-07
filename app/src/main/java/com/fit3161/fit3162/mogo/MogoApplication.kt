package com.fit3161.fit3162.mogo

import android.app.Application
import android.util.Log
import com.fit3161.fit3162.mogo.data.remote.RoutesApiService
import com.fit3161.fit3162.mogo.data.repo.MapsRepository
import com.fit3161.fit3162.mogo.data.repo.PlacesRepository
import com.google.android.gms.maps.MapsInitializer
import com.google.android.libraries.places.api.Places
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MogoApplication : Application() {

    // Define Supabase client instance for db connection/repos when needed.
    val supabase: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }

    val mapsRepository: MapsRepository by lazy {

        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("RoutesAPI", message)
        }.apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .addHeader("X-Android-Package", BuildConfig.APPLICATION_ID)
                    .addHeader("X-Android-Cert", BuildConfig.MAPS_SHA1_FINGERPRINT)
                    .build()
                Log.d("RoutesAPI", "Request URL: ${request.url}")
                Log.d("RoutesAPI", "X-Android-Package: ${BuildConfig.APPLICATION_ID}")
                Log.d("RoutesAPI", "X-Android-Cert: ${BuildConfig.MAPS_SHA1_FINGERPRINT}")
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .build()

        val routesApiService = Retrofit.Builder()
            .baseUrl("https://routes.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RoutesApiService::class.java)

        MapsRepository(
            context = this,
            apiService = routesApiService,
            apiKey = BuildConfig.MAPS_API_KEY,
            fusedLocationProviderClient = com.google.android.gms.location.LocationServices
                .getFusedLocationProviderClient(this)
        )
    }

    val placesRepository: PlacesRepository by lazy {
        PlacesRepository(Places.createClient(this))
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Places SDK (must be called before creating PlacesClient)
        Places.initializeWithNewPlacesApiEnabled(this, BuildConfig.MAPS_API_KEY)

        // Force the modern renderer globally before the UI starts
        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST) { renderer ->
            Log.d("MapsApp", "Renderer callback triggered: $renderer")
        }

        // Warm up repository on background thread
        Thread { mapsRepository }.start()
    }
}
