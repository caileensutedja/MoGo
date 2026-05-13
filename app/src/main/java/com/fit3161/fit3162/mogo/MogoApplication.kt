package com.fit3161.fit3162.mogo

import android.app.Application
import android.util.Log
import com.fit3161.fit3162.mogo.data.remote.RoutesApiService
import com.fit3161.fit3162.mogo.data.repo.MapsRepository
import com.fit3161.fit3162.mogo.data.repo.PlacesRepository
import com.google.android.gms.maps.MapsInitializer
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
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

    // REMOVED: old commented-out mapsRepository block
    // REMOVED: duplicate "// 2. MapsRepository..." comment

    val mapsRepository: MapsRepository by lazy {

        // ADDED: dedicated logging interceptor with RoutesAPI tag
        // so 403 response bodies are visible in Logcat under "RoutesAPI"
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
                    // CHANGED: was hardcoded "com.fit3161.fit3162.mogo"
                    // now uses BuildConfig.APPLICATION_ID so it stays in sync automatically
                    .addHeader("X-Android-Package", BuildConfig.APPLICATION_ID)
                    // CHANGED: was hardcoded SHA-1 with colons "11:AF:B9:DF:..."
                    // now reads from local.properties via secrets plugin
                    // IMPORTANT: value in local.properties must be WITHOUT colons
                    // e.g. MAPS_SHA1_FINGERPRINT=11AFB9DF9355C84F31C01DE7C31372247E25B294
                    .addHeader("X-Android-Cert", BuildConfig.MAPS_SHA1_FINGERPRINT)
                    .build()

                // ADDED: debug logging to verify headers are correct
                Log.d("RoutesAPI", "Request URL: ${request.url}")
                Log.d("RoutesAPI", "X-Android-Package: ${BuildConfig.APPLICATION_ID}")
                Log.d("RoutesAPI", "X-Android-Cert: ${BuildConfig.MAPS_SHA1_FINGERPRINT}")

                chain.proceed(request)
            }
            // ADDED: attach the logging interceptor
            .addInterceptor(loggingInterceptor)
            // REMOVED: the old .apply { if (DEBUG) ... } block — replaced by loggingInterceptor above
            .build()

        val routesApiService = Retrofit.Builder()
            .baseUrl("https://routes.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RoutesApiService::class.java)

        MapsRepository(
            apiService = routesApiService,
            apiKey = BuildConfig.MAPS_API_KEY,
            fusedLocationProviderClient = com.google.android.gms.location.LocationServices
                .getFusedLocationProviderClient(this)
        )
    }

    /**
     * Wraps the Google Places SDK for autocomplete + place details lookups.
     * Used by the upload-ride form for address autocomplete.
     *
     * Places.initialize() must run before this is accessed — see onCreate().
     */
    val placesRepository: PlacesRepository by lazy {
        val placesClient: PlacesClient = Places.createClient(this)
        PlacesRepository(placesClient)
    }

    override fun onCreate() {
        super.onCreate()

        // ADDED: Initialize Places SDK before anything tries to create a PlacesClient.
        // Idempotent — guarded with isInitialized() in case of process restarts.
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }

        // CHANGED: was wrapped in Thread { ... }.start()
        // MapsInitializer MUST run on the main thread (Play Services requirement)
        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST) { renderer ->
            Log.d("MapsApp", "Renderer callback triggered: $renderer")
        }

        // ADDED: warm up repository on background thread so Retrofit/OkHttp
        // class loading doesn't block the first UI frame
        Thread {
            mapsRepository // triggers the lazy block
        }.start()
    }
}

//package com.fit3161.fit3162.mogo
//
//import android.app.Application
//import android.util.Log
//import com.fit3161.fit3162.mogo.data.remote.RoutesApiService
//import com.fit3161.fit3162.mogo.data.repo.MapsRepository
//import com.google.android.gms.maps.MapsInitializer
//import io.github.jan.supabase.SupabaseClient
//import io.github.jan.supabase.auth.Auth
//import io.github.jan.supabase.createSupabaseClient
//import io.github.jan.supabase.postgrest.Postgrest
//import io.github.jan.supabase.realtime.Realtime
//import okhttp3.OkHttpClient
//import okhttp3.logging.HttpLoggingInterceptor
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//
///**
// * Custom Application class that acts as entry point of the app.
// * This allows for singletons/"single source of truths" objects (e.g. maps navigation client).
// *
// * A custom Application class should be added/registered to AndroidManifest.xml file.
// *
// * Contains:
// * - Supabase client (for database, authentication, realtime db, etc.).
// *
// */
//class MogoApplication : Application() {
//
//    val supabase: SupabaseClient by lazy {
//        createSupabaseClient(
//            supabaseUrl = BuildConfig.SUPABASE_URL,
//            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
//        ) {
//            install(Auth)
//            install(Postgrest)
//            install(Realtime)
//        }
//    }
//
//    val mapsRepository: MapsRepository by lazy {
//        val okHttpClient = OkHttpClient.Builder()
//            .addInterceptor { chain ->
//                // Inject Android verification headers for the restricted API Key
//                val request = chain.request().newBuilder()
//                    .addHeader("X-Android-Package", BuildConfig.APPLICATION_ID)
//                    // Provide your exact SHA-1 fingerprint here (colons are usually fine,
//                    // but if it fails, remove the colons: 11AFB9DF...)
//                    .addHeader("X-Android-Cert", BuildConfig.MAPS_SHA1_FINGERPRINT)
//                    .build()
//                chain.proceed(request)
//            }
//            .apply {
//                if (BuildConfig.DEBUG) {
//                    addInterceptor(HttpLoggingInterceptor().apply {
//                        level = HttpLoggingInterceptor.Level.BODY
//                    })
//                }
//            }
//            .build()
//
//        val routesApiService = Retrofit.Builder()
//            .baseUrl("https://routes.googleapis.com/")
//            .client(okHttpClient)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//            .create(RoutesApiService::class.java)
//
//        MapsRepository(
//            apiService = routesApiService,
//            apiKey = BuildConfig.MAPS_API_KEY,
//            fusedLocationProviderClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this) // TODO: Refactor if possible
//        )
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//
//        // Force the modern renderer globally before the UI starts
//        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST) { renderer ->
//            Log.d("MapsApp", "Renderer callback triggered: $renderer")
//        }
//
////        Thread {
////            mapsRepository
////        }.start()
//
//    }
//
//}