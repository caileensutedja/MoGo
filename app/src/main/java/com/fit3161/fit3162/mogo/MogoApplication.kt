package com.fit3161.fit3162.mogo

import android.app.Application
import android.util.Log
import com.fit3161.fit3162.mogo.data.remote.RoutesApiService
import com.fit3161.fit3162.mogo.data.repo.MapsRepository
import com.google.android.gms.maps.MapsInitializer
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


    // 2. MapsRepository and its dependencies created on demand
//    val mapsRepository: MapsRepository by lazy {
//        val okHttpClient = OkHttpClient.Builder()
//            .apply {
//                if (BuildConfig.DEBUG) {
//                    addInterceptor(HttpLoggingInterceptor().apply {
//                        level = HttpLoggingInterceptor.Level.BODY
//                    })
//                }
//            }
//            .build()
    // 2. MapsRepository and its dependencies created on demand
    val mapsRepository: MapsRepository by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                // Inject Android verification headers for the restricted API Key
                val request = chain.request().newBuilder()
                    .addHeader("X-Android-Package", "com.fit3161.fit3162.mogo")
                    // Provide your exact SHA-1 fingerprint here (colons are usually fine,
                    // but if it fails, remove the colons: 11AFB9DF...)
                    .addHeader("X-Android-Cert", "11:AF:B9:DF:93:55:C8:4F:31:C0:1D:E7:C3:13:72:24:7E:25:B2:94")
                    .build()
                chain.proceed(request)
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
            }
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
            fusedLocationProviderClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this) // TODO: Refactor if possible
        )
    }

    override fun onCreate() {
        super.onCreate()

        // Force the modern renderer globally before the UI starts
        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST) { renderer ->
            Log.d("MapsApp", "Renderer callback triggered: $renderer")
        }


    }

}
