package dev.vlad.app.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.vlad.app.ui.theme.LocalVladColors
import dev.vlad.app.ui.theme.ShapeFull
import dev.vlad.app.ui.theme.Spacing

/** Durum rengi rolü (tokens.md "Durum → renk eşlemesi"). Renk tek başına anlam taşımaz: etiket her zaman ikon + metin. */
enum class Tone { Primary, Tertiary, Error, Success, Neutral }

data class ToneColors(val container: Color, val content: Color, val accent: Color)

@Composable
fun Tone.colors(): ToneColors {
    val m = MaterialTheme.colorScheme
    val v = LocalVladColors.current
    return when (this) {
        Tone.Primary -> ToneColors(m.primaryContainer, m.onPrimaryContainer, m.primary)
        Tone.Tertiary -> ToneColors(m.tertiaryContainer, m.onTertiaryContainer, m.tertiary)
        Tone.Error -> ToneColors(m.errorContainer, m.onErrorContainer, m.error)
        Tone.Success -> ToneColors(v.successContainer, v.onSuccessContainer, v.success)
        Tone.Neutral -> ToneColors(m.surfaceVariant, m.onSurfaceVariant, m.outline)
    }
}

/** Yalnız bilgilendirici etiket (dokunulmaz): ikon + metin, 24 dp görsel yükseklik. */
@Composable
fun StatusTag(
    label: String,
    icon: ImageVector,
    tone: Tone,
    modifier: Modifier = Modifier,
) {
    val c = tone.colors()
    Surface(modifier = modifier, shape = ShapeFull, color = c.container, contentColor = c.content) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.s2, vertical = Spacing.s1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = Spacing.s1))
        }
    }
}
