package com.lnbti.agrotrace

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.lnbti.agrotrace.db.AppDatabase
import com.lnbti.agrotrace.db.DocumentEntity
import com.lnbti.agrotrace.models.*
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class ExtractionActivity : AppCompatActivity() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var currentDocType: Int = 1
    private lateinit var editContainer: LinearLayout
    private var extractedDataRaw: String? = null

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

    private lateinit var progressBar: com.google.android.material.progressindicator.CircularProgressIndicator
    private lateinit var tvStatus: TextView
    private lateinit var btnConfirm: Button
    private lateinit var btnRescan: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_extraction)

        currentDocType = intent.getIntExtra(EXTRA_DOC_TYPE, 1)
        editContainer = findViewById(R.id.editContainer)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)
        btnConfirm = findViewById(R.id.btnSave)
        btnRescan = findViewById(R.id.btnRetry)

        btnConfirm.visibility = View.GONE
        btnRescan.visibility = View.GONE

        btnConfirm.setOnClickListener { confirmAndSave() }
        btnRescan.setOnClickListener { finish() }
        findViewById<View>(R.id.btnClose).setOnClickListener { finish() }

        if (ExtractionState.imageFile != null) {
            startExtraction()
        } else {
            tvStatus.text = "No image provided"
        }
    }

    private fun startExtraction() {
        progressBar.visibility = View.VISIBLE
        tvStatus.text = "🔍 AI Extraction in progress..."
        editContainer.removeAllViews()

        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                android.graphics.BitmapFactory.decodeFile(ExtractionState.imageFile?.absolutePath)
            } ?: return@launch handleError("Failed to decode image")

            val prompt = getPromptForType(currentDocType)
            val client = GeminiClient(BuildConfig.GEMINI_API_KEY)
            
            client.generateContent(prompt, bitmap).onSuccess { jsonString ->
                val cleanJson = cleanJsonResponse(jsonString)
                extractedDataRaw = cleanJson
                withContext(Dispatchers.Main) {
                    processExtractedData(cleanJson)
                }
            }.onFailure { e -> handleError(e.message ?: "Extraction failed") }
        }
    }

    private fun getPromptForType(type: Int): String {
        return """
            You are an expert document understanding AI specializing in Sinhala agricultural documents.
            Analyze the provided image and extract information for ${getDocumentTypeName(type)}.
            
            Return ONLY valid JSON. No markdown, no explanations.
            
            ${getSpecificFieldsForType(type)}
            
            RULES:
            - Preserve Sinhala and English exactly as written.
            - Dates: Convert to YYYY-MM-DD.
            - If missing, return null.
        """.trimIndent()
    }

    private fun getDocumentTypeName(type: Int): String {
        return when(type) {
            1 -> "Land Approval Form"
            2 -> "Crop Registration Form"
            3 -> "Field/Lot Inspection Report"
            4 -> "Final Field Inspection Report"
            5 -> "Seed Test Request Form"
            6 -> "Seed Test Report"
            7 -> "Labeling Document"
            else -> "Form"
        }
    }

    private fun getSpecificFieldsForType(type: Int): String {
        return when(type) {
            1 -> "{ 'see_act_registration_no': '...', 'form_date': '...', 'farmer_name': '...', 'address': '...', 'lot_no_for_seeds': [...], 'land_address': [...], 'transplanted_date': [...], 'crop_id': [...], 'variety': [...], 'land_area': [...], 'quantity_of_seeds_used': [...] }"
            2 -> "{ 'registration_no': '...', 'form_no': '...', 'seed_act_registration_no': '...', 'farmer_registration_no': '...', 'date': '...', 'name_of_seed_producer': '...', 'address_of_seed_producer': '...', 'field_no': [...], 'crop_grown_in_last_two_seasons': [...], 'harvest_date': [...], 'payment_no': '...', 'payment_amount': '...', 'registration_officer': '...' }"
            3 -> "{ 'inspection_no': '...', 'inspection_date': '...', 'seed_act_registration_no': '...', 'farmer_registration_no': '...', 'field_no': '...', 'observation': '...', 'inspection_round': '...' }"
            4 -> "{ 'harvest_inspect_no': '...', 'farmer_registration_no': '...', 'final_inspection_date': '...', 'extent_accepted': '...', 'extent_rejected': '...', 'estimated_seed_yield': '...', 'decision': [...], 'officer_sign': { 'name': '...' } }"
            5 -> "{ 'request_no': '...', 'date': '...', 'lot_no': '...', 'crop': '...', 'variety': '...', 'class_of_seed': '...', 'weight_of_lot': '...', 'no_of_containers': '...', 'sender_name': '...', 'sender_address': '...' }"
            6 -> "{ 'report_no': '...', 'test_date': '...', 'germination_percentage': '...', 'purity_percentage': '...', 'moisture_content': '...', 'inert_matter': '...', 'other_seeds': '...', 'status': '...' }"
            7 -> "{ 'label_serial_no': '...', 'lot_no': '...', 'crop': '...', 'variety': '...', 'date_of_test': '...', 'valid_until': '...', 'net_weight': '...' }"
            else -> "{}"
        }
    }

    private fun processExtractedData(jsonString: String) {
        progressBar.visibility = View.GONE
        btnConfirm.visibility = View.VISIBLE
        btnRescan.visibility = View.VISIBLE
        tvStatus.text = "✅ Data ready for review"

        try {
            when (currentDocType) {
                1 -> renderType1(json.decodeFromString<LandApprovalResult>(jsonString))
                2 -> renderType2(json.decodeFromString<CropRegistrationResult>(jsonString))
                3 -> renderType3(json.decodeFromString<InspectionFormResult>(jsonString))
                4 -> renderType4(json.decodeFromString<FinalInspectionResult>(jsonString))
                5 -> renderType5(json.decodeFromString<SeedTestRequestResult>(jsonString))
                6 -> renderType6(json.decodeFromString<SeedTestReportResult>(jsonString))
                7 -> renderType7(json.decodeFromString<LabelingResult>(jsonString))
            }
        } catch (e: Exception) {
            handleError("UI Rendering error: ${e.message}")
        }
    }

    private fun renderType1(data: LandApprovalResult) {
        addSectionHeader("Header Information")
        addField("Seed Act Reg No", data.see_act_registration_no)
        addField("Form Date", data.form_date)
        addField("Farmer Name", data.farmer_name)
        addField("Farmer Address", data.address)
        
        addSectionHeader("Table Records")
        data.lot_no_for_seeds.indices.forEach { i ->
            addSubsection("Entry ${i+1}")
            addField("Lot Number", data.lot_no_for_seeds.getOrNull(i))
            addField("Variety", data.variety.getOrNull(i))
            addField("Area", data.land_area.getOrNull(i))
        }
    }

    private fun renderType2(data: CropRegistrationResult) {
        addSectionHeader("Producer Details")
        addField("Name", data.name_of_seed_producer)
        addField("Address", data.address_of_seed_producer)
        addField("Reg No", data.registration_no)
        
        addSectionHeader("Field Details")
        data.field_no.indices.forEach { i ->
            addSubsection("Field ${i+1}")
            addField("Field No", data.field_no.getOrNull(i))
            addField("Crops Grown", data.crop_grown_in_last_two_seasons.getOrNull(i))
            addField("Harvest Date", data.harvest_date.getOrNull(i))
        }
    }

    private fun renderType3(data: InspectionFormResult) {
        addSectionHeader("Inspection Details")
        addField("Inspection No", data.inspection_no)
        addField("Date", data.inspection_date)
        addField("Seed Act Reg No", data.seed_act_registration_no)
        addField("Field No", data.field_no)
        addField("Observations", data.observation)
    }

    private fun renderType4(data: FinalInspectionResult) {
        addSectionHeader("Final Inspection")
        addField("Inspect No", data.harvest_inspect_no)
        addField("Date", data.final_inspection_date)
        addField("Extent Accepted", data.extent_accepted)
        addField("Estimated Yield", data.estimated_seed_yield)
        addField("Officer", data.officer_sign?.name)
    }

    private fun renderType5(data: SeedTestRequestResult) {
        addSectionHeader("Test Request")
        addField("Request No", data.request_no)
        addField("Lot No", data.lot_no)
        addField("Crop", data.crop)
        addField("Variety", data.variety)
        addField("Sender", data.sender_name)
    }

    private fun renderType6(data: SeedTestReportResult) {
        addSectionHeader("Test Report")
        addField("Report No", data.report_no)
        addField("Germination %", data.germination_percentage)
        addField("Purity %", data.purity_percentage)
        addField("Moisture %", data.moisture_content)
        addField("Status", data.status)
    }

    private fun renderType7(data: LabelingResult) {
        addSectionHeader("Label Details")
        addField("Serial No", data.label_serial_no)
        addField("Lot No", data.lot_no)
        addField("Crop", data.crop)
        addField("Valid Until", data.valid_until)
    }

    private fun addField(label: String, value: String?) {
        val layout = TextInputLayout(this, null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox_Dense)
        layout.hint = label
        layout.setPadding(0, 0, 0, 24)
        
        val editText = TextInputEditText(layout.context)
        editText.setText(value ?: "")
        editText.textSize = 14f
        editText.setTextColor(getColor(R.color.onBackground))
        
        layout.addView(editText)
        editContainer.addView(layout)
    }

    private fun addSectionHeader(title: String) {
        val tv = TextView(this)
        tv.text = title.uppercase()
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
        tv.setPadding(0, 32, 0, 12)
        tv.setTextColor(getColor(R.color.primary))
        tv.typeface = android.graphics.Typeface.DEFAULT_BOLD
        tv.letterSpacing = 0.05f
        editContainer.addView(tv)
    }

    private fun addSubsection(title: String) {
        val tv = TextView(this)
        tv.text = title
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11f)
        tv.setPadding(8, 8, 0, 4)
        tv.setTextColor(getColor(R.color.secondary))
        editContainer.addView(tv)
    }

    private fun confirmAndSave() {
        val rawData = extractedDataRaw ?: return
        lifecycleScope.launch {
            val title = getDocumentTypeName(currentDocType)
            val summary = "Scanned on ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())}"
            
            val entity = DocumentEntity(
                type = currentDocType,
                title = title,
                summary = summary,
                rawJson = rawData
            )
            
            AppDatabase.getDatabase(this@ExtractionActivity).documentDao().insert(entity)
            
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ExtractionActivity, "Saved to Journal", Toast.LENGTH_SHORT).show()
                ResultsActivity.start(this@ExtractionActivity)
                finish()
            }
        }
    }

    private fun cleanJsonResponse(jsonString: String): String {
        return jsonString.trim()
            .removeSurrounding("```json", "```")
            .removeSurrounding("```")
            .trim()
    }

    private fun handleError(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            progressBar.visibility = View.GONE
            tvStatus.text = "❌ Error"
            Toast.makeText(this@ExtractionActivity, message, Toast.LENGTH_LONG).show()
            btnRescan.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ExtractionState.imageFile?.delete()
    }
}
