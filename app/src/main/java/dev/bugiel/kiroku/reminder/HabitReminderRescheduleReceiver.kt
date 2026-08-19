package dev.bugiel.kiroku.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.bugiel.kiroku.KirokuApplication

class HabitReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in supportedActions) return

        val pendingResult = goAsync()
        val application = context.applicationContext as KirokuApplication
        application.rescheduleHabitReminders { pendingResult.finish() }
    }

    companion object {
        private val supportedActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
