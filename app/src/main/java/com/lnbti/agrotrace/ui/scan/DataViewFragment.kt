package com.lnbti.agrotrace.ui.scan

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.lnbti.agrotrace.DocumentTypeUtils
import com.lnbti.agrotrace.R
import com.lnbti.agrotrace.databinding.FragmentDataViewBinding
import com.lnbti.agrotrace.db.AppDatabase
import com.lnbti.agrotrace.db.DocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.File

class DataViewFragment : Fragment() {

    private var _binding: FragmentDataViewBinding? = null
    private val binding get() = _binding!!

    private val args: DataViewFragmentArgs by navArgs()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }
    private val editableFields = linkedMapOf<String, TextInputEditText>()
    private lateinit var originalData: JsonObject
    private var saveJob: Job? = null

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

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.tvDocumentType.text = DocumentTypeUtils.name(args.docType)
        bindScannedImage()
        renderData(args.extractedData)

        binding.btnSave.setOnClickListener { confirmAndSave() }
        binding.btnExport.setOnClickListener { shareEditedData() }
    }

    private fun bindScannedImage() {
        val imageFile = File(args.imagePath)
        binding.cardScanPreview.isVisible = imageFile.isFile
        if (imageFile.isFile) {
            Glide.with(this)
                .load(imageFile)
                .centerCrop()
                .into(binding.ivScannedDocument)
        }
    }

    private fun renderData(rawJson: String) {
        binding.dynamicFieldsContainer.removeAllViews()
        editableFields.clear()

        originalData = runCatching {
            json.parseToJsonElement(rawJson) as? JsonObject
                ?: JsonObject(mapOf("extracted_data" to JsonPrimitive(rawJson)))
        }.getOrElse {
            JsonObject(mapOf("extracted_data" to JsonPrimitive(rawJson)))
        }

        val primitiveEntries = originalData
            .filterValues { it is JsonPrimitive || it is JsonNull }
            .filterKeys { it != "document_type" }

        if (primitiveEntries.isNotEmpty()) {
            val container = addSection(
                "Document information",
                "Main details detected on the form"
            )
            primitiveEntries.forEach { (key, value) ->
                addEditableField(container, key, labelFor(key), primitiveContent(value))
            }
        }

        originalData.forEach { (key, value) ->
            if (key == "document_type" || key in primitiveEntries) return@forEach
            when (value) {
                is JsonObject -> renderObjectSection(key, value)
                is JsonArray -> renderArraySection(key, value)
                else -> Unit
            }
        }

        if (editableFields.isEmpty()) {
            originalData = JsonObject(
                originalData + ("extracted_data" to JsonPrimitive(rawJson))
            )
            val container = addSection(
                "Extracted content",
                "Enter the required information manually"
            )
            addEditableField(container, "extracted_data", "Extracted data", rawJson)
        }
        refreshSummary()
    }

    private fun renderObjectSection(key: String, value: JsonObject) {
        val container = addSection(
            labelFor(key),
            "Review the extracted ${labelFor(key).lowercase()}"
        )
        if (value.isEmpty()) {
            addEditableField(container, "$key.value", "Value", "")
        } else {
            value.forEach { (childKey, childValue) ->
                if (childValue is JsonPrimitive || childValue is JsonNull) {
                    addEditableField(
                        container,
                        "$key.$childKey",
                        labelFor(childKey),
                        primitiveContent(childValue)
                    )
                }
            }
        }
    }

    private fun renderArraySection(key: String, value: JsonArray) {
        val container = addSection(
            labelFor(key),
            "Multiple entries can be corrected independently"
        )
        if (value.isEmpty()) {
            addEditableField(container, "$key[0]", "${labelFor(key)} 1", "")
            return
        }

        value.forEachIndexed { index, item ->
            when (item) {
                is JsonPrimitive, JsonNull -> addEditableField(
                    container,
                    "$key[$index]",
                    "${singularLabel(key)} ${index + 1}",
                    primitiveContent(item)
                )

                is JsonObject -> item.forEach { (childKey, childValue) ->
                    if (childValue is JsonPrimitive || childValue is JsonNull) {
                        addEditableField(
                            container,
                            "$key[$index].$childKey",
                            "${index + 1}. ${labelFor(childKey)}",
                            primitiveContent(childValue)
                        )
                    }
                }

                else -> Unit
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
            ).apply { bottomMargin = 4.dp }
        )
        return content
    }

    private fun addEditableField(
        parent: LinearLayout,
        path: String,
        label: String,
        value: String?
    ) {
        val layout = TextInputLayout(
            requireContext(),
            null,
            com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox
        ).apply {
            hint = label
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(
                14.dp.toFloat(),
                14.dp.toFloat(),
                14.dp.toFloat(),
                14.dp.toFloat()
            )
            endIconMode = TextInputLayout.END_ICON_CUSTOM
        }
        val editText = TextInputEditText(layout.context).apply {
            setText(value.orEmpty())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            minHeight = 54.dp
            setPadding(14.dp, paddingTop, 14.dp, paddingBottom)
        }
        layout.addView(editText)
        parent.addView(
            layout,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12.dp }
        )
        editableFields[path] = editText

        fun updateState() {
            val blank = editText.text.isNullOrBlank()
            layout.error = if (blank) {
                "Missing value — enter manually if available"
            } else {
                null
            }
            layout.helperText = if (blank) null else "Extracted value • editable"
            layout.setEndIconDrawable(
                if (blank) R.drawable.ic_info else R.drawable.ic_check_circle
            )
            layout.setEndIconTintList(
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        if (blank) R.color.pending else R.color.success
                    )
                )
            )
            refreshSummary()
        }

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) = updateState()

            override fun afterTextChanged(s: Editable?) = Unit
        })
        updateState()
    }

    private fun refreshSummary() {
        if (_binding == null) return
        val missing = editableFields.values.count { it.text.isNullOrBlank() }
        val total = editableFields.size
        val completed = (total - missing).coerceAtLeast(0)
        val progress = if (total == 0) 0 else (completed * 100 / total)

        binding.reviewProgress.setProgressCompat(progress, true)
        binding.tvFieldSummary.text = when {
            total == 0 -> "No editable fields were detected."
            missing == 0 -> "$total fields detected • all values are ready to verify"
            else -> "$total fields detected • $missing ${if (missing == 1) "value needs" else "values need"} attention"
        }
        binding.tvReviewHint.text = if (missing == 0) {
            "All detected values are filled. Make any corrections before saving."
        } else {
            "$missing blank ${if (missing == 1) "field is" else "fields are"} highlighted for manual entry."
        }
        binding.bannerReview.setBackgroundResource(
            if (missing == 0) R.drawable.bg_success_soft else R.drawable.bg_warning_soft
        )
    }

    private fun confirmAndSave() {
        if (saveJob?.isActive == true) return

        val missing = editableFields.values.count { it.text.isNullOrBlank() }
        if (missing == 0) {
            saveDocument()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("$missing blank ${if (missing == 1) "field" else "fields"}")
            .setMessage(
                "You can return to the form and enter missing values, " +
                    "or save the document with blanks."
            )
            .setNegativeButton("Review") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Save anyway") { _, _ -> saveDocument() }
            .show()
    }

    private fun saveDocument() {
        if (saveJob?.isActive == true || _binding == null) return

        val prepared = runCatching {
            val edited = rebuildElement(originalData, "") as JsonObject
            SavePayload(
                json = json.encodeToString(JsonObject.serializer(), edited),
                summary = createSummary(edited)
            )
        }.getOrElse { error ->
            showSaveError(error)
            return
        }

        val appContext = requireContext().applicationContext
        setSaving(true)

        saveJob = viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(appContext).documentDao().insert(
                        DocumentEntity(
                            type = args.docType,
                            title = DocumentTypeUtils.name(args.docType),
                            summary = prepared.summary,
                            rawJson = prepared.json
                        )
                    )
                }
            }

            if (_binding == null) return@launch

            result.onSuccess { insertedId ->
                if (insertedId <= 0L) {
                    setSaving(false)
                    showSaveError(IllegalStateException("The database did not return a record ID."))
                    return@onSuccess
                }
                Toast.makeText(
                    requireContext(),
                    "Document saved to History",
                    Toast.LENGTH_SHORT
                ).show()
                navigateToHistorySafely()
            }.onFailure { error ->
                Log.e(TAG, "Failed to save reviewed document", error)
                setSaving(false)
                showSaveError(error)
            }
        }
    }

    private fun setSaving(saving: Boolean) {
        if (_binding == null) return
        binding.btnSave.isEnabled = !saving
        binding.btnExport.isEnabled = !saving
        binding.btnSave.text = if (saving) "Saving…" else "Save document"
        binding.saveProgress.isVisible = saving
    }

    private fun showSaveError(error: Throwable) {
        if (_binding == null || !isAdded) return
        val message = error.message?.takeIf { it.isNotBlank() }
            ?: "An unexpected local database error occurred."
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Document was not saved")
            .setMessage(
                "$message\n\nYour edited values are still on this screen. " +
                    "Please try again."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun navigateToHistorySafely() {
        if (!isAdded || _binding == null) return
        val controller = findNavController()

        runCatching {
            controller.navigate(R.id.action_global_history)
        }.onFailure { error ->
            Log.e(TAG, "Saved successfully but navigation to History failed", error)
            setSaving(false)
            Snackbar.make(
                binding.root,
                "Saved successfully. Open History from the bottom menu.",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun shareEditedData() {
        val body = runCatching {
            val edited = rebuildElement(originalData, "") as JsonObject
            "${DocumentTypeUtils.name(args.docType)}\n\n" +
                json.encodeToString(JsonObject.serializer(), edited)
        }.getOrElse { error ->
            Snackbar.make(
                binding.root,
                "Could not prepare data for sharing: ${error.message}",
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, DocumentTypeUtils.name(args.docType))
                    putExtra(Intent.EXTRA_TEXT, body)
                },
                "Share extracted data"
            )
        )
    }

    private fun rebuildElement(element: JsonElement, path: String): JsonElement = when (element) {
        is JsonObject -> {
            if (element.isEmpty() && path.isNotBlank()) {
                val editedValue = editableFields["$path.value"]?.text?.toString().orEmpty()
                if (editedValue.isBlank()) {
                    JsonObject(emptyMap())
                } else {
                    JsonObject(mapOf("value" to JsonPrimitive(editedValue)))
                }
            } else {
                JsonObject(element.mapValues { (key, value) ->
                    val childPath = if (path.isBlank()) key else "$path.$key"
                    rebuildElement(value, childPath)
                })
            }
        }

        is JsonArray -> {
            if (element.isEmpty()) {
                val value = editableFields["$path[0]"]?.text?.toString().orEmpty()
                if (value.isBlank()) {
                    JsonArray(emptyList())
                } else {
                    JsonArray(listOf(JsonPrimitive(value)))
                }
            } else {
                JsonArray(
                    element.mapIndexed { index, value ->
                        rebuildElement(value, "$path[$index]")
                    }
                )
            }
        }

        is JsonPrimitive, JsonNull -> {
            val editedValue = editableFields[path]?.text?.toString()
            when {
                editedValue == null -> element
                editedValue.isBlank() -> JsonNull
                else -> JsonPrimitive(editedValue)
            }
        }

        else -> element
    }

    private fun createSummary(data: JsonObject): String {
        val preferredKeys = listOf(
            "farmer_name",
            "name_of_seed_producer",
            "farmer_registration_no",
            "registration_no",
            "lot_no",
            "lot_no_for_seeds",
            "request_no",
            "report_no",
            "label_serial_no",
            "inspection_no",
            "harvest_inspect_no"
        )
        preferredKeys.forEach { key ->
            val value = data[key] ?: return@forEach
            when (value) {
                is JsonPrimitive -> value.contentOrNull
                    ?.takeIf { it.isNotBlank() }
                    ?.let { return it }

                is JsonArray -> value.firstOrNull()?.let { item ->
                    if (item is JsonPrimitive) {
                        item.contentOrNull
                            ?.takeIf { it.isNotBlank() }
                            ?.let { return it }
                    }
                }

                else -> Unit
            }
        }
        return "Verified ${DocumentTypeUtils.name(args.docType)}"
    }

    private fun primitiveContent(element: JsonElement): String = when (element) {
        JsonNull -> ""
        is JsonPrimitive -> element.contentOrNull.orEmpty()
        else -> element.toString()
    }

    private fun labelFor(key: String): String {
        val aliases = mapOf(
            "see_act_registration_no" to "Seed Act registration no.",
            "harvest_inspect_no" to "Harvest inspection no.",
            "no_of_containers" to "Number of containers",
            "officer_sign" to "Officer seal and signature",
            "crop_id" to "Crop",
            "form_no" to "Form no.",
            "lot_no" to "Lot no.",
            "lot_no_for_seeds" to "Seed lot numbers"
        )
        return aliases[key] ?: key
            .replace('_', ' ')
            .trim()
            .replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
    }

    private fun singularLabel(key: String): String {
        val label = labelFor(key)
        return when {
            label.endsWith("ies") -> label.dropLast(3) + "y"
            label.endsWith("s") -> label.dropLast(1)
            else -> label
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class SavePayload(
        val json: String,
        val summary: String
    )

    companion object {
        private const val TAG = "DataViewFragment"
    }
}
