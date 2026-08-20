package dev.bugiel.kiroku.ui.util

import android.content.Context
import dev.bugiel.kiroku.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun Context.appLocale(): Locale = resources.configuration.locales[0] ?: Locale.ENGLISH

fun formatUpdatedAt(context: Context, timestamp: Long, today: LocalDate): String {
    val dateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
    val locale = context.appLocale()
    val time = dateTime.format(DateTimeFormatter.ofPattern(context.getString(R.string.time_pattern), locale))
    return when (dateTime.toLocalDate()) {
        today -> context.getString(R.string.today_at, time)
        today.minusDays(1) -> context.getString(R.string.yesterday_at, time)
        else -> dateTime.format(
            DateTimeFormatter.ofPattern(context.getString(R.string.date_pattern_short), locale),
        )
    }
}

fun formatLongDate(context: Context, date: LocalDate): String = date.format(
    DateTimeFormatter.ofPattern(context.getString(R.string.date_pattern_long), context.appLocale()),
)

fun formatTodayDate(context: Context, date: LocalDate): String {
    val locale = context.appLocale()
    return date.format(
        DateTimeFormatter.ofPattern(context.getString(R.string.today_date_pattern), locale),
    ).replaceFirstChar { it.titlecase(locale) }
}

fun formatMonth(context: Context, month: java.time.YearMonth): String {
    val locale = context.appLocale()
    return month.format(
        DateTimeFormatter.ofPattern(context.getString(R.string.month_pattern), locale),
    ).replaceFirstChar { it.titlecase(locale) }
}

