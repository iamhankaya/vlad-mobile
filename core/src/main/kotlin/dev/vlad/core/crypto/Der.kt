package dev.vlad.core.crypto

import java.math.BigInteger

/**
 * Katı ECDSA P-256 DER imza denetimi (saf). JCA/OpenSSL minimal olmayan DER'i ve yüksek-S'i kabul eder; sözleşme
 * etmez: imza yalnız minimal kısa biçim DER ve `s <= n/2` ise kabul edilir (`server/src/auth/der.ts` ile aynı kural).
 */
object Der {
    val P256_N = BigInteger("ffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551", 16)
    val P256_HALF_N: BigInteger = P256_N.shiftRight(1)

    private const val SEQUENCE = 0x30
    private const val INTEGER = 0x02
    private const val LONG_FORM = 0x80
    private const val MIN_LEN = 8
    private const val MAX_LEN = 72

    data class Rs(val r: BigInteger, val s: BigInteger)

    private fun readInt(
        b: ByteArray,
        i: Int,
    ): Pair<BigInteger, Int>? {
        if (i + 1 >= b.size || (b[i].toInt() and 0xff) != INTEGER) return null
        val len = b[i + 1].toInt() and 0xff
        if (len < 1 || len >= LONG_FORM || i + 2 + len > b.size) return null
        val first = b[i + 2].toInt() and 0xff
        if (first and LONG_FORM != 0) return null // negatif
        if (len > 1 && first == 0 && (b[i + 3].toInt() and 0xff) and LONG_FORM == 0) return null // gereksiz sıfır
        return BigInteger(1, b.copyOfRange(i + 2, i + 2 + len)) to (i + 2 + len)
    }

    /** Minimal kısa biçim `SEQUENCE{INTEGER r, INTEGER s}`; başka her şey (uzun biçim, dolgu, sondaki bayt) `null`. */
    fun parseStrict(sig: ByteArray): Rs? {
        if (sig.size !in MIN_LEN..MAX_LEN) return null
        if ((sig[0].toInt() and 0xff) != SEQUENCE || (sig[1].toInt() and 0xff) != sig.size - 2) return null
        val r = readInt(sig, 2) ?: return null
        val s = readInt(sig, r.second) ?: return null
        if (s.second != sig.size) return null
        val inRange = { v: BigInteger -> v.signum() > 0 && v < P256_N }
        return if (inRange(r.first) && inRange(s.first)) Rs(r.first, s.first) else null
    }

    /** Yalnız katı DER ve low-S için true. */
    fun isCanonical(sig: ByteArray): Boolean = parseStrict(sig)?.let { it.s <= P256_HALF_N } ?: false

    private fun encInt(v: BigInteger): ByteArray {
        val bytes = v.toByteArray() // BigInteger zaten minimal: yalnız işaret biti için gerekirse öne 0x00 koyar
        return byteArrayOf(INTEGER.toByte(), bytes.size.toByte()) + bytes
    }

    /** (r, s) için minimal DER. */
    fun encode(
        r: BigInteger,
        s: BigInteger,
    ): ByteArray {
        val body = encInt(r) + encInt(s)
        return byteArrayOf(SEQUENCE.toByte(), body.size.toByte()) + body
    }

    /**
     * İmzadan sonra low-S normalizasyonu: `s > n/2` ise `n - s`. Giriş geçerli DER olmalı; çıktı minimal DER ve
     * low-S'tir. Android Keystore imzaları rastgele S üretir, normalize edilmezse sunucu yaklaşık yarısını reddeder.
     */
    fun normalizeLowS(sig: ByteArray): ByteArray {
        val rs = parseStrict(sig) ?: throw IllegalArgumentException("geçersiz DER imza")
        val s = if (rs.s > P256_HALF_N) P256_N.subtract(rs.s) else rs.s
        return encode(rs.r, s)
    }
}
