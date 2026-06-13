package com.lnbti.agrotrace.ui.theme

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File

class DocumentCropperActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_BITMAP = "extra_bitmap"

        fun start(context: AppCompatActivity, bitmap: Bitmap) {
            // In production, save bitmap to file and pass URI
            // For simplicity here, using a static reference
            DocumentCropperState.alignedBitmap = bitmap
            context.startActivity(android.content.Intent(context, DocumentCropperActivity::class.java))
        }
    }

    // Object to pass bitmap between activities (use proper serialization in production)
    object DocumentCropperState {
        @JvmStatic var alignedBitmap: Bitmap? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cropper)

        val bitmap = DocumentCropperState.alignedBitmap ?: run {
            finish()
            return
        }

        // Define field regions (these are example coordinates for an ID card layout)
        // Adjust based on your specific document type
        val fieldRegions = mapOf(
            "full_name" to Region(x = 0.05f, y = 0.15f, width = 0.9f, height = 0.08f),
            "document_number" to Region(x = 0.05f, y = 0.30f, width = 0.6f, height = 0.06f),
            "date_of_birth" to Region(x = 0.05f, y = 0.42f, width = 0.4f, height = 0.06f),
            "expiry_date" to Region(x = 0.55f, y = 0.42f, width = 0.4f, height = 0.06f),
            "address" to Region(x = 0.05f, y = 0.55f, width = 0.9f, height = 0.12f)
        )

        // Crop all fields
        lifecycleScope.launch {
            val croppedFields = cropFields(bitmap, fieldRegions)

            // Save cropped fields to cache
            val fieldFiles = saveCroppedFields(croppedFields)

            // Move to extraction stage
            GeminiExtractionActivity.start(this@DocumentCropperActivity, fieldFiles)
        }
    }

    data class Region(
        val x: Float,      // Relative position (0.0 to 1.0)
        val y: Float,
        val width: Float,
        val height: Float
    )

    /**
     * Crop specific regions from the aligned document
     * Uses relative coordinates so it works with any resolution
     */
    private fun cropFields(bitmap: Bitmap, regions: Map<String, Region>): Map<String, Bitmap> {
        val croppedFields = mutableMapOf<String, Bitmap>()

        for ((fieldName, region) in regions) {
            try {
                val x = (region.x * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                val y = (region.y * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                val width = (region.width * bitmap.width).toInt().coerceIn(1, bitmap.width - x)
                val height = (region.height * bitmap.height).toInt().coerceIn(1, bitmap.height - y)

                val cropped = Bitmap.createBitmap(bitmap, x, y, width, height)
                croppedFields[fieldName] = cropped
                Log.d("Cropper", "Cropped $fieldName: ${width}x${height}")
            } catch (e: Exception) {
                Log.e("Cropper", "Failed to crop $fieldName", e)
            }
        }

        return croppedFields
    }

    /**
     * Save cropped bitmaps to files for Gemini processing
     */
    private fun saveCroppedFields(croppedFields: Map<String, Bitmap>): Map<String, File> {
        val cacheDir = cacheDir
        val fieldFiles = mutableMapOf<String, File>()

        for ((fieldName, bitmap) in croppedFields) {
            val file = File(cacheDir, "field_${fieldName}.jpg")
            file.outputStream().use { out ->
                // Compress to reduce size (Gemini works well with ~1MB images)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            fieldFiles[fieldName] = file
        }

        return fieldFiles
    }
}