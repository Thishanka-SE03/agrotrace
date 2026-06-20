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
    val lot_no_for_seeds: String? = null,
    val land_address: String? = null,
    val transplanted_date: String? = null,
    val seed_act_registration_no: String? = null,
    val form_date: String? = null
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
                        Lot No: ${data.lot_no_for_seeds ?: "Not found"}
                        Address: ${data.land_address ?: "Not found"}
                        Transplanted: ${data.transplanted_date ?: "Not found"}
                        Form Date: ${data.form_date ?: "Not found"}
                        Reg No: ${data.seed_act_registration_no ?: "Not found"}
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
            Analyze the provided image of a Land Approval Form and extract the following fields.
            
            Sinhala Label -> JSON Key
            බීජ තොග අංකය -> lot_no_for_seeds
            ගොවිපල පිහිටුවා ඇති ස්ථානය -> land_address
            වගා කළ දිනය -> transplanted_date
            බීජ පනත යටතේ ලියාපදිංචි අංකය -> seed_act_registration_no
            දිනය (Form Date) -> form_date
            
            Extraction Rules:
            1. If multiple records or a list exists, extract ONLY the first/main record. Do NOT concatenate multiple values.
            2. For dates ('transplanted_date' and 'form_date'), convert to YYYY-MM-DD format (e.g., 20/11/2025 becomes 2025-11-20). This is CRITICAL.
            3. For 'lot_no_for_seeds', extract only the first identifier found.
            4. Locate the Sinhala labels even if there are minor OCR or formatting differences.
            5. Trim unnecessary spaces and punctuation.
            6. If a value cannot be determined, return null.
            7. Do not guess missing values.
            
            Return ONLY valid JSON in this format:
            {
              "lot_no_for_seeds": "single_value",
              "land_address": "single_value",
              "transplanted_date": "YYYY-MM-DD",
              "seed_act_registration_no": "single_value",
              "form_date": "YYYY-MM-DD"
            }
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
            // 1. Ensure dependencies exist (Handle Foreign Key Constraints)
            val lotNo = data.lot_no_for_seeds
            val regNo = data.seed_act_registration_no
            
            if (lotNo != null && regNo != null) {
                // First ensure the registration exists in seed_act_registrations
                repository.ensureRegistrationExists(regNo).onFailure { e ->
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnSave.isEnabled = true
                        tvStatus.text = "❌ Failed to register Seed Act No: ${e.message}"
                        Log.e("Supabase", "Registration insertion failed", e)
                    }
                    return@launch
                }

                // Then ensure the lot number exists in seed_act_lot_no
                repository.ensureSeedLotNoExists(lotNo, regNo).onFailure { e ->
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnSave.isEnabled = true
                        tvStatus.text = "❌ Failed to register Lot No: ${e.message}"
                        Log.e("Supabase", "Lot registration failed", e)
                    }
                    return@launch
                }
            } else if (lotNo != null) {
                // Handle case where regNo might be null but lotNo is present
                val effectiveRegNo = "N/A"
                repository.ensureRegistrationExists(effectiveRegNo)
                repository.ensureSeedLotNoExists(lotNo, effectiveRegNo).onFailure { e ->
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnSave.isEnabled = true
                        tvStatus.text = "❌ Failed to register Lot No (N/A): ${e.message}"
                    }
                    return@launch
                }
            }

            // 2. Map to Database Model
            val lotNoChecked = lotNo ?: "MISSING_LOT"
            val form = LandApprovalForm(
                lot_no_for_seeds = lotNoChecked,
                land_address = data.land_address,
                transplanted_date = data.transplanted_date,
                form_date = data.form_date,
                form_id = 1
            )

            // 3. Save to database
            repository.insertLandApprovalForm(form).onSuccess { docId ->
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnSave.isEnabled = true
                    tvStatus.text = "✅ Saved to Supabase! (ID: ${docId.take(8)}...)"
                    btnSave.visibility = View.GONE
                    btnRetry.visibility = View.GONE

                    // Navigate to results after a brief pause
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