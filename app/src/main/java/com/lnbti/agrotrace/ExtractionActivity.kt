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
    val lot_no_for_seeds: String? = null,
    val land_address: String? = null,
    val transplanted_date: String? = null,
    val form_date: String? = null,
    val see_act_registration_no: String? = null,
    val farmer_name: String? = null,
    val contact_no: String? = null,
    val address: String? = null,
    val crop_id: List<String?> = emptyList(),
    val variety: List<String?> = emptyList(),
    val land_area: List<String?> = emptyList(),
    val quantity_of_seeds_used: List<String?> = emptyList()
)

class ExtractionActivity : AppCompatActivity() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var extractedData: ExtractionResult? = null

    companion object {
        private const val EXTRA_IMAGE_FILE = "extra_image_file"

        fun start(context: AppCompatActivity, bitmap: Bitmap, imageFileName: String) {
            // Save bitmap to a temp file that the next activity can read
            val file = File(context.cacheDir, "extraction_${imageFileName}")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            ExtractionState.imageFile = file
            context.startActivity(
                android.content.Intent(context, ExtractionActivity::class.java).apply {
                    putExtra(EXTRA_IMAGE_FILE, file.absolutePath)
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
        tvStatus.text = "🔍 Extracting text with Gemini AI (Sinhala Support)..."
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

            // Extract structured data from the document using Gemini
            val result = extractLandApprovalForm(bitmap)

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                
                result.onSuccess { data ->
                    extractedData = data
                    tvResults.text = """
                        Lot No: ${data.lot_no_for_seeds ?: "N/A"}
                        Farmer: ${data.farmer_name ?: "N/A"}
                        Land Address: ${data.land_address ?: "N/A"}
                        Transplanted: ${data.transplanted_date ?: "N/A"}
                        Form Date: ${data.form_date ?: "N/A"}
                        Reg No: ${data.see_act_registration_no ?: "N/A"}
                        Address: ${data.address ?: "N/A"}
                    """.trimIndent()
                    tvStatus.text = "✅ Extraction complete"
                    btnSave.visibility = View.VISIBLE
                }.onFailure { e ->
                    tvResults.text = "❌ Failed to parse response: ${e.message}"
                    tvStatus.text = "❌ Extraction failed"
                }
                
                btnRetry.visibility = View.VISIBLE
            }
        }
    }

    private suspend fun extractLandApprovalForm(bitmap: Bitmap): Result<ExtractionResult> {
        val client = GeminiClient(BuildConfig.GEMINI_API_KEY)
        
        val prompt = """
            You are an expert document understanding AI specializing in extracting structured data from Sinhala agricultural documents.
            
            Extract data for Document Type 1: Land Approval Form.
            
            ----------------------------------------------------
            FIELD MAPPING
            ----------------------------------------------------
            Extract these values from the CURRENT image provided:
            
            lot_no_for_seeds -> Find බීජ තොග අංකය. Extract the value next to it. 
               - CRITICAL: This is often a code like 'P/2/25/MIL/RG11/025' or 'P/1/25/HIL/RGII/061'.
               - BE CAREFUL with '11' vs 'II'. Look closely at the font. 
               - If it's in a table, take it from the first row of the table.
            
            land_address -> Value next to හෝ ගොවිපල පිහිටුවා ඇති ස්ථානය (Often found in the table header or first row).
            
            transplanted_date -> Value next to වගා කළ දිනය. 
            
            form_date -> Value next to දිනය (The date the form was signed, usually near the bottom or top center, NOT transplanted date).
            
            see_act_registration_no -> Value next to බීජ පනතේ ලියාපදිංචි අංකය.
            
            contact_no -> Value next to දුරකථන අංකය.
            
            Farmer Details (Top Right Dotted area):
            - The FIRST line inside the dotted area is farmer_name.
            - All remaining lines inside that box belong to address. Combine with commas.
            
            ----------------------------------------------------
            TABLE EXTRACTION (ARRAYS)
            ----------------------------------------------------
            The form contains a table. Extract EVERY visible row for these fields and return as JSON ARRAYS:
            
            crop_id -> ALL values under භෝගය column
            variety -> ALL values under ප්‍රභේදය column
            land_area -> ALL values under වපසරිය column
            quantity_of_seeds_used -> ALL values under භාවිතා කල බීජ ප්‍රමාණය column
            
            ----------------------------------------------------
            OUTPUT FORMAT
            ----------------------------------------------------
            Return ONLY valid JSON. No markdown, no notes.
            
            {
              "document_type": "land_approval_form",
              "lot_no_for_seeds": "string",
              "land_address": "string",
              "transplanted_date": "YYYY-MM-DD",
              "form_date": "YYYY-MM-DD",
              "see_act_registration_no": "string",
              "farmer_name": "string",
              "contact_no": "string",
              "address": "string",
              "crop_id": ["string"],
              "variety": ["string"],
              "land_area": ["string"],
              "quantity_of_seeds_used": ["string"]
            }
            
            ----------------------------------------------------
            EXTRACTION RULES
            ----------------------------------------------------
            • OCR PRECISION: Pay extremely close attention to alphanumeric codes (Lot numbers). Do not confuse '1' (one) with 'I' (capital i).
            • NO MEMORY: Extract only what you see in the current image. Do not use values from any previous examples.
            • Dates: Convert to YYYY-MM-DD format.
            • If a value is missing, return null.
        """.trimIndent()

        return client.generateContent(prompt, bitmap).mapCatching { jsonString ->
            // Handle potential markdown formatting in Gemini response
            val cleanJson = jsonString.trim()
                .removeSurrounding("```json", "```")
                .removeSurrounding("```")
                .trim()
            
            json.decodeFromString<ExtractionResult>(cleanJson)
        }
    }

    private fun saveToSupabase() {
        val data = extractedData ?: return
        
        tvStatus.text = "💾 Saving to Supabase..."
        btnSave.isEnabled = false
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            // Determine max rows from arrays
            val rowCount = listOf(data.crop_id, data.variety, data.land_area, data.quantity_of_seeds_used)
                .maxOf { it.size }
            
            val forms = (0 until rowCount).map { i ->
                TempForm1(
                    lot_no_for_seeds = data.lot_no_for_seeds,
                    land_address = data.land_address,
                    transplanted_date = data.transplanted_date,
                    form_date = data.form_date,
                    see_act_registration_no = data.see_act_registration_no,
                    farmer_name = data.farmer_name,
                    contact_no = data.contact_no,
                    address = data.address,
                    crop_id = data.crop_id.getOrNull(i) ?: data.crop_id.lastOrNull(),
                    variety = data.variety.getOrNull(i) ?: data.variety.lastOrNull(),
                    land_area = data.land_area.getOrNull(i) ?: data.land_area.lastOrNull(),
                    quantity_of_seeds_used = data.quantity_of_seeds_used.getOrNull(i) ?: data.quantity_of_seeds_used.lastOrNull()
                )
            }

            // Save all records to database table "temp form 1"
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
                    Log.e("Supabase", "Insert failed", e)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up temp files
        ExtractionState.imageFile?.delete()
    }
}