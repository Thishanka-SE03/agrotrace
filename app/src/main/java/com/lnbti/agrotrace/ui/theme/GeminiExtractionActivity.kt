package com.lnbti.agrotrace.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.ImagePart
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.TextPart
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class GeminiExtractionActivity : AppCompatActivity() {

    private val viewModel: ExtractionViewModel by viewModels()

    companion object {
        private const val EXTRA_FIELD_FILES = "extra_field_files"

        fun start(context: AppCompatActivity, fieldFiles: Map<String, File>) {
            ExtractionState.fieldFiles = fieldFiles
            context.startActivity(
                android.content.Intent(context, GeminiExtractionActivity::class.java)
            )
        }
    }

    object ExtractionState {
        @JvmStatic var fieldFiles: Map<String, File> = emptyMap()
    }

    // Initialize Gemini model
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY, // Store in local.properties
            generationConfig = generationConfig {
                temperature = 0.1f  // Low temperature for consistent extraction
                topK = 20
                topP = 0.9f
                maxOutputTokens = 1024
            },
            safetySettings = listOf(
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE)
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_extraction)

        val fieldFiles = ExtractionState.fieldFiles
        if (fieldFiles.isEmpty()) {
            Toast.makeText(this, "No fields to process", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Start extraction
        extractAllFields(fieldFiles)
    }

    private fun extractAllFields(fieldFiles: Map<String, File>) {
        lifecycleScope.launch {
            viewModel.isExtracting.value = true
            val results = mutableMapOf<String, String>()

            // Process each field image through Gemini
            for ((fieldName, file) in fieldFiles) {
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        BitmapFactory.decodeFile(file.absolutePath)
                    }

                    val prompt = buildPromptForField(fieldName)

                    val content = Content(
                        parts = listOf(
                            TextPart(prompt),
                            ImagePart(bitmap)
                        )
                    )

                    val response = generativeModel.generateContent(content)
                    val extractedText = response.text?.trim() ?: ""

                    results[fieldName] = extractedText
                    Log.d("Gemini", "Extracted $fieldName: $extractedText")

                    // Update progress
                    viewModel.extractedFields.value = results.toMap()

                } catch (e: Exception) {
                    Log.e("Gemini", "Failed to extract $fieldName", e)
                    results[fieldName] = ""
                }
            }

            // Alternative: Extract ALL fields at once with structured JSON output
            // Uncomment below if you want single-call extraction:
            // val allFieldsJson = extractStructured(fieldFiles.values.first())

            viewModel.isExtracting.value = false
            viewModel.extractedFields.value = results

            // Save to database and navigate
            saveToDatabase(results)
        }
    }

    /**
     * Build specific prompt for each field type
     */
    private fun buildPromptForField(fieldName: String): String {
        return when (fieldName) {
            "full_name" -> "Extract the full name from this document image. Return ONLY the name, nothing else."
            "document_number" -> "Extract the document/ID number from this image. Return ONLY the number."
            "date_of_birth" -> "Extract the date of birth from this image. Return in YYYY-MM-DD format only."
            "expiry_date" -> "Extract the expiry date from this image. Return in YYYY-MM-DD format only."
            "address" -> "Extract the full address from this image. Return ONLY the address text."
            else -> "Extract the text from this image field. Return ONLY the extracted value."
        }
    }

    /**
     * Alternative: Single-call structured extraction with JSON output
     */
    private suspend fun extractStructured(documentBitmap: Bitmap): String? {
        val jsonSchema = """
        {
            "type": "object",
            "properties": {
                "full_name": {"type": "string"},
                "document_number": {"type": "string"},
                "date_of_birth": {"type": "string"},
                "expiry_date": {"type": "string"},
                "address": {"type": "string"}
            },
            "required": ["full_name", "document_number"]
        }
        """.trimIndent()

        val content = Content(
            parts = listOf(
                TextPart("Extract all fields from this ID document. Return ONLY valid JSON matching this schema:\n$jsonSchema"),
                ImagePart(documentBitmap)
            )
        )

        val response = generativeModel.generateContent(content)
        return response.text
    }

    private fun saveToDatabase(fields: Map<String, String>) {
        lifecycleScope.launch(Dispatchers.IO) {
            val document = ScannedDocument(
                fullName = fields["full_name"] ?: "",
                documentNumber = fields["document_number"] ?: "",
                dateOfBirth = fields["date_of_birth"] ?: "",
                expiryDate = fields["expiry_date"] ?: "",
                address = fields["address"] ?: "",
                timestamp = System.currentTimeMillis(),
                syncStatus = "pending" // Will sync when online
            )

            val id = viewModel.insertDocument(document)

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@GeminiExtractionActivity,
                    "Document saved: ${document.fullName}",
                    Toast.LENGTH_LONG
                ).show()

                // Navigate to results or home
                ResultsActivity.start(this@GeminiExtractionActivity, document)
                finish()
            }
        }
    }
}