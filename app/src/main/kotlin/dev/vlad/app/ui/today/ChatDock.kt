package dev.vlad.app.ui.today

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.vlad.app.ui.theme.ShapeFull
import dev.vlad.app.ui.theme.Spacing

const val TAG_CHAT_DOCK = "chat-dock"

/**
 * Sohbet kutusu iskeleti (DESIGN.md "kapalı" varyantı): yerleşim ve erişilebilirlik hazır, gönderim yok.
 * Gönderim S11c'de bağlanır; o zamana dek girdi ve gönder düğmesi pasif ve nedeni TalkBack'e okunur.
 */
@Composable
fun ChatDock(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth().testTag(TAG_CHAT_DOCK), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                enabled = false,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                shape = ShapeFull,
                placeholder = { Text("VLAD'a yaz — sohbet bu sürümde kapalı") },
            )
            IconButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.padding(start = Spacing.s2).semantics { contentDescription = "Gönder, pasif: sohbet bu sürümde yok" },
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            }
        }
    }
}
