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
import java.io.File

class ExtractionActivity : AppCompatActivity() {

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

            // Extract all text from the document using Gemini 2.5
            val fullText = extractFullTextWithGemini(bitmap)

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                tvResults.text = fullText ?: "❌ No text found"
                tvStatus.text = if (fullText != null && !fullText.startsWith("ERROR")) "✅ Extraction complete" else "❌ Failed to extract text"
                btnSave.visibility = View.VISIBLE
                btnRetry.visibility = View.VISIBLE
            }
        }
    }

    private suspend fun extractFullTextWithGemini(bitmap: Bitmap): String? {
        val client = GeminiClient(BuildConfig.GEMINI_API_KEY)
        
        val prompt = "Extract all text from this image. Support Sinhala and English. Preserve layout. Handle cursive handwriting. Return only the extracted text."

        return client.generateContent(prompt, bitmap).fold(
            onSuccess = { it.trim() },
            onFailure = { e ->
                Log.e("Gemini", "Full text extraction failed", e)
                "ERROR: ${e.message}"
            }
        )
    }

    private fun saveToSupabase() {
        tvStatus.text = "💾 Saving to Supabase..."
        btnSave.isEnabled = false
        progressBar.visibility = View.VISIBLE

        val extractedText = tvResults.text.toString()

        lifecycleScope.launch {
            // 1. Upload image to Storage
            var imageUrl: String? = null
            val imageFile = ExtractionState.imageFile
            if (imageFile != null && imageFile.exists()) {
                val bytes = withContext(Dispatchers.IO) { imageFile.readBytes() }
                val fileName = "doc_${System.currentTimeMillis()}.jpg"

                repository.uploadImage(bytes, fileName).onSuccess { url ->
                    imageUrl = url
                    Log.d("Supabase", "Image uploaded: $url")
                }.onFailure { e ->
                    Log.e("Supabase", "Image upload failed", e)
                }
            }

            // 2. Build document record (saving full text in address or similar if no dedicated field)
            // For now, we just pass the image and minimal info since specific fields are removed
            val document = DocumentRecord(
                full_name = "Extracted Document",
                address = extractedText.take(500), // Save first 500 chars to address for now
                aligned_image_url = imageUrl
            )

            // 3. Save to database
            repository.insertDocument(document).onSuccess { docId ->
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