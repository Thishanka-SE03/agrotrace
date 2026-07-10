package com.agrotrace.scanner.data.remote

import com.google.gson.JsonParser
import okhttp3.ResponseBody

object ApiErrorParser {
    fun message(statusCode: Int, errorBody: ResponseBody?): String {
        val raw = runCatching { errorBody?.string().orEmpty().trim() }.getOrDefault("")
        if (raw.isNotBlank()) {
            val jsonMessage = runCatching {
                val objectValue = JsonParser.parseString(raw).asJsonObject
                listOf("message", "error", "detail", "title")
                    .firstNotNullOfOrNull { key ->
                        objectValue.get(key)?.takeUnless { it.isJsonNull }?.asString
                    }
            }.getOrNull()

            if (!jsonMessage.isNullOrBlank()) return jsonMessage
            if (raw.length <= 240 && !raw.startsWith("<")) return raw
        }

        return when (statusCode) {
            400 -> "The server rejected the pairing or image data."
            401, 403 -> "The claim token is invalid or no longer authorized."
            404 -> "The scan request was not found. It may have expired."
            409 -> "This scan request has already been claimed or uploaded."
            413 -> "The scanned image is larger than the server allows."
            415 -> "The server does not support this image format."
            422 -> "The scan request could not be validated."
            in 500..599 -> "The OCR server encountered an error. Please try again."
            else -> "The server returned HTTP $statusCode."
        }
    }
}
