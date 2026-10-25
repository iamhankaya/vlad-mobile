package dev.vlad.app.ui.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.vlad.app.ui.theme.Spacing

/**
 * Eşleştirme (DESIGN.md §13): sunucu adresi + kod. Adres kullanıcıdan alınır; uygulamada sabit adres yok.
 * QR taraması bu dilimde yok (bkz. rapor açık noktaları): sunucu kodu terminalde gösterir, elle yazılır.
 */
@Composable
fun PairingScreen(
    state: PairingUiState,
    onAddress: (String) -> Unit,
    onCode: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    notice: String? = null,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = Spacing.contentMaxWidth).verticalScroll(rememberScrollState()).padding(Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            Text("VLAD'a bağlan", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text(
                "Sunucuda `vlad-server pair` çalıştır; çıkan kodu ve sunucu adresini buraya yaz. " +
                    "Telefon kendi anahtarını üretir; cihaz sunucudan iptal edilebilir.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.address,
                onValueChange = onAddress,
                label = { Text("Sunucu adresi") },
                placeholder = { Text("https://…") },
                singleLine = true,
                enabled = !state.busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.code,
                onValueChange = onCode,
                label = { Text("Eşleştirme kodu") },
                singleLine = true,
                enabled = !state.busy,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            notice?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
            state.error?.let {
                Text(it.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            Button(onClick = onSubmit, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.minTouch)) {
                if (state.busy) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = Spacing.s2).heightIn(max = 20.dp), strokeWidth = 2.dp)
                    Text("Eşleşiyor…")
                } else {
                    Text("Eşleştir")
                }
            }
        }
    }
}
