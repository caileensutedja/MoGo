package com.fit3161.fit3162.mogo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fit3161.fit3162.mogo.ui.theme.MoGoTheme
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

// Supabase client.
val supabase = createSupabaseClient(
    supabaseUrl = "https://kzxwaxbvhztbkgkancgh.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imt6eHdheGJ2aHp0Ymtna2FuY2doIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzI5NDg5MzAsImV4cCI6MjA4ODUyNDkzMH0.070ky28r-JqP7Nxttuivo8eEFb_2QDnXxND-yM8paHw" // Public anon key
) {
    install(Postgrest)
}
// Notes:
// - Public anon key = anon key and NOT publishable key.
// - Supabase might be updating how it uses API keys and is in the middle of the transition? Unsure.
//   Source:
// https://supabase.com/docs/guides/getting-started/quickstarts/kotlin#:~:text=Changes%20to%20API,Publishable%20key%20section.

@Serializable
data class User(
    val id: Int,
    val firstname: String,
    val surname: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoGoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                    // Code taken directly from Supabase tutorial page.
                    // Surface container using bg color from theme.
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        UsersList()
                    }
                }
            }
        }
    }
}

// Use LaunchedEffect to fetch data from the database and display it in a LazyColumn.
// From Supabase's tutorial page directly:
// Note that we are making a network request from our UI code.
// In production, you should probably use a ViewModel to separate the UI and data fetching logic.
@Composable
fun UsersList() {
    var users by remember { mutableStateOf<List<User>>(listOf()) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            users = supabase.from("users").select().decodeList<User>()
        }
    }
    LazyColumn {
        items(
            users,
            key = { user -> user.id }
        ) {
            user ->
            Text(user.firstname,
                modifier = Modifier.padding(8.dp),
                )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MoGoTheme {
        Greeting("Android")
    }
}