package dev.bugiel.kiroku.ui.notes

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bugiel.kiroku.R
import dev.bugiel.kiroku.domain.model.NoteAttachment
import dev.bugiel.kiroku.ui.markdown.MarkdownFormatAction
import dev.bugiel.kiroku.ui.markdown.applyMarkdownFormat
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class NoteEditorMode { EDIT, PREVIEW }

@Composable
fun NoteModeSelector(
    mode: NoteEditorMode,
    onModeChange: (NoteEditorMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = mode == NoteEditorMode.EDIT,
            onClick = { onModeChange(NoteEditorMode.EDIT) },
            label = { Text(stringResource(R.string.markdown_edit)) },
        )
        FilterChip(
            selected = mode == NoteEditorMode.PREVIEW,
            onClick = { onModeChange(NoteEditorMode.PREVIEW) },
            label = { Text(stringResource(R.string.markdown_preview)) },
        )
    }
}

@Composable
fun MarkdownToolbar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textPlaceholder = stringResource(R.string.markdown_text_placeholder)
    val codePlaceholder = stringResource(R.string.markdown_code_placeholder)
    val linkPlaceholder = stringResource(R.string.markdown_link_placeholder)
    val actions = listOf(
        Triple(MarkdownFormatAction.BOLD, Icons.Default.FormatBold, R.string.format_bold),
        Triple(MarkdownFormatAction.ITALIC, Icons.Default.FormatItalic, R.string.format_italic),
        Triple(MarkdownFormatAction.HEADING, Icons.Default.Title, R.string.format_heading),
        Triple(MarkdownFormatAction.BULLET_LIST, Icons.AutoMirrored.Filled.FormatListBulleted, R.string.format_list),
        Triple(MarkdownFormatAction.TASK_LIST, Icons.Default.CheckBox, R.string.format_task),
        Triple(MarkdownFormatAction.QUOTE, Icons.Default.FormatQuote, R.string.format_quote),
        Triple(MarkdownFormatAction.CODE, Icons.Default.Code, R.string.format_code),
        Triple(MarkdownFormatAction.LINK, Icons.Default.Link, R.string.format_link),
    )
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(actions, key = { it.first.name }) { (action, icon, description) ->
            IconButton(onClick = {
                onValueChange(
                    applyMarkdownFormat(
                        value = value,
                        action = action,
                        textPlaceholder = textPlaceholder,
                        codePlaceholder = codePlaceholder,
                        linkPlaceholder = linkPlaceholder,
                    ),
                )
            }) {
                Icon(icon, contentDescription = stringResource(description))
            }
        }
    }
}

@Composable
fun AttachmentsSection(
    attachments: List<NoteAttachment>,
    fileFor: (NoteAttachment) -> File,
    onOpen: (NoteAttachment) -> Unit,
    onExport: (NoteAttachment) -> Unit,
    onDelete: (NoteAttachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (attachments.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(R.string.attachments),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        attachments.forEach { attachment ->
            Card(onClick = { onOpen(attachment) }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AttachmentThumbnail(attachment, fileFor(attachment))
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(
                            text = attachment.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = formatFileSize(attachment.sizeBytes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onExport(attachment) }) {
                        Icon(Icons.Default.Download, contentDescription = stringResource(R.string.export_attachment))
                    }
                    IconButton(onClick = { onDelete(attachment) }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_attachment))
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentThumbnail(attachment: NoteAttachment, file: File) {
    if (attachment.isImage) {
        val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, file) {
            value = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = 8 })
            }
        }
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                contentScale = ContentScale.Crop,
            )
        } ?: Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(32.dp))
    } else {
        Icon(
            imageVector = if (attachment.isPdf) Icons.Default.PictureAsPdf else Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1024L * 1024 -> "%.1f MB".format(bytes / (1024f * 1024f))
    bytes >= 1024L -> "%.1f KB".format(bytes / 1024f)
    else -> "$bytes B"
}
