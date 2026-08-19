package dev.bugiel.kiroku.data.repository

import dev.bugiel.kiroku.data.local.dao.NoteDao
import dev.bugiel.kiroku.data.local.entity.toDomain
import dev.bugiel.kiroku.data.local.entity.toEntity
import dev.bugiel.kiroku.domain.NoteFilter
import dev.bugiel.kiroku.domain.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface NoteRepository {
    fun observeNotes(query: String): Flow<List<Note>>
    suspend fun getNote(id: Long): Note?
    suspend fun save(note: Note): Long
    suspend fun delete(note: Note)
    suspend fun setPinned(id: Long, isPinned: Boolean, updatedAt: Long)
}

class OfflineNoteRepository(
    private val dao: NoteDao,
) : NoteRepository {
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
}

