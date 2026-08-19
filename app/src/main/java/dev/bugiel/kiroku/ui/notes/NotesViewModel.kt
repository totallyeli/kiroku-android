package dev.bugiel.kiroku.ui.notes

import android.net.Uri
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

sealed interface NoteImportState {
    data object Idle : NoteImportState
    data object Importing : NoteImportState
    data class Finished(val imported: Int, val failed: Int) : NoteImportState
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(
    private val repository: NoteRepository,
    private val dateClock: DateClock,
) : ViewModel() {
    val query = MutableStateFlow("")
    val importState = MutableStateFlow<NoteImportState>(NoteImportState.Idle)

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

    fun importNotes(uris: List<Uri>) {
        if (uris.isEmpty() || importState.value is NoteImportState.Importing) return
        viewModelScope.launch {
            importState.value = NoteImportState.Importing
            val result = repository.importTextDocuments(uris, dateClock.nowMillis())
            importState.value = NoteImportState.Finished(result.imported, result.failed)
        }
    }

    fun consumeImportResult() {
        importState.value = NoteImportState.Idle
    }
}
