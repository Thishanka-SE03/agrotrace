package com.lnbti.agrotrace

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Shared labels and readable export formatting for extracted form JSON. */
object DocumentDataFormatter {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = true
    }

    data class FieldStats(val total: Int, val completed: Int) {
        val missing: Int get() = (total - completed).coerceAtLeast(0)
    }

    fun parseObject(rawJson: String): JsonObject = runCatching {
        json.parseToJsonElement(rawJson) as? JsonObject
    }.getOrNull() ?: JsonObject(mapOf("extracted_data" to JsonPrimitive(rawJson)))

    fun primitiveContent(element: JsonElement): String = when (element) {
        JsonNull -> ""
        is JsonPrimitive -> element.contentOrNull.orEmpty()
        else -> element.toString()
    }

    fun labelFor(key: String): String {
        val aliases = mapOf(
            "shared" to "Shared form information",
            "records" to "Land and crop records",
            "field_records" to "Field records",
            "observation_records" to "Observation records",
            "test_results" to "Seed test results",
            "registration_officer" to "Registration officer",
            "sampling_officer" to "Sampling officer",
            "officer_in_charge" to "Officer in charge",
            "officer_sign" to "Inspecting officer",
            "see_act_registration_no" to "Seed Act registration no.",
            "seed_act_registration_no" to "Seed Act registration no.",
            "farmer_registration_no" to "Farmer registration no.",
            "registration_no" to "Registration no.",
            "harvest_inspect_no" to "Harvest inspection no.",
            "inspection_no" to "Inspection no.",
            "request_id" to "Request ID",
            "request_form_no" to "Request form no.",
            "b_report_no" to "B report no.",
            "form_no" to "Form no.",
            "lot_no" to "Lot no.",
            "lot_no_used" to "Lot no. used",
            "lot_no_for_seeds" to "Seed lot no.",
            "crop_id" to "Crop",
            "contact_no" to "Contact no.",
            "no_of_labels_issued" to "Number of labels issued",
            "no_of_bags" to "Number of bags",
            "quantity_of_seeds_used" to "Quantity of seeds used",
            "quantity_of_seed_lot" to "Quantity of seed lot",
            "date_labeled_and_sealed" to "Date labelled and sealed",
            "date_of_labeling" to "Date of labelling",
            "date_received_for_test" to "Date received for test",
            "date_finished_test" to "Date test concluded",
            "test_requested_date" to "Test request date",
            "name_of_seed_producer" to "Seed producer name",
            "address_of_seed_producer" to "Seed producer address",
            "seed_producer_name" to "Seed producer name",
            "name_of_contractor" to "Contractor name",
            "name_of_grower" to "Grower name",
            "crop_grown_in_last_two_seasons" to "Crops grown in last two seasons",
            "other_distinguish_varieties" to "Other distinguishable varieties",
            "pest_and_diseases" to "Pests and diseases",
            "just_discernible_seed" to "Just discernible seed",
            "light_and_mechanical_damaged_seed" to "Light and mechanically damaged seed",
            "other_crop_seed_percent" to "Other crop seed (%)",
            "normal_seedlings" to "Normal seedlings",
            "abnormal_seedlings" to "Abnormal seedlings",
            "fresh_ungerminated_seed" to "Fresh ungerminated seed",
            "inert_matter" to "Inert matter",
            "pure_seed" to "Pure seed",
            "weed_seed" to "Weed seed",
            "seed_class" to "Seed class",
            "lab_id" to "Laboratory",
            "process_at" to "Processed at",
            "full_text" to "Full seal text"
        )
        return aliases[key] ?: key
            .replace('_', ' ')
            .trim()
            .replaceFirstChar { first ->
                if (first.isLowerCase()) first.titlecase() else first.toString()
            }
    }

    fun singularLabel(key: String): String {
        val label = labelFor(key)
        return when {
            label.endsWith("ies", ignoreCase = true) -> label.dropLast(3) + "y"
            label.endsWith("s", ignoreCase = true) -> label.dropLast(1)
            else -> label
        }
    }


    fun createSummary(data: JsonObject, fallback: String): String {
        val preferredKeys = listOf(
            "farmer_name",
            "name_of_seed_producer",
            "name_of_grower",
            "seed_producer_name",
            "farmer_registration_no",
            "registration_no",
            "lot_no",
            "lot_no_used",
            "lot_no_for_seeds",
            "request_id",
            "request_form_no",
            "b_report_no",
            "inspection_no",
            "harvest_inspect_no",
            "form_no"
        )

        preferredKeys.forEach { preferredKey ->
            findValueForKey(data, preferredKey)
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        }
        return fallback
    }

    private fun findValueForKey(element: JsonElement, targetKey: String): String? = when (element) {
        is JsonObject -> {
            element[targetKey]?.let { value ->
                when (value) {
                    is JsonPrimitive -> value.contentOrNull
                    is JsonArray -> value.firstOrNull()?.let(::primitiveContent)
                    else -> null
                }
            } ?: element.values.firstNotNullOfOrNull { child ->
                findValueForKey(child, targetKey)
            }
        }
        is JsonArray -> element.firstNotNullOfOrNull { child ->
            findValueForKey(child, targetKey)
        }
        else -> null
    }

    fun countFields(element: JsonElement): FieldStats = when (element) {
        is JsonObject -> element
            .filterKeys { it != "document_type" }
            .values
            .map(::countFields)
            .fold(FieldStats(0, 0)) { total, next ->
                FieldStats(total.total + next.total, total.completed + next.completed)
            }

        is JsonArray -> element
            .map(::countFields)
            .fold(FieldStats(0, 0)) { total, next ->
                FieldStats(total.total + next.total, total.completed + next.completed)
            }

        JsonNull -> FieldStats(total = 1, completed = 0)
        is JsonPrimitive -> FieldStats(
            total = 1,
            completed = if (element.contentOrNull.isNullOrBlank()) 0 else 1
        )
        else -> FieldStats(0, 0)
    }

    fun toReadableText(rawJson: String): String {
        val root = parseObject(rawJson)
        val output = StringBuilder()
        appendObject(output, root, 0)
        return output.toString().trim()
    }

    private fun appendObject(output: StringBuilder, value: JsonObject, level: Int) {
        value.forEach { (key, element) ->
            if (key == "document_type") return@forEach
            when (element) {
                is JsonObject -> {
                    appendHeading(output, labelFor(key), level)
                    if (element.isEmpty()) {
                        appendValue(output, "Value", "Not provided", level + 1)
                    } else {
                        appendObject(output, element, level + 1)
                    }
                }

                is JsonArray -> {
                    appendHeading(output, labelFor(key), level)
                    if (element.isEmpty()) {
                        appendValue(output, "Status", "No records saved", level + 1)
                    } else {
                        element.forEachIndexed { index, item ->
                            when (item) {
                                is JsonObject -> {
                                    appendHeading(
                                        output,
                                        "${singularLabel(key)} ${index + 1}",
                                        level + 1
                                    )
                                    appendObject(output, item, level + 2)
                                }
                                else -> appendValue(
                                    output,
                                    "${singularLabel(key)} ${index + 1}",
                                    primitiveContent(item).ifBlank { "Not provided" },
                                    level + 1
                                )
                            }
                        }
                    }
                }

                else -> appendValue(
                    output,
                    labelFor(key),
                    primitiveContent(element).ifBlank { "Not provided" },
                    level
                )
            }
        }
    }

    private fun appendHeading(output: StringBuilder, heading: String, level: Int) {
        if (output.isNotEmpty()) output.appendLine()
        output.append("  ".repeat(level)).append(heading).appendLine()
    }

    private fun appendValue(output: StringBuilder, label: String, value: String, level: Int) {
        output.append("  ".repeat(level))
            .append(label)
            .append(": ")
            .append(value)
            .appendLine()
    }
}
