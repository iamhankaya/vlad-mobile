package dev.vlad.core

import dev.vlad.core.crypto.B64Url
import dev.vlad.core.crypto.CanonicalException
import dev.vlad.core.crypto.EnvelopeInput
import dev.vlad.core.crypto.Signatures
import dev.vlad.core.crypto.canonicalQuery
import dev.vlad.core.crypto.challengeBytes
import dev.vlad.core.crypto.envelopeString
import dev.vlad.core.crypto.revokeBytes
import dev.vlad.core.crypto.sha256Hex
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private fun JsonObject.str(k: String) = getValue(k).jsonPrimitive.content

private fun JsonObject.section(k: String): List<JsonObject> = (getValue(k) as JsonArray).map { it.jsonObject }

class SigningVectorsTest {
    private val v = ContractFiles.vectors
    private val keys = v.section("keys").associate { it.str("id") to it.str("spki_b64u") }

    private fun envelopeInput(o: JsonObject) =
        EnvelopeInput(
            method = o.str("method"),
            path = o.str("path"),
            rawQuery = o.str("raw_query"),
            tsMs = o.getValue("ts_ms").jsonPrimitive.long,
            nonce = o.str("nonce"),
            body = B64Url.decode(o.str("body_b64u")),
        )

    @Test
    fun `mobile-core signing vectors version`() {
        assertEquals(2L, v.getValue("vectors_version").jsonPrimitive.long)
        assertEquals(6, keys.size)
    }

    @Test
    fun `mobile-core canonical query vectors`() {
        val ok = v.section("canonical_query")
        assertTrue(ok.isNotEmpty())
        for (c in ok) assertEquals(c.str("expected"), canonicalQuery(c.str("raw_query")), c.str("id"))
        val bad = v.section("canonical_query_errors")
        assertTrue(bad.isNotEmpty())
        for (c in bad) {
            val e = assertFailsWith<CanonicalException>(c.str("id")) { canonicalQuery(c.str("raw_query")) }
            assertEquals(CanonicalException.Code.BAD_QUERY, e.code, c.str("id"))
        }
    }

    @Test
    fun `mobile-core body hash vectors`() {
        for (c in v.section("body_hash")) assertEquals(c.str("sha256_hex"), sha256Hex(B64Url.decode(c.str("body_b64u"))), c.str("id"))
    }

    @Test
    fun `mobile-core signing vectors verify`() {
        var valid = 0
        var invalid = 0
        fun check(
            c: JsonObject,
            text: ByteArray,
        ) {
            val got = Signatures.verify(keys.getValue(c.str("key_id")), text, c.str("signature_b64u"))
            assertEquals(c.getValue("expect_valid").jsonPrimitive.boolean, got, c.str("id"))
            if (got) valid++ else invalid++
        }
        for (c in v.section("envelope")) {
            val s = envelopeString(envelopeInput(c.getValue("input").jsonObject))
            assertEquals(c.str("envelope_string"), s, c.str("id"))
            check(c, s.toByteArray(Charsets.UTF_8))
        }
        for (c in v.section("challenge")) {
            val bytes = challengeBytes(c.getValue("fields").jsonObject.mapValues { it.value.jsonPrimitive.content })
            assertContentEquals(c.str("challenge_text").toByteArray(Charsets.UTF_8), bytes, c.str("id"))
            check(c, bytes)
        }
        for (c in v.section("revoke")) {
            val f = c.getValue("fields").jsonObject
            val bytes = revokeBytes(f.str("device"), f.str("signer"), f.getValue("ts_ms").jsonPrimitive.long)
            assertContentEquals(c.str("revoke_text").toByteArray(Charsets.UTF_8), bytes, c.str("id"))
            check(c, bytes)
        }
        for (c in v.section("grant")) check(c, c.str("payload").toByteArray(Charsets.UTF_8))
        // Hem kabul hem ret yönü gerçekten koşulmuş olmalı (boş döngüyle yeşil olmasın).
        val all = listOf("envelope", "challenge", "revoke", "grant").flatMap { v.section(it) }
        assertEquals(all.count { it.getValue("expect_valid").jsonPrimitive.boolean }, valid)
        assertTrue(valid >= 5, "geçerli vektör sayısı: $valid")
        assertTrue(invalid >= 20, "geçersiz vektör sayısı: $invalid")
    }

    @Test
    fun `mobile-core envelope and revoke error vectors`() {
        for (c in v.section("envelope_errors")) {
            assertFailsWith<CanonicalException>(c.str("id")) { envelopeString(envelopeInput(c.getValue("input").jsonObject)) }
        }
        for (c in v.section("revoke_errors")) {
            val f = c.getValue("fields").jsonObject
            assertFailsWith<CanonicalException>(c.str("id")) {
                revokeBytes(f.str("device"), f.str("signer"), f.getValue("ts_ms").jsonPrimitive.long)
            }
        }
    }

    @Test
    fun `mobile-core canonical challenge bytes golden`() {
        val fields =
            linkedMapOf(
                "action_id" to "A-7Q2M9K4X",
                "kind" to "merge_phase",
                "repo" to "iamhankaya/vlad",
                "pr" to "42",
                "base" to "vlad/T20261024-94",
                "head_sha" to "0123456789abcdef0123456789abcdef01234567",
                "task" to "T20261025-61",
                "nonce" to "AAECAwQFBgcICQoLDA0ODw",
                "exp" to "1761350460",
            )
        val golden =
            "vlad-approve/v1\naction_id=A-7Q2M9K4X\nkind=merge_phase\nrepo=iamhankaya/vlad\npr=42\n" +
                "base=vlad/T20261024-94\nhead_sha=0123456789abcdef0123456789abcdef01234567\ntask=T20261025-61\n" +
                "nonce=AAECAwQFBgcICQoLDA0ODw\nexp=1761350460"
        assertContentEquals(golden.toByteArray(Charsets.UTF_8), challengeBytes(fields))
        // Sıra girdideki anahtar sırasına değil sabit sıraya bağlıdır.
        assertContentEquals(challengeBytes(fields), challengeBytes(fields.entries.reversed().associate { it.key to it.value }))
    }

    @Test
    fun `mobile-core challenge rejects injection missing and extra fields`() {
        val ok =
            mapOf(
                "action_id" to "A-1", "kind" to "merge_phase", "repo" to "a/b", "pr" to "1", "base" to "main",
                "head_sha" to "0".repeat(40), "task" to "T20261025-67", "nonce" to "A".repeat(22), "exp" to "1761350460",
            )
        challengeBytes(ok)
        assertFailsWith<CanonicalException> { challengeBytes(ok + ("base" to "main\nexp=1")) }
        assertFailsWith<CanonicalException> { challengeBytes(ok - "task") }
        assertFailsWith<CanonicalException> { challengeBytes(ok + ("extra" to "x")) }
        assertFailsWith<CanonicalException> { challengeBytes(ok + ("head_sha" to "0".repeat(39))) }
        assertFailsWith<CanonicalException> { challengeBytes(ok + ("pr" to "0")) }
    }
}
