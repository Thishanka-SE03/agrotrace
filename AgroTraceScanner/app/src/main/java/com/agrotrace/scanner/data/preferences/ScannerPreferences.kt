package com.agrotrace.scanner.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agrotrace.scanner.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.scannerDataStore by preferencesDataStore(name = "agrotrace_scanner")

class ScannerPreferences(private val context: Context) {

    val baseUrlFlow: Flow<String> = context.scannerDataStore.data.map { values ->
        values[BASE_URL] ?: BuildConfig.DEFAULT_API_BASE_URL
    }

    suspend fun getBaseUrl(): String = baseUrlFlow.first()

    suspend fun setBaseUrl(baseUrl: String) {
        context.scannerDataStore.edit { values ->
            values[BASE_URL] = baseUrl
        }
    }

    suspend fun getOrCreateDeviceId(): String {
        val existing = context.scannerDataStore.data.first()[DEVICE_ID]
        if (!existing.isNullOrBlank()) return existing

        val generated = UUID.randomUUID().toString()
        context.scannerDataStore.edit { values ->
            if (values[DEVICE_ID].isNullOrBlank()) {
                values[DEVICE_ID] = generated
            }
        }
        return context.scannerDataStore.data.first()[DEVICE_ID] ?: generated
    }

    private companion object {
        val BASE_URL = stringPreferencesKey("ocr_api_base_url")
        val DEVICE_ID = stringPreferencesKey("scanner_device_id")
    }
}
