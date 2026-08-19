package dev.bugiel.kiroku.ui.notes

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.createBitmap
import dev.bugiel.kiroku.R
import dev.bugiel.kiroku.domain.model.NoteAttachment
import dev.bugiel.kiroku.ui.markdown.MarkdownContent
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentViewer(
    attachment: NoteAttachment,
    file: File,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(attachment.displayName, maxLines = 1) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.close),
                                )
                            }
                        },
                    )
                },
            ) { padding ->
                when {
                    attachment.isImage -> ImageViewer(file, Modifier.padding(padding))
                    attachment.isPdf -> PdfViewer(file, Modifier.padding(padding))
                    attachment.isMarkdown -> MarkdownFileViewer(file, Modifier.padding(padding))
                    else -> ViewerError(Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun ImageViewer(file: File, modifier: Modifier = Modifier) {
    val bitmap by produceState<Bitmap?>(initialValue = null, file) {
        value = withContext(Dispatchers.IO) { decodeScaledBitmap(file, 1800) }
    }
    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(12.dp),
                contentScale = ContentScale.Fit,
            )
        } ?: CircularProgressIndicator()
    }
}

private sealed interface PdfPageState {
    data object Loading : PdfPageState
    data class Loaded(val bitmap: Bitmap, val pageCount: Int) : PdfPageState
    data object Failed : PdfPageState
}

@Composable
private fun PdfViewer(file: File, modifier: Modifier = Modifier) {
    var pageIndex by remember(file) { mutableIntStateOf(0) }
    val pageState by produceState<PdfPageState>(PdfPageState.Loading, file, pageIndex) {
        value = withContext(Dispatchers.IO) { renderPdfPage(file, pageIndex) }
    }
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            when (val state = pageState) {
                PdfPageState.Loading -> CircularProgressIndicator()
                PdfPageState.Failed -> ViewerError()
                is PdfPageState.Loaded -> Image(
                    bitmap = state.bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        val pageCount = (pageState as? PdfPageState.Loaded)?.pageCount ?: 0
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { pageIndex-- }, enabled = pageIndex > 0) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous_page))
            }
            Text(
                text = if (pageCount > 0) stringResource(R.string.pdf_page, pageIndex + 1, pageCount) else "—",
            )
            IconButton(onClick = { pageIndex++ }, enabled = pageCount > 0 && pageIndex + 1 < pageCount) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_page))
            }
        }
    }
}

@Composable
private fun MarkdownFileViewer(file: File, modifier: Modifier = Modifier) {
    val markdown by produceState<String?>(initialValue = null, file) {
        value = withContext(Dispatchers.IO) {
            runCatching { readMarkdown(file) }
                .getOrNull()
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        markdown?.let {
            MarkdownContent(
                markdown = it,
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            )
        } ?: CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}

private fun readMarkdown(file: File): String {
    file.inputStream().buffered().use { input ->
        val output = ByteArrayOutputStream(DEFAULT_BUFFER_SIZE)
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= MAX_MARKDOWN_VIEW_BYTES) { "Die Markdown-Datei ist zu groß." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray().toString(Charsets.UTF_8).removePrefix("\uFEFF")
    }
}

@Composable
private fun ViewerError(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.attachment_open_error),
            color = MaterialTheme.colorScheme.error,
        )
    }
}

private fun decodeScaledBitmap(file: File, targetSize: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    var sampleSize = 1
    while (bounds.outWidth / sampleSize > targetSize || bounds.outHeight / sampleSize > targetSize) {
        sampleSize *= 2
    }
    return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sampleSize })
}

private fun renderPdfPage(file: File, pageIndex: Int): PdfPageState = runCatching {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            if (pageIndex !in 0 until renderer.pageCount) return PdfPageState.Failed
            renderer.openPage(pageIndex).use { page ->
                val scale = minOf(1f, TARGET_PDF_WIDTH.toFloat() / page.width)
                val width = maxOf(1, (page.width * scale).toInt())
                val height = maxOf(1, (page.height * scale).toInt())
                val bitmap = createBitmap(width, height)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                PdfPageState.Loaded(bitmap, renderer.pageCount)
            }
        }
    }
}.getOrElse { PdfPageState.Failed }

private const val TARGET_PDF_WIDTH = 1440
private const val MAX_MARKDOWN_VIEW_BYTES = 5 * 1024 * 1024
