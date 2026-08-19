package dev.bugiel.kiroku.domain.model

data class Habit(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val iconKey: String = HabitIconKey.STAR,
    val colorKey: String = HabitColorKey.GREEN,
    val createdEpochDay: Long,
    val isActive: Boolean = true,
    val dueTimeMinutes: Int? = null,
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
    const val WALK = "walk"
    const val RUN = "run"
    const val BICYCLE = "bicycle"
    const val MEDITATION = "meditation"
    const val FOOD = "food"
    const val MEDICATION = "medication"
    const val CLEANING = "cleaning"
    const val WORK = "work"
    const val STUDY = "study"
    const val MUSIC = "music"
    const val SUN = "sun"
    const val COFFEE = "coffee"
    const val SMOKE_FREE = "smoke_free"
    const val SAVINGS = "savings"
    const val LANGUAGE = "language"
    const val PETS = "pets"
    const val WELLNESS = "wellness"
    const val SPORTS = "sports"
    const val CODING = "coding"
    const val CREATIVE = "creative"
    const val SHOPPING = "shopping"
    const val CALL = "call"

    val quick = listOf(STAR, HEART, WATER, FITNESS, BOOK, MOON)
    val all = quick + listOf(
        WALK,
        RUN,
        BICYCLE,
        MEDITATION,
        FOOD,
        MEDICATION,
        CLEANING,
        WORK,
        STUDY,
        MUSIC,
        SUN,
        COFFEE,
        SMOKE_FREE,
        SAVINGS,
        LANGUAGE,
        PETS,
        WELLNESS,
        SPORTS,
        CODING,
        CREATIVE,
        SHOPPING,
        CALL,
    )
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
