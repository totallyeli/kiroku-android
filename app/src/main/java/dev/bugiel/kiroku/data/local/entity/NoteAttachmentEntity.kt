package dev.bugiel.kiroku.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import dev.bugiel.kiroku.domain.model.NoteAttachment

@Entity(
    tableName = "note_attachments",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("noteId")],
)
data class NoteAttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Long,
    val displayName: String,
    val mimeType: String,
    val storedFileName: String,
    val sizeBytes: Long,
    val createdAt: Long,
)

fun NoteAttachmentEntity.toDomain() = NoteAttachment(
    id = id,
    noteId = noteId,
    displayName = displayName,
    mimeType = mimeType,
    storedFileName = storedFileName,
    sizeBytes = sizeBytes,
    createdAt = createdAt,
)

fun NoteAttachment.toEntity() = NoteAttachmentEntity(
    id = id,
    noteId = noteId,
    displayName = displayName,
    mimeType = mimeType,
    storedFileName = storedFileName,
    sizeBytes = sizeBytes,
    createdAt = createdAt,
)
