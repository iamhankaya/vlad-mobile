package dev.vlad.app.ui.today

import androidx.lifecycle.ViewModel
import dev.vlad.core.repo.HomeRepository
import dev.vlad.core.repo.TodaySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class TodayViewModel(private val repo: HomeRepository) : ViewModel() {
    private val _snapshot = MutableStateFlow(repo.current())
    val snapshot: StateFlow<TodaySnapshot> = _snapshot.asStateFlow()

    /** Ağ çağrısı IO'da; iptal edilirse (ekran durdu) görüntü eskisinde kalır. */
    suspend fun refresh() {
        _snapshot.value = withContext(Dispatchers.IO) { repo.refresh() }
    }
}
