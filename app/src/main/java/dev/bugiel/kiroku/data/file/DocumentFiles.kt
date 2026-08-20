package dev.bugiel.kiroku.data.file

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream

data class DocumentMetadata(
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long?,
)

fun ContentResolver.documentMetadata(uri: Uri): DocumentMetadata {
    var displayName: String? = null
    var sizeBytes: Long? = null
    query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0 && !cursor.isNull(nameIndex)) displayName = cursor.getString(nameIndex)
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) sizeBytes = cursor.getLong(sizeIndex)
        }
    }
    val fallbackName = uri.lastPathSegment?.substringAfterLast('/').orEmpty().ifBlank { "Dokument" }
    return DocumentMetadata(
        displayName = displayName?.takeIf { it.isNotBlank() } ?: fallbackName,
        mimeType = getType(uri).orEmpty(),
        sizeBytes = sizeBytes,
    )
}

fun ContentResolver.readBytes(uri: Uri, maximumBytes: Int): ByteArray {
    openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream(minOf(maximumBytes, DEFAULT_BUFFER_SIZE))
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= maximumBytes) { "The file is too large." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
    error("The file could not be opened.")
}
