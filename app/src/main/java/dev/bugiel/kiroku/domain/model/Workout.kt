package dev.bugiel.kiroku.domain.model

data class WorkoutPlan(
    val id: Long,
    val name: String,
    val exercises: List<WorkoutExercise>,
    val sessions: List<WorkoutSession>,
) {
    val activeExercises: List<WorkoutExercise> get() = exercises.filterNot { it.isArchived }

    fun history(exerciseId: Long): List<WorkoutExerciseHistory> = sessions.mapNotNull { session ->
        session.exercises.firstOrNull { it.exerciseId == exerciseId && it.sets.isNotEmpty() }?.let {
            WorkoutExerciseHistory(session.id, session.epochDay, it)
        }
    }.sortedByDescending { it.epochDay }

    fun peakWeightKg(exerciseId: Long): Double? = history(exerciseId)
        .flatMap { it.exercise.sets }.maxOfOrNull { it.weightKg }
}

data class WorkoutExercise(val id: Long, val name: String, val isArchived: Boolean)

data class WorkoutSession(val id: Long, val epochDay: Long, val exercises: List<WorkoutSessionExercise>) {
    val loggedExercises: Int get() = exercises.count { it.sets.isNotEmpty() }
}

data class WorkoutSessionExercise(
    val id: Long,
    val exerciseId: Long,
    val name: String,
    val sets: List<WorkoutSet>,
)

data class WorkoutSet(val weightKg: Double, val reps: Int)

data class WorkoutExerciseHistory(
    val sessionId: Long,
    val epochDay: Long,
    val exercise: WorkoutSessionExercise,
)
