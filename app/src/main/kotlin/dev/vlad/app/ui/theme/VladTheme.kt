package dev.vlad.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/** Dinamik renk kapalı (`dynamicColor = false`); sistem açık/koyu temasını izler. */
@Composable
fun VladTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalVladColors provides if (darkTheme) DarkVladColors else LightVladColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = VladTypography,
            shapes = VladShapes,
            content = content,
        )
    }
}
