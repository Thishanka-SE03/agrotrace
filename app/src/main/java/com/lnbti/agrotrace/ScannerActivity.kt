package com.lnbti.agrotrace

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File

class ScannerActivity : AppCompatActivity() {

    private lateinit var scanner: GmsDocumentScanner
    private lateinit var tvStatus: TextView
    private lateinit var btnScan: Button
    private lateinit var btnGallery: Button

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanningResult?.let { handleScanningResult(it) }
        } else {
            tvStatus.text = "Scanning cancelled"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_scanner)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        tvStatus = findViewById(R.id.tvStatus)
        btnScan = findViewById(R.id.btnScan)
        btnGallery = findViewById(R.id.btnGallery)

        setupScanner()

        btnScan.setOnClickListener { startScanning() }
        btnGallery.setOnClickListener { openGallery() }
    }

    private fun setupScanner() {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(RESULT_FORMAT_JPEG)
            .setScannerMode(SCANNER_MODE_FULL)
            .build()

        scanner = GmsDocumentScanning.getClient(options)
    }

    private fun startScanning() {
        tvStatus.text = "Starting scanner..."
        scanner.getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                tvStatus.text = "Camera opened — point at your document"
                val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
                scannerLauncher.launch(intentSenderRequest)
            }
            .addOnFailureListener { e ->
                tvStatus.text = "Scanner unavailable: ${e.message}"
                Toast.makeText(this, "ML Kit scanner not available on this device", Toast.LENGTH_LONG).show()
            }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        galleryLauncher.launch(intent)
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                tvStatus.text = "Processing image..."
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    bitmap?.let { navigateToExtraction(it, "gallery_image") }
                } catch (e: Exception) {
                    tvStatus.text = "Failed to load image: ${e.message}"
                }
            }
        }
    }

    private fun handleScanningResult(result: GmsDocumentScanningResult) {
        val pages = result.pages
        if (pages.isNullOrEmpty()) {
            tvStatus.text = "No document detected"
            Toast.makeText(this, "No document found. Try again.", Toast.LENGTH_SHORT).show()
            return
        }

        tvStatus.text = "✅ Document detected! Processing..."

        // ML Kit already did perspective correction
        val page = pages[0]
        val uri = page.imageUri

        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                // Save bitmap to cache for extraction
                val cacheFile = File(cacheDir, "scanned_doc.jpg")
                cacheFile.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }

                navigateToExtraction(bitmap, cacheFile.name)
            } else {
                tvStatus.text = "Failed to decode image"
            }
        } catch (e: Exception) {
            tvStatus.text = "Error: ${e.message}"
            Log.e("Scanner", "Processing error", e)
        }
    }

    private fun navigateToExtraction(bitmap: Bitmap, imageFileName: String) {
        ExtractionActivity.start(this, bitmap, imageFileName)
        finish()
    }
}