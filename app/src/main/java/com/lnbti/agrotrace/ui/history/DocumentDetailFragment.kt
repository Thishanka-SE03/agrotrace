package com.lnbti.agrotrace.ui.history

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.lnbti.agrotrace.DocumentDataFormatter
import com.lnbti.agrotrace.DocumentSchema
import com.lnbti.agrotrace.DocumentTypeUtils
import com.lnbti.agrotrace.R
import com.lnbti.agrotrace.databinding.FragmentDocumentDetailBinding
import com.lnbti.agrotrace.db.AppDatabase
import com.lnbti.agrotrace.db.DocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentDetailFragment : Fragment() {

    private var _binding: FragmentDocumentDetailBinding? = null
    private val binding get() = _binding!!
    private val args: DocumentDetailFragmentArgs by navArgs()
    private var document: DocumentEntity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
                bindHeader(loaded)
                renderSavedData(loaded.type, loaded.rawJson)
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

    private fun bindHeader(loaded: DocumentEntity) {
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
    }

    private fun renderSavedData(documentType: Int, rawJson: String) {
        binding.dynamicFieldsContainer.removeAllViews()
        val data = DocumentSchema.mergeWithExpected(
            documentType,
            DocumentDataFormatter.parseObject(rawJson)
        )
        val stats = DocumentDataFormatter.countFields(data)

        binding.tvFieldSummary.text = when {
            stats.total == 0 -> "No structured fields were stored for this document."
            stats.missing == 0 -> "${stats.total} form fields • all values are available"
            else -> "${stats.total} form fields • ${stats.missing} ${if (stats.missing == 1) "value is" else "values are"} blank"
        }
        binding.bannerDataSummary.setBackgroundResource(
            if (stats.missing == 0) R.drawable.bg_success_soft else R.drawable.bg_warning_soft
        )

        val primitiveEntries = data
            .filterValues { it is JsonPrimitive || it is JsonNull }
            .filterKeys { it != "document_type" }

        if (primitiveEntries.isNotEmpty()) {
            val container = addSection(
                "Document information",
                "Main values verified before this record was saved"
            )
            primitiveEntries.forEach { (key, value) ->
                addReadOnlyField(
                    container,
                    DocumentDataFormatter.labelFor(key),
                    DocumentDataFormatter.primitiveContent(value)
                )
            }
        }

        data.forEach { (key, value) ->
            if (key == "document_type" || key in primitiveEntries) return@forEach
            when (value) {
                is JsonObject -> renderObjectSection(key, value)
                is JsonArray -> renderArraySection(key, value)
                else -> Unit
            }
        }

        if (binding.dynamicFieldsContainer.childCount == 0) {
            val container = addSection(
                "Saved content",
                "The original response was not structured as a JSON object"
            )
            addReadOnlyField(container, "Extracted data", rawJson)
        }
    }

    private fun renderObjectSection(key: String, value: JsonObject) {
        val title = DocumentDataFormatter.labelFor(key)
        val container = addSection(
            title,
            "Verified ${title.lowercase(Locale.getDefault())}"
        )

        if (value.isEmpty()) {
            addReadOnlyField(container, "Value", "")
            return
        }

        value.forEach { (childKey, childValue) ->
            when (childValue) {
                is JsonPrimitive, JsonNull -> addReadOnlyField(
                    container,
                    DocumentDataFormatter.labelFor(childKey),
                    DocumentDataFormatter.primitiveContent(childValue)
                )
                is JsonObject -> {
                    addRecordHeader(container, DocumentDataFormatter.labelFor(childKey))
                    childValue.forEach { (nestedKey, nestedValue) ->
                        if (nestedValue is JsonPrimitive || nestedValue is JsonNull) {
                            addReadOnlyField(
                                container,
                                DocumentDataFormatter.labelFor(nestedKey),
                                DocumentDataFormatter.primitiveContent(nestedValue)
                            )
                        }
                    }
                }
                is JsonArray -> renderNestedArray(container, childKey, childValue)
                else -> Unit
            }
        }
    }

    private fun renderArraySection(key: String, value: JsonArray) {
        val title = DocumentDataFormatter.labelFor(key)
        val container = addSection(
            title,
            "${value.size} ${if (value.size == 1) "saved entry" else "saved entries"}"
        )

        if (value.isEmpty()) {
            addEmptyMessage(container, "No records were saved in this section")
            return
        }

        value.forEachIndexed { index, item ->
            when (item) {
                is JsonObject -> {
                    addRecordHeader(
                        container,
                        "${DocumentDataFormatter.singularLabel(key)} ${index + 1}"
                    )
                    if (item.isEmpty()) {
                        addEmptyMessage(container, "No values were saved for this entry")
                    } else {
                        item.forEach { (childKey, childValue) ->
                            when (childValue) {
                                is JsonPrimitive, JsonNull -> addReadOnlyField(
                                    container,
                                    DocumentDataFormatter.labelFor(childKey),
                                    DocumentDataFormatter.primitiveContent(childValue)
                                )
                                is JsonObject -> {
                                    addRecordHeader(
                                        container,
                                        DocumentDataFormatter.labelFor(childKey),
                                        compact = true
                                    )
                                    childValue.forEach { (nestedKey, nestedValue) ->
                                        if (nestedValue is JsonPrimitive || nestedValue is JsonNull) {
                                            addReadOnlyField(
                                                container,
                                                DocumentDataFormatter.labelFor(nestedKey),
                                                DocumentDataFormatter.primitiveContent(nestedValue)
                                            )
                                        }
                                    }
                                }
                                else -> Unit
                            }
                        }
                    }
                }

                else -> addReadOnlyField(
                    container,
                    "${DocumentDataFormatter.singularLabel(key)} ${index + 1}",
                    DocumentDataFormatter.primitiveContent(item)
                )
            }
        }
    }

    private fun renderNestedArray(parent: LinearLayout, key: String, value: JsonArray) {
        addRecordHeader(parent, DocumentDataFormatter.labelFor(key), compact = true)
        if (value.isEmpty()) {
            addEmptyMessage(parent, "No entries saved")
            return
        }
        value.forEachIndexed { index, item ->
            when (item) {
                is JsonObject -> {
                    addRecordHeader(parent, "Entry ${index + 1}", compact = true)
                    item.forEach { (childKey, childValue) ->
                        if (childValue is JsonPrimitive || childValue is JsonNull) {
                            addReadOnlyField(
                                parent,
                                DocumentDataFormatter.labelFor(childKey),
                                DocumentDataFormatter.primitiveContent(childValue)
                            )
                        }
                    }
                }
                else -> addReadOnlyField(
                    parent,
                    "Entry ${index + 1}",
                    DocumentDataFormatter.primitiveContent(item)
                )
            }
        }
    }

    private fun addSection(title: String, subtitle: String): LinearLayout {
        val header = TextView(requireContext()).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.onBackground))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(2.dp, 20.dp, 2.dp, 4.dp)
        }
        binding.dynamicFieldsContainer.addView(header)

        val description = TextView(requireContext()).apply {
            text = subtitle
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.onSurfaceVariant))
            setPadding(2.dp, 0, 2.dp, 10.dp)
        }
        binding.dynamicFieldsContainer.addView(description)

        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14.dp, 14.dp, 14.dp, 4.dp)
        }
        val card = MaterialCardView(requireContext()).apply {
            radius = 20.dp.toFloat()
            cardElevation = 0f
            setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface))
            strokeWidth = 1.dp
            strokeColor = ContextCompat.getColor(requireContext(), R.color.outlineVariant)
            addView(content)
        }
        binding.dynamicFieldsContainer.addView(
            card,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 2.dp }
        )
        return content
    }

    private fun addRecordHeader(parent: LinearLayout, title: String, compact: Boolean = false) {
        val header = TextView(parent.context).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (compact) 12f else 13f)
            setTextColor(ContextCompat.getColor(context, R.color.primary))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(2.dp, if (parent.childCount == 0) 0 else 10.dp, 2.dp, 8.dp)
        }
        parent.addView(header)
    }

    private fun addEmptyMessage(parent: LinearLayout, message: String) {
        parent.addView(
            TextView(parent.context).apply {
                text = message
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(ContextCompat.getColor(context, R.color.onSurfaceVariant))
                setPadding(2.dp, 2.dp, 2.dp, 14.dp)
            }
        )
    }

    private fun addReadOnlyField(parent: LinearLayout, label: String, value: String) {
        val blank = value.isBlank()
        val layout = TextInputLayout(parent.context).apply {
            hint = label
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(
                14.dp.toFloat(),
                14.dp.toFloat(),
                14.dp.toFloat(),
                14.dp.toFloat()
            )
            helperText = if (blank) "No value was saved" else "Verified value"
            endIconMode = TextInputLayout.END_ICON_CUSTOM
            setEndIconDrawable(if (blank) R.drawable.ic_info else R.drawable.ic_check_circle)
            setEndIconTintList(
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        if (blank) R.color.pending else R.color.success
                    )
                )
            )
        }

        val editText = TextInputEditText(layout.context).apply {
            setText(if (blank) "Not provided" else value)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            isFocusable = false
            isFocusableInTouchMode = false
            isCursorVisible = false
            isLongClickable = true
            setTextIsSelectable(true)
            minHeight = 54.dp
            setPadding(14.dp, paddingTop, 14.dp, paddingBottom)
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (blank) R.color.onSurfaceVariant else R.color.onSurface
                )
            )
        }
        layout.addView(editText)
        parent.addView(
            layout,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12.dp }
        )
    }

    private fun shareDocument(document: DocumentEntity) {
        val readableData = DocumentDataFormatter.toReadableText(document.rawJson)
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, document.title)
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "${document.title}\n${document.summary}\n\n$readableData"
                    )
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

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "DocumentDetailFragment"
    }
}
