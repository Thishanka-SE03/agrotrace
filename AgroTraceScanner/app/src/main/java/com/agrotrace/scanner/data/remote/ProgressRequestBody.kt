package com.agrotrace.scanner.data.remote

import android.content.ContentResolver
import android.net.Uri
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink

class ProgressRequestBody(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
    private val mediaType: MediaType?,
    private val onProgress: (uploaded: Long, total: Long) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = mediaType

    override fun contentLength(): Long =
        contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            descriptor.length
        } ?: -1L

    override fun writeTo(sink: BufferedSink) {
        val total = contentLength()
        var uploaded = 0L

        val input = contentResolver.openInputStream(uri)
            ?: error("The captured image can no longer be opened.")

        input.use { stream ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = stream.read(buffer)
                if (read == -1) break
                sink.write(buffer, 0, read)
                uploaded += read
                if (total > 0) onProgress(uploaded, total)
            }
        }
    }
}
