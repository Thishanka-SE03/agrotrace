package com.agrotrace.scanner.domain.model

data class PairingPayload(
    val scanId: String,
    val pairingCode: String,
    val claimToken: String
) {
    val shortScanId: String
        get() = scanId.take(8).uppercase()
}
