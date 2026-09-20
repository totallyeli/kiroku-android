package dev.bugiel.kiroku.reminder

import android.content.Context
import androidx.core.content.edit

// Snooze targets are persisted so they survive process death, a reboot or a timezone
// change: rescheduleReminders() re-arms stored alarms instead of silently dropping them.
class TodoSnoozeStore(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences("kiroku_todo_snoozes", Context.MODE_PRIVATE)

    /** Returns the persisted snooze target in epoch millis, or 0 when none is stored. */
    fun snoozeUntil(todoId: Long): Long = preferences.getLong("snooze_$todoId", 0L)

    fun setSnoozeUntil(todoId: Long, atMillis: Long) {
        preferences.edit { putLong("snooze_$todoId", atMillis) }
    }

    fun clear(todoId: Long) {
        preferences.edit { remove("snooze_$todoId") }
    }
}
