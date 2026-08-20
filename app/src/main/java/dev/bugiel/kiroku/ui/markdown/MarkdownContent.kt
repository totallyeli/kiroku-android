package dev.bugiel.kiroku.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.HtmlInline
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text as MarkdownText
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser

private val markdownParser: Parser = Parser.builder().build()

@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val document = remember(markdown) { markdownParser.parse(markdown) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MarkdownChildren(document)
    }
}

@Composable
private fun MarkdownChildren(parent: Node) {
    var child = parent.firstChild
    while (child != null) {
        MarkdownBlock(child)
        child = child.next
    }
}

@Composable
private fun MarkdownBlock(node: Node) {
    when (node) {
        is Heading -> Text(
            text = inlineContent(node),
            style = when (node.level) {
                1 -> MaterialTheme.typography.headlineMedium
                2 -> MaterialTheme.typography.headlineSmall
                else -> MaterialTheme.typography.titleLarge
            },
            fontWeight = FontWeight.Bold,
        )

        is Paragraph -> Text(
            text = inlineContent(node),
            style = MaterialTheme.typography.bodyLarge,
        )

        is BulletList -> MarkdownList(node, ordered = false)
        is OrderedList -> MarkdownList(node, ordered = true)
        is BlockQuote -> Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(10.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MarkdownChildren(node)
            }
        }

        is FencedCodeBlock -> CodeBlock(node.literal)
        is IndentedCodeBlock -> CodeBlock(node.literal)
        is ThematicBreak -> HorizontalDivider()
        is HtmlBlock -> CodeBlock(node.literal)
        else -> if (node.firstChild != null) MarkdownChildren(node)
    }
}

@Composable
private fun MarkdownList(list: Node, ordered: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        var child = list.firstChild
        var index = 1
        while (child != null) {
            if (child is ListItem) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (ordered) "${index++}." else "•",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(end = 9.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        MarkdownChildren(child)
                    }
                }
            }
            child = child.next
        }
    }
}

@Composable
private fun CodeBlock(code: String) {
    Text(
        text = code.trimEnd(),
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(10.dp))
            .horizontalScroll(rememberScrollState())
            .padding(12.dp),
    )
}

@Composable
private fun inlineContent(parent: Node): AnnotatedString {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surfaceContainerHighest
    val imageLabel = stringResource(dev.bugiel.kiroku.R.string.markdown_image_label)
    return AnnotatedString.Builder().apply {
        appendInlineChildren(parent, linkColor, codeBackground, imageLabel)
    }.toAnnotatedString()
}

private fun AnnotatedString.Builder.appendInlineChildren(
    parent: Node,
    linkColor: Color,
    codeBackground: Color,
    imageLabel: String,
) {
    var child = parent.firstChild
    while (child != null) {
        when (child) {
            is MarkdownText -> append(child.literal)
            is Emphasis -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                appendInlineChildren(child, linkColor, codeBackground, imageLabel)
            }
            is StrongEmphasis -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                appendInlineChildren(child, linkColor, codeBackground, imageLabel)
            }
            is Code -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)) {
                append(child.literal)
            }
            is Link -> withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                appendInlineChildren(child, linkColor, codeBackground, imageLabel)
            }
            is Image -> {
                append("[$imageLabel: ")
                appendPlainChildren(child)
                append("]")
            }
            is SoftLineBreak -> append(' ')
            is HardLineBreak -> append('\n')
            is HtmlInline -> append(child.literal)
            else -> appendInlineChildren(child, linkColor, codeBackground, imageLabel)
        }
        child = child.next
    }
}

private fun AnnotatedString.Builder.appendPlainChildren(parent: Node) {
    var child = parent.firstChild
    while (child != null) {
        if (child is MarkdownText) append(child.literal) else appendPlainChildren(child)
        child = child.next
    }
}
