package dev.bugiel.kiroku.data.local

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase
import dev.bugiel.kiroku.data.local.dao.HabitDao
import dev.bugiel.kiroku.data.local.dao.NoteDao
import dev.bugiel.kiroku.data.local.dao.NoteAttachmentDao
import dev.bugiel.kiroku.data.local.entity.HabitCompletionEntity
import dev.bugiel.kiroku.data.local.entity.HabitEntity
import dev.bugiel.kiroku.data.local.entity.NoteEntity
import dev.bugiel.kiroku.data.local.entity.NoteAttachmentEntity

@Database(
    entities = [NoteEntity::class, NoteAttachmentEntity::class, HabitEntity::class, HabitCompletionEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class KirokuDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun noteAttachmentDao(): NoteAttachmentDao
    abstract fun habitDao(): HabitDao
}
