package dev.bugiel.kiroku.domain

import dev.bugiel.kiroku.domain.model.Note

object NoteFilter {
    fun filterAndSort(notes: Iterable<Note>, query: String): List<Note> {
        val normalizedQuery = query.trim()
        return notes
            .asSequence()
            .filter { note ->
                normalizedQuery.isEmpty() ||
                    note.title.contains(normalizedQuery, ignoreCase = true) ||
                    note.content.contains(normalizedQuery, ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<Note> { it.isPinned }
                    .thenByDescending { it.updatedAt }
                    .thenByDescending { it.id },
            )
            .toList()
    }
}

