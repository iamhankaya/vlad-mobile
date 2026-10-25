package dev.vlad.core.sse

import java.io.ByteArrayOutputStream

/** Dispatch edilmiş bir SSE olayı. [event] alanı yoksa `message`. [id] akışın o ana kadarki son `id:` değeri. */
data class SseEvent(val id: String?, val event: String, val data: String)

class SseException(message: String) : IllegalStateException(message)

/**
 * Artımlı `text/event-stream` ayrıştırıcısı (WHATWG SSE). Bayt parçaları keyfi yerden bölünebilir (satır sonu,
 * çok baytlı UTF-8 karakterin ortası, `\r` / `\n` arası); satırlar bayt düzeyinde bölünür, UTF-8 çözümü satır
 * tamamlanınca yapılır. `: keepalive` yorum satırları yok sayılır. [maxLineBytes] aşılırsa [SseException].
 */
class SseParser(private val maxLineBytes: Int = DEFAULT_MAX_LINE_BYTES) {
    private val line = ByteArrayOutputStream()
    private var sawCr = false
    private var atStart = true
    private val data = StringBuilder()
    private var hasData = false
    private var eventName = ""

    /** Son görülen `id:` (yeniden bağlanırken `Last-Event-ID`). */
    var lastEventId: String? = null
        private set

    /** Sunucunun önerdiği yeniden bağlanma gecikmesi (ms), `retry:` ile gelirse. */
    var retryMs: Long? = null
        private set

    fun feed(
        chunk: ByteArray,
        offset: Int = 0,
        length: Int = chunk.size - offset,
    ): List<SseEvent> {
        val out = mutableListOf<SseEvent>()
        for (i in offset until offset + length) {
            val b = chunk[i].toInt() and BYTE_MASK
            if (sawCr) {
                sawCr = false
                if (b == LF) continue // \r\n çifti zaten bitirildi
            }
            when (b) {
                CR -> {
                    sawCr = true
                    endLine(out)
                }
                LF -> endLine(out)
                else -> {
                    if (line.size() >= maxLineBytes) throw SseException("SSE satırı $maxLineBytes bayttan uzun")
                    line.write(b)
                }
            }
        }
        return out
    }

    private fun endLine(out: MutableList<SseEvent>) {
        var text = line.toString(Charsets.UTF_8)
        line.reset()
        if (atStart) {
            atStart = false
            text = text.removePrefix(BOM)
        }
        if (text.isEmpty()) {
            dispatch(out)
            return
        }
        if (text.startsWith(":")) return
        val colon = text.indexOf(':')
        val field = if (colon < 0) text else text.substring(0, colon)
        val value = if (colon < 0) "" else text.substring(colon + 1).removePrefix(" ")
        when (field) {
            "event" -> eventName = value
            "data" -> {
                if (hasData) data.append('\n')
                data.append(value)
                hasData = true
            }
            "id" -> if ('\u0000' !in value) lastEventId = value
            "retry" -> if (value.isNotEmpty() && value.all { it in '0'..'9' }) retryMs = value.toLongOrNull()
        }
    }

    private fun dispatch(out: MutableList<SseEvent>) {
        if (hasData) out += SseEvent(lastEventId, eventName.ifEmpty { "message" }, data.toString())
        data.setLength(0)
        hasData = false
        eventName = ""
    }

    private companion object {
        const val DEFAULT_MAX_LINE_BYTES = 1 shl 20
        const val BYTE_MASK = 0xff
        const val CR = 0x0d
        const val LF = 0x0a
        const val BOM = ""
    }
}
