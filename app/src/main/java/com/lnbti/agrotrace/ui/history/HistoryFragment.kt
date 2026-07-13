package com.lnbti.agrotrace.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.lnbti.agrotrace.R
import com.lnbti.agrotrace.databinding.FragmentHistoryBinding
import com.lnbti.agrotrace.db.AppDatabase
import com.lnbti.agrotrace.db.DocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnCalendar.setOnClickListener {
            Toast.makeText(requireContext(), "Filter by date coming soon", Toast.LENGTH_SHORT).show()
        }

        loadHistory()
    }

    private fun loadHistory() {
        binding.tvEmpty.visibility = View.GONE
        binding.documentsContainer.removeAllViews()

        viewLifecycleOwner.lifecycleScope.launch {
            val docs = AppDatabase.getDatabase(requireContext()).documentDao().getAll()
            withContext(Dispatchers.Main) {
                if (docs.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                } else {
                    docs.forEach { doc ->
                        addDocumentCard(doc)
                    }
                }
            }
        }
    }

    private fun addDocumentCard(doc: DocumentEntity) {
        val inflater = LayoutInflater.from(requireContext())
        val card = inflater.inflate(R.layout.item_document, binding.documentsContainer, false) as com.google.android.material.card.MaterialCardView

        card.findViewById<TextView>(R.id.tvFullName).text = doc.title
        card.findViewById<TextView>(R.id.tvDate).text = doc.summary
        card.findViewById<TextView>(R.id.tvImageUrl).text = "Stored in Journal"

        card.findViewById<View>(R.id.btnShare).setOnClickListener {
            Toast.makeText(requireContext(), "Sharing: ${doc.title}", Toast.LENGTH_SHORT).show()
        }
        
        card.setOnClickListener {
            Toast.makeText(requireContext(), "View details coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.documentsContainer.addView(card)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
