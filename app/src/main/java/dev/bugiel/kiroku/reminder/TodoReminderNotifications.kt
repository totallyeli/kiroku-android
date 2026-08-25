package dev.bugiel.kiroku.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import dev.bugiel.kiroku.MainActivity
import dev.bugiel.kiroku.R
import dev.bugiel.kiroku.domain.model.Todo

object TodoReminderNotifications {
    private const val CHANNEL_ID = "todo_reminders"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.todo_reminder_channel),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.todo_reminder_channel_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun show(context: Context, todo: Todo) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val openIntent = PendingIntent.getActivity(
            context,
            ReminderIds.todoOpen(todo.id),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                data = "kiroku://todo/${todo.id}".toUri()
                putExtra(MainActivity.EXTRA_REMINDER_TODO_ID, todo.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val completeIntent = actionIntent(context, todo.id, TodoReminderReceiver.ACTION_COMPLETE)
        val snoozeIntent = actionIntent(context, todo.id, TodoReminderReceiver.ACTION_SNOOZE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_habit_notification)
            .setContentTitle(todo.title)
            .setContentText(context.getString(R.string.todo_notification_due))
            .setContentIntent(openIntent)
            .addAction(0, context.getString(R.string.todo_notification_complete), completeIntent)
            .addAction(0, context.getString(R.string.todo_notification_snooze), snoozeIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        NotificationManagerCompat.from(context).notify(ReminderIds.todoNotification(todo.id), notification)
    }

    fun cancel(context: Context, todoId: Long) {
        NotificationManagerCompat.from(context).cancel(ReminderIds.todoNotification(todoId))
    }

    private fun actionIntent(context: Context, todoId: Long, action: String): PendingIntent {
        val requestCode = if (action == TodoReminderReceiver.ACTION_COMPLETE) {
            ReminderIds.todoComplete(todoId)
        } else {
            ReminderIds.todoSnoozeAction(todoId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, TodoReminderReceiver::class.java).apply {
                this.action = action
                data = "kiroku://todo-reminder/$todoId/$action".toUri()
                putExtra(TodoReminderReceiver.EXTRA_TODO_ID, todoId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
