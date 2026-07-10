package com.agrotrace.scanner.domain.parser

import com.agrotrace.scanner.domain.model.PairingPayload
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID

object PairingPayloadParser {

    fun parse(rawPayload: String): Result<PairingPayload> = runCatching {
        val raw = rawPayload.trim()
        require(raw.isNotEmpty()) { "The QR payload is empty." }

        val uri = URI(raw)
        require(uri.scheme.equals("agrotrace", ignoreCase = true)) {
            "This is not an AgroTrace QR code."
        }
        require(uri.host.equals("ocr", ignoreCase = true)) {
            "The AgroTrace QR code has an invalid destination."
        }
        require(uri.path == "/claim") {
            "The AgroTrace QR code is not a claim request."
        }

        val query = parseQuery(uri.rawQuery.orEmpty())
        val scanId = query["scanId"].orEmpty()
        val pairingCode = query["code"].orEmpty()
        val claimToken = query["token"].orEmpty()

        UUID.fromString(scanId)
        require(pairingCode.matches(Regex("\\d{6}"))) {
            "The pairing code must contain six digits."
        }
        require(claimToken.length >= 16) {
            "The claim token is missing or invalid."
        }

        PairingPayload(
            scanId = scanId,
            pairingCode = pairingCode,
            claimToken = claimToken
        )
    }

    private fun parseQuery(rawQuery: String): Map<String, String> =
        rawQuery.split('&')
            .filter { it.isNotBlank() }
            .mapNotNull { pair ->
                val separator = pair.indexOf('=')
                if (separator <= 0) return@mapNotNull null
                val key = decode(pair.substring(0, separator))
                val value = decode(pair.substring(separator + 1))
                key to value
            }
            .toMap()

    private fun decode(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}
