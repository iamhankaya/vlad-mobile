package dev.vlad.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.vlad.app.data.PairedDevice
import dev.vlad.app.debug.DebugTools
import dev.vlad.app.ui.pairing.PairingScreen
import dev.vlad.app.ui.pairing.PairingViewModel
import dev.vlad.app.ui.settings.SettingsScreen
import dev.vlad.app.ui.theme.VladTheme
import dev.vlad.app.ui.today.TodayRoute

private enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("Bugün", Icons.Filled.Home),
    PROJECTS("Projeler", Icons.Filled.Build),
    MEMORY("Hafıza", Icons.Filled.Search),
    SETTINGS("Ayarlar", Icons.Filled.Settings),
}

/** Eşleşmemiş cihaz yalnız eşleştirme ekranını görür (DESIGN.md §13). */
@Composable
fun AppRoot(container: AppContainer) {
    VladTheme {
        var device by remember { mutableStateOf(container.store.load()) }
        val paired = device
        if (paired == null) {
            val vm: PairingViewModel = viewModel(factory = viewModelFactory { initializer { PairingViewModel(container) { device = it } } })
            val state by vm.state.collectAsStateWithLifecycle()
            PairingScreen(state, vm::setAddress, vm::setCode, vm::submit, Modifier.fillMaxSize().safeDrawingPadding())
        } else {
            MainScaffold(container, paired) {
                container.unpair()
                device = null
            }
        }
    }
}

@Composable
private fun MainScaffold(
    container: AppContainer,
    device: PairedDevice,
    onUnpair: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        val mod = Modifier.padding(padding).fillMaxSize()
        val repository = remember(device) { container.homeRepository(device) }
        when (tab) {
            Tab.TODAY -> TodayRoute(repository, onRepair = onUnpair, modifier = mod)
            Tab.SETTINGS -> SettingsScreen(device, onUnpair, mod) { DebugTools.SettingsEntry(container) }
            else ->
                Box(mod, contentAlignment = Alignment.Center) {
                    Text("Bu bölüm sonraki sürümde.")
                }
        }
    }
}
