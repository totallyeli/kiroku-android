package dev.bugiel.kiroku.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF406650),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC2ECD0),
    onPrimaryContainer = Color(0xFF002111),
    secondary = Color(0xFF506356),
    tertiary = Color(0xFF3A656C),
    surface = Color(0xFFFFF8F5),
    surfaceVariant = Color(0xFFDDE5DD),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA6D0B5),
    onPrimary = Color(0xFF123724),
    primaryContainer = Color(0xFF294E39),
    onPrimaryContainer = Color(0xFFC2ECD0),
    secondary = Color(0xFFB7CCBC),
    tertiary = Color(0xFFA2CED5),
    surface = Color(0xFF181C19),
    surfaceVariant = Color(0xFF414942),
)

@Composable
fun KirokuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = KirokuTypography,
        content = content,
    )
}

