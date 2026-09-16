package dev.bugiel.kiroku.data.local.entity

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutMappingTest {
    @Test fun `relation mapping sorts days exercises and sets while retaining snapshots`() {
        val mapped = WorkoutPlanWithHistory(
            WorkoutPlanEntity(1, "Push", 123),
            listOf(WorkoutExerciseEntity(1, 1, "Renamed bench", 1, true), WorkoutExerciseEntity(2, 1, "Press", 0)),
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
