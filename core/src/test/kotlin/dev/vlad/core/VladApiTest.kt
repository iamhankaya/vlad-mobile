package dev.vlad.core

import dev.vlad.core.api.ApiFailure
import dev.vlad.core.api.ApiResult
import dev.vlad.core.api.Card
import dev.vlad.core.api.CardKind
import dev.vlad.core.api.ClockSync
import dev.vlad.core.api.Home
import dev.vlad.core.api.HttpRequest
import dev.vlad.core.api.HttpResponse
import dev.vlad.core.api.PairRequest
import dev.vlad.core.api.ServerAddress
import dev.vlad.core.api.SessionCredentials
import dev.vlad.core.api.TaskState
import dev.vlad.core.api.Transport
import dev.vlad.core.api.VladApi
import dev.vlad.core.api.VladJson
import dev.vlad.core.crypto.B64Url
import dev.vlad.core.crypto.Der
import dev.vlad.core.crypto.EnvelopeInput
import dev.vlad.core.crypto.Signatures
import dev.vlad.core.crypto.envelopeString
import dev.vlad.core.reduce.Banner
import dev.vlad.core.repo.HomeRepository
import dev.vlad.core.repo.sortAsks
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.IOException
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun fixtureBody(name: String): String {
    val f = ContractFiles.apiFixtures.first { it.first == name }.second
    return f["response"]!!.jsonObject["body"].toString()
}

private class FakeTransport(private val handler: (HttpRequest) -> HttpResponse) : Transport {
    val requests = mutableListOf<HttpRequest>()

    override fun execute(request: HttpRequest): HttpResponse {
        requests += request
        return handler(request)
    }
}

class VladApiTest {
    private val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
    private val spki = B64Url.encode(keyPair.public.encoded)
    private val creds = SessionCredentials("d-test") { Signatures.signLowS(keyPair.private, it) }
    private val nonce = "AAAAAAAAAAAAAAAAAAAAAA"

    @Test
    @DisplayName("mobile-core: getHome signs envelope with required headers")
    fun getHomeSignsEnvelope() {
        val t = FakeTransport { HttpResponse(200, fixtureBody("get-home.200.json").toByteArray()) }
        val api = VladApi("https://vlad.example.test", t, clock = { 1_761_350_400_000 }, nonce = { nonce })
        val r = api.getHome(creds)
        assertIs<ApiResult.Ok<Home>>(r)
        assertEquals(2, r.value.asks.size)
        val req = t.requests.single()
        assertEquals("GET", req.method)
        assertEquals("https://vlad.example.test/v1/home", req.url)
        assertEquals("d-test", req.headers["X-Vlad-Device"])
        assertEquals("1761350400000", req.headers["X-Vlad-Ts"])
        assertEquals(nonce, req.headers["X-Vlad-Nonce"])
        val msg = envelopeString(EnvelopeInput("GET", "/v1/home", tsMs = 1_761_350_400_000, nonce = nonce)).toByteArray()
        assertTrue(Signatures.verify(spki, msg, req.headers.getValue("X-Vlad-Sig")))
        assertTrue(Der.isCanonical(B64Url.decode(req.headers.getValue("X-Vlad-Sig"))))
    }

    @Test
    @DisplayName("mobile-core: non low-S signer is refused before sending")
    fun highSIsRefused() {
        val t = FakeTransport { error("gönderilmemeli") }
        val badSigner =
            SessionCredentials("d-test") { m ->
                // Kasıtlı yüksek-S: normalize edilmemiş imza.
                val rs = Der.parseStrict(Signatures.signLowS(keyPair.private, m))!!
                Der.encode(rs.r, Der.P256_N.subtract(rs.s))
            }
        val api = VladApi("https://vlad.example.test", t, nonce = { nonce })
        val f = (api.getHome(badSigner) as ApiResult.Failed).failure
        assertIs<ApiFailure.Signing>(f)
        assertTrue(t.requests.isEmpty())
    }

    @Test
    @DisplayName("mobile-core: pair posts unsigned json and decodes device id")
    fun pairUnsigned() {
        val t = FakeTransport { HttpResponse(200, fixtureBody("pair.200.json").toByteArray()) }
        val api = VladApi("https://vlad.example.test", t)
        val r = api.pair(PairRequest("7Q2M9K4X", "Pixel", "spki1", "spki2"))
        assertIs<ApiResult.Ok<*>>(r)
        val req = t.requests.single()
        assertEquals("https://vlad.example.test/v1/pair", req.url)
        assertNull(req.headers["X-Vlad-Sig"])
        val body = VladJson.parseToJsonElement(req.body.toString(Charsets.UTF_8)).jsonObject
        assertEquals(JsonPrimitive("7Q2M9K4X"), body["code"])
        assertEquals(JsonPrimitive("spki2"), body["approval_pub"])
    }

    @Test
    @DisplayName("mobile-core: http errors and network failures map to ApiFailure")
    fun failures() {
        val unauthorized = FakeTransport { HttpResponse(401, fixtureBody("get-home.401-device-revoked.json").toByteArray()) }
        val f = (VladApi("https://x.test", unauthorized, nonce = { nonce }).getHome(creds) as ApiResult.Failed).failure
        assertIs<ApiFailure.Http>(f)
        assertEquals(dev.vlad.core.api.ErrorCode.DEVICE_REVOKED, f.code)

        val down = FakeTransport { throw IOException("unreachable") }
        val n = (VladApi("https://x.test", down, nonce = { nonce }).getHome(creds) as ApiResult.Failed).failure
        assertIs<ApiFailure.Network>(n)

        val junk = FakeTransport { HttpResponse(200, "<html>".toByteArray()) }
        val b = (VladApi("https://x.test", junk, nonce = { nonce }).getHome(creds) as ApiResult.Failed).failure
        assertIs<ApiFailure.BadResponse>(b)
    }

    @Test
    @DisplayName("mobile-core: server address accepts only https origin")
    fun serverAddress() {
        assertEquals("https://vlad.example.test", ServerAddress.parse(" vlad.example.test "))
        assertEquals("https://vlad.example.test:8443", ServerAddress.parse("https://vlad.example.test:8443/"))
        assertNull(ServerAddress.parse("http://vlad.example.test"))
        assertNull(ServerAddress.parse("https://vlad.example.test/api"))
        assertNull(ServerAddress.parse("https://u:p@vlad.example.test"))
        assertNull(ServerAddress.parse("https://vlad.example.test?x=1"))
        assertNull(ServerAddress.parse(""))
        assertNull(ServerAddress.parse("not a url"))
    }

    @Test
    @DisplayName("mobile-core: home repository keeps cache when offline and flags auth loss")
    fun repositoryOfflineAndAuth() {
        var mode = "ok"
        val t =
            FakeTransport {
                when (mode) {
                    "ok" -> HttpResponse(200, fixtureBody("get-home.200.json").toByteArray())
                    "revoked" -> HttpResponse(401, fixtureBody("get-home.401-device-revoked.json").toByteArray())
                    "503" -> HttpResponse(503, """{"error":{"code":"bad_request","message_tr":"x"}}""".toByteArray())
                    else -> throw IOException("down")
                }
            }
        var now = 1_000L
        val repo = HomeRepository(VladApi("https://x.test", t, nonce = { nonce }), creds) { now }
        val first = repo.refresh()
        assertEquals(2, first.state.home!!.asks.size)
        assertEquals(Banner.NONE, first.state.banner)
        assertEquals(1_000L, first.lastUpdatedMs)

        now = 2_000L
        mode = "down"
        val off = repo.refresh()
        assertEquals(Banner.OFFLINE, off.state.banner)
        assertEquals(2, off.state.home!!.asks.size)
        assertEquals(1_000L, off.lastUpdatedMs)

        mode = "503"
        val srv = repo.refresh()
        assertEquals(503, srv.serverStatus)
        assertEquals(Banner.NONE, srv.state.banner)

        mode = "revoked"
        assertEquals(Banner.AUTH_LOST, repo.refresh().state.banner)
    }

    @Test
    @DisplayName("mobile-core: unknown enum values decode as UNKNOWN")
    fun unknownEnums() {
        val body = fixtureBody("get-home.200.json")
        val mutated =
            body
                .replace("\"kind\":\"vlad_question\"", "\"kind\":\"hologram\"")
                .replace("\"state\":\"running\"", "\"state\":\"teleporting\"")
        val home = VladJson.decodeFromString<Home>(mutated)
        assertEquals(CardKind.UNKNOWN, home.asks[1].kind)
        assertEquals(CardKind.GATE, home.asks[0].kind)
        assertEquals(TaskState.UNKNOWN, home.workers[0].state)
        val mutated2 =
            body
                .replace("\"ci\":\"green\"", "\"ci\":\"purple\"")
                .replace("\"merge_kind\":\"merge_phase\"", "\"merge_kind\":\"new_kind\"")
        val gate = VladJson.decodeFromString<Home>(mutated2).asks[0].gate!!
        assertEquals(dev.vlad.core.api.CiState.UNKNOWN, gate.ci)
        assertNull(gate.mergeKind)
    }

    @Test
    @DisplayName("mobile-core: asks sort puts gates and failures before questions then oldest first")
    fun sorting() {
        fun card(
            id: String,
            kind: CardKind,
            created: String,
        ) = Card(
            id = id,
            kind = kind,
            project = "p",
            state = dev.vlad.core.api.CardState.OPEN,
            title = id,
            options = emptyList(),
            created = created,
            updated = created,
        )
        val sorted =
            sortAsks(
                listOf(
                    card("q-new", CardKind.VLAD_QUESTION, "2026-10-25T10:00:00Z"),
                    card("g-new", CardKind.GATE, "2026-10-25T09:00:00Z"),
                    card("q-old", CardKind.VLAD_QUESTION, "2026-10-25T08:00:00Z"),
                    card("ci", CardKind.CI_RED, "2026-10-25T07:00:00Z"),
                ),
            )
        assertEquals(listOf("ci", "g-new", "q-old", "q-new"), sorted.map { it.id })
    }

    @Test
    @DisplayName("mobile-core: signer failure becomes Signing failure and repository asks to re-pair")
    fun signerFailure() {
        val t = FakeTransport { error("gönderilmemeli") }
        val broken = SessionCredentials("d-test") { throw java.security.ProviderException("keystore kayıp") }
        val api = VladApi("https://x.test", t, nonce = { nonce })
        assertIs<ApiFailure.Signing>((api.getHome(broken) as ApiResult.Failed).failure)
        val checked = SessionCredentials("d-test") { throw java.security.InvalidKeyException("geçersiz") }
        assertIs<ApiFailure.Signing>((api.getHome(checked) as ApiResult.Failed).failure)
        assertTrue(t.requests.isEmpty())
        val snap = HomeRepository(api, broken) { 1L }.refresh()
        assertEquals(Banner.AUTH_LOST, snap.state.banner)
    }

    @Test
    @DisplayName("mobile-core: clock sync accepts up to 5 minutes and halves the round trip")
    fun clockSync() {
        val five = ClockSync.MAX_SKEW_MS
        // RTT 2000 ms: sunucu saati gidiş-dönüşün ortasında (before + 1000) okunmuş sayılır.
        assertEquals(0L, ClockSync.offset(10_000, 12_000, 11_000))
        assertEquals(five, ClockSync.offset(10_000, 12_000, 11_000 + five))
        assertEquals(-five, ClockSync.offset(10_000, 12_000, 11_000 - five))
        assertNull(ClockSync.offset(10_000, 12_000, 11_000 + five + 1))
        assertNull(ClockSync.offset(10_000, 12_000, 11_000 - five - 1))
        // Uzak gelecek (kötü sunucu +30 gün) reddedilir.
        assertNull(ClockSync.offset(10_000, 12_000, 10_000 + 30L * 24 * 3_600_000))
        // Saat geriye gitmişse RTT 0 sayılır.
        assertEquals(500L, ClockSync.offset(10_000, 9_000, 10_500))
    }
}
