package dev.bugiel.kiroku.domain

import dev.bugiel.kiroku.domain.model.HabitStats

/**
 * A current streak ends today, or yesterday when today is not complete yet.
 * Duplicate and unordered epoch days are normalized before calculation.
 */
object StreakCalculator {
    fun calculate(completedEpochDays: Iterable<Long>, todayEpochDay: Long): HabitStats {
        val days = completedEpochDays.toSortedSet()
        if (days.isEmpty()) return HabitStats()

        val currentAnchor = when {
            todayEpochDay in days -> todayEpochDay
            todayEpochDay - 1 in days -> todayEpochDay - 1
            else -> null
        }

        var current = 0
        if (currentAnchor != null) {
            var day = currentAnchor
            while (day in days) {
                current++
                day--
            }
        }

        var longest = 0
        var run = 0
        var previous: Long? = null
        days.forEach { day ->
            run = if (previous != null && day == previous + 1) run + 1 else 1
            longest = maxOf(longest, run)
            previous = day
        }

        return HabitStats(
            currentStreak = current,
            longestStreak = longest,
            totalCompletedDays = days.size,
        )
    }
}

