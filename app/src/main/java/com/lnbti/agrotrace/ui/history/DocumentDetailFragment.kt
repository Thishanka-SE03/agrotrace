package com.lnbti.agrotrace.ui.history

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lnbti.agrotrace.DocumentTypeUtils
import com.lnbti.agrotrace.databinding.FragmentDocumentDetailBinding
import com.lnbti.agrotrace.db.AppDatabase
import com.lnbti.agrotrace.db.DocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentDetailFragment : Fragment() {

    private var _binding: FragmentDocumentDetailBinding? = null
    private val binding get() = _binding!!
    private val args: DocumentDetailFragmentArgs by navArgs()
    private var document: DocumentEntity? = null
    private val json = Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDocumentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.btnShare.setOnClickListener { document?.let(::shareDocument) }
        binding.btnDelete.setOnClickListener { document?.let(::confirmDelete) }
        loadDocument()
    }

    private fun loadDocument() {
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(appContext)
                        .documentDao()
                        .getById(args.documentId)
                }
            }
            if (_binding == null) return@launch

            result.onSuccess { loaded ->
                if (loaded == null) {
                    findNavController().popBackStack()
                    return@onSuccess
                }
                document = loaded
                binding.ivDocumentIcon.setImageResource(DocumentTypeUtils.icon(loaded.type))
                binding.ivDocumentIcon.imageTintList = ContextCompat.getColorStateList(
                    requireContext(),
                    DocumentTypeUtils.color(loaded.type)
                )
                binding.tvDocumentType.text = loaded.title
                binding.tvSummary.text = loaded.summary
                binding.tvDate.text = SimpleDateFormat(
                    "dd MMMM yyyy • hh:mm a",
                    Locale.getDefault()
                ).format(Date(loaded.timestamp))
                binding.tvRawData.text = prettyJson(loaded.rawJson)
            }.onFailure { error ->
                Log.e(TAG, "Could not load saved document", error)
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Could not open document")
                    .setMessage(error.message ?: "The local record is unavailable.")
                    .setPositiveButton("Back") { _, _ ->
                        findNavController().popBackStack()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun prettyJson(raw: String): String = runCatching {
        val element = json.parseToJsonElement(raw)
        json.encodeToString(element)
    }.getOrDefault(raw)

    private fun shareDocument(document: DocumentEntity) {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, document.title)
                    putExtra(Intent.EXTRA_TEXT, "${document.title}\n${document.summary}\n\n${prettyJson(document.rawJson)}")
                },
                "Share document"
            )
        )
    }

    private fun confirmDelete(document: DocumentEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete this document?")
            .setMessage("This removes the local history record. This action cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                val appContext = requireContext().applicationContext
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = runCatching {
                        withContext(Dispatchers.IO) {
                            AppDatabase.getDatabase(appContext)
                                .documentDao()
                                .delete(document.id)
                        }
                    }
                    if (_binding == null) return@launch
                    result.onSuccess {
                        findNavController().popBackStack()
                    }.onFailure { error ->
                        Log.e(TAG, "Could not delete document", error)
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Delete failed")
                            .setMessage(error.message ?: "The document could not be removed.")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "DocumentDetailFragment"
    }
}
