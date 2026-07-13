package com.lnbti.agrotrace

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

object DocumentTypeUtils {
    fun name(type: Int): String = when (type) {
        1 -> "Land Approval Form"
        2 -> "Crop Registration Form"
        3 -> "Inspection Form"
        4 -> "Final Field Inspection Form"
        5 -> "Seed Test Request Form"
        6 -> "Seed Test Report"
        7 -> "Labeling Document"
        else -> "Agricultural Document"
    }

    fun description(type: Int): String = when (type) {
        1 -> "Approve land and seed-production details"
        2 -> "Register crops, fields, and producer details"
        3 -> "Record field inspection observations"
        4 -> "Capture final inspection decisions and yield"
        5 -> "Prepare a seed-testing laboratory request"
        6 -> "Store germination, purity, and moisture results"
        7 -> "Record certification label information"
        else -> "Scan and extract agricultural document data"
    }

    @ColorRes
    fun color(type: Int): Int = when (type) {
        1 -> R.color.doc_type_1
        2 -> R.color.doc_type_2
        3 -> R.color.doc_type_3
        4 -> R.color.doc_type_4
        5 -> R.color.doc_type_5
        6 -> R.color.doc_type_6
        7 -> R.color.doc_type_7
        else -> R.color.primary
    }

    @DrawableRes
    fun icon(type: Int): Int = when (type) {
        1 -> R.drawable.ic_description
        2 -> R.drawable.ic_crop
        3 -> R.drawable.ic_check_circle
        4 -> R.drawable.ic_description
        5 -> R.drawable.ic_description
        6 -> R.drawable.ic_description
        7 -> R.drawable.ic_description
        else -> R.drawable.ic_description
    }

    fun category(type: Int): String = when (type) {
        1 -> "land"
        2 -> "registration"
        3, 4 -> "inspection"
        5, 6, 7 -> "seed"
        else -> "other"
    }
}
