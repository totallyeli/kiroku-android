package dev.bugiel.kiroku.data.repository

import android.content.Context
import android.net.Uri
import dev.bugiel.kiroku.data.file.documentMetadata
import dev.bugiel.kiroku.data.local.dao.NoteAttachmentDao
import dev.bugiel.kiroku.data.local.entity.NoteAttachmentEntity
import dev.bugiel.kiroku.data.local.entity.toDomain
import dev.bugiel.kiroku.domain.model.NoteAttachment
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface AttachmentRepository {
    fun observeForNote(noteId: Long): Flow<List<NoteAttachment>>
    suspend fun add(noteId: Long, uri: Uri, createdAt: Long): NoteAttachment
    suspend fun delete(attachment: NoteAttachment)
    suspend fun deleteStoredFiles(attachments: List<NoteAttachment>)
    suspend fun export(attachment: NoteAttachment, destination: Uri)
    fun fileFor(attachment: NoteAttachment): File
}

class OfflineAttachmentRepository(
    context: Context,
    private val dao: NoteAttachmentDao,
) : AttachmentRepository {
    private val applicationContext = context.applicationContext
    private val attachmentDirectory = File(applicationContext.filesDir, "note_attachments")

    override fun observeForNote(noteId: Long): Flow<List<NoteAttachment>> =
        dao.observeForNote(noteId).map { attachments -> attachments.map { it.toDomain() } }

    override suspend fun add(noteId: Long, uri: Uri, createdAt: Long): NoteAttachment = withContext(Dispatchers.IO) {
        val resolver = applicationContext.contentResolver
        val metadata = resolver.documentMetadata(uri)
        val mimeType = normalizedMimeType(metadata.displayName, metadata.mimeType)
        require(isSupported(metadata.displayName, mimeType)) { "This file type is not supported." }
        metadata.sizeBytes?.let { require(it <= MAX_ATTACHMENT_BYTES) { "The file is too large." } }

        attachmentDirectory.mkdirs()
        val extension = metadata.displayName.substringAfterLast('.', "")
            .filter(Char::isLetterOrDigit)
            .lowercase()
            .take(10)
        val storedFileName = UUID.randomUUID().toString() + extension.takeIf { it.isNotEmpty() }?.let { ".$it" }.orEmpty()
        val target = File(attachmentDirectory, storedFileName)
        val temporary = File(attachmentDirectory, "$storedFileName.part")

        try {
            val copiedBytes = resolver.openInputStream(uri)?.use { input ->
                temporary.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_ATTACHMENT_BYTES) { "The file is too large." }
                        output.write(buffer, 0, count)
                    }
                    total
                }
            } ?: error("The file could not be opened.")
            check(temporary.renameTo(target)) { "The file could not be saved." }

            val entity = NoteAttachmentEntity(
                noteId = noteId,
                displayName = metadata.displayName.take(MAX_DISPLAY_NAME_LENGTH),
                mimeType = mimeType,
                storedFileName = storedFileName,
                sizeBytes = copiedBytes,
                createdAt = createdAt,
            )
            val id = dao.insert(entity)
            entity.copy(id = id).toDomain()
        } catch (error: Throwable) {
            temporary.delete()
            target.delete()
            throw error
        }
    }

    override suspend fun delete(attachment: NoteAttachment) = withContext(Dispatchers.IO) {
        val entity = dao.getById(attachment.id) ?: return@withContext
        dao.delete(entity)
        fileFor(entity.toDomain()).delete()
    }

    override suspend fun deleteStoredFiles(attachments: List<NoteAttachment>) = withContext(Dispatchers.IO) {
        attachments.forEach { fileFor(it).delete() }
    }

    override suspend fun export(attachment: NoteAttachment, destination: Uri) = withContext(Dispatchers.IO) {
        val source = fileFor(attachment)
        check(source.isFile) { "The attachment was not found." }
        applicationContext.contentResolver.openOutputStream(destination, "w")?.use { output ->
            source.inputStream().buffered().use { input -> input.copyTo(output) }
        } ?: error("The destination could not be opened.")
        Unit
    }

    override fun fileFor(attachment: NoteAttachment): File = File(attachmentDirectory, attachment.storedFileName)

    private fun normalizedMimeType(displayName: String, reportedMimeType: String): String = when {
        reportedMimeType.isNotBlank() -> reportedMimeType
        displayName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
        displayName.endsWith(".md", ignoreCase = true) -> "text/markdown"
        else -> "application/octet-stream"
    }

    private fun isSupported(displayName: String, mimeType: String): Boolean =
        mimeType.startsWith("image/") ||
            mimeType == "application/pdf" ||
            mimeType == "text/markdown" ||
            displayName.endsWith(".md", ignoreCase = true)

    companion object {
        private const val MAX_ATTACHMENT_BYTES = 50L * 1024 * 1024
        private const val MAX_DISPLAY_NAME_LENGTH = 180
    }
}
