package com.fit3161.fit3162.mogo

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class TestItem(
    val id: Int? = null,
    val message: String
)

class TestViewModel : ViewModel() {

    fun testConnection() {
        viewModelScope.launch {
            try {
                val result = supabase
                    .from("test")
                    .select()
                    .decodeList<TestItem>()

                Log.d("SupabaseTest", "Success! Data: $result")
            } catch (e: Exception) {
                Log.e("SupabaseTest", "Failed: ${e.message}")
            }
        }
    }
}