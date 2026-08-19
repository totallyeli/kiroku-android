package dev.bugiel.kiroku.domain.model

data class Note(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean = false,
    val colorKey: String = NoteColorKey.NONE,
) {
    val isEmpty: Boolean
        get() = title.isBlank() && content.isBlank()
}

object NoteColorKey {
    const val NONE = "none"
    const val SAND = "sand"
    const val ROSE = "rose"
    const val SAGE = "sage"
    const val SKY = "sky"
    const val LAVENDER = "lavender"

    val all = listOf(NONE, SAND, ROSE, SAGE, SKY, LAVENDER)
}

