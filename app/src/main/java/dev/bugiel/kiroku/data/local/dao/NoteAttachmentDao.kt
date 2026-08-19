package dev.bugiel.kiroku.data.local.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import dev.bugiel.kiroku.data.local.entity.NoteAttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteAttachmentDao {
    @Query("SELECT * FROM note_attachments WHERE noteId = :noteId ORDER BY createdAt, id")
    fun observeForNote(noteId: Long): Flow<List<NoteAttachmentEntity>>

    @Query("SELECT * FROM note_attachments WHERE id = :id")
    suspend fun getById(id: Long): NoteAttachmentEntity?

    @Query("SELECT * FROM note_attachments WHERE noteId = :noteId")
    suspend fun getForNote(noteId: Long): List<NoteAttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(attachment: NoteAttachmentEntity): Long

    @Delete
    suspend fun delete(attachment: NoteAttachmentEntity)

    @Query("DELETE FROM note_attachments WHERE noteId = :noteId")
    suspend fun deleteForNote(noteId: Long)
}
