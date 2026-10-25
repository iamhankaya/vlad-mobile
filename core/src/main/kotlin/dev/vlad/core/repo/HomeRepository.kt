package dev.vlad.core.repo

import dev.vlad.core.api.ApiFailure
import dev.vlad.core.api.ApiResult
import dev.vlad.core.api.Card
import dev.vlad.core.api.CardKind
import dev.vlad.core.api.SessionCredentials
import dev.vlad.core.api.VladApi
import dev.vlad.core.reduce.Connection
import dev.vlad.core.reduce.HomeEvent
import dev.vlad.core.reduce.HomeReducer
import dev.vlad.core.reduce.HomeState

/** Ekranın çizdiği tek görüntü. [serverStatus] son yenilemenin 5xx kodu (yoksa `null`). */
data class TodaySnapshot(
    val state: HomeState = HomeState(),
    val lastUpdatedMs: Long? = null,
    val serverStatus: Int? = null,
    val loading: Boolean = true,
)

/**
 * `GET /v1/home` yenilemesi. Başarısız yenileme son başarılı görüntüyü korur (çevrimdışı önbellek), yalnız bandı açar.
 * Bloklayıcıdır: çağıran arka plan iş parçacığında koşturur.
 */
class HomeRepository(
    private val api: VladApi,
    private val creds: SessionCredentials,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    @Volatile
    private var snapshot = TodaySnapshot()

    fun current(): TodaySnapshot = snapshot

    fun refresh(): TodaySnapshot {
        val prev = snapshot
        var state = prev.state
        var updated = prev.lastUpdatedMs
        var serverStatus: Int? = null
        when (val r = api.getHome(creds)) {
            is ApiResult.Ok -> {
                state = HomeReducer.reduce(state, HomeEvent.Loaded(r.value))
                // SSE bu dilimde yok: ulaşılabilir = yoklama.
                state = HomeReducer.reduce(state, HomeEvent.ConnectionChanged(Connection.POLLING))
                updated = clock()
            }
            is ApiResult.Failed -> {
                val f = r.failure
                state = HomeReducer.reduce(state, HomeEvent.LoadFailed(f.code.takeIf { f is ApiFailure.Http && f.status < SERVER_ERROR }))
                if (f is ApiFailure.Http && f.status >= SERVER_ERROR) {
                    serverStatus = f.status
                    state = HomeReducer.reduce(state, HomeEvent.ConnectionChanged(Connection.POLLING))
                }
            }
        }
        snapshot = TodaySnapshot(state, updated, serverStatus, loading = false)
        return snapshot
    }

    private companion object {
        const val SERVER_ERROR = 500
    }
}

/**
 * "Senden bekleniyor" sırası (DESIGN.md): kırmızı CI/çakışma ve onay kapısı önce, sonra sorular; aynı öncelikte eski önce
 * (`created` ISO-8601 olduğu için metin sırası zaman sırasıdır).
 */
fun sortAsks(asks: List<Card>): List<Card> = asks.sortedWith(compareBy<Card>({ askPriority(it.kind) }, { it.created }))

private fun askPriority(kind: CardKind): Int =
    when (kind) {
        CardKind.CI_RED, CardKind.CONFLICT, CardKind.GATE, CardKind.ACTION_REQUEST -> 0
        else -> 1
    }
