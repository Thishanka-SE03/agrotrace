package com.lnbti.agrotrace.ui.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.lnbti.agrotrace.databinding.FragmentExtractionBinding
import com.lnbti.agrotrace.viewmodel.ExtractionState
import com.lnbti.agrotrace.viewmodel.ScannerViewModel
import kotlinx.coroutines.launch

class ExtractionFragment : Fragment() {

    private var _binding: FragmentExtractionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScannerViewModel by viewModels()
    private val args: ExtractionFragmentArgs by navArgs()

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

        // Start extraction
        viewModel.extractData(args.imagePath, args.docType)

        // Observe state
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.extractionState.collect { state ->
                    handleState(state)
                }
            }
        }
    }

    private fun handleState(state: ExtractionState) {
        when (state) {
            is ExtractionState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.tvStatus.text = "Extracting fields..."
            }
            is ExtractionState.Success -> {
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "Data ready to view"
                
                // Auto navigate after small delay
                viewLifecycleOwner.lifecycleScope.launch {
                    kotlinx.coroutines.delay(1000)
                    val action = ExtractionFragmentDirections.actionExtractionFragmentToDataViewFragment(
                        extractedData = state.data
                    )
                    findNavController().navigate(action)
                }
            }
            is ExtractionState.Error -> {
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "Error occurred"
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
            }
            is ExtractionState.Idle -> {
                // Do nothing
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
