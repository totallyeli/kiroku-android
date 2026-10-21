package dev.bugiel.kiroku.data.local.entity

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutMappingTest {
    @Test fun `global exercise history maps plan identity snapshot and sorted sets`() {
        val mapped = WorkoutExerciseHistoryRow(20001, 42,
            WorkoutEntryWithSets(WorkoutSessionExerciseEntity(7, 8, 9, "Historical name", 0), listOf(
                WorkoutSetEntity(2, 7, 1, 90.0, 5), WorkoutSetEntity(1, 7, 0, 80.0, 8),
            )),
        ).toDomain()
        assertThat(mapped.planId).isEqualTo(42L)
        assertThat(mapped.sessionId).isEqualTo(8L)
        assertThat(mapped.epochDay).isEqualTo(20001L)
        assertThat(mapped.exercise.id).isEqualTo(7L)
        assertThat(mapped.exercise.exerciseId).isEqualTo(9L)
        assertThat(mapped.exercise.name).isEqualTo("Historical name")
        assertThat(mapped.exercise.sets.map { it.weightKg }).containsExactly(80.0, 90.0).inOrder()
    }

    @Test fun `assignment position ties use global identity for stable ordering`() {
        val mapped = WorkoutPlanWithHistory(WorkoutPlanEntity(1, "Push", 123), listOf(
            WorkoutAssignedExercise(WorkoutPlanExerciseEntity(1, 20, 0), WorkoutExerciseEntity(20, "A")),
            WorkoutAssignedExercise(WorkoutPlanExerciseEntity(1, 10, 0), WorkoutExerciseEntity(10, "Z")),
        ), emptyList()).toDomain()
        assertThat(mapped.exercises.map { it.id }).containsExactly(10L, 20L).inOrder()
    }

    @Test fun `relation mapping sorts days exercises and sets while retaining snapshots`() {
        val mapped = WorkoutPlanWithHistory(
            WorkoutPlanEntity(1, "Push", 123),
            listOf(
                WorkoutAssignedExercise(WorkoutPlanExerciseEntity(1, 1, 1), WorkoutExerciseEntity(1, "Renamed bench", true)),
                WorkoutAssignedExercise(WorkoutPlanExerciseEntity(1, 2, 0), WorkoutExerciseEntity(2, "Press")),
            ),
            listOf(
                WorkoutSessionWithEntries(WorkoutSessionEntity(1, 1, 20000, 123), emptyList()),
                WorkoutSessionWithEntries(WorkoutSessionEntity(2, 1, 20001, 456), listOf(
                    WorkoutEntryWithSets(WorkoutSessionExerciseEntity(2, 2, 1, "Original bench", 1), listOf(
                        WorkoutSetEntity(2, 2, 1, 82.5, 5), WorkoutSetEntity(1, 2, 0, 60.0, 12),
                    )),
                    WorkoutEntryWithSets(WorkoutSessionExerciseEntity(3, 2, 2, "Press", 0), emptyList()),
                )),
            ),
        ).toDomain()
        assertThat(mapped.sessions.map { it.epochDay }).containsExactly(20001L, 20000L).inOrder()
        assertThat(mapped.exercises.map { it.id }).containsExactly(2L, 1L).inOrder()
        assertThat(mapped.activeExercises.map { it.id }).containsExactly(2L)
        val entries = mapped.sessions.first().exercises
        assertThat(entries.map { it.name }).containsExactly("Press", "Original bench").inOrder()
        assertThat(entries.last().sets.map { it.reps }).containsExactly(12, 5).inOrder()
        assertThat(mapped.peakWeightKg(1)).isEqualTo(82.5)
    }
}
