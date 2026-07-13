package com.lnbti.agrotrace.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lnbti.agrotrace.BuildConfig
import com.lnbti.agrotrace.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    fun extractData(imagePath: String, docType: Int) {
        viewModelScope.launch {
            _extractionState.value = ExtractionState.Loading
            
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    BitmapFactory.decodeFile(imagePath)
                } ?: throw Exception("Failed to decode image")

                val prompt = getPromptForType(docType)
                val client = GeminiClient(BuildConfig.GEMINI_API_KEY)
                
                client.generateContent(prompt, bitmap).onSuccess { jsonString ->
                    val cleanJson = cleanJsonResponse(jsonString)
                    _extractionState.value = ExtractionState.Success(cleanJson)
                }.onFailure { e ->
                    _extractionState.value = ExtractionState.Error(e.message ?: "Extraction failed")
                }
            } catch (e: Exception) {
                _extractionState.value = ExtractionState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    private fun cleanJsonResponse(jsonString: String): String {
        return jsonString.trim()
            .removeSurrounding("```json", "```")
            .removeSurrounding("```")
            .trim()
    }

    private fun getPromptForType(type: Int): String {
        val typeName = when(type) {
            1 -> "Land Approval Form"
            2 -> "Crop Registration Form"
            3 -> "Field/Lot Inspection Report"
            4 -> "Final Field Inspection Report"
            5 -> "Seed Test Request Form"
            6 -> "Seed Test Report"
            7 -> "Labeling Document"
            else -> "Form"
        }

        val fields = when(type) {
            1 -> "{ 'see_act_registration_no': '...', 'form_date': '...', 'farmer_name': '...', 'address': '...', 'lot_no_for_seeds': [...], 'land_address': [...], 'transplanted_date': [...], 'crop_id': [...], 'variety': [...], 'land_area': [...], 'quantity_of_seeds_used': [...] }"
            2 -> "{ 'registration_no': '...', 'form_no': '...', 'seed_act_registration_no': '...', 'farmer_registration_no': '...', 'date': '...', 'name_of_seed_producer': '...', 'address_of_seed_producer': '...', 'field_no': [...], 'crop_grown_in_last_two_seasons': [...], 'harvest_date': [...], 'payment_no': '...', 'payment_amount': '...', 'registration_officer': '...' }"
            3 -> "{ 'inspection_no': '...', 'inspection_date': '...', 'seed_act_registration_no': '...', 'farmer_registration_no': '...', 'field_no': '...', 'observation': '...', 'inspection_round': '...' }"
            4 -> "{ 'harvest_inspect_no': '...', 'farmer_registration_no': '...', 'final_inspection_date': '...', 'extent_accepted': '...', 'extent_rejected': '...', 'estimated_seed_yield': '...', 'decision': [...], 'officer_sign': { 'name': '...' } }"
            5 -> "{ 'request_no': '...', 'date': '...', 'lot_no': '...', 'crop': '...', 'variety': '...', 'class_of_seed': '...', 'weight_of_lot': '...', 'no_of_containers': '...', 'sender_name': '...', 'sender_address': '...' }"
            6 -> "{ 'report_no': '...', 'test_date': '...', 'germination_percentage': '...', 'purity_percentage': '...', 'moisture_content': '...', 'inert_matter': '...', 'other_seeds': '...', 'status': '...' }"
            7 -> "{ 'label_serial_no': '...', 'lot_no': '...', 'crop': '...', 'variety': '...', 'date_of_test': '...', 'valid_until': '...', 'net_weight': '...' }"
            else -> "{}"
        }

        return """
            You are an expert document understanding AI specializing in Sinhala agricultural documents.
            Analyze the provided image and extract information for $typeName.
            
            Return ONLY valid JSON. No markdown, no explanations.
            
            $fields
            
            RULES:
            - Preserve Sinhala and English exactly as written.
            - Dates: Convert to YYYY-MM-DD.
            - If missing, return null.
        """.trimIndent()
    }
}
