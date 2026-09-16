package dev.bugiel.kiroku.data.local

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase
import dev.bugiel.kiroku.data.local.dao.HabitDao
import dev.bugiel.kiroku.data.local.dao.NoteDao
import dev.bugiel.kiroku.data.local.dao.NoteAttachmentDao
import dev.bugiel.kiroku.data.local.dao.TodoDao
import dev.bugiel.kiroku.data.local.entity.HabitCompletionEntity
import dev.bugiel.kiroku.data.local.entity.HabitEntity
import dev.bugiel.kiroku.data.local.entity.NoteEntity
import dev.bugiel.kiroku.data.local.entity.NoteAttachmentEntity
import dev.bugiel.kiroku.data.local.entity.TodoEntity
import dev.bugiel.kiroku.data.local.dao.WorkoutDao
import dev.bugiel.kiroku.data.local.entity.WorkoutPlanEntity
import dev.bugiel.kiroku.data.local.entity.WorkoutExerciseEntity
import dev.bugiel.kiroku.data.local.entity.WorkoutSessionEntity
import dev.bugiel.kiroku.data.local.entity.WorkoutSessionExerciseEntity
import dev.bugiel.kiroku.data.local.entity.WorkoutSetEntity

@Database(
    entities = [
        NoteEntity::class,
        NoteAttachmentEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        TodoEntity::class,
        WorkoutPlanEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSessionExerciseEntity::class,
        WorkoutSetEntity::class,
    ],
    version = 6,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
    ],
)
abstract class KirokuDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun noteAttachmentDao(): NoteAttachmentDao
    abstract fun habitDao(): HabitDao
    abstract fun todoDao(): TodoDao
    abstract fun workoutDao(): WorkoutDao
}
