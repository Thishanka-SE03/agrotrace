package com.lnbti.agrotrace

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

@Serializable
data class ExtractionResult(
    val document_type: String,
    val lot_no_for_seeds: List<String?> = emptyList(),
    val land_address: List<String?> = emptyList(),
    val transplanted_date: List<String?> = emptyList(),
    val form_date: String? = null,
    val see_act_registration_no: String? = null,
    val farmer_name: String? = null,
    val contact_no: List<String?> = emptyList(),
    val address: String? = null,
    val crop_id: List<String?> = emptyList(),
    val variety: List<String?> = emptyList(),
    val land_area: List<String?> = emptyList(),
    val quantity_of_seeds_used: List<String?> = emptyList()
)

@Serializable
data class CropRegistrationResult(
    val document_type: String,
    val registration_no: String? = null,
    val form_no: String? = null,
    val seed_act_registration_no: String? = null,
    val farmer_registration_no: String? = null,
    val date: String? = null,
    val name_of_seed_producer: String? = null,
    val address_of_seed_producer: String? = null,
    val field_no: List<String?> = emptyList(),
    val crop_grown_in_last_two_seasons: List<String?> = emptyList(),
    val harvest_date: List<String?> = emptyList(),
    val payment_no: String? = null,
    val payment_amount: String? = null,
    val registration_officer: String? = null
)

@Serializable
data class InspectionFormResult(
    val document_type: String,
    val inspection_no: String? = null,
    val inspection_date: String? = null,
    val seed_act_registration_no: String? = null,
    val farmer_registration_no: String? = null,
    val field_no: String? = null,
    val observation: String? = null,
    val inspection_round: JsonElement? = null
)

@Serializable
data class FinalInspectionResult(
    val document_type: String,
    val harvest_inspect_no: String? = null,
    val farmer_registration_no: String? = null,
    val final_inspection_date: String? = null,
    val extent_accepted: String? = null,
    val extent_rejected: String? = null,
    val estimated_seed_yield: String? = null,
    val other_distinguish_varieties: List<String?> = emptyList(),
    val pest_and_diseases: List<String?> = emptyList(),
    val remarks: List<String?> = emptyList(),
    val decision: List<String?> = emptyList(),
    val officer_sign: OfficerSign? = null
)

@Serializable
data class OfficerSign(
    val name: String? = null,
    val designation: String? = null,
    val organization: String? = null,
    val department: String? = null,
    val office: String? = null,
    val full_text: String? = null
)

class ExtractionActivity : AppCompatActivity() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var extractedData: ExtractionResult? = null
    private var cropRegData: CropRegistrationResult? = null
    private var inspectionData: InspectionFormResult? = null
    private var finalInspectionData: FinalInspectionResult? = null
    private var currentDocType: Int = 1

    companion object {
        private const val EXTRA_IMAGE_FILE = "extra_image_file"
        private const val EXTRA_DOC_TYPE = "extra_doc_type"

        fun start(context: AppCompatActivity, bitmap: Bitmap, imageFileName: String, docType: Int = 1) {
            val file = File(context.cacheDir, "extraction_${imageFileName}")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            ExtractionState.imageFile = file
            context.startActivity(
                android.content.Intent(context, ExtractionActivity::class.java).apply {
                    putExtra(EXTRA_IMAGE_FILE, file.absolutePath)
                    putExtra(EXTRA_DOC_TYPE, docType)
                }
            )
        }
    }

    object ExtractionState {
        @JvmStatic var imageFile: File? = null
    }

    private val repository = DocumentRepository()
    private lateinit var progressBar: com.google.android.material.progressindicator.CircularProgressIndicator
    private lateinit var tvStatus: TextView
    private lateinit var tvResults: TextView
    private lateinit var btnSave: Button
    private lateinit var btnRetry: Button
    private lateinit var btnCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_extraction)

        currentDocType = intent.getIntExtra(EXTRA_DOC_TYPE, 1)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)
        tvResults = findViewById(R.id.tvResults)
        btnSave = findViewById(R.id.btnSave)
        btnRetry = findViewById(R.id.btnRetry)
        btnCancel = findViewById(R.id.btnCancel)

        btnSave.visibility = View.GONE
        btnRetry.visibility = View.GONE

        btnSave.setOnClickListener { saveToSupabase() }
        btnRetry.setOnClickListener { 
            // In the new UI, this is "Export as PDF"
            Toast.makeText(this, "Export as PDF coming soon", Toast.LENGTH_SHORT).show()
        }
        btnCancel.setOnClickListener { finish() }
        findViewById<View>(R.id.btnClose).setOnClickListener { finish() }

        val imageFile = intent.getStringExtra(EXTRA_IMAGE_FILE)
        if (imageFile != null) {
            ExtractionState.imageFile = File(imageFile)
            startExtraction()
        } else {
            tvStatus.text = "No image provided"
            btnCancel.visibility = View.VISIBLE
        }
    }

    private fun startExtraction() {
        val imageFile = ExtractionState.imageFile
        if (imageFile == null || !imageFile.exists()) {
            tvStatus.text = "❌ Image file not found"
            return
        }

        btnSave.visibility = View.GONE
        btnRetry.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        tvStatus.text = "🔍 Extracting text with Gemini AI..."
        tvResults.text = ""

        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath)
            }

            if (bitmap == null) {
                tvStatus.text = "❌ Failed to decode image"
                progressBar.visibility = View.GONE
                btnRetry.visibility = View.VISIBLE
                return@launch
            }

            if (currentDocType == 1) {
                extractType1(bitmap)
            } else if (currentDocType == 2) {
                extractType2(bitmap)
            } else if (currentDocType == 3) {
                extractType3(bitmap)
            } else {
                extractType4(bitmap)
            }
        }
    }

    private suspend fun extractType1(bitmap: Bitmap) {
        val prompt = """
            You are an expert document understanding AI. Extract structured data from this Land Approval Form.
            
            ----------------------------------------------------
            HEADER FIELDS (Single Values)
            ----------------------------------------------------
            see_act_registration_no -> Value next to බීජ පනතේ ලියාපදිංචි අංකය.
            form_date -> Value next to දිනය (Form signature date, NOT transplanted date).
            
            Farmer Details (Top Right Dotted Box):
            - farmer_name: The FIRST line inside the box.
            - address: All REMAINING lines in that box combined with commas.
            
            ----------------------------------------------------
            TABLE FIELDS (JSON ARRAYS)
            ----------------------------------------------------
            The form has a main table. Extract EVERY row. Even if some values are repeated, include them in the array for each row.
            
            lot_no_for_seeds -> Values under භාවිතා කළ බීජ තොග අංකය column.
            land_address -> Values under හෝ ගොවිපල පිහිටුවා ඇති ස්ථානය column.
            transplanted_date -> Values under වගා කළ දිනය column. Convert to YYYY-MM-DD.
            contact_no -> Values under දුරකථන අංකය column.
            crop_id -> Values under භෝගය column.
            variety -> Values under ප්‍රභේදය column.
            land_area -> Values under වපසරිය column.
            quantity_of_seeds_used -> Values under භාවිතා කළ බීජ ප්‍රමාණය column.
            
            Return ONLY valid JSON.
            {
              "document_type": "land_approval_form",
              "see_act_registration_no": "string",
              "form_date": "YYYY-MM-DD",
              "farmer_name": "string",
              "address": "string",
              "lot_no_for_seeds": ["string"],
              "land_address": ["string"],
              "transplanted_date": ["YYYY-MM-DD"],
              "contact_no": ["string"],
              "crop_id": ["string"],
              "variety": ["string"],
              "land_area": ["string"],
              "quantity_of_seeds_used": ["string"]
            }
        """.trimIndent()

        val client = GeminiClient(BuildConfig.GEMINI_API_KEY)
        client.generateContent(prompt, bitmap).onSuccess { jsonString ->
            val cleanJson = cleanJsonResponse(jsonString)
            try {
                extractedData = json.decodeFromString<ExtractionResult>(cleanJson)
                withContext(Dispatchers.Main) {
                    displayType1Results(extractedData!!)
                }
            } catch (e: Exception) {
                handleError("Failed to parse response: ${e.message}")
            }
        }.onFailure { e ->
            handleError("Extraction failed: ${e.message}")
        }
    }

    private suspend fun extractType2(bitmap: Bitmap) {
        val prompt = """
            You are an expert document understanding AI specializing in extracting structured data from Sinhala agricultural documents.
            Extract data for Document Type 2: Crop Registration for Seed Certification Form.
            Return ONLY valid JSON. No markdown, no notes.
            
            {
              "document_type": "crop_registration_form",
              "registration_no": "Value next to ලියාපදිංචි අංකය",
              "form_no": "Value printed next to No. (top-right)",
              "seed_act_registration_no": "Value next to Seed Act Registration No",
              "farmer_registration_no": "Value next to ලියාපදිංචි අංකය (farmer registration)",
              "date": "Value next to දිනය",
              "name_of_seed_producer": "First line in Name and Address of Seed Producer section",
              "address_of_seed_producer": "Remaining lines in Name and Address section, combined with commas",
              "field_no": ["Values under Field No/Name column"],
              "crop_grown_in_last_two_seasons": ["Values under Crops grown during last two seasons column"],
              "harvest_date": ["Values under Approx. date of harvest column"],
              "payment_no": "Value under Payments Table No column",
              "payment_amount": "Value under Payments Table Amount column",
              "registration_officer": "Designation or office name from seal/stamp near bottom"
            }
        """.trimIndent()

        val client = GeminiClient(BuildConfig.GEMINI_API_KEY)
        client.generateContent(prompt, bitmap).onSuccess { jsonString ->
            val cleanJson = cleanJsonResponse(jsonString)
            try {
                cropRegData = json.decodeFromString<CropRegistrationResult>(cleanJson)
                withContext(Dispatchers.Main) {
                    displayType2Results(cropRegData!!)
                }
            } catch (e: Exception) {
                handleError("Failed to parse response: ${e.message}")
            }
        }.onFailure { e ->
            handleError("Extraction failed: ${e.message}")
        }
    }

    private fun cleanJsonResponse(jsonString: String): String {
        return jsonString.trim()
            .removeSurrounding("```json", "```")
            .removeSurrounding("```")
            .trim()
    }

    private suspend fun handleError(message: String) {
        withContext(Dispatchers.Main) {
            progressBar.visibility = View.GONE
            tvStatus.text = "❌ Error"
            tvResults.text = message
            btnRetry.visibility = View.VISIBLE
        }
    }

    private fun displayType1Results(data: ExtractionResult) {
        progressBar.visibility = View.GONE
        tvStatus.text = "✅ Extraction complete"
        tvResults.text = """
            Farmer: ${data.farmer_name ?: "null"}
            Reg No: ${data.see_act_registration_no ?: "null"}
            Form Date: ${data.form_date ?: "null"}
            Address: ${data.address ?: "null"}
            
            --- Table Records Found: ${data.crop_id.size} ---
            First Lot: ${data.lot_no_for_seeds.firstOrNull() ?: "null"}
            First Date: ${data.transplanted_date.firstOrNull() ?: "null"}
        """.trimIndent()
        btnSave.visibility = View.VISIBLE
        btnRetry.visibility = View.VISIBLE
    }

    private fun displayType2Results(data: CropRegistrationResult) {
        progressBar.visibility = View.GONE
        tvStatus.text = "✅ Extraction complete (Preview Only)"
        tvResults.text = """
            Reg No: ${data.registration_no ?: "null"}
            Form No: ${data.form_no ?: "null"}
            Seed Producer: ${data.name_of_seed_producer ?: "null"}
            Date: ${data.date ?: "null"}
            
            --- Table Entries: ${data.field_no.size} ---
            First Field: ${data.field_no.firstOrNull() ?: "null"}
            
            --- Payment ---
            Payment No: ${data.payment_no ?: "null"}
            Amount: ${data.payment_amount ?: "null"}
            
            Officer: ${data.registration_officer ?: "null"}
        """.trimIndent()
        btnSave.visibility = View.GONE // As requested, don't save type 2 for now
        btnRetry.visibility = View.VISIBLE
    }

    private suspend fun extractType3(bitmap: Bitmap) {
        val prompt = """
            You are an expert document understanding AI. Extract structured data from this Field/Lot Inspection Report.
            Return ONLY valid JSON. No markdown, no notes.
            
            {
              "document_type": "inspection_form",
              "inspection_no": "Value next to No. (top-right corner)",
              "inspection_date": "Value next to Date",
              "seed_act_registration_no": "Value next to Seed Act Registration No",
              "farmer_registration_no": "Value next to Registration No (NOT Seed Act Reg No)",
              "field_no": "Value next to Field No/Name",
              "observation": "ALL text under Observations / Instructions, joined into a single string with spaces",
              "inspection_round": "Determine which oval checkbox is marked (1st Inspection, 2nd Inspection, 3rd Inspection, 1st Re-inspection, 2nd Re-inspection, 3rd Re-inspection). Return as string, or array if multiple are marked."
            }
            
            ----------------------------------------------------
            RULES
            ----------------------------------------------------
            • Read handwritten text carefully.
            • Join multiline observations into one readable sentence.
            • If a value is missing, return null.
        """.trimIndent()

        val client = GeminiClient(BuildConfig.GEMINI_API_KEY)
        client.generateContent(prompt, bitmap).onSuccess { jsonString ->
            val cleanJson = cleanJsonResponse(jsonString)
            try {
                inspectionData = json.decodeFromString<InspectionFormResult>(cleanJson)
                withContext(Dispatchers.Main) {
                    displayType3Results(inspectionData!!)
                }
            } catch (e: Exception) {
                handleError("Failed to parse response: ${e.message}")
            }
        }.onFailure { e ->
            handleError("Extraction failed: ${e.message}")
        }
    }

    private fun displayType3Results(data: InspectionFormResult) {
        progressBar.visibility = View.GONE
        tvStatus.text = "✅ Extraction complete (Preview Only)"
        
        val roundDisplay = when {
            data.inspection_round == null -> "null"
            data.inspection_round.toString().startsWith("[") -> {
                try {
                    data.inspection_round.jsonArray.joinToString(", ") { it.jsonPrimitive.content }
                } catch (_: Exception) { data.inspection_round.toString() }
            }
            else -> {
                try {
                    data.inspection_round.jsonPrimitive.content
                } catch (_: Exception) { data.inspection_round.toString() }
            }
        }

        tvResults.text = """
            Inspection No: ${data.inspection_no ?: "null"}
            Date: ${data.inspection_date ?: "null"}
            Reg No (Seed Act): ${data.seed_act_registration_no ?: "null"}
            Reg No (Farmer): ${data.farmer_registration_no ?: "null"}
            Field No: ${data.field_no ?: "null"}
            Round: $roundDisplay
            
            Observation: ${data.observation ?: "null"}
        """.trimIndent()
        btnSave.visibility = View.GONE
        btnRetry.visibility = View.VISIBLE
    }

    private suspend fun extractType4(bitmap: Bitmap) {
        val prompt = """
            You are an expert document understanding AI. Extract structured data from this Final Field Inspection Report.
            Return ONLY valid JSON. No markdown, no notes.
            
            {
              "document_type": "final_field_inspection_report",
              "harvest_inspect_no": "Value next to No. (top-right corner)",
              "farmer_registration_no": "Value next to Registration No",
              "final_inspection_date": "Value next to Date (Convert to YYYY-MM-DD)",
              "extent_accepted": "Value next to Extent Accepted (Ac.)",
              "extent_rejected": "Value next to Extent Rejected (Ac.)",
              "estimated_seed_yield": "Value next to Estimated Seed Yield (Bu/Kg)",
              "other_distinguish_varieties": ["Values from Other Distinguish Varieties column"],
              "pest_and_diseases": ["Values from Pest & Diseases column"],
              "remarks": ["Values from Remarks column, join multiline entries"],
              "decision": ["Values from Decision (Accepted or Rejected) column"],
              "officer_sign": {
                "name": "Officer name from seal/stamp",
                "designation": "Designation from seal/stamp",
                "organization": "Organization from seal/stamp",
                "department": "Department from seal/stamp",
                "office": "Office/Location from seal/stamp",
                "full_text": "Complete text found in the seal"
              }
            }
            
            ----------------------------------------------------
            RULES
            ----------------------------------------------------
            • Extent accepted/rejected: Include units if written (e.g., '1 AC').
            • Decision: Must be exactly 'Accepted' or 'Rejected' if readable.
            • Officer seal: Extract all readable text from the stamp near the bottom.
            • If a value is missing, return null.
        """.trimIndent()

        val client = GeminiClient(BuildConfig.GEMINI_API_KEY)
        client.generateContent(prompt, bitmap).onSuccess { jsonString ->
            val cleanJson = cleanJsonResponse(jsonString)
            try {
                finalInspectionData = json.decodeFromString<FinalInspectionResult>(cleanJson)
                withContext(Dispatchers.Main) {
                    displayType4Results(finalInspectionData!!)
                }
            } catch (e: Exception) {
                handleError("Failed to parse response: ${e.message}")
            }
        }.onFailure { e ->
            handleError("Extraction failed: ${e.message}")
        }
    }

    private fun displayType4Results(data: FinalInspectionResult) {
        progressBar.visibility = View.GONE
        tvStatus.text = "✅ Extraction complete (Preview Only)"
        
        tvResults.text = """
            Harvest Inspect No: ${data.harvest_inspect_no ?: "null"}
            Reg No: ${data.farmer_registration_no ?: "null"}
            Date: ${data.final_inspection_date ?: "null"}
            
            Accepted: ${data.extent_accepted ?: "null"}
            Rejected: ${data.extent_rejected ?: "null"}
            Yield: ${data.estimated_seed_yield ?: "null"}
            
            --- Table Rows: ${data.remarks.size} ---
            Decision: ${data.decision.firstOrNull() ?: "null"}
            
            Officer: ${data.officer_sign?.name ?: "null"}
            Full Seal: ${data.officer_sign?.full_text ?: "null"}
        """.trimIndent()
        btnSave.visibility = View.GONE
        btnRetry.visibility = View.VISIBLE
    }

    private fun saveToSupabase() {
        val data = extractedData ?: return
        if (currentDocType != 1) return

        tvStatus.text = "💾 Saving to Supabase..."
        btnSave.isEnabled = false
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val rowCount = listOf(
                data.lot_no_for_seeds,
                data.land_address,
                data.transplanted_date,
                data.crop_id, 
                data.variety, 
                data.land_area, 
                data.quantity_of_seeds_used
            ).maxOf { it.size }
            
            val forms = (0 until rowCount).map { i ->
                TempForm1(
                    lot_no_for_seeds = data.lot_no_for_seeds.getOrNull(i) ?: data.lot_no_for_seeds.lastOrNull(),
                    land_address = data.land_address.getOrNull(i) ?: data.land_address.lastOrNull(),
                    transplanted_date = data.transplanted_date.getOrNull(i) ?: data.transplanted_date.lastOrNull(),
                    form_date = data.form_date,
                    see_act_registration_no = data.see_act_registration_no,
                    farmer_name = data.farmer_name,
                    contact_no = data.contact_no.getOrNull(i) ?: data.contact_no.lastOrNull(),
                    address = data.address,
                    crop_id = data.crop_id.getOrNull(i) ?: data.crop_id.lastOrNull(),
                    variety = data.variety.getOrNull(i) ?: data.variety.lastOrNull(),
                    land_area = data.land_area.getOrNull(i) ?: data.land_area.lastOrNull(),
                    quantity_of_seeds_used = data.quantity_of_seeds_used.getOrNull(i) ?: data.quantity_of_seeds_used.lastOrNull()
                )
            }

            repository.insertTempForm1Batch(forms).onSuccess {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnSave.isEnabled = true
                    tvStatus.text = "✅ Saved ${forms.size} records to Supabase!"
                    btnSave.visibility = View.GONE
                    btnRetry.visibility = View.GONE

                    launch {
                        kotlinx.coroutines.delay(1500)
                        ResultsActivity.start(this@ExtractionActivity)
                        finish()
                    }
                }
            }.onFailure { e ->
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnSave.isEnabled = true
                    tvStatus.text = "❌ Save failed: ${e.message}"
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ExtractionState.imageFile?.delete()
    }
}
