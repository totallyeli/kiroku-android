package dev.bugiel.kiroku.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bugiel.kiroku.data.repository.NoteRepository
import dev.bugiel.kiroku.domain.model.Note
import dev.bugiel.kiroku.domain.model.NoteColorKey
import dev.bugiel.kiroku.domain.time.DateClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class NoteEditorState(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isPinned: Boolean = false,
    val colorKey: String = NoteColorKey.NONE,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
) {
    val isEmpty: Boolean
        get() = title.isBlank() && content.isBlank()

    fun toNote() = Note(
        id = id,
        title = title,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isPinned = isPinned,
        colorKey = colorKey,
    )
}

class NoteEditorViewModel(
    noteId: Long,
    private val repository: NoteRepository,
    private val dateClock: DateClock,
) : ViewModel() {
    private val mutableState = MutableStateFlow(NoteEditorState())
    val state: StateFlow<NoteEditorState> = mutableState.asStateFlow()
    private val saveMutex = Mutex()
    private var autosaveJob: Job? = null

    init {
        viewModelScope.launch {
            val now = dateClock.nowMillis()
            val note = noteId.takeIf { it != 0L }?.let { repository.getNote(it) }
            mutableState.value = if (note == null) {
                NoteEditorState(createdAt = now, updatedAt = now, isLoading = false)
            } else {
                NoteEditorState(
                    id = note.id,
                    title = note.title,
                    content = note.content,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                    isPinned = note.isPinned,
                    colorKey = note.colorKey,
                    isLoading = false,
                )
            }
        }
    }

    fun setTitle(value: String) = update { copy(title = value) }
    fun setContent(value: String) = update { copy(content = value) }
    fun togglePinned() = update { copy(isPinned = !isPinned) }
    fun setColor(key: String) = update { copy(colorKey = key) }

    private fun update(transform: NoteEditorState.() -> NoteEditorState) {
        if (mutableState.value.isLoading) return
        mutableState.value = mutableState.value.transform().copy(hasError = false)
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(650)
            persistIfNeeded()
        }
    }

    fun flush() {
        autosaveJob?.cancel()
        viewModelScope.launch { persistIfNeeded() }
    }

    fun finish(onFinished: () -> Unit) {
        autosaveJob?.cancel()
        viewModelScope.launch {
            val success = runCatching {
                saveMutex.withLock {
                    val snapshot = mutableState.value
                    if (snapshot.isEmpty) {
                        if (snapshot.id != 0L) repository.delete(snapshot.toNote())
                    } else {
                        persistLocked(snapshot)
                    }
                }
            }.isSuccess
            if (success) onFinished() else mutableState.value = mutableState.value.copy(hasError = true)
        }
    }

    fun delete(onDeleted: () -> Unit) {
        autosaveJob?.cancel()
        viewModelScope.launch {
            val snapshot = mutableState.value
            if (snapshot.id != 0L) repository.delete(snapshot.toNote())
            onDeleted()
        }
    }

    private suspend fun persistIfNeeded() {
        runCatching {
            saveMutex.withLock {
                val snapshot = mutableState.value
                if (!snapshot.isEmpty) persistLocked(snapshot)
            }
        }.onFailure {
            mutableState.value = mutableState.value.copy(hasError = true)
        }
    }

    private suspend fun persistLocked(snapshot: NoteEditorState) {
        val updated = snapshot.copy(updatedAt = dateClock.nowMillis(), hasError = false)
        val id = repository.save(updated.toNote())
        mutableState.value = updated.copy(id = id)
    }
}

