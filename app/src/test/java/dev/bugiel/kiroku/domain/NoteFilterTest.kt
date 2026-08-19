package dev.bugiel.kiroku.domain

import com.google.common.truth.Truth.assertThat
import dev.bugiel.kiroku.domain.model.Note
import org.junit.Test

class NoteFilterTest {
    @Test
    fun `pinned notes sort before newer unpinned notes`() {
        val notes = listOf(
            note(id = 1, title = "Neu", updatedAt = 300),
            note(id = 2, title = "Angeheftet alt", updatedAt = 100, pinned = true),
            note(id = 3, title = "Angeheftet neu", updatedAt = 200, pinned = true),
        )

        val result = NoteFilter.filterAndSort(notes, "")

        assertThat(result.map { it.id }).containsExactly(3L, 2L, 1L).inOrder()
    }

    @Test
    fun `search matches title and content ignoring case`() {
        val notes = listOf(
            note(id = 1, title = "Einkaufsliste", content = "Milch"),
            note(id = 2, title = "Ideen", content = "REISE nach Japan"),
            note(id = 3, title = "Arbeit", content = "Besprechung"),
        )

        assertThat(NoteFilter.filterAndSort(notes, "einkauf").map { it.id }).containsExactly(1L)
        assertThat(NoteFilter.filterAndSort(notes, "reise").map { it.id }).containsExactly(2L)
    }

    @Test
    fun `blank query returns every note`() {
        val notes = listOf(note(1L, "A"), note(2L, "B"))

        assertThat(NoteFilter.filterAndSort(notes, "   ")).hasSize(2)
    }

    private fun note(
        id: Long,
        title: String,
        content: String = "",
        updatedAt: Long = id,
        pinned: Boolean = false,
    ) = Note(
        id = id,
        title = title,
        content = content,
        createdAt = 0,
        updatedAt = updatedAt,
        isPinned = pinned,
    )
}
