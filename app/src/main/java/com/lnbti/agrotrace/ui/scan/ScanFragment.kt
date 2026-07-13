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
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.lnbti.agrotrace.DocumentTypeUtils
import com.lnbti.agrotrace.databinding.FragmentScanBinding
import java.io.File

class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var scanner: GmsDocumentScanner
    private var selectedDocType: Int = 0
    private var launchInProgress = false

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        launchInProgress = false
        if (result.resultCode == RESULT_OK) {
            GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.let(::handleScanningResult)
        } else {
            Snackbar.make(binding.root, "Scanning cancelled", Snackbar.LENGTH_SHORT).show()
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
        if (launchInProgress) return
        selectedDocType = docType
        launchInProgress = true
        Snackbar.make(
            binding.root,
            "Opening ${DocumentTypeUtils.name(docType)} scanner…",
            Snackbar.LENGTH_SHORT
        ).show()

        scanner.getStartScanIntent(requireActivity())
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { error ->
                launchInProgress = false
                Toast.makeText(
                    requireContext(),
                    "Scanner unavailable: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun handleScanningResult(result: GmsDocumentScanningResult) {
        val page = result.pages?.firstOrNull() ?: return
        val cacheFile = File(
            requireContext().cacheDir,
            "agrotrace_${selectedDocType}_${System.currentTimeMillis()}.jpg"
        )

        runCatching {
            requireContext().contentResolver.openInputStream(page.imageUri)?.use { input ->
                cacheFile.outputStream().use(input::copyTo)
            } ?: error("Unable to read the scanned image")
        }.onSuccess {
            val action = ScanFragmentDirections.actionNavScanToExtractionFragment(
                imagePath = cacheFile.absolutePath,
                docType = selectedDocType
            )
            findNavController().navigate(action)
        }.onFailure { error ->
            Toast.makeText(
                requireContext(),
                "Could not save the scan: ${error.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
