package com.lnbti.agrotrace.ui.theme

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.lnbti.agrotrace.R
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Point
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult.Page

class DocumentScannerActivity : AppCompatActivity() {

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanningResult?.let { handleScanningResult(it) }
        }
    }

    private lateinit var scanner: GmsDocumentScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupScanner()
        startScanning() // Or trigger from a button
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
        scanner.getStartScanIntent(this)
            .addOnSuccessListener { intent ->
                scannerLauncher.launch(intent)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Scanner not available: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleScanningResult(result: GmsDocumentScanningResult) {
        val pages: List<Page>? = result.pages
        if (pages.isNullOrEmpty()) {
            Toast.makeText(this, "No pages detected", Toast.LENGTH_SHORT).show()
            return
        }

        // ML Kit already does perspective correction!
        // Get the aligned image URI
        val pageUri = pages[0].imageUri

        // Convert to Bitmap for field cropping
        val bitmap = loadBitmapFromUri(pageUri)
        bitmap?.let {
            // Pass to next screen
            DocumentCropperActivity.start(this, it)
            finish()
        }
    }

    private fun loadBitmapFromUri(uri: android.net.Uri): Bitmap? {
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                android.graphics.BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            Log.e("Scanner", "Error loading bitmap", e)
            null
        }
    }
}