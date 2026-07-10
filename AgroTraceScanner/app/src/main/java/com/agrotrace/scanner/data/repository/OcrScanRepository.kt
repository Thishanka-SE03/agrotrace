package com.agrotrace.scanner.data.repository

import com.agrotrace.scanner.domain.model.CapturedDocument
import com.agrotrace.scanner.domain.model.PairingPayload

interface OcrScanRepository {
    suspend fun claim(pairing: PairingPayload): AppResult<Unit>

    suspend fun upload(
        pairing: PairingPayload,
        document: CapturedDocument,
        onProgress: (Float) -> Unit
    ): AppResult<Unit>
}
