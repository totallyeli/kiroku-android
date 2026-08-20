package dev.bugiel.kiroku.ui.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

enum class MarkdownFormatAction {
    BOLD,
    ITALIC,
    HEADING,
    BULLET_LIST,
    TASK_LIST,
    QUOTE,
    CODE,
    LINK,
}

fun applyMarkdownFormat(
    value: TextFieldValue,
    action: MarkdownFormatAction,
    textPlaceholder: String = "Text",
    codePlaceholder: String = "Code",
    linkPlaceholder: String = "Link text",
): TextFieldValue = when (action) {
    MarkdownFormatAction.BOLD -> wrap(value, "**", "**", textPlaceholder)
    MarkdownFormatAction.ITALIC -> wrap(value, "*", "*", textPlaceholder)
    MarkdownFormatAction.CODE -> wrap(value, "`", "`", codePlaceholder)
    MarkdownFormatAction.HEADING -> prefixCurrentLine(value, "# ")
    MarkdownFormatAction.BULLET_LIST -> prefixCurrentLine(value, "- ")
    MarkdownFormatAction.TASK_LIST -> prefixCurrentLine(value, "- [ ] ")
    MarkdownFormatAction.QUOTE -> prefixCurrentLine(value, "> ")
    MarkdownFormatAction.LINK -> link(value, linkPlaceholder)
}

private fun wrap(value: TextFieldValue, prefix: String, suffix: String, placeholder: String): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    val selected = value.text.substring(start, end).ifEmpty { placeholder }
    val inserted = prefix + selected + suffix
    val updated = value.text.replaceRange(start, end, inserted)
    val selection = if (start == end) {
        TextRange(start + prefix.length, start + prefix.length + selected.length)
    } else {
        TextRange(start + inserted.length)
    }
    return value.copy(text = updated, selection = selection)
}

private fun prefixCurrentLine(value: TextFieldValue, prefix: String): TextFieldValue {
    val cursor = value.selection.min
    val lineStart = value.text.lastIndexOf('\n', startIndex = (cursor - 1).coerceAtLeast(0))
        .let { if (it < 0) 0 else it + 1 }
    val updated = value.text.substring(0, lineStart) + prefix + value.text.substring(lineStart)
    return value.copy(
        text = updated,
        selection = TextRange(value.selection.start + prefix.length, value.selection.end + prefix.length),
    )
}

private fun link(value: TextFieldValue, placeholder: String): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    val label = value.text.substring(start, end).ifEmpty { placeholder }
    val url = "https://"
    val inserted = "[$label]($url)"
    return value.copy(
        text = value.text.replaceRange(start, end, inserted),
        selection = TextRange(start + label.length + 3, start + label.length + 3 + url.length),
    )
}
