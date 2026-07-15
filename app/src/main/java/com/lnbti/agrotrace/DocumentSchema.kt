package com.lnbti.agrotrace

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Stable UI/database skeletons for all seven extraction contracts.
 *
 * Gemini is instructed to return every key, but this merge also protects the review
 * screen when a key, officer block or complete table section is omitted.
 */
object DocumentSchema {

    private val json = Json { isLenient = true; explicitNulls = true }

    private val schemas: Map<Int, JsonObject> = mapOf(
        1 to schema(
            """
            {
              "document_type": "land_approval_form",
              "shared": {
                "lot_no_for_seeds": null,
                "land_address": null,
                "transplanted_date": null,
                "form_date": null,
                "see_act_registration_no": null,
                "farmer_name": null,
                "address": null
              },
              "records": [
                {
                  "contact_no": null,
                  "crop_id": null,
                  "variety": null,
                  "land_area": null,
                  "quantity_of_seeds_used": null
                }
              ]
            }
            """
        ),
        2 to schema(
            """
            {
              "document_type": "crop_registration_form",
              "registration_no": null,
              "form_no": null,
              "seed_act_registration_no": null,
              "farmer_registration_no": null,
              "date": null,
              "name_of_seed_producer": null,
              "address_of_seed_producer": null,
              "field_records": [
                {
                  "field_no": null,
                  "crop_grown_in_last_two_seasons": null,
                  "harvest_date": null
                }
              ],
              "payment_no": null,
              "payment_amount": null,
              "registration_officer": {
                "name": null,
                "designation": null,
                "organization": null,
                "department": null,
                "office": null,
                "full_text": null
              }
            }
            """
        ),
        3 to schema(
            """
            {
              "document_type": "inspection_form",
              "inspection_no": null,
              "inspection_date": null,
              "seed_act_registration_no": null,
              "farmer_registration_no": null,
              "field_no": null,
              "observation": null,
              "inspection_round": null
            }
            """
        ),
        4 to schema(
            """
            {
              "document_type": "final_field_inspection_report",
              "harvest_inspect_no": null,
              "farmer_registration_no": null,
              "final_inspection_date": null,
              "extent_accepted": null,
              "extent_rejected": null,
              "estimated_seed_yield": null,
              "observation_records": [
                {
                  "other_distinguish_varieties": null,
                  "pest_and_diseases": null,
                  "remarks": null,
                  "decision": null
                }
              ],
              "officer_sign": {
                "name": null,
                "designation": null,
                "organization": null,
                "department": null,
                "office": null,
                "full_text": null
              }
            }
            """
        ),
        5 to schema(
            """
            {
              "document_type": "seed_test_request_form",
              "request_id": null,
              "lab_id": null,
              "process_at": null,
              "species": null,
              "name_of_contractor": null,
              "lot_no": null,
              "date_sampled": null,
              "date_labeled_and_sealed": null,
              "no_of_labels_issued": null,
              "remarks": null,
              "region": null,
              "farmer_registration_no": null,
              "test_requested_date": null,
              "processing": null,
              "sampling_officer": {
                "name": null,
                "designation": null,
                "region": null,
                "full_text": null
              }
            }
            """
        ),
        6 to schema(
            """
            {
              "document_type": "seed_test_report",
              "b_report_no": null,
              "report_date": null,
              "date_received_for_test": null,
              "request_form_no": null,
              "date_finished_test": null,
              "farmer_registration_no": null,
              "seed_act_registration_no": null,
              "crop": null,
              "variety": null,
              "seed_class": null,
              "date_of_harvest": null,
              "seed_producer_name": null,
              "date_sampled": null,
              "lot_no": null,
              "quantity_of_seed_lot": null,
              "recommendation": null,
              "officer_in_charge": {
                "name": null,
                "designation": null,
                "office": null,
                "full_text": null
              },
              "test_results": {
                "other_distinguish_varieties": null,
                "just_discernible_seed": null,
                "other_crop_seed": null,
                "light_and_mechanical_damaged_seed": null,
                "appearance": null,
                "smell": null,
                "pure_seed": null,
                "inert_matter": null,
                "weed_seed": null,
                "other_crop_seed_percent": null,
                "normal_seedlings": null,
                "abnormal_seedlings": null,
                "hard_seed": null,
                "fresh_ungerminated_seed": null,
                "dead_seed": null,
                "viability": null,
                "moisture": null
              }
            }
            """
        ),
        7 to schema(
            """
            {
              "document_type": "labeling_document_form",
              "date_of_sampling": null,
              "date_of_labeling": null,
              "no_of_labels_issued": null,
              "no_of_bags": null,
              "lot_no_used": null,
              "b_report_no": null,
              "region": null,
              "registration_no": null,
              "crop": null,
              "variety": null,
              "seed_class": null,
              "name_of_grower": null
            }
            """
        )
    )

    fun mergeWithExpected(type: Int, extracted: JsonObject): JsonObject {
        val expected = schemas[type] ?: return extracted
        return mergeElement(expected, extracted) as JsonObject
    }

    /** Remove blank placeholder rows while preserving all scalar/object keys as null. */
    fun pruneBlankArrayRecords(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(
            element.mapValues { (_, value) -> pruneBlankArrayRecords(value) }
        )

        is JsonArray -> JsonArray(
            element.map(::pruneBlankArrayRecords).filterNot(::isCompletelyBlankObject)
        )

        else -> element
    }

    private fun mergeElement(expected: JsonElement, actual: JsonElement?): JsonElement {
        if (actual == null || actual is JsonNull) {
            return when (expected) {
                is JsonObject -> JsonObject(
                    expected.mapValues { (_, value) -> mergeElement(value, null) }
                )
                is JsonArray -> expected
                else -> JsonNull
            }
        }

        return when {
            expected is JsonObject && actual is JsonObject -> {
                val expectedKeys = expected.mapValues { (key, expectedValue) ->
                    mergeElement(expectedValue, actual[key])
                }
                val extras = actual.filterKeys { it !in expected }
                JsonObject(expectedKeys + extras)
            }

            expected is JsonArray && actual is JsonArray -> {
                val template = expected.firstOrNull()
                when {
                    actual.isEmpty() -> expected
                    template == null -> actual
                    else -> JsonArray(actual.map { item -> mergeElement(template, item) })
                }
            }

            else -> actual
        }
    }

    private fun isCompletelyBlankObject(element: JsonElement): Boolean {
        if (element !is JsonObject) return false
        if (element.isEmpty()) return true
        return element.values.all(::isBlankValue)
    }

    private fun isBlankValue(element: JsonElement): Boolean = when (element) {
        JsonNull -> true
        is JsonPrimitive -> element.contentOrNull.isNullOrBlank()
        is JsonObject -> element.values.all(::isBlankValue)
        is JsonArray -> element.isEmpty() || element.all(::isBlankValue)
        else -> false
    }

    private fun schema(raw: String): JsonObject =
        json.parseToJsonElement(raw.trimIndent()) as JsonObject
}
