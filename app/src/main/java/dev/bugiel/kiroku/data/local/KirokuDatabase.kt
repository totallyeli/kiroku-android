package dev.bugiel.kiroku.data.local

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase
import dev.bugiel.kiroku.data.local.dao.HabitDao
import dev.bugiel.kiroku.data.local.dao.NoteDao
import dev.bugiel.kiroku.data.local.entity.HabitCompletionEntity
import dev.bugiel.kiroku.data.local.entity.HabitEntity
import dev.bugiel.kiroku.data.local.entity.NoteEntity

@Database(
    entities = [NoteEntity::class, HabitEntity::class, HabitCompletionEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class KirokuDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun habitDao(): HabitDao
}
