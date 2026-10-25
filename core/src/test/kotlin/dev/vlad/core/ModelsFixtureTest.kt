package dev.vlad.core

import dev.vlad.core.api.ApproveResponse
import dev.vlad.core.api.Card
import dev.vlad.core.api.Challenge
import dev.vlad.core.api.ChatAccepted
import dev.vlad.core.api.ChatHistory
import dev.vlad.core.api.DeviceList
import dev.vlad.core.api.ErrorCode
import dev.vlad.core.api.ErrorResponse
import dev.vlad.core.api.EventList
import dev.vlad.core.api.FindingList
import dev.vlad.core.api.GrantCreated
import dev.vlad.core.api.GrantList
import dev.vlad.core.api.Health
import dev.vlad.core.api.Home
import dev.vlad.core.api.OkResponse
import dev.vlad.core.api.PairResponse
import dev.vlad.core.api.PipelineList
import dev.vlad.core.api.Project
import dev.vlad.core.api.ProjectList
import dev.vlad.core.api.Task
import dev.vlad.core.api.VaultFile
import dev.vlad.core.api.VaultSearchResponse
import dev.vlad.core.api.VladJson
import dev.vlad.core.crypto.B64Url
import dev.vlad.core.crypto.challengeBytes
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ModelsFixtureTest {
    /** operationId -> 2xx yanıt gövdesinin modeli. Eşlenmeyen bir 2xx fixture testi bilerek düşürür. */
    private val success: Map<String, KSerializer<*>> =
        mapOf(
            "getHome" to serializer<Home>(),
            "getCard" to serializer<Card>(),
            "answerCard" to serializer<OkResponse>(),
            "getTask" to serializer<Task>(),
            "requestTaskChanges" to serializer<OkResponse>(),
            "listPipelines" to serializer<PipelineList>(),
            "listProjects" to serializer<ProjectList>(),
            "getProject" to serializer<Project>(),
            "listFindings" to serializer<FindingList>(),
            "searchVault" to serializer<VaultSearchResponse>(),
            "readVaultFile" to serializer<VaultFile>(),
            "postChat" to serializer<ChatAccepted>(),
            "getChatHistory" to serializer<ChatHistory>(),
            "prepareAction" to serializer<Challenge>(),
            "approveAction" to serializer<ApproveResponse>(),
            "rejectAction" to serializer<OkResponse>(),
            "listGrants" to serializer<GrantList>(),
            "createGrant" to serializer<GrantCreated>(),
            "revokeGrant" to serializer<OkResponse>(),
            "takeoverActive" to serializer<OkResponse>(),
            "listDevices" to serializer<DeviceList>(),
            "putDeviceFcm" to serializer<OkResponse>(),
            "revokeDevice" to serializer<OkResponse>(),
            "listEvents" to serializer<EventList>(),
            "getHealth" to serializer<Health>(),
            "pairDevice" to serializer<PairResponse>(),
        )

    private fun body(f: JsonObject): JsonElement = f.getValue("response").jsonObject.getValue("body")

    private fun status(f: JsonObject): Int = f.getValue("response").jsonObject.getValue("status").jsonPrimitive.int

    @Test
    @DisplayName("mobile-core: api fixtures decode into models")
    fun `mobile-core api fixtures decode into models`() {
        var decoded = 0
        var errors = 0
        for ((name, f) in ContractFiles.apiFixtures) {
            val op = f.getValue("operationId").jsonPrimitive.content
            if (status(f) in 200..299) {
                val ser = success[op] ?: continue // streamEvents: gövde SSE metni, SseParserTest'te
                VladJson.decodeFromJsonElement(ser, body(f))
                decoded++
            } else {
                val e = VladJson.decodeFromJsonElement(serializer<ErrorResponse>(), body(f))
                assertTrue(e.error.messageTr.isNotEmpty(), name)
                errors++
            }
        }
        assertTrue(decoded >= 25, "çözülen 2xx fixture: $decoded")
        assertTrue(errors >= 15, "çözülen hata fixture: $errors")
    }

    @Test
    @DisplayName("mobile-core: all success operations are covered by a fixture")
    fun `mobile-core all success operations are covered by a fixture`() {
        val ops =
            ContractFiles.apiFixtures.filter {
                status(it.second) in 200..299
            }.map { it.second.getValue("operationId").jsonPrimitive.content }.toSet()
        assertEquals(emptySet(), success.keys - ops, "fixture'ı olmayan eşleme")
    }

    @Test
    @DisplayName("mobile-core: home fixture field values")
    fun `mobile-core home fixture field values`() {
        val f = ContractFiles.apiFixtures.single { it.first == "get-home.200.json" }.second
        val home = VladJson.decodeFromJsonElement(serializer<Home>(), body(f))
        assertEquals(17, home.seq)
        assertEquals(2, home.asks.size)
        assertEquals("gate-T20261025-61", home.asks[0].id)
        assertEquals("0123456789abcdef0123456789abcdef01234567", home.asks[0].gate?.headSha)
        assertEquals(60, home.workers.single().progress?.pct)
        assertTrue(home.activeVlad.mine)
    }

    @Test
    @DisplayName("mobile-core: error codes are all known")
    fun `mobile-core error codes are all known`() {
        for ((name, f) in ContractFiles.apiFixtures) {
            if (status(f) in 200..299) continue
            val code = body(f).jsonObject.getValue("error").jsonObject.getValue("code").jsonPrimitive.content
            assertNotNull(ErrorCode.entries.find { VladJson.encodeToString(serializer<ErrorCode>(), it) == "\"$code\"" }, "$name: $code")
        }
    }

    @Test
    @DisplayName("mobile-core: prepare fixture challenge bytes match fields")
    fun `mobile-core prepare fixture challenge bytes match fields`() {
        val f = ContractFiles.apiFixtures.single { it.first == "prepare-action.200.json" }.second
        val c = VladJson.decodeFromJsonElement(serializer<Challenge>(), body(f))
        // İstemci imzalamadan önce sunucunun gönderdiği baytların alanlarla örtüştüğünü kendisi yeniden kurar.
        assertContentEquals(java.util.Base64.getDecoder().decode(c.bytesB64), challengeBytes(c.fields.asMap()))
        assertEquals(c.exp, c.fields.exp.toLong())
        assertTrue(B64Url.encode(challengeBytes(c.fields.asMap())).isNotEmpty())
    }
}
