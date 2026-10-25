package dev.vlad.app.debug

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.vlad.app.AppContainer
import dev.vlad.app.ui.theme.Spacing
import dev.vlad.core.api.randomNonce
import dev.vlad.core.crypto.B64Url
import dev.vlad.core.crypto.Der
import dev.vlad.core.crypto.EnvelopeInput
import dev.vlad.core.crypto.envelopeString

/**
 * Yalnız debug yapısı: gerçek Keystore `session` anahtarıyla örnek zarfı imzalar ve `signing-vectors.json` `envelope`
 * biçiminde bir vektör üretir (vlad reposuna elle eklenir; sunucu doğrulaması cihaz kontrol listesi maddesidir).
 * Yalnız açık anahtar ve test iletisi çıkar; özel anahtar Keystore'dan çıkmaz. `approval` anahtarı kullanıcı doğrulaması
 * ister (S9c), burada imzalanmaz.
 */
object DebugTools {
    @Composable
    fun SettingsEntry(container: AppContainer) {
        var output by remember { mutableStateOf<String?>(null) }
        val context = LocalContext.current
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Text("Hata ayıklama (yalnız debug yapısı)", style = MaterialTheme.typography.titleSmall)
            Button(
                onClick = { output = exportVector(container) },
                modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.minTouch),
            ) { Text("Gerçek Keystore imzasını dışa aktar") }
            output?.let { text ->
                SelectionContainer { Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
                OutlinedButton(
                    onClick = { copySensitive(context, text) },
                    modifier = Modifier.heightIn(min = Spacing.minTouch),
                ) { Text("Panoya kopyala") }
            }
        }
    }

    private const val FIXED_TS_MS = 1_761_350_400_000L

    /** Panoya "hassas" işaretiyle (Android 13+ önizlemede gizler). */
    private fun copySensitive(
        context: Context,
        text: String,
    ) {
        val clip = ClipData.newPlainText("vlad-vector", text)
        clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
    }

    private fun exportVector(container: AppContainer): String {
        val signer = container.sessionSigner
        // Sabit, geçmiş zaman damgası (sözleşme vektörleriyle aynı): dışa aktarılan çıktı sunucuda tekrar oynatılamaz.
        val ts = FIXED_TS_MS
        val nonce = randomNonce()
        val envelope = envelopeString(EnvelopeInput("GET", "/v1/home", tsMs = ts, nonce = nonce))
        val sig = signer.newSignature()
        sig.update(envelope.toByteArray(Charsets.UTF_8))
        val raw = sig.sign()
        val rawHighS = Der.parseStrict(raw)?.let { it.s > Der.P256_HALF_N }
        val normalized = Der.normalizeLowS(raw)
        val esc = envelope.replace("\n", "\\n")
        return buildString {
            appendLine("{")
            appendLine("  \"id\": \"device-keystore-$ts\",")
            appendLine("  \"description\": \"gerçek cihaz Keystore session imzası (raw S yüksek miydi: $rawHighS)\",")
            appendLine("  \"spki_b64u\": \"${signer.publicKeyB64u()}\",")
            appendLine(
                "  \"input\": { \"method\": \"GET\", \"path\": \"/v1/home\", \"raw_query\": \"\", " +
                    "\"ts_ms\": $ts, \"nonce\": \"$nonce\", \"body_b64u\": \"\" },",
            )
            appendLine("  \"envelope_string\": \"$esc\",")
            appendLine("  \"signature_b64u\": \"${B64Url.encode(normalized)}\",")
            appendLine("  \"expect_valid\": true")
            append("}")
        }
    }
}
