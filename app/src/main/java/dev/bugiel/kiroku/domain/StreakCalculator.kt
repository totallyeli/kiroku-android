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

    fun calculateScheduled(
        completedEpochDays: Iterable<Long>,
        todayEpochDay: Long,
        createdEpochDay: Long,
        isScheduledOn: (Long) -> Boolean,
    ): HabitStats {
        val completed = completedEpochDays.toSortedSet()
        val scheduledThroughToday = (createdEpochDay..todayEpochDay).filter(isScheduledOn)
        if (scheduledThroughToday.isEmpty()) {
            return HabitStats(totalCompletedDays = completed.size)
        }

        val lastCompletedAnchor = scheduledThroughToday.asReversed().firstOrNull { occurrence ->
            occurrence < todayEpochDay || occurrence in completed
        }
        var current = 0
        if (lastCompletedAnchor != null && lastCompletedAnchor in completed) {
            val anchorIndex = scheduledThroughToday.indexOf(lastCompletedAnchor)
            for (index in anchorIndex downTo 0) {
                if (scheduledThroughToday[index] !in completed) break
                current++
            }
        }

        var longest = 0
        var run = 0
        scheduledThroughToday.forEach { occurrence ->
            if (occurrence in completed) {
                run++
                longest = maxOf(longest, run)
            } else {
                run = 0
            }
        }

        return HabitStats(
            currentStreak = current,
            longestStreak = longest,
            totalCompletedDays = completed.size,
        )
    }
}
