package dev.bugiel.kiroku.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmokeFree
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import dev.bugiel.kiroku.R
import dev.bugiel.kiroku.domain.model.HabitIconKey

fun habitIcon(key: String): ImageVector = when (key) {
    HabitIconKey.HEART -> Icons.Default.Favorite
    HabitIconKey.WATER -> Icons.Default.WaterDrop
    HabitIconKey.FITNESS -> Icons.Default.FitnessCenter
    HabitIconKey.BOOK -> Icons.AutoMirrored.Filled.MenuBook
    HabitIconKey.MOON -> Icons.Default.Bedtime
    HabitIconKey.WALK -> Icons.AutoMirrored.Filled.DirectionsWalk
    HabitIconKey.RUN -> Icons.AutoMirrored.Filled.DirectionsRun
    HabitIconKey.BICYCLE -> Icons.AutoMirrored.Filled.DirectionsBike
    HabitIconKey.MEDITATION -> Icons.Default.SelfImprovement
    HabitIconKey.FOOD -> Icons.Default.Restaurant
    HabitIconKey.MEDICATION -> Icons.Default.Medication
    HabitIconKey.CLEANING -> Icons.Default.CleaningServices
    HabitIconKey.WORK -> Icons.Default.Work
    HabitIconKey.STUDY -> Icons.Default.School
    HabitIconKey.MUSIC -> Icons.Default.MusicNote
    HabitIconKey.SUN -> Icons.Default.WbSunny
    HabitIconKey.COFFEE -> Icons.Default.LocalCafe
    HabitIconKey.SMOKE_FREE -> Icons.Default.SmokeFree
    HabitIconKey.SAVINGS -> Icons.Default.Savings
    HabitIconKey.LANGUAGE -> Icons.Default.Translate
    HabitIconKey.PETS -> Icons.Default.Pets
    HabitIconKey.WELLNESS -> Icons.Default.Spa
    HabitIconKey.SPORTS -> Icons.Default.SportsSoccer
    HabitIconKey.CODING -> Icons.Default.Code
    HabitIconKey.CREATIVE -> Icons.Default.Palette
    HabitIconKey.SHOPPING -> Icons.Default.ShoppingCart
    HabitIconKey.CALL -> Icons.Default.Phone
    else -> Icons.Default.Star
}

fun habitIconLabel(key: String): Int = when (key) {
    HabitIconKey.HEART -> R.string.icon_heart
    HabitIconKey.WATER -> R.string.icon_water
    HabitIconKey.FITNESS -> R.string.icon_fitness
    HabitIconKey.BOOK -> R.string.icon_book
    HabitIconKey.MOON -> R.string.icon_moon
    HabitIconKey.WALK -> R.string.icon_walk
    HabitIconKey.RUN -> R.string.icon_run
    HabitIconKey.BICYCLE -> R.string.icon_bicycle
    HabitIconKey.MEDITATION -> R.string.icon_meditation
    HabitIconKey.FOOD -> R.string.icon_food
    HabitIconKey.MEDICATION -> R.string.icon_medication
    HabitIconKey.CLEANING -> R.string.icon_cleaning
    HabitIconKey.WORK -> R.string.icon_work
    HabitIconKey.STUDY -> R.string.icon_study
    HabitIconKey.MUSIC -> R.string.icon_music
    HabitIconKey.SUN -> R.string.icon_sun
    HabitIconKey.COFFEE -> R.string.icon_coffee
    HabitIconKey.SMOKE_FREE -> R.string.icon_smoke_free
    HabitIconKey.SAVINGS -> R.string.icon_savings
    HabitIconKey.LANGUAGE -> R.string.icon_language
    HabitIconKey.PETS -> R.string.icon_pets
    HabitIconKey.WELLNESS -> R.string.icon_wellness
    HabitIconKey.SPORTS -> R.string.icon_sports
    HabitIconKey.CODING -> R.string.icon_coding
    HabitIconKey.CREATIVE -> R.string.icon_creative
    HabitIconKey.SHOPPING -> R.string.icon_shopping
    HabitIconKey.CALL -> R.string.icon_call
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
