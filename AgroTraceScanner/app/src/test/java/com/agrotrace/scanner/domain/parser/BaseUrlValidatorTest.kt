package com.agrotrace.scanner.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseUrlValidatorTest {

    @Test
    fun removesTrailingSlash() {
        val result = BaseUrlValidator.normalize("http://10.0.2.2:8080/")
        assertEquals("http://10.0.2.2:8080", result.getOrThrow())
    }

    @Test
    fun rejectsUrlWithoutHttpScheme() {
        assertTrue(BaseUrlValidator.normalize("10.0.2.2:8080").isFailure)
    }
}
