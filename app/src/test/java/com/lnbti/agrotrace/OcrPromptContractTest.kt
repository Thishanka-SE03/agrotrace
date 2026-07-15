package com.lnbti.agrotrace

import org.junit.Assert.assertTrue
import org.junit.Test

class OcrPromptContractTest {

    @Test
    fun everyDocumentTypeUsesItsFinalExtractionContract() {
        val requiredKeys = mapOf(
            1 to listOf("land_approval_form", "shared", "records", "lot_no_for_seeds"),
            2 to listOf("crop_registration_form", "field_records", "registration_officer"),
            3 to listOf("inspection_form", "inspection_round", "observation"),
            4 to listOf("final_field_inspection_report", "observation_records", "officer_sign"),
            5 to listOf("seed_test_request_form", "request_id", "lab_id", "sampling_officer"),
            6 to listOf("seed_test_report", "b_report_no", "test_results", "officer_in_charge"),
            7 to listOf("labeling_document_form", "no_of_labels_issued", "name_of_grower")
        )

        requiredKeys.forEach { (type, keys) ->
            val prompt = OcrPrompts.forType(type)
            keys.forEach { key ->
                assertTrue("Document type $type prompt must contain $key", prompt.contains(key))
            }
            assertTrue(prompt.contains("Return ONLY valid JSON"))
            assertTrue(prompt.contains("Keep all JSON keys exactly as provided"))
        }
    }
}
