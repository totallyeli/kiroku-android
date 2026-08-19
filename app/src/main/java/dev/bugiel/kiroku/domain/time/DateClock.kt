package dev.bugiel.kiroku.domain.time

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

interface DateClock {
    fun today(): LocalDate
    fun nowMillis(): Long
}

class SystemDateClock(
    private val clock: Clock? = null,
) : DateClock {
    override fun today(): LocalDate = clock?.let(LocalDate::now) ?: LocalDate.now(ZoneId.systemDefault())
    override fun nowMillis(): Long = clock?.millis() ?: System.currentTimeMillis()
}
