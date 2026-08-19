package dev.bugiel.kiroku.data.repository

import android.content.Context
import android.net.Uri
import dev.bugiel.kiroku.data.file.documentMetadata
import dev.bugiel.kiroku.data.file.readBytes
import dev.bugiel.kiroku.data.local.dao.NoteDao
import dev.bugiel.kiroku.data.local.entity.toDomain
import dev.bugiel.kiroku.data.local.entity.toEntity
import dev.bugiel.kiroku.domain.NoteFilter
import dev.bugiel.kiroku.domain.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface NoteRepository {
    fun observeNotes(query: String): Flow<List<Note>>
    suspend fun getNote(id: Long): Note?
    suspend fun save(note: Note): Long
    suspend fun delete(note: Note)
    suspend fun setPinned(id: Long, isPinned: Boolean, updatedAt: Long)
    suspend fun importTextDocuments(uris: List<Uri>, timestamp: Long): NoteImportResult
}

data class NoteImportResult(val imported: Int, val failed: Int)

class OfflineNoteRepository(
    private val dao: NoteDao,
    context: Context,
) : NoteRepository {
    private val contentResolver = context.applicationContext.contentResolver
    override fun observeNotes(query: String): Flow<List<Note>> = dao.observeAll().map { entities ->
        NoteFilter.filterAndSort(entities.map { it.toDomain() }, query)
    }

    override suspend fun getNote(id: Long): Note? = dao.getById(id)?.toDomain()

    override suspend fun save(note: Note): Long {
        return if (note.id == 0L) {
            dao.insert(note.toEntity())
        } else {
            dao.update(note.toEntity())
            note.id
        }
    }

    override suspend fun delete(note: Note) = dao.delete(note.toEntity())

    override suspend fun setPinned(id: Long, isPinned: Boolean, updatedAt: Long) {
        dao.setPinned(id, isPinned, updatedAt)
    }

    override suspend fun importTextDocuments(uris: List<Uri>, timestamp: Long): NoteImportResult =
        withContext(Dispatchers.IO) {
            var imported = 0
            var failed = 0
            uris.forEachIndexed { index, uri ->
                runCatching {
                    val metadata = contentResolver.documentMetadata(uri)
                    require(
                        metadata.displayName.endsWith(".txt", ignoreCase = true) ||
                            metadata.displayName.endsWith(".md", ignoreCase = true),
                    )
                    val bytes = contentResolver.readBytes(uri, MAX_IMPORT_BYTES)
                    val content = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
                    val title = metadata.displayName.substringBeforeLast('.').ifBlank { metadata.displayName }
                    val createdAt = timestamp + index
                    dao.insert(
                        Note(
                            title = title,
                            content = content,
                            createdAt = createdAt,
                            updatedAt = createdAt,
                        ).toEntity(),
                    )
                }.onSuccess { imported++ }.onFailure { failed++ }
            }
            NoteImportResult(imported = imported, failed = failed)
        }

    companion object {
        private const val MAX_IMPORT_BYTES = 5 * 1024 * 1024
    }
}
