package dev.vlad.core

import dev.vlad.core.crypto.B64Url
import dev.vlad.core.crypto.Der
import dev.vlad.core.crypto.Signatures
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DerTest {
    private val msg = "vlad".toByteArray()

    private fun keyPair() = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()

    @Test
    fun `mobile-core low-S normalization signs verifiable and canonical`() {
        val kp = keyPair()
        val spki = B64Url.encode(kp.public.encoded)
        var sawHigh = 0
        repeat(300) {
            val raw =
                Signature.getInstance("SHA256withECDSA").run {
                    initSign(kp.private)
                    update(msg)
                    sign()
                }
            val rs = assertNotNull(Der.parseStrict(raw))
            if (rs.s > Der.P256_HALF_N) {
                sawHigh++
                // Normalize edilmemiş yüksek-S imza sözleşmede geçersizdir (JCA kendisi kabul etse de).
                assertFalse(Signatures.verify(spki, msg, B64Url.encode(raw)))
            }
            val fixed = Der.normalizeLowS(raw)
            assertTrue(Der.isCanonical(fixed))
            assertTrue(Signatures.verify(spki, msg, B64Url.encode(fixed)))
            val srs = assertNotNull(Der.parseStrict(fixed))
            assertEquals(rs.r, srs.r)
            assertEquals(Der.P256_N.subtract(rs.s).min(rs.s), srs.s)
            assertTrue(Signatures.verify(spki, msg, B64Url.encode(Signatures.signLowS(kp.private, msg))))
        }
        // Yaklaşık yarısı yüksek-S çıkar; hiç çıkmadıysa test normalizasyonu sınamamış demektir.
        assertTrue(sawHigh in 50..250, "yüksek-S sayısı: $sawHigh / 300")
    }

    @Test
    fun `mobile-core strict der rejects malformed`() {
        val r = BigInteger.ONE.shiftLeft(200)
        val s = BigInteger.TWO
        val good = Der.encode(r, s)
        assertTrue(Der.isCanonical(good))
        assertContentEquals(good, Der.encode(Der.parseStrict(good)!!.r, Der.parseStrict(good)!!.s))
        assertNull(Der.parseStrict(good + byteArrayOf(0))) // sondaki bayt
        assertNull(Der.parseStrict(good.copyOf(good.size - 1))) // kesik
        assertNull(
            Der.parseStrict(byteArrayOf(0x30, 0x81.toByte(), (good.size - 2).toByte()) + good.copyOfRange(2, good.size)),
        ) // uzun biçim
        // öndeki gereksiz sıfır: INTEGER 00 01
        val padded = byteArrayOf(0x30, 0x08, 0x02, 0x02, 0x00, 0x01, 0x02, 0x02, 0x00, 0x01)
        assertNull(Der.parseStrict(padded))
        // negatif tamsayı
        assertNull(Der.parseStrict(byteArrayOf(0x30, 0x06, 0x02, 0x01, 0x81.toByte(), 0x02, 0x01, 0x01)))
        assertNull(Der.parseStrict(ByteArray(64) { 1 })) // ham r||s
        // s = 0 ve s = n kapsam dışı
        assertNull(Der.parseStrict(Der.encode(r, BigInteger.ZERO)))
        assertNull(Der.parseStrict(Der.encode(r, Der.P256_N)))
    }

    @Test
    fun `mobile-core high-S boundary`() {
        val r = BigInteger.TEN
        assertTrue(Der.isCanonical(Der.encode(r, Der.P256_HALF_N)))
        assertFalse(Der.isCanonical(Der.encode(r, Der.P256_HALF_N.add(BigInteger.ONE))))
        val fixed = Der.normalizeLowS(Der.encode(r, Der.P256_HALF_N.add(BigInteger.ONE)))
        assertTrue(Der.isCanonical(fixed))
    }
}
