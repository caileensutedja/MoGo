package com.fit3161.fit3162.mogo.data.repo

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class ImageUploadRepository(
    private val supabase: SupabaseClient,
    private val context: Context
) {

    suspend fun uploadProfileImage(userId: String, imageUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes != null) {
                    val fileName = "avatars/$userId/${UUID.randomUUID()}.jpg"
                    supabase.storage.from("avatars").upload(fileName, bytes) {
                        contentType = ContentType.Image.JPEG
                    }
                    supabase.storage.from("avatars").publicUrl(fileName)
                } else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
