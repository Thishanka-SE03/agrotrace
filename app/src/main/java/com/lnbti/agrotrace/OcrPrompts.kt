package com.lnbti.agrotrace

/**
 * Exact Gemini extraction contracts for the seven AgroTrace seed-certification forms.
 *
 * Keep the JSON keys stable because the review screen, history screen and future
 * Supabase table mapping all depend on these contracts.
 */
object OcrPrompts {

    fun forType(type: Int): String = when (type) {
        1 -> type1LandApproval()
        2 -> type2CropRegistration()
        3 -> type3Inspection()
        4 -> type4FinalFieldInspection()
        5 -> type5SeedTestRequest()
        6 -> type6SeedTestReport()
        7 -> type7LabelingAndSealing()
        else -> error("Unsupported document type: $type")
    }

fun type1LandApproval(): String = """
        You are an expert document understanding AI.

        Extract structured data from this Land Approval Form.

        Return ONLY valid JSON.
        No markdown, no notes, no explanations.

        The document contains shared form information and a table with one or more records.
        Extract every visible table row in order.

        {
          "document_type": "land_approval_form",
          "shared": {
            "lot_no_for_seeds": "Value next to බීජ තොග අංකය",
            "land_address": "Value next to ගොවිපල පිහිටුවා ඇති ස්ථානය",
            "transplanted_date": "Value next to වගා කළ දිනය",
            "form_date": "Value next to දිනය. This is the form date, not the transplanted date",
            "see_act_registration_no": "Value next to බීජ පනතේ ලියාපදිංචි අංකය",
            "farmer_name": "First handwritten row in the dotted section at the top-right",
            "address": "All remaining handwritten rows in the same top-right dotted section, joined with commas"
          },
          "records": [
            {
              "contact_no": "Value under දුරකථන අංකය for this table row",
              "crop_id": "Value under භෝගය for this table row",
              "variety": "Value under ප්‍රභේදය for this table row",
              "land_area": "Value under වපසරිය for this table row",
              "quantity_of_seeds_used": "Value under භාවිතා කල බීජ ප්‍රමාණය for this table row"
            }
          ]
        }

        RULES

        • Read printed and handwritten text carefully.
        • Extract every non-empty table row.
        • Keep values from the same row together in one object.
        • Keep table records in the same order as the document.
        • The first line in the top-right dotted section is farmer_name.
        • The remaining lines in that section are address.
        • Preserve Sinhala and English values exactly as written.
        • Ignore stamps, signatures, page numbers, logos and unrelated notes.
        • Do not guess.
        • If a shared value is missing or unreadable, return null.
        • If a table-cell value is missing or unreadable, return null.
        • If no table records are readable, return an empty records array.
        • Keep all JSON keys exactly as provided.
    """.trimIndent()

    fun type2CropRegistration(): String = """
        You are an expert document understanding AI.

        Extract structured data from this Crop Registration for Seed Certification Form.

        Return ONLY valid JSON.
        No markdown, no notes, no explanations.

        {
          "document_type": "crop_registration_form",
          "registration_no": "Value next to Registration No at the upper-left area",
          "form_no": "Printed value next to No. at the top-right corner",
          "seed_act_registration_no": "Value next to Seed Act Registration No",
          "farmer_registration_no": "Farmer registration number written in the relevant ලියාපදිංචි අංකය or registration field. Do not copy the Seed Act Registration No",
          "date": "Value next to දිනය or Date near the top section",
          "name_of_seed_producer": "First handwritten row in the dotted Name and Address of Seed Producer section",
          "address_of_seed_producer": "All remaining handwritten rows in that dotted section, joined with commas",
          "field_records": [
            {
              "field_no": "Value under Field No/Name for this row",
              "crop_grown_in_last_two_seasons": "Value under Crops grown during last two seasons for this row",
              "harvest_date": "Value under Approx. date of harvest for this row"
            }
          ],
          "payment_no": "Value under No. in the Payments section",
          "payment_amount": "Value under Amount in the Payments section",
          "registration_officer": {
            "name": "Officer name from the seal near Name, Designation & Signature of Officer",
            "designation": "Officer designation from the same seal",
            "organization": "Organization or service from the seal",
            "department": "Department from the seal",
            "office": "Office, branch or location from the seal",
            "full_text": "All readable text from the seal, excluding the signature mark"
          }
        }

        RULES

        • Read printed and handwritten text carefully.
        • The first line in the producer dotted section is the producer name.
        • The remaining lines are the producer address.
        • Extract every non-empty main-table row into field_records.
        • Keep values from the same table row together.
        • Keep field_records in document order.
        • Read the payment number only from the Payments section.
        • Read the payment amount only from the Amount column.
        • Extract all readable officer-seal details.
        • Preserve Sinhala and English exactly as written.
        • Ignore unrelated notes, signatures, page numbers and logos.
        • Do not guess.
        • Return null for unreadable scalar or object fields.
        • Use an empty array if no table rows are readable.
        • Keep all JSON keys exactly as provided.
    """.trimIndent()

    fun type3Inspection(): String = """
        You are an expert document understanding AI.

        Extract structured data from this Field/Lot Inspection Report.

        Return ONLY valid JSON.
        No markdown, no notes, no explanations.

        {
          "document_type": "inspection_form",
          "inspection_no": "Value next to No. at the top-right corner",
          "inspection_date": "Value next to Date",
          "seed_act_registration_no": "Value next to Seed Act Registration No",
          "farmer_registration_no": "Value next to Registration No. Do not use the Seed Act Registration No",
          "field_no": "Value next to Field No/Name",
          "observation": "All handwritten text under Observations / Instructions, joined into one readable string with spaces",
          "inspection_round": "Determine which oval option is marked: 1st Inspection, 2nd Inspection, 3rd Inspection, 1st Re-inspection, 2nd Re-inspection or 3rd Re-inspection. Return a string, or an array if multiple are marked"
        }

        RULES

        • Read printed and handwritten text carefully.
        • Inspect the six oval options visually.
        • A selected oval may be crossed, ticked, circled, darkened or otherwise marked.
        • Return only the selected inspection-round label or labels.
        • Join multiline observations into one readable sentence.
        • Preserve Sinhala and English exactly as written.
        • Ignore signatures, page numbers, logos and unrelated handwritten notes.
        • Do not guess.
        • If a value is missing or unreadable, return null.
        • If no oval can be confidently identified, inspection_round must be null.
        • Keep all JSON keys exactly as provided.
    """.trimIndent()

    fun type4FinalFieldInspection(): String = """
        You are an expert document understanding AI.

        Extract structured data from this Final Field Inspection Report.

        Return ONLY valid JSON.
        No markdown, no notes, no explanations.

        {
          "document_type": "final_field_inspection_report",
          "harvest_inspect_no": "Value next to No. at the top-right corner",
          "farmer_registration_no": "Value next to Registration No",
          "final_inspection_date": "Value next to Date",
          "extent_accepted": "Value next to Extent Accepted (Ac.). Include the written unit",
          "extent_rejected": "Value next to Extent Rejected (Ac.). Include the written unit",
          "estimated_seed_yield": "Value next to Estimated Seed Yield (Bu/Kg). Preserve the written unit",
          "observation_records": [
            {
              "other_distinguish_varieties": "Value under Other Distinguish Varieties for this row",
              "pest_and_diseases": "Value under Pest & Diseases for this row",
              "remarks": "All text under Remarks for this row, joined with spaces",
              "decision": "Value under Decision (Accepted or Rejected) for this row"
            }
          ],
          "officer_sign": {
            "name": "Officer name from the seal near Name, Designation & Signature of Inspecting Officer",
            "designation": "Officer designation from the seal",
            "organization": "Organization or service from the seal",
            "department": "Department from the seal",
            "office": "Office or location from the seal",
            "full_text": "All readable seal text, excluding the signature mark"
          }
        }

        RULES

        • Read printed and handwritten text carefully.
        • Extract every non-empty observation-table row.
        • Keep values from the same row together.
        • Keep observation_records in document order.
        • Return decision as the written value, such as Accepted or Rejected.
        • Join multiline remarks into one readable string.
        • Extract all readable officer-seal details.
        • Preserve Sinhala and English exactly as written.
        • Ignore unrelated notes, signatures, page numbers and logos.
        • Do not guess.
        • Return null for unreadable scalar or object fields.
        • Use an empty array if no observation rows are readable.
        • Keep all JSON keys exactly as provided.
    """.trimIndent()

    fun type5SeedTestRequest(): String = """
        You are an expert document understanding AI.

        Extract structured data from this Seed Test Request Form - B/C.

        Return ONLY valid JSON.
        No markdown, no notes, no explanations.

        {
          "document_type": "seed_test_request_form",
          "request_id": "Value next to No. near the top-right corner",
          "lab_id": "From Peradeniya, M.I., Alutharama, Bata-ata and Paranthan, return only the laboratory name that is not crossed out",
          "process_at": "Value next to Processed at",
          "species": "Value next to Species",
          "name_of_contractor": "Value next to Name of Contractor",
          "lot_no": "Value next to Lot No",
          "date_sampled": "Value next to Date Sampled",
          "date_labeled_and_sealed": "Value next to Date Labeled & Sealed",
          "no_of_labels_issued": "Complete value next to Label No.s & No. of Labels",
          "remarks": "All text under Remarks, joined into one readable string with spaces",
          "region": "Value next to Region",
          "farmer_registration_no": "Value next to Registration No",
          "test_requested_date": "Value next to Date near the top of the form",
          "processing": "Determine whether Machine or Manual is selected in the Processing field",
          "sampling_officer": {
            "name": "Officer name from Name, Designation & Signature of Sampling Officer",
            "designation": "Officer designation from the same section",
            "region": "Officer region, office or location if written",
            "full_text": "All readable text from the sampling-officer section, excluding the signature mark"
          }
        }

        RULES

        • Read printed and handwritten text carefully.
        • For lab_id, visually inspect which laboratory name is not crossed out.
        • Return only one uncrossed laboratory name.
        • For processing, determine whether Machine or Manual is selected.
        • Selection may be shown by crossing, circling, ticking, underlining or handwriting.
        • Join multiline remarks into one readable sentence.
        • Preserve Sinhala and English exactly as written.
        • Ignore unrelated annotations, page numbers and logos.
        • Ignore signature shapes but read nearby officer text.
        • Do not guess.
        • If a value is missing or unreadable, return null.
        • Keep all JSON keys exactly as provided.
    """.trimIndent()

    fun type6SeedTestReport(): String = """
        You are an expert document understanding AI.

        Extract structured data from this Seed Test Report.

        Return ONLY valid JSON.
        No markdown, no notes, no explanations.

        {
          "document_type": "seed_test_report",
          "b_report_no": "Value next to Report No, including the report prefix and number",
          "report_date": "Value next to Date below the Report No",
          "date_received_for_test": "Value next to Date Received",
          "request_form_no": "Value next to Request Form No",
          "date_finished_test": "Value next to Date Concluded",
          "farmer_registration_no": "Value next to Registration No",
          "seed_act_registration_no": "Value next to Seed Act Registration No",
          "crop": "Value next to Crop",
          "variety": "Value next to Variety or ප්‍රභේදය",
          "seed_class": "Value next to Seed Class",
          "date_of_harvest": "Value next to Date of Harvest",
          "seed_producer_name": "Value next to Seed Producer",
          "date_sampled": "Value next to Date Sampled",
          "lot_no": "Value next to Lot No",
          "quantity_of_seed_lot": "Value next to Quantity of Seed Lot, including units and bag count",
          "recommendation": "Value inside Recommendation, such as ACCEPTED or REJECTED",
          "officer_in_charge": {
            "name": "Officer name from the Officer In Charge section",
            "designation": "Officer designation",
            "office": "Laboratory, office or location",
            "full_text": "All readable text from the Officer In Charge stamp or section, excluding the signature mark"
          },
          "test_results": {
            "other_distinguish_varieties": "Value in column 01",
            "just_discernible_seed": "Value in column 02",
            "other_crop_seed": "Value in column 03",
            "light_and_mechanical_damaged_seed": "Value in column 04",
            "appearance": "Value in column 05",
            "smell": "Value in column 06",
            "pure_seed": "Value in column 07",
            "inert_matter": "Value in column 08",
            "weed_seed": "Value in column 09",
            "other_crop_seed_percent": "Value in column 10",
            "normal_seedlings": "Value in column 11",
            "abnormal_seedlings": "Value in column 12",
            "hard_seed": "Value in column 13",
            "fresh_ungerminated_seed": "Value in column 14",
            "dead_seed": "Value in column 15",
            "viability": "Value in column 16",
            "moisture": "Value in column 17"
          }
        }

        RULES

        • Read printed and handwritten text carefully.
        • Read all seventeen result-table columns.
        • Match each result with the correct numbered column.
        • Do not shift values into neighboring columns.
        • Return null for blank or unreadable table cells.
        • Preserve symbols, percentages, decimal points and grading letters such as G.
        • Recommendation should contain only the readable recommendation value.
        • Extract all readable Officer In Charge details.
        • Preserve Sinhala and English exactly as written.
        • Ignore unrelated notes, page numbers and logos.
        • Do not guess.
        • Keep all JSON keys exactly as provided.
    """.trimIndent()

    fun type7LabelingAndSealing(): String = """
        You are an expert document understanding AI.

        Extract structured data from this Labeling and Sealing Document.

        Return ONLY valid JSON.
        No markdown, no notes, no explanations.

        {
          "document_type": "labeling_document_form",
          "date_of_sampling": "Value next to Date of Sampling",
          "date_of_labeling": "Value next to Date of Labelling or Date of Labeling",
          "no_of_labels_issued": "Value under No. of Labels in the first Labels Issued table",
          "no_of_bags": "Value under No of Bags in the same first-table row",
          "lot_no_used": "Value next to Lot No",
          "b_report_no": "Value next to B Report No, including prefix and number",
          "region": "Value next to Region",
          "registration_no": "Value next to Registration No",
          "crop": "Value next to Crop",
          "variety": "Value next to Variety",
          "seed_class": "Value next to Seed Class",
          "name_of_grower": "Grower's handwritten name from the Signature of Grower or Name area. Do not return the signature mark itself"
        }

        RULES

        • Read printed and handwritten text carefully.
        • Use the No. of Labels value from the first Labels Issued table.
        • Use the No of Bags value from the same row.
        • Do not confuse From and To label-range values with no_of_labels_issued.
        • Use the Total in the second table only when the first-table No. of Labels value is unreadable.
        • Extract name_of_grower from the bottom-left grower section.
        • If the grower's name is written on the Signature line instead of the Name line, still return it.
        • Do not return the officer's name from the bottom-right.
        • Ignore Document No, Final Report No, STL Label No, PC Label No and No of Seal Used.
        • Preserve Sinhala and English exactly as written.
        • Ignore unrelated notes, page numbers and logos.
        • Do not guess.
        • If a value is missing or unreadable, return null.
        • Keep all JSON keys exactly as provided.
    """.trimIndent()
}
