package dev.bugiel.kiroku.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Makes exercises reusable without coalescing names or changing any existing identity.
 *
 * Room runs migrations in a transaction. Foreign-key enforcement stays enabled:
 * descendants are backed up and dropped before rebuilding the exercise parent,
 * then restored parent-first so CASCADE can never consume recorded history.
 */
val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE TEMP TABLE workout_migration_exercises AS SELECT * FROM workout_exercises")
        connection.execSQL("CREATE TEMP TABLE workout_migration_entries AS SELECT * FROM workout_session_exercises")
        connection.execSQL("CREATE TEMP TABLE workout_migration_sets AS SELECT * FROM workout_sets")
        // Preserve AUTOINCREMENT high-water marks too, including previously deleted IDs.
        connection.execSQL("""
            CREATE TEMP TABLE workout_migration_sequences AS
            SELECT name, seq FROM sqlite_sequence
            WHERE name IN ('workout_exercises', 'workout_session_exercises', 'workout_sets')
        """.trimIndent())

        connection.execSQL("DROP TABLE workout_sets")
        connection.execSQL("DROP TABLE workout_session_exercises")
        connection.execSQL("DROP TABLE workout_exercises")

        connection.execSQL("""
            CREATE TABLE workout_exercises (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                isArchived INTEGER NOT NULL
            )
        """.trimIndent())
        connection.execSQL("""
            INSERT INTO workout_exercises (id, name, isArchived)
            SELECT id, name, isArchived FROM workout_migration_exercises
        """.trimIndent())
        connection.execSQL("""
            CREATE TABLE workout_plan_exercises (
                planId INTEGER NOT NULL,
                exerciseId INTEGER NOT NULL,
                position INTEGER NOT NULL,
                PRIMARY KEY(planId, exerciseId),
                FOREIGN KEY(planId) REFERENCES workout_plans(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(exerciseId) REFERENCES workout_exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        connection.execSQL("CREATE INDEX index_workout_plan_exercises_exerciseId ON workout_plan_exercises (exerciseId)")
        connection.execSQL("""
            INSERT INTO workout_plan_exercises (planId, exerciseId, position)
            SELECT planId, id, position FROM workout_migration_exercises
        """.trimIndent())

        connection.execSQL("""
            CREATE TABLE workout_session_exercises (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sessionId INTEGER NOT NULL,
                exerciseId INTEGER NOT NULL,
                name TEXT NOT NULL,
                position INTEGER NOT NULL,
                FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(exerciseId) REFERENCES workout_exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        connection.execSQL("CREATE UNIQUE INDEX index_workout_session_exercises_sessionId_exerciseId ON workout_session_exercises (sessionId, exerciseId)")
        connection.execSQL("CREATE INDEX index_workout_session_exercises_exerciseId ON workout_session_exercises (exerciseId)")
        connection.execSQL("""
            INSERT INTO workout_session_exercises (id, sessionId, exerciseId, name, position)
            SELECT id, sessionId, exerciseId, name, position FROM workout_migration_entries
        """.trimIndent())
        connection.execSQL("""
            CREATE TABLE workout_sets (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sessionExerciseId INTEGER NOT NULL,
                position INTEGER NOT NULL,
                weightKg REAL NOT NULL,
                reps INTEGER NOT NULL,
                FOREIGN KEY(sessionExerciseId) REFERENCES workout_session_exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        connection.execSQL("CREATE UNIQUE INDEX index_workout_sets_sessionExerciseId_position ON workout_sets (sessionExerciseId, position)")
        connection.execSQL("""
            INSERT INTO workout_sets (id, sessionExerciseId, position, weightKg, reps)
            SELECT id, sessionExerciseId, position, weightKg, reps FROM workout_migration_sets
        """.trimIndent())
        connection.execSQL("""
            DELETE FROM sqlite_sequence
            WHERE name IN ('workout_exercises', 'workout_session_exercises', 'workout_sets')
        """.trimIndent())
        connection.execSQL("INSERT INTO sqlite_sequence (name, seq) SELECT name, seq FROM workout_migration_sequences")

        connection.execSQL("DROP TABLE workout_migration_sets")
        connection.execSQL("DROP TABLE workout_migration_entries")
        connection.execSQL("DROP TABLE workout_migration_exercises")
        connection.execSQL("DROP TABLE workout_migration_sequences")
    }
}
