package dev.bugiel.kiroku.domain.model

data class NoteAttachment(
    val id: Long = 0,
    val noteId: Long,
    val displayName: String,
    val mimeType: String,
    val storedFileName: String,
    val sizeBytes: Long,
    val createdAt: Long,
) {
    val isImage: Boolean
        get() = mimeType.startsWith("image/")

    val isPdf: Boolean
        get() = mimeType == "application/pdf"

    val isMarkdown: Boolean
        get() = displayName.endsWith(".md", ignoreCase = true) || mimeType == "text/markdown"
}
