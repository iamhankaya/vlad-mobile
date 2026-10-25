package dev.vlad.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// docs/design/mobile/tokens.md (vlad reposu) §Renk. Dinamik renk kapalı: durum renkleri duvar kağıdına göre kaymaz.

val LightColors: ColorScheme =
    lightColorScheme(
        primary = Color(0xFF0B6E6E),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFC6EFEC),
        onPrimaryContainer = Color(0xFF00201F),
        secondary = Color(0xFF4A6361),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCCE8E5),
        onSecondaryContainer = Color(0xFF05201F),
        tertiary = Color(0xFF7A5200),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDEA8),
        onTertiaryContainer = Color(0xFF271700),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
        background = Color(0xFFF6FAF9),
        onBackground = Color(0xFF161D1C),
        surface = Color(0xFFF6FAF9),
        onSurface = Color(0xFF161D1C),
        surfaceVariant = Color(0xFFDAE5E3),
        onSurfaceVariant = Color(0xFF3F4947),
        surfaceContainerLow = Color(0xFFF0F5F4),
        surfaceContainer = Color(0xFFEAF0EE),
        surfaceContainerHigh = Color(0xFFE4EAE9),
        outline = Color(0xFF6F7977),
        outlineVariant = Color(0xFFBEC9C7),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF2B3231),
        inverseOnSurface = Color(0xFFECF2F0),
    )

val DarkColors: ColorScheme =
    darkColorScheme(
        primary = Color(0xFF5FD3D0),
        onPrimary = Color(0xFF00201F),
        primaryContainer = Color(0xFF00504F),
        onPrimaryContainer = Color(0xFFC6EFEC),
        secondary = Color(0xFFB1CCC9),
        onSecondary = Color(0xFF1C3533),
        secondaryContainer = Color(0xFF324B49),
        onSecondaryContainer = Color(0xFFCCE8E5),
        tertiary = Color(0xFFF5BC5C),
        onTertiary = Color(0xFF432C00),
        tertiaryContainer = Color(0xFF5E3F00),
        onTertiaryContainer = Color(0xFFFFDEA8),
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFF9DEDC),
        background = Color(0xFF0E1514),
        onBackground = Color(0xFFDDE4E2),
        surface = Color(0xFF0E1514),
        onSurface = Color(0xFFDDE4E2),
        surfaceVariant = Color(0xFF3F4947),
        onSurfaceVariant = Color(0xFFBEC9C7),
        surfaceContainerLow = Color(0xFF161D1C),
        surfaceContainer = Color(0xFF1A2120),
        surfaceContainerHigh = Color(0xFF242B2A),
        outline = Color(0xFF899391),
        outlineVariant = Color(0xFF3F4947),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFDDE4E2),
        inverseOnSurface = Color(0xFF2B3231),
    )

/** M3 rollerinde olmayan özel renkler. */
data class VladColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val codeSurface: Color,
)

val LightVladColors =
    VladColors(
        success = Color(0xFF1B6B3A),
        onSuccess = Color(0xFFFFFFFF),
        successContainer = Color(0xFFCDEFD6),
        onSuccessContainer = Color(0xFF00210F),
        codeSurface = Color(0xFFE4EAE9),
    )

val DarkVladColors =
    VladColors(
        success = Color(0xFF7FD99B),
        onSuccess = Color(0xFF00391C),
        successContainer = Color(0xFF0F4F29),
        onSuccessContainer = Color(0xFFCDEFD6),
        codeSurface = Color(0xFF1F2625),
    )

val LocalVladColors = staticCompositionLocalOf { LightVladColors }
