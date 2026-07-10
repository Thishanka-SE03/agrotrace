package com.agrotrace.scanner.data.repository

import android.content.Context
import com.agrotrace.scanner.data.preferences.ScannerPreferences
import com.agrotrace.scanner.data.remote.ApiErrorParser
import com.agrotrace.scanner.data.remote.OcrScanApi
import com.agrotrace.scanner.data.remote.ProgressRequestBody
import com.agrotrace.scanner.data.remote.dto.ClaimScanRequestDto
import com.agrotrace.scanner.domain.model.CapturedDocument
import com.agrotrace.scanner.domain.model.PairingPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class OcrScanRepositoryImpl(
    private val context: Context,
    private val api: OcrScanApi,
    private val preferences: ScannerPreferences
) : OcrScanRepository {

    override suspend fun claim(pairing: PairingPayload): AppResult<Unit> = withContext(Dispatchers.IO) {
        executeSafely {
            val baseUrl = preferences.getBaseUrl().trimEnd('/')
            val deviceId = preferences.getOrCreateDeviceId()
            val response = api.claimScan(
                url = "$baseUrl/api/v1/ocr/scans/${pairing.scanId}/claim",
                request = ClaimScanRequestDto(
                    deviceId = deviceId,
                    pairingCode = pairing.pairingCode,
                    claimToken = pairing.claimToken
                )
            )

            if (response.isSuccessful) {
                response.body()?.close()
                AppResult.Success(Unit)
            } else {
                AppResult.Failure(
                    message = ApiErrorParser.message(response.code(), response.errorBody()),
                    statusCode = response.code()
                )
            }
        }
    }

    override suspend fun upload(
        pairing: PairingPayload,
        document: CapturedDocument,
        onProgress: (Float) -> Unit
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        executeSafely {
            val baseUrl = preferences.getBaseUrl().trimEnd('/')
            val deviceId = preferences.getOrCreateDeviceId()
            val resolver = context.contentResolver
            val mimeType = resolver.getType(document.uri) ?: "image/jpeg"

            val imageBody = ProgressRequestBody(
                contentResolver = resolver,
                uri = document.uri,
                mediaType = mimeType.toMediaTypeOrNull()
            ) { uploaded, total ->
                if (total > 0) onProgress((uploaded.toDouble() / total).toFloat().coerceIn(0f, 1f))
            }

            val imagePart = MultipartBody.Part.createFormData(
                name = "image",
                filename = document.displayName.ifBlank { "agrotrace-scan.jpg" },
                body = imageBody
            )
            val textType = "text/plain".toMediaTypeOrNull()

            val response = api.uploadImage(
                url = "$baseUrl/api/v1/ocr/scans/${pairing.scanId}/image",
                image = imagePart,
                deviceId = deviceId.toRequestBody(textType),
                claimToken = pairing.claimToken.toRequestBody(textType)
            )

            if (response.isSuccessful) {
                response.body()?.close()
                onProgress(1f)
                AppResult.Success(Unit)
            } else {
                AppResult.Failure(
                    message = ApiErrorParser.message(response.code(), response.errorBody()),
                    statusCode = response.code()
                )
            }
        }
    }

    private suspend fun <T> executeSafely(block: suspend () -> AppResult<T>): AppResult<T> = try {
        block()
    } catch (error: IOException) {
        AppResult.Failure(
            "Could not connect to the OCR server. Check the backend URL and network connection."
        )
    } catch (error: Exception) {
        AppResult.Failure(error.message ?: "An unexpected scanner error occurred.")
    }
}
