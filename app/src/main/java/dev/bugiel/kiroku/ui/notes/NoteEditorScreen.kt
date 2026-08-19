package dev.bugiel.kiroku.ui.notes

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bugiel.kiroku.R
import dev.bugiel.kiroku.domain.model.NoteAttachment
import dev.bugiel.kiroku.domain.model.NoteColorKey
import dev.bugiel.kiroku.ui.markdown.MarkdownContent
import dev.bugiel.kiroku.ui.util.formatLongDate
import dev.bugiel.kiroku.ui.util.noteContainerColor
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var attachmentToDelete by remember { mutableStateOf<NoteAttachment?>(null) }
    var attachmentToView by remember { mutableStateOf<NoteAttachment?>(null) }
    var attachmentToExport by remember { mutableStateOf<NoteAttachment?>(null) }
    val saveError = stringResource(R.string.save_error)

    val attachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
        viewModel.addAttachments(it)
    }
    val attachmentExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val attachment = attachmentToExport
        attachmentToExport = null
        if (uri != null && attachment != null) viewModel.exportAttachment(attachment, uri)
    }

    LaunchedEffect(state.hasError) {
        if (state.hasError) snackbarHostState.showSnackbar(saveError)
    }
    DisposableEffect(viewModel) {
        onDispose { viewModel.flush() }
    }

    val leave = { viewModel.finish(onNavigateBack) }
    BackHandler(enabled = !state.isLoading, onBack = leave)

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_note_title)) },
            text = { Text(stringResource(R.string.delete_note_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    viewModel.delete(onNavigateBack)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    attachmentToDelete?.let { attachment ->
        AlertDialog(
            onDismissRequest = { attachmentToDelete = null },
            title = { Text(stringResource(R.string.delete_attachment_title)) },
            text = { Text(stringResource(R.string.delete_attachment_message, attachment.displayName)) },
            confirmButton = {
                TextButton(onClick = {
                    attachmentToDelete = null
                    viewModel.deleteAttachment(attachment)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { attachmentToDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    attachmentToView?.let { attachment ->
        AttachmentViewer(
            attachment = attachment,
            file = viewModel.fileFor(attachment),
            onDismiss = { attachmentToView = null },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = leave) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            attachmentPicker.launch(
                                arrayOf("image/*", "application/pdf", "text/markdown", "text/plain"),
                            )
                        },
                        enabled = !state.isLoading,
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = stringResource(R.string.attach_document))
                    }
                    IconButton(onClick = viewModel::togglePinned, enabled = !state.isLoading) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = stringResource(
                                if (state.isPinned) R.string.unpin_note else R.string.pin_note,
                            ),
                            tint = if (state.isPinned) MaterialTheme.colorScheme.primary else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    if (state.id != 0L) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_note))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = noteContainerColor(state.colorKey)),
            )
        },
        containerColor = noteContainerColor(state.colorKey),
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            NoteEditorContent(
                state = state,
                onTitleChange = viewModel::setTitle,
                onContentChange = viewModel::setContent,
                onColorChange = viewModel::setColor,
                fileFor = viewModel::fileFor,
                onOpenAttachment = { attachmentToView = it },
                onExportAttachment = {
                    attachmentToExport = it
                    attachmentExporter.launch(it.displayName)
                },
                onDeleteAttachment = { attachmentToDelete = it },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun NoteEditorContent(
    state: NoteEditorState,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    fileFor: (NoteAttachment) -> java.io.File,
    onOpenAttachment: (NoteAttachment) -> Unit,
    onExportAttachment: (NoteAttachment) -> Unit,
    onDeleteAttachment: (NoteAttachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val creationDate = Instant.ofEpochMilli(state.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
    var editorMode by rememberSaveable { mutableStateOf(NoteEditorMode.EDIT) }
    var editorValue by remember(state.id) {
        mutableStateOf(TextFieldValue(state.content, selection = TextRange(state.content.length)))
    }

    LaunchedEffect(state.content) {
        if (editorValue.text != state.content) {
            editorValue = editorValue.copy(
                text = state.content,
                selection = TextRange(editorValue.selection.end.coerceAtMost(state.content.length)),
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = stringResource(R.string.created_on, formatLongDate(context, creationDate)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 14.dp),
        )
        OutlinedTextField(
            value = state.title,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.note_title_hint)) },
            textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            singleLine = false,
            maxLines = 3,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Next),
            shape = MaterialTheme.shapes.large,
        )
        NoteModeSelector(
            mode = editorMode,
            onModeChange = { editorMode = it },
            modifier = Modifier.padding(top = 12.dp),
        )
        if (editorMode == NoteEditorMode.EDIT) {
            MarkdownToolbar(
                value = editorValue,
                onValueChange = { formatted ->
                    editorValue = formatted
                    onContentChange(formatted.text)
                },
                modifier = Modifier.padding(top = 4.dp),
            )
            OutlinedTextField(
                value = editorValue,
                onValueChange = {
                    editorValue = it
                    onContentChange(it.text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.note_content_hint)) },
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = MaterialTheme.shapes.large,
            )
        } else {
            if (state.content.isBlank()) {
                Text(
                    text = stringResource(R.string.markdown_empty_preview),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp).padding(top = 20.dp),
                )
            } else {
                MarkdownContent(
                    markdown = state.content,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp).padding(vertical = 16.dp),
                )
            }
        }

        AttachmentsSection(
            attachments = state.attachments,
            fileFor = fileFor,
            onOpen = onOpenAttachment,
            onExport = onExportAttachment,
            onDelete = onDeleteAttachment,
            modifier = Modifier.padding(top = 18.dp),
        )
        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Text(
            text = stringResource(R.string.note_color),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 18.dp, bottom = 12.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            NoteColorKey.all.forEach { key ->
                val description = when (key) {
                    NoteColorKey.SAND -> R.string.color_sand
                    NoteColorKey.ROSE -> R.string.color_rose
                    NoteColorKey.SAGE -> R.string.color_sage
                    NoteColorKey.SKY -> R.string.color_sky
                    NoteColorKey.LAVENDER -> R.string.color_lavender
                    else -> R.string.no_color
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(noteContainerColor(key))
                        .then(
                            if (state.colorKey == key) {
                                Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            } else {
                                Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            },
                        )
                        .clickable(
                            onClickLabel = stringResource(description),
                            onClick = { onColorChange(key) },
                        ),
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
