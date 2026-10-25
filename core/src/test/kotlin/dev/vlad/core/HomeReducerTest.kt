package dev.vlad.core

import dev.vlad.core.api.ErrorCode
import dev.vlad.core.api.Home
import dev.vlad.core.api.VladJson
import dev.vlad.core.reduce.Banner
import dev.vlad.core.reduce.Connection
import dev.vlad.core.reduce.HomeEvent
import dev.vlad.core.reduce.HomeReducer
import dev.vlad.core.reduce.HomeState
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeReducerTest {
    private fun home(name: String): Home {
        val f = ContractFiles.apiFixtures.single { it.first == name }.second
        return VladJson.decodeFromJsonElement(serializer<Home>(), f.getValue("response").jsonObject.getValue("body"))
    }

    private val h17 = home("get-home.200.json")
    private val h18 = home("get-home.200-warnings-quota.json")

    private fun run(
        vararg ev: HomeEvent,
        from: HomeState = HomeState(),
    ) = ev.fold(from, HomeReducer::reduce)

    @Test
    fun `mobile-core reducer states initial needs load`() {
        val s = HomeState()
        assertNull(s.home)
        assertTrue(s.stale)
        assertEquals(Banner.NONE, s.banner)
        assertEquals(Connection.CONNECTING, s.connection)
    }

    @Test
    fun `mobile-core reducer states loaded then fresh`() {
        val s = run(HomeEvent.Loaded(h17))
        assertEquals(h17, s.home)
        assertFalse(s.stale)
    }

    @Test
    fun `mobile-core reducer states newer relevant event marks stale and reload clears`() {
        var s = run(HomeEvent.Loaded(h17), HomeEvent.Streamed(18, "task.state"))
        assertTrue(s.stale)
        s = run(HomeEvent.Loaded(h18), from = s)
        assertFalse(s.stale) // h18.seq == 18
    }

    @Test
    fun `mobile-core reducer states replayed and old events are ignored`() {
        val s = run(HomeEvent.Loaded(h17), HomeEvent.Streamed(17, "task.state"), HomeEvent.Streamed(3, "card.opened"))
        assertFalse(s.stale)
        assertEquals(0, s.relevantSeq)
    }

    @Test
    fun `mobile-core reducer states chat events do not invalidate home`() {
        val s = run(HomeEvent.Loaded(h17), HomeEvent.Streamed(40, "chat.delta"), HomeEvent.Streamed(41, "chat.done"))
        assertFalse(s.stale)
    }

    @Test
    fun `mobile-core reducer states unknown event type is treated as relevant`() {
        assertTrue(run(HomeEvent.Loaded(h17), HomeEvent.Streamed(18, "future.thing")).stale)
    }

    @Test
    fun `mobile-core reducer states event before first load is remembered`() {
        val s = run(HomeEvent.Streamed(30, "card.opened"), HomeEvent.Loaded(h17))
        assertTrue(s.stale) // yükleme sırasında daha yeni bir olay gelmiş
    }

    @Test
    fun `mobile-core reducer states late older response does not overwrite newer home`() {
        val s = run(HomeEvent.Loaded(h18), HomeEvent.Loaded(h17))
        assertEquals(h18, s.home)
    }

    @Test
    fun `mobile-core reducer states offline keeps last home and shows banner`() {
        var s = run(HomeEvent.Loaded(h17), HomeEvent.LoadFailed(null))
        assertEquals(h17, s.home)
        assertEquals(Banner.OFFLINE, s.banner)
        s = run(HomeEvent.Loaded(h18), from = s)
        assertEquals(Banner.OFFLINE, s.banner) // bağlantı durumu ayrıca LIVE olana dek bant kalır
        s = run(HomeEvent.ConnectionChanged(Connection.LIVE), from = s)
        assertEquals(Banner.NONE, s.banner)
    }

    @Test
    fun `mobile-core reducer states revoked device shows auth banner until a load succeeds`() {
        var s = run(HomeEvent.Loaded(h17), HomeEvent.LoadFailed(ErrorCode.DEVICE_REVOKED))
        assertEquals(Banner.AUTH_LOST, s.banner)
        assertEquals(h17, s.home)
        s = run(HomeEvent.LoadFailed(ErrorCode.UNAUTHORIZED), from = s)
        assertEquals(Banner.AUTH_LOST, s.banner)
        s = run(HomeEvent.Loaded(h18), from = s)
        assertEquals(Banner.NONE, s.banner)
    }

    @Test
    fun `mobile-core reducer states server error does not claim offline`() {
        val s = run(HomeEvent.ConnectionChanged(Connection.LIVE), HomeEvent.LoadFailed(ErrorCode.RATE_LIMITED))
        assertEquals(Banner.NONE, s.banner)
        assertEquals(Connection.LIVE, s.connection)
    }
}
