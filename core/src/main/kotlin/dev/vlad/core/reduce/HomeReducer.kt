package dev.vlad.core.reduce

import dev.vlad.core.api.ErrorCode
import dev.vlad.core.api.Home

/** Olay akışının durumu. `OFFLINE` = ağ yok / sunucuya ulaşılamıyor; `POLLING` = SSE yok, `/events` yoklaması. */
enum class Connection { CONNECTING, LIVE, POLLING, OFFLINE }

/** Ekranın üstünde gösterilecek band; kanıt isteyen bir "bağlantı yok" ya da "yeniden eşleştir" durumu. */
enum class Banner { NONE, OFFLINE, AUTH_LOST }

/**
 * "Bugün" ekranının saf durumu. [home] son başarılı `GET /home` görüntüsüdür (yenileme başarısız olsa da korunur).
 * SSE olayları yalnız geçersiz kılma ipucudur: durumu değiştirmez, [stale] yapar; istemci `GET /home` ile yeniler.
 */
data class HomeState(
    val home: Home? = null,
    /** Görülen, ana ekranı etkileyen en yüksek olay sırası. */
    val relevantSeq: Long = 0,
    val connection: Connection = Connection.CONNECTING,
    val lastError: ErrorCode? = null,
) {
    /** `GET /home` yenilemesi gerekiyor: hiç yüklenmedi ya da yüklenen görüntüden yeni ilgili olay görüldü. */
    val stale: Boolean get() = home == null || relevantSeq > home.seq

    val banner: Banner
        get() =
            when {
                lastError == ErrorCode.DEVICE_REVOKED || lastError == ErrorCode.UNAUTHORIZED -> Banner.AUTH_LOST
                connection == Connection.OFFLINE -> Banner.OFFLINE
                else -> Banner.NONE
            }
}

sealed interface HomeEvent {
    data class Loaded(val home: Home) : HomeEvent

    /** `GET /home` başarısız. [code] sunucu hata kodu; ağ hatasında `null`. */
    data class LoadFailed(val code: ErrorCode?) : HomeEvent

    /** SSE ya da yoklama ile gelen olay. [type] ham metindir: bilinmeyen tür (yeni sunucu) güvenli tarafta "ilgili" sayılır. */
    data class Streamed(val seq: Long, val type: String) : HomeEvent

    data class ConnectionChanged(val connection: Connection) : HomeEvent
}

private val NOT_HOME_RELEVANT = setOf("chat.delta", "chat.done")

object HomeReducer {
    fun reduce(
        state: HomeState,
        event: HomeEvent,
    ): HomeState =
        when (event) {
            is HomeEvent.Loaded -> {
                val old = state.home
                // Geç gelen eski yanıt yeni görüntünün üstüne yazmaz.
                if (old != null && event.home.seq < old.seq) {
                    state
                } else {
                    state.copy(home = event.home, lastError = null)
                }
            }
            is HomeEvent.LoadFailed ->
                state.copy(
                    lastError = event.code,
                    connection = if (event.code == null) Connection.OFFLINE else state.connection,
                )
            is HomeEvent.Streamed ->
                if (event.type in NOT_HOME_RELEVANT || event.seq <= state.relevantSeq || event.seq <= (state.home?.seq ?: 0)) {
                    state
                } else {
                    state.copy(relevantSeq = event.seq)
                }
            is HomeEvent.ConnectionChanged -> state.copy(connection = event.connection)
        }
}
