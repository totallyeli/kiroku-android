package dev.bugiel.kiroku.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
) : TodoReminderScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(todo: Todo) {
        cancelSnooze(todo.id)
        val triggerAt = TodoReminderTime.triggerAtMillis(todo)
        if (!TodoReminderTime.shouldSchedule(todo, System.currentTimeMillis()) || triggerAt == null) {
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
        setAlarm(
            triggerAtMillis = TodoReminderTime.snoozeUntilMillis(nowMillis),
            pendingIntent = requireNotNull(snoozePendingIntent(todoId, PendingIntent.FLAG_UPDATE_CURRENT)),
        )
    }

    override fun cancel(todoId: Long) {
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
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
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
