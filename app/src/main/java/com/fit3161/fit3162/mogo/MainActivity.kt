package com.fit3161.fit3162.mogo

import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.fit3161.fit3162.mogo.ui.navigation.AppNavigation
import com.fit3161.fit3162.mogo.ui.theme.MoGoTheme
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        // Handle the deep link from the verification email
//        val supabase = (application as MogoApplication).supabase
//        supabase.handleDeeplinks(intent = intent)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoGoTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(application = application as MogoApplication)
                }

//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                }
            }
        }
    }

//    override fun onNewIntent(intent: Intent) {
//        super.onNewIntent(intent)
//        val supabase = (application as MogoApplication).supabase
//        supabase.handleDeeplinks(intent = intent)
//    }
}

