package dev.bugiel.kiroku.domain.model

data class Habit(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val iconKey: String = HabitIconKey.STAR,
    val colorKey: String = HabitColorKey.GREEN,
    val createdEpochDay: Long,
    val isActive: Boolean = true,
)

data class HabitWithStatus(
    val habit: Habit,
    val isCompletedToday: Boolean,
    val stats: HabitStats,
)

data class HabitStats(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalCompletedDays: Int = 0,
)

object HabitIconKey {
    const val STAR = "star"
    const val HEART = "heart"
    const val WATER = "water"
    const val FITNESS = "fitness"
    const val BOOK = "book"
    const val MOON = "moon"

    val all = listOf(STAR, HEART, WATER, FITNESS, BOOK, MOON)
}

object HabitColorKey {
    const val GREEN = "green"
    const val BLUE = "blue"
    const val ORANGE = "orange"
    const val PINK = "pink"
    const val PURPLE = "purple"
    const val TEAL = "teal"

    val all = listOf(GREEN, BLUE, ORANGE, PINK, PURPLE, TEAL)
}

