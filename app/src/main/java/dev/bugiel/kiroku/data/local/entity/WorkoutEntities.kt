package dev.bugiel.kiroku.data.local.entity

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import androidx.room3.Relation
import dev.bugiel.kiroku.domain.model.WorkoutExercise
import dev.bugiel.kiroku.domain.model.WorkoutExerciseHistory
import dev.bugiel.kiroku.domain.model.WorkoutPlan
import dev.bugiel.kiroku.domain.model.WorkoutSession
import dev.bugiel.kiroku.domain.model.WorkoutSessionExercise
import dev.bugiel.kiroku.domain.model.WorkoutSet

@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(tableName = "workout_exercises")
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isArchived: Boolean = false,
) {
    fun toDomain() = WorkoutExercise(id, name, isArchived)
}

@Entity(
    tableName = "workout_plan_exercises",
    primaryKeys = ["planId", "exerciseId"],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlanEntity::class, parentColumns = ["id"], childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorkoutExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("exerciseId")],
)
data class WorkoutPlanExerciseEntity(
    val planId: Long,
    val exerciseId: Long,
    val position: Int,
)

data class WorkoutAssignedExercise(
    @Embedded val assignment: WorkoutPlanExerciseEntity,
    @Relation(parentColumns = ["exerciseId"], entityColumns = ["id"])
    val exercise: WorkoutExerciseEntity,
)

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [ForeignKey(
        entity = WorkoutPlanEntity::class, parentColumns = ["id"], childColumns = ["planId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["planId", "epochDay"], unique = true)],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val epochDay: Long,
    val createdAt: Long,
)

@Entity(
    tableName = "workout_session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorkoutExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["sessionId", "exerciseId"], unique = true), Index("exerciseId")],
)
data class WorkoutSessionExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    // A snapshot keeps old workout days readable when the routine is renamed or edited.
    val name: String,
    val position: Int,
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [ForeignKey(
        entity = WorkoutSessionExerciseEntity::class, parentColumns = ["id"], childColumns = ["sessionExerciseId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["sessionExerciseId", "position"], unique = true)],
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionExerciseId: Long,
    val position: Int,
    val weightKg: Double,
    val reps: Int,
)

data class WorkoutEntryWithSets(
    @Embedded val entry: WorkoutSessionExerciseEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["sessionExerciseId"]) val sets: List<WorkoutSetEntity>,
) {
    fun toDomain() = WorkoutSessionExercise(
        entry.id, entry.exerciseId, entry.name,
        sets.sortedBy { it.position }.map { WorkoutSet(it.weightKg, it.reps) },
    )
}

data class WorkoutExerciseHistoryRow(
    val epochDay: Long,
    val planId: Long,
    @Embedded val entry: WorkoutEntryWithSets,
) {
    fun toDomain() = WorkoutExerciseHistory(entry.entry.sessionId, epochDay, entry.toDomain(), planId)
}

data class WorkoutSessionWithEntries(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["sessionId"], entity = WorkoutSessionExerciseEntity::class)
    val entries: List<WorkoutEntryWithSets>,
) {
    fun toDomain() = WorkoutSession(
        session.id, session.epochDay, entries.sortedBy { it.entry.position }.map { it.toDomain() },
    )
}

data class WorkoutPlanWithHistory(
    @Embedded val plan: WorkoutPlanEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["planId"], entity = WorkoutPlanExerciseEntity::class)
    val exercises: List<WorkoutAssignedExercise>,
    @Relation(parentColumns = ["id"], entityColumns = ["planId"], entity = WorkoutSessionEntity::class)
    val sessions: List<WorkoutSessionWithEntries>,
) {
    fun toDomain() = WorkoutPlan(
        plan.id, plan.name,
        exercises.sortedWith(compareBy({ it.assignment.position }, { it.exercise.id })).map { it.exercise.toDomain() },
        sessions.sortedByDescending { it.session.epochDay }.map { it.toDomain() },
    )
}
