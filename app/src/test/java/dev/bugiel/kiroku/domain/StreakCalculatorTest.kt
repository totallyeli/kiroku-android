package dev.bugiel.kiroku.domain

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class StreakCalculatorTest {
    private val today = LocalDate.of(2026, 8, 19).toEpochDay()

    @Test
    fun `current streak includes today when completed`() {
        val stats = StreakCalculator.calculate(listOf(today - 2, today - 1, today), today)

        assertThat(stats.currentStreak).isEqualTo(3)
    }

    @Test
    fun `current streak remains active when only yesterday is completed`() {
        val stats = StreakCalculator.calculate(listOf(today - 2, today - 1), today)

        assertThat(stats.currentStreak).isEqualTo(2)
    }

    @Test
    fun `broken current streak returns zero`() {
        val stats = StreakCalculator.calculate(listOf(today - 4, today - 3, today - 2), today)

        assertThat(stats.currentStreak).isEqualTo(0)
        assertThat(stats.longestStreak).isEqualTo(3)
    }

    @Test
    fun `longest streak finds longest consecutive sequence`() {
        val stats = StreakCalculator.calculate(
            listOf(today - 10, today - 9, today - 5, today - 4, today - 3, today),
            today,
        )

        assertThat(stats.longestStreak).isEqualTo(3)
        assertThat(stats.currentStreak).isEqualTo(1)
    }

    @Test
    fun `duplicates and unordered dates are normalized`() {
        val stats = StreakCalculator.calculate(
            listOf(today, today - 2, today - 1, today, today - 1),
            today,
        )

        assertThat(stats.currentStreak).isEqualTo(3)
        assertThat(stats.longestStreak).isEqualTo(3)
        assertThat(stats.totalCompletedDays).isEqualTo(3)
    }

    @Test
    fun `streak crosses month and year boundary`() {
        val janSecond = LocalDate.of(2026, 1, 2).toEpochDay()
        val stats = StreakCalculator.calculate(
            listOf(
                LocalDate.of(2025, 12, 30).toEpochDay(),
                LocalDate.of(2025, 12, 31).toEpochDay(),
                LocalDate.of(2026, 1, 1).toEpochDay(),
                janSecond,
            ),
            janSecond,
        )

        assertThat(stats.currentStreak).isEqualTo(4)
        assertThat(stats.longestStreak).isEqualTo(4)
    }

    @Test
    fun `leap day is consecutive`() {
        val marchFirst = LocalDate.of(2024, 3, 1).toEpochDay()
        val stats = StreakCalculator.calculate(
            listOf(
                LocalDate.of(2024, 2, 28).toEpochDay(),
                LocalDate.of(2024, 2, 29).toEpochDay(),
                marchFirst,
            ),
            marchFirst,
        )

        assertThat(stats.currentStreak).isEqualTo(3)
        assertThat(stats.longestStreak).isEqualTo(3)
    }

    @Test
    fun `empty history returns zero statistics`() {
        val stats = StreakCalculator.calculate(emptyList(), today)

        assertThat(stats.currentStreak).isEqualTo(0)
        assertThat(stats.longestStreak).isEqualTo(0)
        assertThat(stats.totalCompletedDays).isEqualTo(0)
    }
}

