package dev.bugiel.kiroku.domain.model

data class Todo(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val iconKey: String = HabitIconKey.STAR,
    val colorKey: String = HabitColorKey.GREEN,
    val priority: String = TodoPriority.NONE,
    val dueEpochDay: Long,
    val dueTimeMinutes: Int,
    val reminderOffsetMinutes: Int? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null,
) {
    val isCompleted: Boolean get() = completedAt != null
}

object TodoPriority {
    const val NONE = "none"
    const val LOW = "low"
    const val MEDIUM = "medium"
    const val HIGH = "high"

    val all = listOf(NONE, LOW, MEDIUM, HIGH)

    fun rank(priority: String): Int = when (priority) {
        HIGH -> 3
        MEDIUM -> 2
        LOW -> 1
        else -> 0
    }
}

object TodoReminderOffset {
    const val AT_DUE_TIME = 0
    const val FIVE_MINUTES = 5
    const val FIFTEEN_MINUTES = 15
    const val THIRTY_MINUTES = 30
    const val ONE_HOUR = 60
    const val ONE_DAY = 24 * 60

    val all = listOf(AT_DUE_TIME, FIVE_MINUTES, FIFTEEN_MINUTES, THIRTY_MINUTES, ONE_HOUR, ONE_DAY)
}
