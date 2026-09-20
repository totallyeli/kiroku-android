package dev.bugiel.kiroku.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import dev.bugiel.kiroku.data.local.entity.WorkoutEntryWithSets
import dev.bugiel.kiroku.data.local.entity.WorkoutExerciseEntity
import dev.bugiel.kiroku.data.local.entity.WorkoutPlanEntity
import dev.bugiel.kiroku.data.local.entity.WorkoutPlanWithHistory
import dev.bugiel.kiroku.data.local.entity.WorkoutSessionEntity
import dev.bugiel.kiroku.data.local.entity.WorkoutSessionExerciseEntity
import dev.bugiel.kiroku.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WorkoutDao {
    @Transaction
    @Query("SELECT * FROM workout_plans ORDER BY createdAt ASC, id ASC")
    abstract fun observePlans(): Flow<List<WorkoutPlanWithHistory>>

    @Insert
    abstract suspend fun insertPlan(plan: WorkoutPlanEntity): Long

    @Query("UPDATE workout_plans SET name = :name WHERE id = :id")
    abstract suspend fun renamePlan(id: Long, name: String)

    @Query("DELETE FROM workout_plans WHERE id = :id")
    abstract suspend fun deletePlan(id: Long)

    @Query("SELECT * FROM workout_exercises WHERE planId = :planId ORDER BY position, id")
    abstract suspend fun exercises(planId: Long): List<WorkoutExerciseEntity>

    @Insert
    abstract suspend fun insertExercise(exercise: WorkoutExerciseEntity): Long

    @Query("UPDATE workout_exercises SET name = :name WHERE id = :id")
    abstract suspend fun renameExercise(id: Long, name: String)

    @Query("UPDATE workout_exercises SET isArchived = :archived WHERE id = :id")
    abstract suspend fun archiveExercise(id: Long, archived: Boolean)

    @Query("SELECT id FROM workout_sessions WHERE planId = :planId AND epochDay = :epochDay")
    abstract suspend fun sessionForDay(planId: Long, epochDay: Long): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Insert
    abstract suspend fun insertEntries(entries: List<WorkoutSessionExerciseEntity>)

    @Transaction
    open suspend fun startSession(planId: Long, epochDay: Long, nowMillis: Long): Long {
        // A repeated tap resumes the existing day, preserving every saved set.
        sessionForDay(planId, epochDay)?.let { return it }
        val active = exercises(planId).filterNot { it.isArchived }
        require(active.isNotEmpty()) { "Plan $planId has no active exercises" }
        val sessionId = insertSession(WorkoutSessionEntity(planId = planId, epochDay = epochDay, createdAt = nowMillis))
        // With IGNORE on the unique (planId, epochDay) index, a racing insert returns -1: re-resolve and resume.
        val effectiveSessionId = if (sessionId != -1L) sessionId else sessionForDay(planId, epochDay) ?: return -1L
        if (effectiveSessionId == sessionId) {
            insertEntries(active.mapIndexed { index, exercise ->
                WorkoutSessionExerciseEntity(
                    sessionId = effectiveSessionId, exerciseId = exercise.id, name = exercise.name, position = index,
                )
            })
        }
        return effectiveSessionId
    }

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    abstract suspend fun deleteSession(id: Long)

    @Transaction
    @Query("SELECT * FROM workout_session_exercises WHERE id = :id")
    abstract suspend fun getEntry(id: Long): WorkoutEntryWithSets?

    @Query("DELETE FROM workout_sets WHERE sessionExerciseId = :entryId")
    abstract suspend fun deleteSets(entryId: Long)

    @Insert
    abstract suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Transaction
    open suspend fun replaceSets(entryId: Long, sets: List<WorkoutSetEntity>) {
        check(getEntry(entryId) != null)
        deleteSets(entryId)
        insertSets(sets)
    }

    @Transaction
    open suspend fun addExercise(planId: Long, name: String): Long = insertExercise(
        WorkoutExerciseEntity(
            planId = planId, name = name,
            position = (exercises(planId).maxOfOrNull { it.position } ?: -1) + 1,
        ),
    )
}
