package com.lnbti.agrotrace

import android.app.Application
import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

class DocumentScannerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeSupabase()
    }

    private fun initializeSupabase() {
        try {
            Supabase.client = createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Postgrest)
                install(Storage)
                install(Auth)  // Uncomment when adding user authentication
            }
            Log.d("App", "✅ Supabase initialized")
        } catch (e: Exception) {
            Log.e("App", "❌ Supabase init failed: ${e.message}")
        }
    }
}