package dev.bugiel.kiroku.ui.util

import android.content.Context
import dev.bugiel.kiroku.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val germanLocale = Locale.GERMANY

fun formatUpdatedAt(context: Context, timestamp: Long, today: LocalDate): String {
    val dateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
    val time = dateTime.format(DateTimeFormatter.ofPattern(context.getString(R.string.time_pattern), germanLocale))
    return when (dateTime.toLocalDate()) {
        today -> context.getString(R.string.today_at, time)
        today.minusDays(1) -> context.getString(R.string.yesterday_at, time)
        else -> dateTime.format(
            DateTimeFormatter.ofPattern(context.getString(R.string.date_pattern_short), germanLocale),
        )
    }
}

fun formatLongDate(context: Context, date: LocalDate): String = date.format(
    DateTimeFormatter.ofPattern(context.getString(R.string.date_pattern_long), germanLocale),
)

fun formatTodayDate(context: Context, date: LocalDate): String = date.format(
    DateTimeFormatter.ofPattern(context.getString(R.string.today_date_pattern), germanLocale),
).replaceFirstChar { it.titlecase(germanLocale) }

fun formatMonth(context: Context, month: java.time.YearMonth): String = month.format(
    DateTimeFormatter.ofPattern(context.getString(R.string.month_pattern), germanLocale),
).replaceFirstChar { it.titlecase(germanLocale) }

