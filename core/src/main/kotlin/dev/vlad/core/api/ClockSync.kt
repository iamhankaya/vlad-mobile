package dev.vlad.core.api

import kotlin.math.abs

/**
 * Eşleştirme yanıtındaki `server_time` ile cihaz saati arasındaki fark. Yanıt imzasızdır (kötü sunucu/MITM değer uydurabilir),
 * bu yüzden fark sınırlıdır: [MAX_SKEW_MS] aşılırsa eşleşme reddedilir ve kullanıcıdan cihaz saatini düzeltmesi istenir.
 */
object ClockSync {
    const val MAX_SKEW_MS = 5 * 60 * 1000L

    /** Sunucu saati, isteğin gidiş-dönüş ortasında okunmuş sayılır (RTT/2). Sınır aşılırsa `null`. */
    fun offset(
        beforeMs: Long,
        afterMs: Long,
        serverTimeMs: Long,
    ): Long? {
        val rtt = (afterMs - beforeMs).coerceAtLeast(0)
        val offset = serverTimeMs - (beforeMs + rtt / 2)
        return offset.takeIf { abs(it) <= MAX_SKEW_MS }
    }
}
