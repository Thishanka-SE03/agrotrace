package com.lnbti.agrotrace.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

interface BaseFormResult {
    val document_type: String
}

// ── TYPE 1: LAND APPROVAL FORM ──────────────────────────────────────────
@Serializable
data class LandApprovalResult(
    override val document_type: String = "land_approval_form",
    val shared: LandApprovalShared? = null,
    val records: List<LandApprovalRecord> = emptyList()
) : BaseFormResult

@Serializable
data class LandApprovalShared(
    val lot_no_for_seeds: String? = null,
    val land_address: String? = null,
    val transplanted_date: String? = null,
    val form_date: String? = null,
    val see_act_registration_no: String? = null,
    val farmer_name: String? = null,
    val address: String? = null
)

@Serializable
data class LandApprovalRecord(
    val contact_no: String? = null,
    val crop_id: String? = null,
    val variety: String? = null,
    val land_area: String? = null,
    val quantity_of_seeds_used: String? = null
)

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
    val field_records: List<CropFieldRecord> = emptyList(),
    val payment_no: String? = null,
    val payment_amount: String? = null,
    val registration_officer: OfficerDetails? = null
) : BaseFormResult

@Serializable
data class CropFieldRecord(
    val field_no: String? = null,
    val crop_grown_in_last_two_seasons: String? = null,
    val harvest_date: String? = null
)

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
    val observation_records: List<FinalInspectionObservation> = emptyList(),
    val officer_sign: OfficerDetails? = null
) : BaseFormResult

@Serializable
data class FinalInspectionObservation(
    val other_distinguish_varieties: String? = null,
    val pest_and_diseases: String? = null,
    val remarks: String? = null,
    val decision: String? = null
)

// ── TYPE 5: SEED TEST REQUEST FORM ──────────────────────────────────────
@Serializable
data class SeedTestRequestResult(
    override val document_type: String = "seed_test_request_form",
    val request_id: String? = null,
    val lab_id: String? = null,
    val process_at: String? = null,
    val species: String? = null,
    val name_of_contractor: String? = null,
    val lot_no: String? = null,
    val date_sampled: String? = null,
    val date_labeled_and_sealed: String? = null,
    val no_of_labels_issued: String? = null,
    val remarks: String? = null,
    val region: String? = null,
    val farmer_registration_no: String? = null,
    val test_requested_date: String? = null,
    val processing: String? = null,
    val sampling_officer: SamplingOfficer? = null
) : BaseFormResult

@Serializable
data class SamplingOfficer(
    val name: String? = null,
    val designation: String? = null,
    val region: String? = null,
    val full_text: String? = null
)

// ── TYPE 6: SEED TEST REPORT ────────────────────────────────────────────
@Serializable
data class SeedTestReportResult(
    override val document_type: String = "seed_test_report",
    val b_report_no: String? = null,
    val report_date: String? = null,
    val date_received_for_test: String? = null,
    val request_form_no: String? = null,
    val date_finished_test: String? = null,
    val farmer_registration_no: String? = null,
    val seed_act_registration_no: String? = null,
    val crop: String? = null,
    val variety: String? = null,
    val seed_class: String? = null,
    val date_of_harvest: String? = null,
    val seed_producer_name: String? = null,
    val date_sampled: String? = null,
    val lot_no: String? = null,
    val quantity_of_seed_lot: String? = null,
    val recommendation: String? = null,
    val officer_in_charge: OfficerInCharge? = null,
    val test_results: SeedTestResults? = null
) : BaseFormResult

@Serializable
data class OfficerInCharge(
    val name: String? = null,
    val designation: String? = null,
    val office: String? = null,
    val full_text: String? = null
)

@Serializable
data class SeedTestResults(
    val other_distinguish_varieties: String? = null,
    val just_discernible_seed: String? = null,
    val other_crop_seed: String? = null,
    val light_and_mechanical_damaged_seed: String? = null,
    val appearance: String? = null,
    val smell: String? = null,
    val pure_seed: String? = null,
    val inert_matter: String? = null,
    val weed_seed: String? = null,
    val other_crop_seed_percent: String? = null,
    val normal_seedlings: String? = null,
    val abnormal_seedlings: String? = null,
    val hard_seed: String? = null,
    val fresh_ungerminated_seed: String? = null,
    val dead_seed: String? = null,
    val viability: String? = null,
    val moisture: String? = null
)

// ── TYPE 7: LABELING AND SEALING DOCUMENT ───────────────────────────────
@Serializable
data class LabelingResult(
    override val document_type: String = "labeling_document_form",
    val date_of_sampling: String? = null,
    val date_of_labeling: String? = null,
    val no_of_labels_issued: String? = null,
    val no_of_bags: String? = null,
    val lot_no_used: String? = null,
    val b_report_no: String? = null,
    val region: String? = null,
    val registration_no: String? = null,
    val crop: String? = null,
    val variety: String? = null,
    val seed_class: String? = null,
    val name_of_grower: String? = null
) : BaseFormResult

@Serializable
data class OfficerDetails(
    val name: String? = null,
    val designation: String? = null,
    val organization: String? = null,
    val department: String? = null,
    val office: String? = null,
    val full_text: String? = null
)
