package dev.vlad.core.crypto

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest

/**
 * Kanonik istek/challenge/iptal bayt dizileri (saf). vlad reposundaki `server/src/auth/canonical.ts` ile birebir;
 * ikisi de `contract/signing-vectors.json` ile sabitlenir. Burada imza doğrulanmaz.
 */
class CanonicalException(val code: Code, message: String) : IllegalArgumentException(message) {
    enum class Code { BAD_QUERY, BAD_ENVELOPE, BAD_CHALLENGE }
}

private val UNRESERVED = Regex("[A-Za-z0-9\\-._~]")
private const val HEX_RADIX = 16
private const val BYTE_MASK = 0xff
private const val HIGH_BIT = 0x80
private const val PERCENT = 0x25

private fun percentDecode(s: String): String {
    val raw = s.replace('+', ' ').toByteArray(Charsets.UTF_8)
    val out = java.io.ByteArrayOutputStream(raw.size)
    var i = 0
    while (i < raw.size) {
        val b = raw[i].toInt() and BYTE_MASK
        if (b == PERCENT) {
            val hi = Character.digit(raw.getOrElse(i + 1) { 0 }.toInt().toChar(), HEX_RADIX)
            val lo = Character.digit(raw.getOrElse(i + 2) { 0 }.toInt().toChar(), HEX_RADIX)
            if (i + 2 >= raw.size || hi < 0 || lo < 0) {
                throw CanonicalException(CanonicalException.Code.BAD_QUERY, "geçersiz yüzde kodlaması")
            }
            out.write(hi * HEX_RADIX + lo)
            i += 3
        } else {
            out.write(b)
            i++
        }
    }
    val decoder =
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
    return try {
        decoder.decode(ByteBuffer.wrap(out.toByteArray())).toString()
    } catch (_: CharacterCodingException) {
        throw CanonicalException(CanonicalException.Code.BAD_QUERY, "sorgu geçerli UTF-8 değil")
    }
}

private fun percentEncode(s: String): String {
    val sb = StringBuilder()
    for (b in s.toByteArray(Charsets.UTF_8)) {
        val v = b.toInt() and BYTE_MASK
        val c = v.toChar()
        if (v < HIGH_BIT && UNRESERVED.matches(c.toString())) {
            sb.append(c)
        } else {
            sb.append('%').append(v.toString(HEX_RADIX).uppercase().padStart(2, '0'))
        }
    }
    return sb.toString()
}

private val bytesOrder =
    Comparator<String> { a, b ->
        val x = a.toByteArray(Charsets.UTF_8)
        val y = b.toByteArray(Charsets.UTF_8)
        java.util.Arrays.compareUnsigned(x, y)
    }

/**
 * Kanonik sorgu (başta `?` yok): `&` ile böl (boş parçalar atılır), ilk `=`'te anahtar/değer, `+` ve `%XX` çöz
 * (katı UTF-8), anahtara göre UTF-8 bayt sırasıyla sırala, yinelenen anahtar (çözümden sonra da) `BAD_QUERY`,
 * yalnız `A-Za-z0-9-._~` açık kalacak şekilde yeniden kodla.
 */
fun canonicalQuery(rawQuery: String): String {
    val q = rawQuery.removePrefix("?")
    val pairs = mutableListOf<Pair<String, String>>()
    for (seg in q.split('&')) {
        if (seg.isEmpty()) continue
        val eq = seg.indexOf('=')
        pairs += if (eq < 0) percentDecode(seg) to "" else percentDecode(seg.substring(0, eq)) to percentDecode(seg.substring(eq + 1))
    }
    val sorted =
        pairs.sortedWith {
                a,
                b,
            ->
            bytesOrder.compare(a.first, b.first).takeIf { it != 0 } ?: bytesOrder.compare(a.second, b.second)
        }
    for (i in 1 until sorted.size) {
        if (sorted[i].first == sorted[i - 1].first) {
            throw CanonicalException(CanonicalException.Code.BAD_QUERY, "yinelenen sorgu anahtarı: ${sorted[i].first}")
        }
    }
    return sorted.joinToString("&") { (k, v) -> "${percentEncode(k)}=${percentEncode(v)}" }
}

fun sha256Hex(body: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(body).joinToString("") { "%02x".format(it) }

/** [path] gönderildiği gibi (kodlu, sorgusuz) imzalanır; çözülmez. */
data class EnvelopeInput(
    val method: String,
    val path: String,
    val rawQuery: String = "",
    val tsMs: Long,
    /** `X-Vlad-Nonce`: tam 22 base64url karakter (16 bayt). */
    val nonce: String,
    val body: ByteArray = ByteArray(0),
)

private val METHOD = Regex("[A-Z]{3,7}")
private val NONCE = Regex("[A-Za-z0-9_-]{22}")

// JS `\s` ile aynı küme (Unicode boşlukları dahil) + kontrol karakterleri + `?` `#`.
private val BAD_PATH_CHAR =
    Regex(
        "[?#\\u0000-\\u001f\\u007f\\t\\n\\u000b\\f\\r \\u00a0\\u1680\\u2000-\\u200a\\u2028\\u2029\\u202f\\u205f\\u3000\\ufeff]",
    )

private fun envelopeError(msg: String): Nothing = throw CanonicalException(CanonicalException.Code.BAD_ENVELOPE, msg)

/**
 * `v1\n<METHOD>\n<yol[?kanonik sorgu]>\n<ts_ms>\n<nonce>\n<sha256hex(ham gövde)>` (sonda satır sonu yok).
 */
fun envelopeString(i: EnvelopeInput): String {
    if (!METHOD.matches(i.method)) envelopeError("yöntem büyük harfli olmalı")
    if (!i.path.startsWith("/") || BAD_PATH_CHAR.containsMatchIn(i.path)) envelopeError("geçersiz yol")
    if (i.tsMs < 0) envelopeError("ts_ms negatif olmayan tam sayı olmalı")
    if (!NONCE.matches(i.nonce)) envelopeError("nonce tam 22 karakter base64url olmalı (16 bayt)")
    val q = canonicalQuery(i.rawQuery)
    val target = if (q.isEmpty()) i.path else "${i.path}?$q"
    return listOf("v1", i.method, target, i.tsMs.toString(), i.nonce, sha256Hex(i.body)).joinToString("\n")
}

/** Alan sırası sabit. */
val CHALLENGE_FIELDS = listOf("action_id", "kind", "repo", "pr", "base", "head_sha", "task", "nonce", "exp")
const val CHALLENGE_HEADER = "vlad-approve/v1"
private val VALUE = Regex("[A-Za-z0-9._/#:-]+")
private val FIELD_RULES =
    mapOf(
        "action_id" to Regex("A-[A-Za-z0-9_-]{1,64}"),
        "kind" to Regex("[a-z_]{1,32}"),
        "repo" to Regex("[A-Za-z0-9._-]+/[A-Za-z0-9._-]+"),
        "pr" to Regex("[1-9][0-9]{0,9}"),
        "base" to Regex("(?!-)(?!.*\\.\\.)[A-Za-z0-9._/-]{1,200}"),
        "head_sha" to Regex("[0-9a-f]{40}"),
        "task" to Regex("T[0-9]{8}-[0-9]{2,}"),
        "nonce" to Regex("[A-Za-z0-9_-]{22}"),
        "exp" to Regex("[1-9][0-9]{0,11}"),
    )

private fun challengeError(msg: String): Nothing = throw CanonicalException(CanonicalException.Code.BAD_CHALLENGE, msg)

private fun checkChallengeField(
    k: String,
    v: String,
): String {
    if (!VALUE.matches(v) || FIELD_RULES.getValue(k).matches(v).not()) challengeError("geçersiz challenge alanı: $k")
    return v
}

/** `approval` anahtarının imzaladığı baytlar: başlık, ardından sabit sırada `anahtar=değer` satırları, `\n` ile birleşik. */
fun challengeBytes(fields: Map<String, String>): ByteArray {
    val extra = fields.keys.filter { it !in CHALLENGE_FIELDS }
    if (extra.isNotEmpty()) challengeError("fazla challenge alanı: ${extra.joinToString(",")}")
    val lines =
        CHALLENGE_FIELDS.map { k ->
            val v = fields[k] ?: challengeError("eksik challenge alanı: $k")
            "$k=${checkChallengeField(k, v)}"
        }
    return (listOf(CHALLENGE_HEADER) + lines).joinToString("\n").toByteArray(Charsets.UTF_8)
}

const val REVOKE_HEADER = "vlad-revoke/v1"
private val DEVICE_ID = Regex("[A-Za-z0-9._-]{1,64}")

/**
 * Cihaz iptali için `approval` anahtarının imzaladığı baytlar:
 * `vlad-revoke/v1\ndevice=<hedef>\nsigner=<çağıran cihaz>\nts=<ts_ms>` (sonda satır sonu yok).
 */
fun revokeBytes(
    device: String,
    signer: String,
    tsMs: Long,
): ByteArray {
    if (!DEVICE_ID.matches(device)) envelopeError("geçersiz cihaz kimliği: device")
    if (!DEVICE_ID.matches(signer)) envelopeError("geçersiz cihaz kimliği: signer")
    if (tsMs < 0) envelopeError("ts_ms negatif olmayan tam sayı olmalı")
    return listOf(REVOKE_HEADER, "device=$device", "signer=$signer", "ts=$tsMs").joinToString("\n").toByteArray(Charsets.UTF_8)
}
