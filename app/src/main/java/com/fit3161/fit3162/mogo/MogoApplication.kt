package com.fit3161.fit3162.mogo

import android.app.Application
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MogoApplication : Application() {

    lateinit var supabase : SupabaseClient
        private set

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            supabase = createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Auth) {
//                    scheme = "mogo"
//                    host = "login-callback"
                }
                install(Postgrest)
                install(Realtime)

                // OkHttp engine is more stable than ktor-client-android on emulators
//                httpEngine = OkHttp.create()
            }
        }
        }

}