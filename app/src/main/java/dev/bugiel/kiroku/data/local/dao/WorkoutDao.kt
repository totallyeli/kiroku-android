package dev.bugiel.kiroku.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import dev.bugiel.kiroku.data.local.entity.WorkoutEntryWithSets
import dev.bugiel.kiroku.data.local.entity.WorkoutExerciseEntity
import dev.bugiel.kiroku.data.local.entity.WorkoutExerciseHistoryRow
import dev.bugiel.kiroku.data.local.entity.WorkoutPlanEntity
import dev.bugiel.kiroku.data.local.entity.WorkoutPlanExerciseEntity
import dev.bugiel.kiroku.data.local.entity.WorkoutPlanWithHistory
import dev.bugiel.kiroku.data.local.entity.WorkoutSessionEntity
import dev.bugiel.kiroku.data.local.entity.WorkoutSessionExerciseEntity
import dev.bugiel.kiroku.data.local.entity.WorkoutSetEntity
import dev.bugiel.kiroku.domain.WorkoutValidation
import dev.bugiel.kiroku.domain.model.WorkoutSet
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WorkoutDao {
    @Transaction
    @Query("SELECT * FROM workout_plans ORDER BY createdAt ASC, id ASC")
    abstract fun observePlans(): Flow<List<WorkoutPlanWithHistory>>

    // Includes archived exercises so callers can restore them and access their history.
    @Query("SELECT * FROM workout_exercises ORDER BY name COLLATE NOCASE, id")
    abstract fun observeExercises(): Flow<List<WorkoutExerciseEntity>>

    @Transaction
    @Query("""
        SELECT entry.*, session.epochDay, session.planId
        FROM workout_session_exercises AS entry
        INNER JOIN workout_sessions AS session ON session.id = entry.sessionId
        WHERE entry.exerciseId = :exerciseId
          AND EXISTS (SELECT 1 FROM workout_sets WHERE sessionExerciseId = entry.id)
        ORDER BY session.epochDay DESC, session.id DESC, entry.id DESC
    """)
    abstract fun observeExerciseHistory(exerciseId: Long): Flow<List<WorkoutExerciseHistoryRow>>

    @Insert
    abstract suspend fun insertPlan(plan: WorkoutPlanEntity): Long

    @Query("UPDATE workout_plans SET name = :name WHERE id = :id")
    abstract suspend fun renamePlan(id: Long, name: String)

    @Query("DELETE FROM workout_plans WHERE id = :id")
    abstract suspend fun deletePlan(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM workout_plans WHERE id = :planId)")
    abstract suspend fun planExists(planId: Long): Boolean

    @Query("""
        SELECT exercise.* FROM workout_exercises AS exercise
        INNER JOIN workout_plan_exercises AS assignment ON assignment.exerciseId = exercise.id
        WHERE assignment.planId = :planId ORDER BY assignment.position, exercise.id
    """)
    abstract suspend fun exercises(planId: Long): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_exercises WHERE id = :exerciseId")
    abstract suspend fun exercise(exerciseId: Long): WorkoutExerciseEntity?

    @Insert
    abstract suspend fun insertExercise(exercise: WorkoutExerciseEntity): Long

    @Transaction
    open suspend fun createExercise(name: String): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Exercise name must not be blank" }
        return insertExercise(WorkoutExerciseEntity(name = trimmed))
    }

    @Query("UPDATE workout_exercises SET name = :name WHERE id = :id")
    protected abstract suspend fun updateExerciseName(id: Long, name: String)

    @Transaction
    open suspend fun renameExercise(id: Long, name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Exercise name must not be blank" }
        requireNotNull(exercise(id)) { "Exercise $id does not exist" }
        updateExerciseName(id, trimmed)
    }

    @Query("UPDATE workout_exercises SET isArchived = :archived WHERE id = :id")
    protected abstract suspend fun updateExerciseArchived(id: Long, archived: Boolean)

    @Transaction
    open suspend fun archiveExercise(id: Long, archived: Boolean) {
        requireNotNull(exercise(id)) { "Exercise $id does not exist" }
        updateExerciseArchived(id, archived)
    }

    @Query("SELECT * FROM workout_plan_exercises WHERE planId = :planId AND exerciseId = :exerciseId")
    abstract suspend fun assignment(planId: Long, exerciseId: Long): WorkoutPlanExerciseEntity?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM workout_plan_exercises WHERE planId = :planId")
    abstract suspend fun nextPlanPosition(planId: Long): Int

    @Insert
    abstract suspend fun insertAssignment(assignment: WorkoutPlanExerciseEntity)

    @Query("DELETE FROM workout_plan_exercises WHERE planId = :planId AND exerciseId = :exerciseId")
    protected abstract suspend fun deleteAssignment(planId: Long, exerciseId: Long)

    @Transaction
    open suspend fun assignExercise(planId: Long, exerciseId: Long) {
        require(planExists(planId)) { "Plan $planId does not exist" }
        val exercise = requireNotNull(exercise(exerciseId)) { "Exercise $exerciseId does not exist" }
        if (assignment(planId, exerciseId) != null) return
        require(!exercise.isArchived) { "Exercise $exerciseId is archived" }
        insertAssignment(WorkoutPlanExerciseEntity(planId, exerciseId, nextPlanPosition(planId)))
    }

    @Transaction
    open suspend fun unassignExercise(planId: Long, exerciseId: Long) {
        require(planExists(planId)) { "Plan $planId does not exist" }
        requireNotNull(exercise(exerciseId)) { "Exercise $exerciseId does not exist" }
        // Only the template assignment is removed; entries, sets, and the library survive.
        deleteAssignment(planId, exerciseId)
    }

    @Transaction
    open suspend fun addExercise(planId: Long, name: String): Long {
        require(planExists(planId)) { "Plan $planId does not exist" }
        val id = createExercise(name)
        assignExercise(planId, id)
        return id
    }

    @Query("SELECT id FROM workout_sessions WHERE planId = :planId AND epochDay = :epochDay")
    abstract suspend fun sessionForDay(planId: Long, epochDay: Long): Long?

    @Query("SELECT EXISTS(SELECT 1 FROM workout_sessions WHERE id = :sessionId)")
    abstract suspend fun sessionExists(sessionId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Insert
    abstract suspend fun insertEntries(entries: List<WorkoutSessionExerciseEntity>)

    @Insert
    abstract suspend fun insertEntry(entry: WorkoutSessionExerciseEntity): Long

    @Transaction
    open suspend fun startSession(planId: Long, epochDay: Long, nowMillis: Long): Long {
        require(planExists(planId)) { "Plan $planId does not exist" }
        // A repeated tap resumes the existing day, preserving every saved set.
        sessionForDay(planId, epochDay)?.let { return it }
        val active = exercises(planId).filterNot { it.isArchived }
        val sessionId = insertSession(WorkoutSessionEntity(planId = planId, epochDay = epochDay, createdAt = nowMillis))
        if (sessionId == -1L) return checkNotNull(sessionForDay(planId, epochDay))
        // An empty assignment list deliberately creates an empty, editable day.
        insertEntries(active.mapIndexed { index, exercise ->
            WorkoutSessionExerciseEntity(
                sessionId = sessionId, exerciseId = exercise.id, name = exercise.name, position = index,
            )
        })
        return sessionId
    }

    @Query("SELECT id FROM workout_session_exercises WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
    abstract suspend fun sessionEntry(sessionId: Long, exerciseId: Long): Long?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM workout_session_exercises WHERE sessionId = :sessionId")
    abstract suspend fun nextSessionPosition(sessionId: Long): Int

    @Transaction
    open suspend fun addSessionExercise(sessionId: Long, exerciseId: Long): Long {
        require(sessionExists(sessionId)) { "Session $sessionId does not exist" }
        val exercise = requireNotNull(exercise(exerciseId)) { "Exercise $exerciseId does not exist" }
        // Resume even an archived exercise already present on this day without modifying it.
        sessionEntry(sessionId, exerciseId)?.let { return it }
        require(!exercise.isArchived) { "Exercise $exerciseId is archived" }
        return insertEntry(WorkoutSessionExerciseEntity(
            sessionId = sessionId, exerciseId = exerciseId, name = exercise.name,
            position = nextSessionPosition(sessionId),
        ))
    }

    @Query("UPDATE workout_session_exercises SET exerciseId = :exerciseId, name = :name WHERE id = :entryId")
    protected abstract suspend fun updateEntryExercise(entryId: Long, exerciseId: Long, name: String)

    /**
     * Replaces only this day's exercise, retaining its entry ID and position.
     * The UI MUST obtain confirmation before calling this if the entry has sets:
     * replacement clears those sets rather than attributing them to another exercise.
     * Selecting the same exercise is a no-op; a duplicate elsewhere on the day is rejected.
     */
    @Transaction
    open suspend fun replaceSessionExercise(entryId: Long, exerciseId: Long): Long {
        val old = requireNotNull(getEntry(entryId)) { "Entry $entryId does not exist" }
        val exercise = requireNotNull(exercise(exerciseId)) { "Exercise $exerciseId does not exist" }
        if (old.entry.exerciseId == exerciseId) return entryId
        require(!exercise.isArchived) { "Exercise $exerciseId is archived" }
        require(sessionEntry(old.entry.sessionId, exerciseId) == null) {
            "Exercise $exerciseId is already in session ${old.entry.sessionId}"
        }
        deleteSets(entryId)
        updateEntryExercise(entryId, exerciseId, exercise.name)
        return entryId
    }

    @Query("DELETE FROM workout_session_exercises WHERE id = :entryId")
    protected abstract suspend fun deleteEntry(entryId: Long)

    @Transaction
    open suspend fun deleteSessionExercise(entryId: Long) {
        requireNotNull(getEntry(entryId)) { "Entry $entryId does not exist" }
        // Explicit day-only deletion cascades to its sets, never to the plan or library.
        deleteEntry(entryId)
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
        requireNotNull(getEntry(entryId)) { "Entry $entryId does not exist" }
        require(sets.all {
            it.sessionExerciseId == entryId && WorkoutValidation.isValid(WorkoutSet(it.weightKg, it.reps))
        }) { "Invalid workout sets" }
        require(sets.map { it.position }.distinct().size == sets.size) { "Duplicate set position" }
        deleteSets(entryId)
        insertSets(sets)
    }
}
