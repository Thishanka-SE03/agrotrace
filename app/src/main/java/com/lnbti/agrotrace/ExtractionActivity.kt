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

class ExtractionActivity : AppCompatActivity() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var extractedData: ExtractionResult? = null
    private var cropRegData: CropRegistrationResult? = null
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
    private lateinit var progressBar: ProgressBar
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
        btnRetry.setOnClickListener { startExtraction() }
        btnCancel.setOnClickListener { finish() }

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
            } else {
                extractType2(bitmap)
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
