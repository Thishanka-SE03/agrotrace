package com.lnbti.agrotrace.ui.scan

import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.lnbti.agrotrace.R
import com.lnbti.agrotrace.databinding.FragmentDataViewBinding
import com.lnbti.agrotrace.models.*
import kotlinx.serialization.json.Json

class DataViewFragment : Fragment() {

    private var _binding: FragmentDataViewBinding? = null
    private val binding get() = _binding!!

    private val args: DataViewFragmentArgs by navArgs()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDataViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        parseAndRenderData(args.extractedData)

        binding.btnSave.setOnClickListener {
            Toast.makeText(requireContext(), "Document Saved", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.nav_home)
        }
        
        binding.btnExport.setOnClickListener {
            Toast.makeText(requireContext(), "Exporting as PDF...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseAndRenderData(jsonString: String) {
        try {
            // Simplified parsing - in a real app, you'd use the docType to decide which model to use
            // For now, let's just try to parse common fields or show raw if it fails
            
            // This is a placeholder for actual complex parsing logic
            // Since we don't have the docType here, we could pass it or infer it.
            // Let's assume we pass it or just show what we find.
            
            // Example for Type 1
            if (jsonString.contains("see_act_registration_no")) {
                val data = json.decodeFromString<LandApprovalResult>(jsonString)
                renderLandApproval(data)
            } else {
                // Fallback: show key-value pairs from raw json if possible
                addSectionHeader("Raw Data")
                addField("Content", jsonString)
            }
        } catch (e: Exception) {
            addSectionHeader("Error Parsing Data")
            addField("Error", e.message)
            addField("Raw JSON", jsonString)
        }
    }

    private fun renderLandApproval(data: LandApprovalResult) {
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

    private fun addField(label: String, value: String?) {
        val layout = TextInputLayout(requireContext(), null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox_Dense)
        layout.hint = label
        layout.setPadding(0, 0, 0, 24)
        
        val editText = TextInputEditText(layout.context)
        editText.setText(value ?: "")
        editText.textSize = 14f
        
        layout.addView(editText)
        binding.dynamicFieldsContainer.addView(layout)
    }

    private fun addSectionHeader(title: String) {
        val tv = TextView(requireContext())
        tv.text = title.uppercase()
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        tv.setPadding(0, 32, 0, 12)
        tv.setTextColor(requireContext().getColor(R.color.primary))
        tv.typeface = Typeface.DEFAULT_BOLD
        tv.letterSpacing = 0.05f
        binding.dynamicFieldsContainer.addView(tv)
    }

    private fun addSubsection(title: String) {
        val tv = TextView(requireContext())
        tv.text = title
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        tv.setPadding(8, 8, 0, 4)
        tv.setTextColor(requireContext().getColor(R.color.secondary))
        binding.dynamicFieldsContainer.addView(tv)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
