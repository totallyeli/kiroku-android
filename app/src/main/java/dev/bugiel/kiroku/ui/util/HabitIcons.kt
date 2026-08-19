package dev.bugiel.kiroku.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import dev.bugiel.kiroku.R
import dev.bugiel.kiroku.domain.model.HabitIconKey

fun habitIcon(key: String): ImageVector = when (key) {
    HabitIconKey.HEART -> Icons.Default.Favorite
    HabitIconKey.WATER -> Icons.Default.WaterDrop
    HabitIconKey.FITNESS -> Icons.Default.FitnessCenter
    HabitIconKey.BOOK -> Icons.AutoMirrored.Filled.MenuBook
    HabitIconKey.MOON -> Icons.Default.Bedtime
    else -> Icons.Default.Star
}

fun habitIconLabel(key: String): Int = when (key) {
    HabitIconKey.HEART -> R.string.icon_heart
    HabitIconKey.WATER -> R.string.icon_water
    HabitIconKey.FITNESS -> R.string.icon_fitness
    HabitIconKey.BOOK -> R.string.icon_book
    HabitIconKey.MOON -> R.string.icon_moon
    else -> R.string.icon_star
}

fun habitColorLabel(key: String): Int = when (key) {
    dev.bugiel.kiroku.domain.model.HabitColorKey.BLUE -> R.string.color_blue
    dev.bugiel.kiroku.domain.model.HabitColorKey.ORANGE -> R.string.color_orange
    dev.bugiel.kiroku.domain.model.HabitColorKey.PINK -> R.string.color_pink
    dev.bugiel.kiroku.domain.model.HabitColorKey.PURPLE -> R.string.color_purple
    dev.bugiel.kiroku.domain.model.HabitColorKey.TEAL -> R.string.color_teal
    else -> R.string.color_green
}
