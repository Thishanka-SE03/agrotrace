package com.agrotrace.scanner.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingPayloadParserTest {

    @Test
    fun parsesValidAgroTraceClaimPayload() {
        val raw = "agrotrace://ocr/claim?scanId=123e4567-e89b-12d3-a456-426614174000&code=123456&token=example_claim_token_123456789"

        val result = PairingPayloadParser.parse(raw)

        assertTrue(result.isSuccess)
        val payload = result.getOrThrow()
        assertEquals("123e4567-e89b-12d3-a456-426614174000", payload.scanId)
        assertEquals("123456", payload.pairingCode)
        assertEquals("example_claim_token_123456789", payload.claimToken)
    }

    @Test
    fun rejectsNonAgroTracePayload() {
        val result = PairingPayloadParser.parse("https://example.com/claim")
        assertTrue(result.isFailure)
    }
}
