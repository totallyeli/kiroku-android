package dev.bugiel.kiroku.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppVersionTest {
    @Test
    fun `parses release tag`() {
        assertThat(AppVersion.parse("v1.2.3")).isEqualTo(AppVersion(1, 2, 3))
    }

    @Test
    fun `compares semantic version components numerically`() {
        assertThat(AppVersion(1, 10, 0)).isGreaterThan(AppVersion(1, 2, 9))
        assertThat(AppVersion(2, 0, 0)).isGreaterThan(AppVersion(1, 99, 99))
    }

    @Test
    fun `rejects incomplete version`() {
        assertThat(AppVersion.parse("1.2")).isNull()
    }
}
