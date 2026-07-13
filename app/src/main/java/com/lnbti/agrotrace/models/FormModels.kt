package com.lnbti.agrotrace.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Common interface or base for all form results to allow unified handling
 */
interface BaseFormResult {
    val document_type: String
}

// ── TYPE 1: LAND APPROVAL FORM ──────────────────────────────────────────
@Serializable
data class LandApprovalResult(
    override val document_type: String = "land_approval_form",
    val see_act_registration_no: String? = null,
    val form_date: String? = null,
    val farmer_name: String? = null,
    val address: String? = null,
    val lot_no_for_seeds: List<String?> = emptyList(),
    val land_address: List<String?> = emptyList(),
    val transplanted_date: List<String?> = emptyList(),
    val contact_no: List<String?> = emptyList(),
    val crop_id: List<String?> = emptyList(),
    val variety: List<String?> = emptyList(),
    val land_area: List<String?> = emptyList(),
    val quantity_of_seeds_used: List<String?> = emptyList()
) : BaseFormResult

// ── TYPE 2: CROP REGISTRATION FORM ──────────────────────────────────────
@Serializable
data class CropRegistrationResult(
    override val document_type: String = "crop_registration_form",
    val registration_no: String? = null,
    val form_no: String? = null,
    val seed_act_registration_no: String? = null,
    val farmer_registration_no: String? = null,
    val date: String? = null,
    val name_of_seed_producer: String? = null,
    val address_of_seed_producer: String? = null,
    val field_no: List<String?> = emptyList(),
    val crop_grown_in_last_two_seasons: List<String?> = emptyList(),
    val harvest_date: List<String?> = emptyList(),
    val payment_no: String? = null,
    val payment_amount: String? = null,
    val registration_officer: String? = null
) : BaseFormResult

// ── TYPE 3: FIELD INSPECTION REPORT ─────────────────────────────────────
@Serializable
data class InspectionFormResult(
    override val document_type: String = "inspection_form",
    val inspection_no: String? = null,
    val inspection_date: String? = null,
    val seed_act_registration_no: String? = null,
    val farmer_registration_no: String? = null,
    val field_no: String? = null,
    val observation: String? = null,
    val inspection_round: JsonElement? = null
) : BaseFormResult

// ── TYPE 4: FINAL FIELD INSPECTION REPORT ───────────────────────────────
@Serializable
data class FinalInspectionResult(
    override val document_type: String = "final_field_inspection_report",
    val harvest_inspect_no: String? = null,
    val farmer_registration_no: String? = null,
    val final_inspection_date: String? = null,
    val extent_accepted: String? = null,
    val extent_rejected: String? = null,
    val estimated_seed_yield: String? = null,
    val other_distinguish_varieties: List<String?> = emptyList(),
    val pest_and_diseases: List<String?> = emptyList(),
    val remarks: List<String?> = emptyList(),
    val decision: List<String?> = emptyList(),
    val officer_sign: OfficerSign? = null
) : BaseFormResult

@Serializable
data class OfficerSign(
    val name: String? = null,
    val designation: String? = null,
    val organization: String? = null,
    val department: String? = null,
    val office: String? = null,
    val full_text: String? = null
)

// ── TYPE 5: SEED TEST REQUEST FORM ──────────────────────────────────────
@Serializable
data class SeedTestRequestResult(
    override val document_type: String = "seed_test_request",
    val request_no: String? = null,
    val date: String? = null,
    val lot_no: String? = null,
    val crop: String? = null,
    val variety: String? = null,
    val class_of_seed: String? = null,
    val weight_of_lot: String? = null,
    val no_of_containers: String? = null,
    val sender_name: String? = null,
    val sender_address: String? = null
) : BaseFormResult

// ── TYPE 6: SEED TEST REPORT ───────────────────────────────────────────
@Serializable
data class SeedTestReportResult(
    override val document_type: String = "seed_test_report",
    val report_no: String? = null,
    val test_date: String? = null,
    val germination_percentage: String? = null,
    val purity_percentage: String? = null,
    val moisture_content: String? = null,
    val inert_matter: String? = null,
    val other_seeds: String? = null,
    val status: String? = null // Passed / Failed
) : BaseFormResult

// ── TYPE 7: LABELING DOCUMENT ─────────────────────────────────────────
@Serializable
data class LabelingResult(
    override val document_type: String = "labeling_document",
    val label_serial_no: String? = null,
    val lot_no: String? = null,
    val crop: String? = null,
    val variety: String? = null,
    val date_of_test: String? = null,
    val valid_until: String? = null,
    val net_weight: String? = null
) : BaseFormResult
