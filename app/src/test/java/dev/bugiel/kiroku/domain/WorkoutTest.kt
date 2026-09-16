package dev.bugiel.kiroku.domain

import com.google.common.truth.Truth.assertThat
import dev.bugiel.kiroku.domain.model.WorkoutExercise
import dev.bugiel.kiroku.domain.model.WorkoutPlan
import dev.bugiel.kiroku.domain.model.WorkoutSession
import dev.bugiel.kiroku.domain.model.WorkoutSessionExercise
import dev.bugiel.kiroku.domain.model.WorkoutSet
import org.junit.Test

class WorkoutTest {
    private val bench = WorkoutExercise(1, "Bench press", false)
    private fun day(id: Long, date: Long, vararg weights: Double) = WorkoutSession(
        id, date, listOf(WorkoutSessionExercise(id, bench.id, bench.name, weights.map { WorkoutSet(it, 8) })),
    )
    private fun plan(vararg days: WorkoutSession) = WorkoutPlan(1, "Push", listOf(bench), days.toList())

    @Test fun `peak includes every set and all dates rather than only the latest day`() {
        val plan = plan(day(1, 20000, 60.0, 82.5, 70.0), day(2, 20001, 75.0))
        assertThat(plan.peakWeightKg(bench.id)).isEqualTo(82.5)
        assertThat(plan.history(bench.id).map { it.epochDay }).containsExactly(20001L, 20000L).inOrder()
    }

    @Test fun `different exercises never contribute to each others peak`() {
        val plan = plan(day(1, 20000, 60.0)).let {
            it.copy(sessions = it.sessions + WorkoutSession(2, 20001, listOf(
                WorkoutSessionExercise(2, 99, "Squat", listOf(WorkoutSet(150.0, 3))),
            )))
        }
        assertThat(plan.peakWeightKg(bench.id)).isEqualTo(60.0)
    }

    @Test fun `editing or deleting a record recalculates peak`() {
        val before = plan(day(1, 20000, 80.0), day(2, 20001, 90.0))
        val corrected = before.copy(sessions = listOf(day(1, 20000, 80.0), day(2, 20001, 70.0)))
        val deleted = before.copy(sessions = before.sessions.filterNot { it.id == 2L })
        assertThat(before.peakWeightKg(bench.id)).isEqualTo(90.0)
        assertThat(corrected.peakWeightKg(bench.id)).isEqualTo(80.0)
        assertThat(deleted.peakWeightKg(bench.id)).isEqualTo(80.0)
    }

    @Test fun `archiving keeps records while hiding exercise from the routine`() {
        val archived = plan(day(1, 20000, 90.0)).copy(exercises = listOf(bench.copy(isArchived = true)))
        assertThat(archived.activeExercises).isEmpty()
        assertThat(archived.peakWeightKg(bench.id)).isEqualTo(90.0)
        assertThat(archived.history(bench.id)).hasSize(1)
    }

    @Test fun `empty sessions are not records and zero weight is a valid record`() {
        assertThat(plan(day(1, 20000)).peakWeightKg(bench.id)).isNull()
        assertThat(plan(day(1, 20000)).history(bench.id)).isEmpty()
        assertThat(day(1, 20000).loggedExercises).isEqualTo(0)
        assertThat(plan(day(1, 20000, 0.0)).peakWeightKg(bench.id)).isEqualTo(0.0)
        assertThat(day(1, 20000, 0.0).loggedExercises).isEqualTo(1)
    }

    @Test fun `decimal comma and decimal point preserve fractional kilograms`() {
        assertThat(WorkoutValidation.parseSet(" 82,5 ", " 8 ")).isEqualTo(WorkoutSet(82.5, 8))
        assertThat(WorkoutValidation.parseSet("82.125", "10")).isEqualTo(WorkoutSet(82.125, 10))
        assertThat(WorkoutValidation.parseSet("0", "12")).isEqualTo(WorkoutSet(0.0, 12))
    }

    @Test fun `rejects blank ambiguous nonfinite or out of range set values`() {
        listOf("", "-1", "NaN", "Infinity", "1e3", "1,2.3", "1 000", "1.2345", "10001").forEach {
            assertThat(WorkoutValidation.parseSet(it, "8")).isNull()
        }
        listOf("", "0", "-1", "1.5", "10001", "2147483648").forEach {
            assertThat(WorkoutValidation.parseSet("60", it)).isNull()
        }
        assertThat(WorkoutValidation.isValid(WorkoutSet(Double.NaN, 8))).isFalse()
        assertThat(WorkoutValidation.isValid(WorkoutSet(Double.POSITIVE_INFINITY, 8))).isFalse()
    }
}
