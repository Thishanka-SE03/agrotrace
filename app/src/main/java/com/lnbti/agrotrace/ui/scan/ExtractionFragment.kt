package com.lnbti.agrotrace.ui.scan

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.lnbti.agrotrace.DocumentTypeUtils
import com.lnbti.agrotrace.R
import com.lnbti.agrotrace.databinding.FragmentExtractionBinding
import com.lnbti.agrotrace.viewmodel.ExtractionState
import com.lnbti.agrotrace.viewmodel.ScannerViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ExtractionFragment : Fragment() {

    private var _binding: FragmentExtractionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScannerViewModel by viewModels()
    private val args: ExtractionFragmentArgs by navArgs()
    private var progressJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExtractionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        Glide.with(this).load(args.imagePath).centerCrop().into(binding.ivDocumentPreview)
        configureSteps()

        if (viewModel.extractionState.value is ExtractionState.Idle) {
            viewModel.extractData(args.imagePath, args.docType)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.extractionState.collect(::handleState)
            }
        }
    }

    private fun configureSteps() {
        stepTitle(binding.stepCaptured.root, "Image captured")
        stepTitle(binding.stepEnhanced.root, "Image enhanced by ML Kit")
        stepTitle(binding.stepGemini.root, "Sent securely to Gemini")
        stepTitle(binding.stepExtracted.root, "Required fields extracted")
        stepTitle(binding.stepPrepared.root, "Review form prepared")
        updateStep(0, active = true)
    }

    private fun stepTitle(step: View, title: String) {
        step.findViewById<TextView>(R.id.tvStepTitle).text = title
    }

    private fun handleState(state: ExtractionState) {
        when (state) {
            is ExtractionState.Loading -> startProgressAnimation()
            is ExtractionState.Success -> {
                progressJob?.cancel()
                for (index in 0..4) updateStep(index, complete = true)
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "Ready for review"
                binding.tvDetail.text = "Check every field before saving ${DocumentTypeUtils.name(args.docType)}."
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(550)
                    val action = ExtractionFragmentDirections.actionExtractionFragmentToDataViewFragment(
                        extractedData = state.data,
                        docType = args.docType,
                        imagePath = args.imagePath
                    )
                    findNavController().navigate(action)
                }
            }
            is ExtractionState.Error -> {
                progressJob?.cancel()
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "Extraction could not finish"
                binding.tvDetail.text = state.message
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
            }
            is ExtractionState.Idle -> Unit
        }
    }

    private fun startProgressAnimation() {
        if (progressJob?.isActive == true) return
        binding.progressBar.visibility = View.VISIBLE
        progressJob = viewLifecycleOwner.lifecycleScope.launch {
            val messages = listOf(
                "Checking image quality" to "ML Kit has captured and enhanced the page.",
                "Sending to Gemini" to "The selected document schema is being prepared.",
                "Reading the document" to "Gemini is matching text to the required fields.",
                "Extracting structured data" to "Missing values will be highlighted for manual review."
            )
            messages.forEachIndexed { index, message ->
                updateStep(index, complete = true)
                if (index + 1 <= 4) updateStep(index + 1, active = true)
                binding.tvStatus.text = message.first
                binding.tvDetail.text = message.second
                delay(900)
            }
        }
    }

    private fun updateStep(index: Int, active: Boolean = false, complete: Boolean = false) {
        val step = listOf(
            binding.stepCaptured.root,
            binding.stepEnhanced.root,
            binding.stepGemini.root,
            binding.stepExtracted.root,
            binding.stepPrepared.root
        ).getOrNull(index) ?: return

        val icon = step.findViewById<ImageView>(R.id.ivStepIcon)
        val progress = step.findViewById<CircularProgressIndicator>(R.id.stepProgress)
        val color = when {
            complete -> R.color.success
            active -> R.color.primary
            else -> R.color.outline
        }
        icon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), color))
        icon.alpha = if (complete || active) 1f else 0.55f
        progress.visibility = if (active && !complete) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        progressJob?.cancel()
        super.onDestroyView()
        _binding = null
    }
}
