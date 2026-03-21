package com.fit3161.fit3162.mogo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.fit3161.fit3162.mogo.ui.navigation.AppNavigation
import com.fit3161.fit3162.mogo.ui.theme.MoGoTheme

/**
 * MainActivity that acts as entry point of app.
 * Calls [AppNavigation] (handles switching between screens).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoGoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    Surface(modifier=Modifier.padding(innerPadding)) {
                        AppNavigation(application = application as MogoApplication)
                    }

                }
            }
        }
    }
}

