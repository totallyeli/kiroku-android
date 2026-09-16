package dev.bugiel.kiroku.di

import android.content.Context
import androidx.room3.Room
import dev.bugiel.kiroku.data.local.KirokuDatabase
import dev.bugiel.kiroku.data.repository.HabitRepository
import dev.bugiel.kiroku.data.repository.AttachmentRepository
import dev.bugiel.kiroku.data.repository.NoteRepository
import dev.bugiel.kiroku.data.repository.OfflineAttachmentRepository
import dev.bugiel.kiroku.data.repository.OfflineHabitRepository
import dev.bugiel.kiroku.data.repository.OfflineNoteRepository
import dev.bugiel.kiroku.data.repository.SettingsRepository
import dev.bugiel.kiroku.data.repository.TodoRepository
import dev.bugiel.kiroku.data.repository.OfflineTodoRepository
import dev.bugiel.kiroku.data.repository.WorkoutRepository
import dev.bugiel.kiroku.data.repository.OfflineWorkoutRepository
import dev.bugiel.kiroku.domain.time.SystemDateClock
import dev.bugiel.kiroku.domain.time.TodayProvider
import dev.bugiel.kiroku.reminder.AndroidHabitReminderScheduler
import dev.bugiel.kiroku.reminder.AndroidTodoReminderScheduler
import dev.bugiel.kiroku.update.AppUpdateRepository
import dev.bugiel.kiroku.update.GitHubUpdateRepository

class AppContainer(context: Context) {
    val dateClock = SystemDateClock()
    val todayProvider = TodayProvider(dateClock)

    private val database: KirokuDatabase by lazy {
        Room.databaseBuilder<KirokuDatabase>(
            context = context.applicationContext,
            name = "kiroku.db",
        ).build()
    }

    val noteRepository: NoteRepository by lazy {
        OfflineNoteRepository(database.noteDao(), context.applicationContext)
    }
    val attachmentRepository: AttachmentRepository by lazy {
        OfflineAttachmentRepository(context.applicationContext, database.noteAttachmentDao())
    }
    val habitRepository: HabitRepository by lazy { OfflineHabitRepository(database.habitDao()) }
    val todoRepository: TodoRepository by lazy { OfflineTodoRepository(database.todoDao()) }
    val workoutRepository: WorkoutRepository by lazy { OfflineWorkoutRepository(database.workoutDao()) }
    val settingsRepository = SettingsRepository(context.applicationContext)
    val habitReminderScheduler = AndroidHabitReminderScheduler(context.applicationContext)
    val todoReminderScheduler = AndroidTodoReminderScheduler(context.applicationContext)
    val appUpdateRepository: AppUpdateRepository = GitHubUpdateRepository(context.applicationContext)
}
