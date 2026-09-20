package dev.bugiel.kiroku.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import dev.bugiel.kiroku.domain.model.Habit
import dev.bugiel.kiroku.domain.model.isScheduledOn
import dev.bugiel.kiroku.domain.time.HabitReminderTime
import java.time.ZonedDateTime

interface HabitReminderScheduler {
    fun schedule(habit: Habit)
    fun cancel(habitId: Long)
}

class AndroidHabitReminderScheduler(
    private val context: Context,
) : HabitReminderScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(habit: Habit) {
        val dueTimeMinutes = habit.dueTimeMinutes
        if (!habit.isActive || dueTimeMinutes == null) {
            cancel(habit.id)
            return
        }

        val triggerAt = HabitReminderTime.nextOccurrence(
            dueTimeMinutes = dueTimeMinutes,
            now = ZonedDateTime.now(),
            isScheduledOn = habit::isScheduledOn,
        )?.toInstant()?.toEpochMilli() ?: run {
            // No future occurrence within the search window (e.g. corrupted weekday mask): skip scheduling.
            cancel(habit.id)
            return
        }

        val pendingIntent = requireNotNull(reminderPendingIntent(habit.id, PendingIntent.FLAG_UPDATE_CURRENT))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    override fun cancel(habitId: Long) {
        HabitReminderNotifications.cancel(context, habitId)
        val pendingIntent = reminderPendingIntent(habitId, PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun reminderPendingIntent(habitId: Long, behaviorFlag: Int): PendingIntent? {
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            data = "kiroku://habit-reminder/$habitId".toUri()
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
        }
        return PendingIntent.getBroadcast(
            context,
            ReminderIds.habitAlarm(habitId),
            intent,
            behaviorFlag or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
