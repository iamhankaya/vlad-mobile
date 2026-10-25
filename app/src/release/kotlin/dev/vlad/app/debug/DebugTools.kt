package dev.vlad.app.debug

import androidx.compose.runtime.Composable
import dev.vlad.app.AppContainer

/** Release yapısında hata ayıklama araçları yoktur (debug kaynak kümesinde gerçek uygulama var). */
object DebugTools {
    @Composable
    @Suppress("UNUSED_PARAMETER")
    fun SettingsEntry(container: AppContainer) = Unit
}
