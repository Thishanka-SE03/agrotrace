package com.agrotrace.scanner.domain.model

import android.net.Uri

data class CapturedDocument(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long?
)
