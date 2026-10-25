package dev.vlad.core

import dev.vlad.core.api.Event
import dev.vlad.core.api.EventType
import dev.vlad.core.api.VladJson
import dev.vlad.core.sse.SseEvent
import dev.vlad.core.sse.SseException
import dev.vlad.core.sse.SseParser
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SseParserTest {
    private fun parseAll(
        bytes: ByteArray,
        split: List<Int> = emptyList(),
    ): List<SseEvent> {
        val p = SseParser()
        val cuts = (listOf(0) + split + listOf(bytes.size)).distinct()
        return cuts.zipWithNext().flatMap { (a, b) -> p.feed(bytes, a, b - a) }
    }

    private val stream = "id: 17\nevent: task.state\ndata: {\"seq\":17}\n\n: keepalive\n\nid: 18\ndata: ğüşiöç İ 🙂\ndata: ikinci\n\n"

    @Test
    @DisplayName("mobile-core: sse parser basic")
    fun `mobile-core sse parser basic`() {
        val ev = parseAll(stream.toByteArray())
        assertEquals(
            listOf(SseEvent("17", "task.state", "{\"seq\":17}"), SseEvent("18", "message", "ğüşiöç İ 🙂\nikinci")),
            ev,
        )
    }

    @Test
    @DisplayName("mobile-core: sse parser chunk boundary utf8")
    fun `mobile-core sse parser chunk boundary utf8`() {
        val bytes = stream.toByteArray()
        val whole = parseAll(bytes)
        // Her olası tek kesme noktası (çok baytlı karakterin ortası dahil) ve bayt bayt besleme aynı sonucu verir.
        for (cut in 1 until bytes.size) assertEquals(whole, parseAll(bytes, listOf(cut)), "kesme $cut")
        assertEquals(whole, parseAll(bytes, (1 until bytes.size).toList()))
    }

    @Test
    @DisplayName("mobile-core: sse parser line endings cr crlf lf and split crlf")
    fun `mobile-core sse parser line endings cr crlf lf and split crlf`() {
        val expected = listOf(SseEvent(null, "message", "a"), SseEvent(null, "message", "b"))
        assertEquals(expected, parseAll("data: a\r\rdata: b\r\r".toByteArray()))
        assertEquals(expected, parseAll("data: a\r\n\r\ndata: b\r\n\r\n".toByteArray()))
        val crlf = "data: a\r\n\r\ndata: b\r\n\r\n".toByteArray()
        for (cut in 1 until crlf.size) assertEquals(expected, parseAll(crlf, listOf(cut)), "kesme $cut")
    }

    @Test
    @DisplayName("mobile-core: sse parser comments empty data and unknown fields")
    fun `mobile-core sse parser comments empty data and unknown fields`() {
        assertEquals(emptyList(), parseAll(": keepalive\n\n: x\n\n".toByteArray()))
        assertEquals(emptyList(), parseAll("event: x\n\n".toByteArray())) // data yok -> olay yok
        assertEquals(listOf(SseEvent(null, "message", "")), parseAll("data\n\n".toByteArray()))
        assertEquals(listOf(SseEvent(null, "message", "x")), parseAll("foo: bar\ndata:x\n\n".toByteArray()))
        // Olay adı bir sonraki olaya taşmaz.
        val ev = parseAll("event: a\ndata: 1\n\ndata: 2\n\n".toByteArray())
        assertEquals(listOf("a", "message"), ev.map { it.event })
    }

    @Test
    @DisplayName("mobile-core: sse parser tracks last event id and retry")
    fun `mobile-core sse parser tracks last event id and retry`() {
        val p = SseParser()
        p.feed("id: 5\nretry: 3000\ndata: x\n\nid: 6\n".toByteArray())
        assertEquals("6", p.lastEventId) // id alanı satır okunur okunmaz işlenir, dispatch beklemez
        assertEquals(3000L, p.retryMs)
        p.feed("retry: abc\n\n".toByteArray())
        assertEquals(3000L, p.retryMs)
    }

    @Test
    @DisplayName("mobile-core: sse parser bom and id with nul")
    fun `mobile-core sse parser bom and id with nul`() {
        val ev = parseAll("data: a\n\n".toByteArray())
        assertEquals(listOf(SseEvent(null, "message", "a")), ev)
        val p = SseParser()
        p.feed("id: 1\u0000\ndata: a\n\n".toByteArray())
        assertEquals(null, p.lastEventId)
    }

    @Test
    @DisplayName("mobile-core: sse parser rejects overlong line")
    fun `mobile-core sse parser rejects overlong line`() {
        val p = SseParser(maxLineBytes = 16)
        assertFailsWith<SseException> { p.feed("data: ${"x".repeat(32)}\n\n".toByteArray()) }
    }

    @Test
    @DisplayName("mobile-core: sse parser contract fixture decodes to Event")
    fun `mobile-core sse parser contract fixture decodes to Event`() {
        val f = ContractFiles.apiFixtures.single { it.first == "stream-events.200.json" }.second
        val text = f.getValue("response").jsonObject.getValue("body").jsonPrimitive.content
        val ev = parseAll(text.toByteArray()).single()
        assertEquals("17", ev.id)
        val decoded = VladJson.decodeFromString(serializer<Event>(), ev.data)
        assertEquals(EventType.TASK_STATE, decoded.type)
        assertEquals(17, decoded.seq)
        assertEquals(ev.event, decoded.type.wire)
    }
}
