package dev.bugiel.kiroku.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
        ).toInstant().toEpochMilli()

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            requireNotNull(reminderPendingIntent(habit.id, PendingIntent.FLAG_UPDATE_CURRENT)),
        )
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
            habitId.hashCode(),
            intent,
            behaviorFlag or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
