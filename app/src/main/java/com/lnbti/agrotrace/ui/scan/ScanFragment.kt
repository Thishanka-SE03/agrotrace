package com.lnbti.agrotrace.ui.scan

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.lnbti.agrotrace.R
import com.lnbti.agrotrace.databinding.FragmentScanBinding
import java.io.File

class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var scanner: GmsDocumentScanner
    private var selectedDocType: Int = 0

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanningResult?.let { handleScanningResult(it) }
        } else {
            Toast.makeText(requireContext(), "Scanning cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupScanner()
        setupListeners()
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

    private fun setupListeners() {
        binding.btnLandApproval.setOnClickListener { startScanning(1) }
        binding.btnCropRegistration.setOnClickListener { startScanning(2) }
        binding.btnInspection.setOnClickListener { startScanning(3) }
        binding.btnFinalFieldInspection.setOnClickListener { startScanning(4) }
        binding.btnSeedTestRequest.setOnClickListener { startScanning(5) }
        binding.btnSeedTestReport.setOnClickListener { startScanning(6) }
        binding.btnLabelingDoc.setOnClickListener { startScanning(7) }
    }

    private fun startScanning(docType: Int) {
        selectedDocType = docType
        scanner.getStartScanIntent(requireActivity())
            .addOnSuccessListener { intentSender ->
                val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
                scannerLauncher.launch(intentSenderRequest)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Scanner unavailable: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun handleScanningResult(result: GmsDocumentScanningResult) {
        val pages = result.pages
        if (pages.isNullOrEmpty()) return

        val page = pages[0]
        val uri = page.imageUri

        // Copy to internal cache for processing
        val cacheFile = File(requireContext().cacheDir, "scanned_doc.jpg")
        try {
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // Navigate to ExtractionFragment
            val action = ScanFragmentDirections.actionNavScanToExtractionFragment(
                imagePath = cacheFile.absolutePath,
                docType = selectedDocType
            )
            findNavController().navigate(action)
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
