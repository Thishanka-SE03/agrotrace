package com.lnbti.agrotrace.viewmodel

import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lnbti.agrotrace.BuildConfig
import com.lnbti.agrotrace.GeminiClient
import com.lnbti.agrotrace.OcrPrompts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

sealed class ExtractionState {
    object Idle : ExtractionState()
    object Loading : ExtractionState()
    data class Success(val data: String) : ExtractionState()
    data class Error(val message: String) : ExtractionState()
}

class ScannerViewModel : ViewModel() {

    private val _extractionState = MutableStateFlow<ExtractionState>(ExtractionState.Idle)
    val extractionState: StateFlow<ExtractionState> = _extractionState

    private val responseJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = true
    }

    fun extractData(imagePath: String, docType: Int) {
        viewModelScope.launch {
            _extractionState.value = ExtractionState.Loading

            try {
                if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                    throw IllegalStateException("Gemini API key is not configured in local.properties")
                }

                val imageFile = File(imagePath)
                if (!imageFile.isFile) {
                    throw IllegalArgumentException("The scanned document image could not be found")
                }

                val bitmap = withContext(Dispatchers.IO) {
                    BitmapFactory.decodeFile(imageFile.absolutePath)
                } ?: throw IllegalStateException("Failed to decode the scanned document image")

                val prompt = OcrPrompts.forType(docType)
                val client = GeminiClient(BuildConfig.GEMINI_API_KEY)

                client.generateContent(prompt, bitmap).onSuccess { response ->
                    runCatching { cleanAndValidateJson(response) }
                        .onSuccess { cleanJson ->
                            _extractionState.value = ExtractionState.Success(cleanJson)
                        }
                        .onFailure { error ->
                            _extractionState.value = ExtractionState.Error(
                                error.message ?: "Gemini returned an unreadable extraction result"
                            )
                        }
                }.onFailure { error ->
                    _extractionState.value = ExtractionState.Error(
                        error.message ?: "Extraction failed"
                    )
                }
            } catch (error: Exception) {
                _extractionState.value = ExtractionState.Error(
                    error.message ?: "An unexpected extraction error occurred"
                )
            }
        }
    }

    /**
     * Gemini occasionally wraps otherwise valid JSON in markdown fences. Strip only
     * that wrapper, isolate the JSON object and validate it before opening review.
     */
    private fun cleanAndValidateJson(response: String): String {
        var cleaned = response.trim()
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val objectStart = cleaned.indexOf('{')
        val objectEnd = cleaned.lastIndexOf('}')
        if (objectStart >= 0 && objectEnd > objectStart) {
            cleaned = cleaned.substring(objectStart, objectEnd + 1)
        }

        val parsed = runCatching { responseJson.parseToJsonElement(cleaned) }
            .getOrElse {
                throw IllegalArgumentException(
                    "Gemini did not return valid JSON. Please scan the document again."
                )
            }

        return parsed.toString()
    }
}
