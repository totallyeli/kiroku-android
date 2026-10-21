package dev.bugiel.kiroku.ui.workouts

import com.google.common.truth.Truth.assertThat
import dev.bugiel.kiroku.domain.model.WorkoutExercise
import dev.bugiel.kiroku.domain.model.WorkoutPlan
import dev.bugiel.kiroku.domain.model.WorkoutSession
import dev.bugiel.kiroku.domain.model.WorkoutSessionExercise
import dev.bugiel.kiroku.domain.model.WorkoutSet
import org.junit.Test

class WorkoutsStateTest {
    private fun plan(id: Long, day: Long, entryId: Long, exerciseId: Long, weight: Double) = WorkoutPlan(
        id, "Plan $id", emptyList(), listOf(WorkoutSession(
            id, day, listOf(WorkoutSessionExercise(entryId, exerciseId, "Historical name", listOf(WorkoutSet(weight, 8)))),
        )),
    )

    @Test fun `displayed peaks include day-only exercises in every plan despite no assignments`() {
        val state = WorkoutsState(
            plans = listOf(plan(1, 20000, 10, 7, 80.0), plan(2, 20001, 20, 7, 100.0)),
            library = listOf(WorkoutExercise(7, "Renamed exercise", true)),
        )
        assertThat(state.peakWeightKg(7)).isEqualTo(100.0)
        assertThat(state.history(7).map { it.planId }).containsExactly(2L, 1L).inOrder()
        assertThat(state.history(7).map { it.exercise.name }).containsExactly("Historical name", "Historical name")
    }

    @Test fun `same-day records in different plans remain separately addressable and newest first`() {
        val state = WorkoutsState(plans = listOf(plan(1, 20000, 10, 7, 80.0), plan(2, 20000, 20, 7, 100.0)))
        assertThat(state.history(7).map { it.exercise.id }).containsExactly(20L, 10L).inOrder()
    }

    @Test fun `displayed peaks follow exercise identity not equal names and recalculate after removal`() {
        val first = plan(1, 20000, 10, 7, 80.0)
        val state = WorkoutsState(
            plans = listOf(first, plan(2, 20001, 20, 8, 100.0)),
            library = listOf(WorkoutExercise(7, "Bench", false), WorkoutExercise(8, "Bench", false)),
        )
        assertThat(state.peakWeightKg(7)).isEqualTo(80.0)
        assertThat(state.peakWeightKg(8)).isEqualTo(100.0)
        assertThat(state.copy(plans = listOf(first)).peakWeightKg(8)).isNull()
        assertThat(state.peakWeightKg(999)).isNull()
    }
}
