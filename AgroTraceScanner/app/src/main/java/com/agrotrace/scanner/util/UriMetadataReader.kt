package com.agrotrace.scanner.util

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.agrotrace.scanner.domain.model.CapturedDocument

object UriMetadataReader {
    fun read(contentResolver: ContentResolver, uri: Uri): CapturedDocument {
        var name = "agrotrace-scan.jpg"
        var size: Long? = null

        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }

        return CapturedDocument(uri = uri, displayName = name, sizeBytes = size)
    }
}
