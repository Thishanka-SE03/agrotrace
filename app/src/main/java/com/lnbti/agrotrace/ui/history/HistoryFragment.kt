package com.lnbti.agrotrace.ui.history

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.lnbti.agrotrace.DocumentDataFormatter
import com.lnbti.agrotrace.DocumentTypeUtils
import com.lnbti.agrotrace.R
import com.lnbti.agrotrace.databinding.FragmentHistoryBinding
import com.lnbti.agrotrace.db.AppDatabase
import com.lnbti.agrotrace.db.DocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private var allDocuments: List<DocumentEntity> = emptyList()
    private var selectedCategory = "all"
    private var selectedDayStart: Long? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = applyFilters()
            override fun afterTextChanged(s: Editable?) = Unit
        })

        binding.chipAll.setOnClickListener { selectedCategory = "all"; applyFilters() }
        binding.chipLand.setOnClickListener { selectedCategory = "land"; applyFilters() }
        binding.chipRegistration.setOnClickListener { selectedCategory = "registration"; applyFilters() }
        binding.chipInspection.setOnClickListener { selectedCategory = "inspection"; applyFilters() }
        binding.chipSeed.setOnClickListener { selectedCategory = "seed"; applyFilters() }
        binding.btnCalendar.setOnClickListener { showDatePicker() }
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(appContext).documentDao().getAll()
                }
            }
            if (_binding == null) return@launch
            result.onSuccess { documents ->
                allDocuments = documents
                applyFilters()
            }.onFailure { error ->
                Log.e(TAG, "Could not load document history", error)
                allDocuments = emptyList()
                applyFilters()
                Snackbar.make(
                    binding.root,
                    "Could not open local history. Please try again.",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun applyFilters() {
        if (_binding == null) return
        val query = binding.etSearch.text?.toString().orEmpty().trim().lowercase(Locale.getDefault())
        val dayEnd = selectedDayStart?.plus(24 * 60 * 60 * 1000)

        val filtered = allDocuments.filter { document ->
            val searchableData = if (query.isBlank()) "" else
                DocumentDataFormatter.toReadableText(document.rawJson)
                    .lowercase(Locale.getDefault())
            val matchesQuery = query.isBlank() ||
                document.title.lowercase(Locale.getDefault()).contains(query) ||
                document.summary.lowercase(Locale.getDefault()).contains(query) ||
                searchableData.contains(query)
            val matchesCategory = selectedCategory == "all" || DocumentTypeUtils.category(document.type) == selectedCategory
            val matchesDate = selectedDayStart == null ||
                (document.timestamp >= selectedDayStart!! && document.timestamp < dayEnd!!)
            matchesQuery && matchesCategory && matchesDate
        }

        binding.tvHistoryCount.text = "${filtered.size} ${if (filtered.size == 1) "saved document" else "saved documents"}"
        binding.documentsContainer.removeAllViews()
        binding.emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.tvEmptySubtitle.text = if (allDocuments.isEmpty()) {
            "Saved and verified scans will appear here."
        } else {
            "Try another search term or remove a filter."
        }
        filtered.forEach(::addDocumentCard)
    }

    private fun addDocumentCard(document: DocumentEntity) {
        val card = layoutInflater.inflate(R.layout.item_document, binding.documentsContainer, false)
        val icon = card.findViewById<ImageView>(R.id.ivDocumentIcon)
        icon.setImageResource(DocumentTypeUtils.icon(document.type))
        icon.imageTintList = ContextCompat.getColorStateList(requireContext(), DocumentTypeUtils.color(document.type))
        card.findViewById<TextView>(R.id.tvDocumentType).text = document.title
        card.findViewById<TextView>(R.id.tvIdentifier).text = document.summary
        card.findViewById<TextView>(R.id.tvDate).text = formatDate(document.timestamp)
        card.findViewById<View>(R.id.btnShare).setOnClickListener { shareDocument(document) }
        card.setOnClickListener {
            val action = HistoryFragmentDirections.actionNavHistoryToDocumentDetailFragment(document.id)
            findNavController().navigate(action)
        }
        binding.documentsContainer.addView(card)
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Filter documents by date")
            .build()
        picker.addOnPositiveButtonClickListener { utcSelection ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = utcSelection
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            selectedDayStart = calendar.timeInMillis
            applyFilters()
            Snackbar.make(
                binding.root,
                "Showing ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.time)}",
                Snackbar.LENGTH_LONG
            ).setAction("Clear") {
                selectedDayStart = null
                applyFilters()
            }.show()
        }
        picker.show(parentFragmentManager, "history_date_picker")
    }

    private fun shareDocument(document: DocumentEntity) {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, document.title)
                    val readableData = DocumentDataFormatter.toReadableText(document.rawJson)
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "${document.title}\n${document.summary}\n\n$readableData"
                    )
                },
                "Share document"
            )
        )
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "HistoryFragment"
    }
}
