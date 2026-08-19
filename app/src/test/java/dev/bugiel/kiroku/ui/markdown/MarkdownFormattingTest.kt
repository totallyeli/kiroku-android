package dev.bugiel.kiroku.ui.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MarkdownFormattingTest {
    @Test
    fun `bold wraps selected text`() {
        val value = TextFieldValue("Hallo Welt", selection = TextRange(6, 10))

        val result = applyMarkdownFormat(value, MarkdownFormatAction.BOLD)

        assertThat(result.text).isEqualTo("Hallo **Welt**")
    }

    @Test
    fun `task list prefixes current line`() {
        val value = TextFieldValue("Erste\nZweite", selection = TextRange(9))

        val result = applyMarkdownFormat(value, MarkdownFormatAction.TASK_LIST)

        assertThat(result.text).isEqualTo("Erste\n- [ ] Zweite")
    }

    @Test
    fun `link selects inserted url`() {
        val value = TextFieldValue("Kiroku", selection = TextRange(0, 6))

        val result = applyMarkdownFormat(value, MarkdownFormatAction.LINK)

        assertThat(result.text).isEqualTo("[Kiroku](https://)")
        assertThat(result.text.substring(result.selection.min, result.selection.max)).isEqualTo("https://")
    }
}
