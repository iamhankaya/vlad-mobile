package dev.vlad.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.vlad.app.data.PairedDevice
import dev.vlad.app.ui.common.CodeChip
import dev.vlad.app.ui.theme.Spacing

/** Ayarlar (S7b'de en az): eşleşme bilgisi ve "Bu cihazdan çık". [extra] debug yapısında hata ayıklama girişidir. */
@Composable
fun SettingsScreen(
    device: PairedDevice,
    onUnpair: () -> Unit,
    modifier: Modifier = Modifier,
    extra: @Composable () -> Unit = {},
) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.padding(Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        Text("Ayarlar", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text("Sunucu", style = MaterialTheme.typography.titleSmall)
        CodeChip(device.serverUrl)
        Text("Bu cihaz", style = MaterialTheme.typography.titleSmall)
        CodeChip(device.deviceId)
        OutlinedButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.minTouch)) {
            Text("Bu cihazdan çık")
        }
        extra()
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Bu cihazdan çıkılsın mı?") },
            text = { Text("Eşleşme ve cihazdaki iki anahtar silinir. Tekrar bağlanmak için sunucudan yeni kod gerekir.") },
            confirmButton = { TextButton(onClick = onUnpair) { Text("Çık") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Vazgeç") } },
        )
    }
}
