package com.agrotrace.scanner.domain.parser

import java.net.URI

object BaseUrlValidator {
    fun normalize(rawValue: String): Result<String> = runCatching {
        val trimmed = rawValue.trim().trimEnd('/')
        require(trimmed.isNotBlank()) { "Enter the backend base URL." }

        val uri = URI(trimmed)
        require(uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) {
            "The backend URL must start with http:// or https://."
        }
        require(!uri.host.isNullOrBlank()) { "The backend URL must contain a valid host." }
        require(uri.rawQuery == null && uri.rawFragment == null) {
            "Use only the server base URL, without a query or fragment."
        }

        trimmed
    }
}
