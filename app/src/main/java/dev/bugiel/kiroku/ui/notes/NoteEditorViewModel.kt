package dev.bugiel.kiroku.ui.notes

import android.net.Uri
import dev.bugiel.kiroku.data.repository.AttachmentRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bugiel.kiroku.data.repository.NoteRepository
import dev.bugiel.kiroku.domain.model.Note
import dev.bugiel.kiroku.domain.model.NoteColorKey
import dev.bugiel.kiroku.domain.model.NoteAttachment
import dev.bugiel.kiroku.domain.time.DateClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val attachments: List<NoteAttachment> = emptyList(),
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
) {
    val isEmpty: Boolean
        get() = title.isBlank() && content.isBlank() && attachments.isEmpty()

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
    private val attachmentRepository: AttachmentRepository,
    private val dateClock: DateClock,
) : ViewModel() {
    private val mutableState = MutableStateFlow(NoteEditorState())
    val state: StateFlow<NoteEditorState> = mutableState.asStateFlow()
    private val saveMutex = Mutex()
    private var autosaveJob: Job? = null
    private var attachmentJob: Job? = null

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
            if (note != null) observeAttachments(note.id)
        }
    }

    fun setTitle(value: String) = update { copy(title = value) }
    fun setContent(value: String) = update { copy(content = value) }
    fun togglePinned() = update { copy(isPinned = !isPinned) }
    fun setColor(key: String) = update { copy(colorKey = key) }

    fun addAttachments(uris: List<Uri>) {
        if (uris.isEmpty()) return
        autosaveJob?.cancel()
        viewModelScope.launch {
            runCatching {
                val noteId = saveMutex.withLock {
                    val snapshot = mutableState.value
                    if (snapshot.id == 0L) persistLocked(snapshot) else snapshot.id
                }
                uris.forEachIndexed { index, uri ->
                    attachmentRepository.add(noteId, uri, dateClock.nowMillis() + index)
                }
            }.onFailure {
                mutableState.update { it.copy(hasError = true) }
            }
        }
    }

    fun deleteAttachment(attachment: NoteAttachment) {
        viewModelScope.launch {
            runCatching { attachmentRepository.delete(attachment) }
                .onFailure { mutableState.update { it.copy(hasError = true) } }
        }
    }

    fun exportAttachment(attachment: NoteAttachment, destination: Uri) {
        viewModelScope.launch {
            runCatching { attachmentRepository.export(attachment, destination) }
                .onFailure { mutableState.update { it.copy(hasError = true) } }
        }
    }

    fun fileFor(attachment: NoteAttachment) = attachmentRepository.fileFor(attachment)

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
                        if (snapshot.id != 0L) {
                            repository.delete(snapshot.toNote())
                            attachmentRepository.deleteStoredFiles(snapshot.attachments)
                        }
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
            if (snapshot.id != 0L) {
                repository.delete(snapshot.toNote())
                attachmentRepository.deleteStoredFiles(snapshot.attachments)
            }
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

    private suspend fun persistLocked(snapshot: NoteEditorState): Long {
        val updated = snapshot.copy(updatedAt = dateClock.nowMillis(), hasError = false)
        val id = repository.save(updated.toNote())
        mutableState.value = updated.copy(id = id)
        if (snapshot.id == 0L) observeAttachments(id)
        return id
    }

    private fun observeAttachments(noteId: Long) {
        attachmentJob?.cancel()
        attachmentJob = viewModelScope.launch {
            attachmentRepository.observeForNote(noteId).collect { attachments ->
                mutableState.update { it.copy(attachments = attachments) }
            }
        }
    }
}
