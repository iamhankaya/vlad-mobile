package dev.vlad.app.ui.today

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.vlad.core.repo.HomeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

private const val POLL_MS = 30_000L

@Composable
fun TodayRoute(
    repository: HomeRepository,
    onRepair: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: TodayViewModel = viewModel(factory = viewModelFactory { initializer { TodayViewModel(repository) } })
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // SSE bu dilimde yok: ekran görünürken 30 sn'de bir yenile.
    LaunchedEffect(vm, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                vm.refresh()
                delay(POLL_MS)
            }
        }
    }
    TodayScreen(
        snapshot = snapshot,
        onRetry = { scope.launch { vm.refresh() } },
        onRepair = onRepair,
        modifier = modifier,
        formatTime = { DateFormat.getTimeFormat(context).format(Date(it)) },
    )
}
