package dev.bugiel.kiroku.domain.time

object HabitHistoryDates {
    fun pastDateOrNull(requestedEpochDay: Long?, todayEpochDay: Long): Long? =
        requestedEpochDay?.takeIf { it < todayEpochDay }

    fun epochDayFromUtcMillis(utcTimeMillis: Long): Long =
        Math.floorDiv(utcTimeMillis, MILLIS_PER_DAY)

    fun utcMillisFromEpochDay(epochDay: Long): Long = epochDay * MILLIS_PER_DAY

    private const val MILLIS_PER_DAY = 86_400_000L
}
