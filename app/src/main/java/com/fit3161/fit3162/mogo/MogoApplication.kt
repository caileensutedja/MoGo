package com.fit3161.fit3162.mogo

import android.app.Application
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

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
    }

}
