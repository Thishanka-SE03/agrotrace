package com.lnbti.agrotrace

import android.graphics.Bitmap
import android.util.Base64
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

@Serializable
data class GeminiRequest(
    val contents: List<ContentPart>
)

@Serializable
data class ContentPart(
    val parts: List<PartData>
)

@Serializable
data class PartData(
    val text: String? = null,
    val inline_data: InlineData? = null
)

@Serializable
data class InlineData(
    val mime_type: String,
    val data: String
)

@Serializable
data class GeminiResponse(
    val candidates: List<Candidate>? = null,
    val error: GeminiError? = null
)

@Serializable
data class Candidate(
    val content: ContentPart
)

@Serializable
data class GeminiError(
    val code: Int,
    val message: String,
    val status: String
)

class GeminiClient(private val apiKey: String) {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { 
                ignoreUnknownKeys = true 
                explicitNulls = false
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
        }
    }

    suspend fun generateContent(prompt: String, bitmap: Bitmap): Result<String> {
        return try {
            val base64Image = bitmapToBase64(bitmap)
            
            val requestBody = GeminiRequest(
                contents = listOf(
                    ContentPart(
                        parts = listOf(
                            PartData(text = prompt),
                            PartData(
                                inline_data = InlineData(
                                    mime_type = "image/jpeg",
                                    data = base64Image
                                )
                            )
                        )
                    )
                )
            )

            // Using gemini-2.5-flash which is the stable production model as of June 2026
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val response: HttpResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status.isSuccess()) {
                val geminiResponse = response.body<GeminiResponse>()
                val text = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (text != null) {
                    Result.success(text)
                } else {
                    Result.failure(Exception("No text in response"))
                }
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(Exception("API Error ${response.status}: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
