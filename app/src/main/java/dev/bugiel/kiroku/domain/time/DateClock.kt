package dev.bugiel.kiroku.domain.time

import java.time.Clock
import java.time.LocalDate

interface DateClock {
    fun today(): LocalDate
    fun nowMillis(): Long
}

class SystemDateClock(
    private val clock: Clock = Clock.systemDefaultZone(),
) : DateClock {
    override fun today(): LocalDate = LocalDate.now(clock)
    override fun nowMillis(): Long = clock.millis()
}

