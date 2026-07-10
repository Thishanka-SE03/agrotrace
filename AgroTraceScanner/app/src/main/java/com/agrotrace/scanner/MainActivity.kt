package com.agrotrace.scanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.agrotrace.scanner.ui.AgroTraceScannerApp
import com.agrotrace.scanner.ui.ScannerViewModel
import com.agrotrace.scanner.ui.ScannerViewModelFactory
import com.agrotrace.scanner.ui.theme.AgroTraceTheme
import com.agrotrace.scanner.util.UriMetadataReader
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

class MainActivity : ComponentActivity() {

    private val viewModel: ScannerViewModel by viewModels {
        val app = application as AgroTraceApplication
        ScannerViewModelFactory(app.container.repository, app.container.preferences)
    }

    private val documentScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode != RESULT_OK) return@registerForActivityResult

        val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
        val imageUri = scanResult?.pages?.firstOrNull()?.imageUri
        if (imageUri == null) {
            viewModel.showError("No document image was returned by the scanner.")
            return@registerForActivityResult
        }

        viewModel.onDocumentCaptured(UriMetadataReader.read(contentResolver, imageUri))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleDeepLink(intent)

        setContent {
            AgroTraceTheme {
                AgroTraceScannerApp(
                    viewModel = viewModel,
                    onScanQr = ::launchQrScanner,
                    onScanDocument = ::launchDocumentScanner
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        intent?.dataString?.let(viewModel::acceptQrPayload)
    }

    private fun launchQrScanner() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()

        GmsBarcodeScanning.getClient(this, options)
            .startScan()
            .addOnSuccessListener { barcode ->
                val payload = barcode.rawValue
                if (payload.isNullOrBlank()) {
                    viewModel.showError("The QR code did not contain readable data.")
                } else {
                    viewModel.acceptQrPayload(payload)
                }
            }
            .addOnFailureListener { error ->
                viewModel.showError(error.message ?: "The QR scanner could not be opened.")
            }
    }

    private fun launchDocumentScanner() {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()

        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                documentScannerLauncher.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            }
            .addOnFailureListener { error ->
                viewModel.showError(
                    error.message ?: "The document scanner is unavailable on this device."
                )
            }
    }
}
