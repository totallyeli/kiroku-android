package dev.bugiel.kiroku.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bugiel.kiroku.data.repository.NoteRepository
import dev.bugiel.kiroku.domain.model.Note
import dev.bugiel.kiroku.domain.time.DateClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(
    private val repository: NoteRepository,
    private val dateClock: DateClock,
) : ViewModel() {
    val query = MutableStateFlow("")

    val notes: StateFlow<List<Note>> = query
        .flatMapLatest(repository::observeNotes)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        query.value = value
    }

    fun togglePinned(note: Note) {
        viewModelScope.launch {
            repository.setPinned(note.id, !note.isPinned, dateClock.nowMillis())
        }
    }
}

