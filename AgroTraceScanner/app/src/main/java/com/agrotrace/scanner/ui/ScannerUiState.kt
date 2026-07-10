package com.agrotrace.scanner.ui

import com.agrotrace.scanner.domain.model.CapturedDocument
import com.agrotrace.scanner.domain.model.PairingPayload

enum class ScannerStep {
    HOME,
    PAIRING,
    CLAIMING,
    CAPTURE,
    PREVIEW,
    UPLOADING,
    SUCCESS,
    SETTINGS
}

data class ScannerUiState(
    val step: ScannerStep = ScannerStep.HOME,
    val baseUrl: String = "",
    val settingsUrlInput: String = "",
    val manualPayload: String = "",
    val pairing: PairingPayload? = null,
    val isClaimed: Boolean = false,
    val document: CapturedDocument? = null,
    val uploadProgress: Float = 0f,
    val errorMessage: String? = null
)
