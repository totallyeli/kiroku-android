package dev.bugiel.kiroku.domain.time

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class TodayProviderTest {
    @Test
    fun `refresh exposes a new local day without touching history`() {
        val clock = MutableDateClock(LocalDate.of(2026, 8, 19))
        val provider = TodayProvider(clock)
        val historicalCompletion = clock.today().toEpochDay()

        clock.date = LocalDate.of(2026, 8, 20)
        provider.refresh()

        assertThat(provider.todayEpochDay.value).isEqualTo(LocalDate.of(2026, 8, 20).toEpochDay())
        assertThat(historicalCompletion).isEqualTo(LocalDate.of(2026, 8, 19).toEpochDay())
        assertThat(provider.todayEpochDay.value).isNotEqualTo(historicalCompletion)
    }

    private class MutableDateClock(
        var date: LocalDate,
    ) : DateClock {
        override fun today(): LocalDate = date
        override fun nowMillis(): Long = date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    }
}

