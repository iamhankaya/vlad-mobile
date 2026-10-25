package dev.vlad.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.vlad.app.ui.theme.Spacing

/** Üst bant ("Sunucuya ulaşılamıyor", "Sınır nedeniyle bekliyor"): ikon + metin, isteğe bağlı eylem. */
@Composable
fun Banner(
    text: String,
    icon: ImageVector,
    tone: Tone,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val c = tone.colors()
    Surface(modifier = modifier.fillMaxWidth(), color = c.container, contentColor = c.content) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2).heightIn(min = Spacing.minTouch),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(horizontal = Spacing.s2),
            )
            if (actionLabel != null && onAction != null) {
                OutlinedButton(onClick = onAction, modifier = Modifier.heightIn(min = Spacing.minTouch)) { Text(actionLabel) }
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.s6, horizontal = Spacing.s4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** "Ne oldu, ne kaybolmadı, ne yapabilirsin" (DESIGN.md ilke 5). */
@Composable
fun ErrorCard(
    title: String,
    body: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Tone.Error.colors()
    Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = c.container, contentColor = c.content) {
        Column(modifier = Modifier.padding(Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onRetry, modifier = Modifier.heightIn(min = Spacing.minTouch)) { Text(retryLabel) }
        }
    }
}

/** İlk yükleme iskeleti; ekran okuyucuya tek açıklama. */
@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 120.dp).semantics { contentDescription = "Yükleniyor" },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {}
}

@Composable
fun CodeChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = dev.vlad.app.ui.theme.LocalVladColors.current.codeSurface,
    ) {
        Text(
            text,
            style = dev.vlad.app.ui.theme.MonoMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = Spacing.s2, vertical = Spacing.s1),
        )
    }
}
