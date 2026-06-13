package com.lnbti.agrotrace.ui.theme

import android.content.Context
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.Serializable

// Make the client accessible app-wide
object SupabaseClient {
    lateinit var client: io.github.jan.supabase.SupabaseClient
        private set

    fun initialize(context: Context) {
        client = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Storage)
            // install(Auth) // Uncomment if using Supabase Auth
        }
    }
}

// Data class matching your Supabase table
@Serializable
data class DocumentRecord(
    val id: String? = null,
    val user_id: String? = null,
    val full_name: String,
    val document_number: String,
    val date_of_birth: String,
    val expiry_date: String,
    val address: String,
    val aligned_image_url: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)