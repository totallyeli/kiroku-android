package dev.bugiel.kiroku.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import dev.bugiel.kiroku.domain.model.Todo
import dev.bugiel.kiroku.domain.time.TodoReminderTime

interface TodoReminderScheduler {
    fun schedule(todo: Todo)
    fun snooze(todoId: Long, nowMillis: Long)
    fun cancel(todoId: Long)
}

class AndroidTodoReminderScheduler(
    private val context: Context,
    private val snoozeStore: TodoSnoozeStore,
) : TodoReminderScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(todo: Todo) {
        // schedule() also runs on process start, BOOT_COMPLETED, package replace and
        // time/timezone changes, so a persisted snooze must be re-armed rather than
        // silently wiped here (it was already cancelled by the old cancelSnooze call).
        val now = System.currentTimeMillis()
        val snoozeAt = snoozeStore.snoozeUntil(todo.id)
        if (snoozeAt > now) {
            setAlarm(
                triggerAtMillis = snoozeAt,
                pendingIntent = requireNotNull(snoozePendingIntent(todo.id, PendingIntent.FLAG_UPDATE_CURRENT)),
            )
        } else if (snoozeAt > 0L) {
            // Stale persisted snooze: it already fired or was consumed, so drop the record.
            snoozeStore.clear(todo.id)
        }

        val triggerAt = TodoReminderTime.triggerAtMillis(todo)
        if (!TodoReminderTime.shouldSchedule(todo, now) || triggerAt == null) {
            cancelPendingIntent(scheduledPendingIntent(todo.id, PendingIntent.FLAG_NO_CREATE))
            return
        }
        setAlarm(
            triggerAtMillis = triggerAt,
            pendingIntent = requireNotNull(scheduledPendingIntent(todo.id, PendingIntent.FLAG_UPDATE_CURRENT)),
        )
    }

    override fun snooze(todoId: Long, nowMillis: Long) {
        cancelSnooze(todoId)
        val snoozeAtMillis = TodoReminderTime.snoozeUntilMillis(nowMillis)
        snoozeStore.setSnoozeUntil(todoId, snoozeAtMillis)
        setAlarm(
            triggerAtMillis = snoozeAtMillis,
            pendingIntent = requireNotNull(snoozePendingIntent(todoId, PendingIntent.FLAG_UPDATE_CURRENT)),
        )
    }

    override fun cancel(todoId: Long) {
        snoozeStore.clear(todoId)
        TodoReminderNotifications.cancel(context, todoId)
        cancelPendingIntent(scheduledPendingIntent(todoId, PendingIntent.FLAG_NO_CREATE))
        cancelSnooze(todoId)
    }

    private fun cancelSnooze(todoId: Long) {
        cancelPendingIntent(snoozePendingIntent(todoId, PendingIntent.FLAG_NO_CREATE))
    }

    private fun cancelPendingIntent(pendingIntent: PendingIntent?) {
        if (pendingIntent == null) return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun setAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun scheduledPendingIntent(todoId: Long, behaviorFlag: Int): PendingIntent? =
        reminderPendingIntent(
            todoId = todoId,
            requestCode = ReminderIds.todoAlarm(todoId),
            uri = "kiroku://todo-reminder/$todoId/scheduled",
            behaviorFlag = behaviorFlag,
        )

    private fun snoozePendingIntent(todoId: Long, behaviorFlag: Int): PendingIntent? =
        reminderPendingIntent(
            todoId = todoId,
            requestCode = ReminderIds.todoSnoozeAlarm(todoId),
            uri = "kiroku://todo-reminder/$todoId/snoozed",
            behaviorFlag = behaviorFlag,
        )

    private fun reminderPendingIntent(
        todoId: Long,
        requestCode: Int,
        uri: String,
        behaviorFlag: Int,
    ): PendingIntent? {
        val intent = Intent(context, TodoReminderReceiver::class.java).apply {
            action = TodoReminderReceiver.ACTION_SHOW
            data = uri.toUri()
            putExtra(TodoReminderReceiver.EXTRA_TODO_ID, todoId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            behaviorFlag or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
