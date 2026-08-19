package dev.bugiel.kiroku.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.bugiel.kiroku.domain.model.HabitColorKey
import dev.bugiel.kiroku.domain.model.NoteColorKey

@Composable
fun noteContainerColor(key: String): Color = when (key) {
    NoteColorKey.SAND -> Color(0xFFFFE8C5)
    NoteColorKey.ROSE -> Color(0xFFFFD9E2)
    NoteColorKey.SAGE -> Color(0xFFD8EBDD)
    NoteColorKey.SKY -> Color(0xFFD6E8FA)
    NoteColorKey.LAVENDER -> Color(0xFFE8DDF5)
    else -> androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
}

fun habitColor(key: String): Color = when (key) {
    HabitColorKey.BLUE -> Color(0xFF3976B8)
    HabitColorKey.ORANGE -> Color(0xFFB86018)
    HabitColorKey.PINK -> Color(0xFFB64A70)
    HabitColorKey.PURPLE -> Color(0xFF7357A5)
    HabitColorKey.TEAL -> Color(0xFF287D78)
    else -> Color(0xFF3E7C55)
}

