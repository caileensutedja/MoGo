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
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fit3161.fit3162.mogo.ui.navigation.AppNavigation
import com.fit3161.fit3162.mogo.ui.navigation.BottomBar
import com.fit3161.fit3162.mogo.ui.navigation.Screen
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
                val navController: NavHostController = rememberNavController()

                // Current route to remember the stack
                val currentRoute =
                    navController.currentBackStackEntryAsState().value?.destination?.route

                // List of screens where we do not want to show the bottom bar
                val hideBottomBarRoutes = listOf(
                    Screen.Welcome.route,
                    Screen.Login.route,
                    Screen.Register.route
                )

                // Variable to allow bottom bar to be shown
                val showBottomBar = currentRoute != null && currentRoute !in hideBottomBarRoutes

                Scaffold(modifier = Modifier.fillMaxSize(),
                    // Show the bottom bar on approved screens
                    bottomBar = {
                        if (showBottomBar) {
                            BottomBar(navController)
                        }
                    }
                ){ innerPadding ->

                    Surface(modifier=Modifier.padding(innerPadding)) {
                        AppNavigation(
                            application = application as MogoApplication,
                            navController = navController // Navigation Controller
                        )
                    }

                }
            }
        }
    }
}

