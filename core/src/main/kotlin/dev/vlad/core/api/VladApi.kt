package dev.vlad.core.api

import dev.vlad.core.crypto.B64Url
import dev.vlad.core.crypto.Der
import dev.vlad.core.crypto.EnvelopeInput
import dev.vlad.core.crypto.envelopeString
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.security.GeneralSecurityException
import java.security.SecureRandom

class HttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String>,
    val body: ByteArray,
)

class HttpResponse(
    val status: Int,
    val body: ByteArray,
)

/** Ağ katmanı (saf arayüz; testte sahte). Ağ hatasında [IOException] fırlatır; 4xx/5xx de [HttpResponse] döner. */
fun interface Transport {
    @Throws(IOException::class)
    fun execute(request: HttpRequest): HttpResponse
}

/** İmza yönü. [sign] çıktısı katı DER ve low-S olmalıdır (Keystore imzalayıcısı `Der.normalizeLowS` uygular). */
fun interface DeviceSigner {
    fun sign(message: ByteArray): ByteArray
}

/** Eşleşmiş cihaz: `X-Vlad-Device` ve `session` anahtarı. */
class SessionCredentials(val deviceId: String, val signer: DeviceSigner)

sealed interface ApiFailure {
    data class Network(val message: String) : ApiFailure

    data class Http(val status: Int, val error: ErrorBody?) : ApiFailure

    data class BadResponse(val message: String) : ApiFailure

    /** İmzalanamadı (Keystore hatası, anahtar yok/geçersiz, canonical olmayan imza). İstek gönderilmedi. */
    data class Signing(val message: String) : ApiFailure

    /** Sunucu hata kodu (varsa); ağ hatasında `null`. */
    val code: ErrorCode? get() = (this as? Http)?.error?.code
}

sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>

    data class Failed(val failure: ApiFailure) : ApiResult<Nothing>
}

/**
 * Sunucu adresi: kullanıcıdan alınır, uygulamada sabit adres yok. Yalnız `https://host[:port]` kökü kabul edilir
 * (yol imzaya girer; ön ek olsaydı sunucunun gördüğü yol değişirdi), kimlik bilgisi/sorgu/parça yok.
 */
object ServerAddress {
    fun parse(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return null
        val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
        val uri =
            try {
                URI(withScheme)
            } catch (_: URISyntaxException) {
                return null
            }
        val host = uri.host
        val ok =
            uri.scheme == "https" && !host.isNullOrEmpty() && uri.userInfo == null && uri.rawQuery == null &&
                uri.rawFragment == null && (uri.rawPath.isNullOrEmpty() || uri.rawPath == "/")
        if (!ok) return null
        return if (uri.port >= 0) "https://$host:${uri.port}" else "https://$host"
    }
}

private const val NONCE_BYTES = 16
private const val SERVER_ERROR = 500
private const val HTTP_OK_MIN = 200
private const val HTTP_OK_MAX = 299

class VladApi(
    baseUrl: String,
    private val transport: Transport,
    private val clock: () -> Long = System::currentTimeMillis,
    private val nonce: () -> String = ::randomNonce,
) {
    private val base = baseUrl.trimEnd('/')

    fun pair(request: PairRequest): ApiResult<PairResponse> {
        val body = VladJson.encodeToString(request).toByteArray(Charsets.UTF_8)
        val headers = mapOf("Content-Type" to "application/json", "Accept" to "application/json")
        return call(HttpRequest("POST", "$base/v1/pair", headers, body)) { VladJson.decodeFromString<PairResponse>(it) }
    }

    fun getHome(creds: SessionCredentials): ApiResult<Home> = signed("GET", "/v1/home", creds) { VladJson.decodeFromString<Home>(it) }

    /** Zarf (`:core` [envelopeString]) `session` anahtarıyla imzalanır; yol olduğu gibi gönderilir ve imzalanır. */
    @Suppress("TooGenericExceptionCaught")
    private fun <T> signed(
        method: String,
        path: String,
        creds: SessionCredentials,
        decode: (String) -> T,
    ): ApiResult<T> {
        val ts = clock()
        val n = nonce()
        val message = envelopeString(EnvelopeInput(method = method, path = path, tsMs = ts, nonce = n)).toByteArray(Charsets.UTF_8)
        val sig =
            try {
                creds.signer.sign(message).also {
                    // Düşük-S/katı DER değilse sunucu 401 verirdi; göndermeden düşür (fail closed).
                    check(Der.isCanonical(it)) { "imza katı DER + low-S değil" }
                }
            } catch (e: GeneralSecurityException) {
                return ApiResult.Failed(ApiFailure.Signing(e.message ?: e.javaClass.simpleName))
            } catch (e: RuntimeException) {
                // Keystore ProviderException/IllegalStateException vb.: uygulama çökmez, yeniden eşleştirme önerilir.
                return ApiResult.Failed(ApiFailure.Signing(e.message ?: e.javaClass.simpleName))
            }
        val headers =
            mapOf(
                "Accept" to "application/json",
                "X-Vlad-Device" to creds.deviceId,
                "X-Vlad-Ts" to ts.toString(),
                "X-Vlad-Nonce" to n,
                "X-Vlad-Sig" to B64Url.encode(sig),
            )
        return call(HttpRequest(method, "$base$path", headers, ByteArray(0)), decode)
    }

    private fun <T> call(
        request: HttpRequest,
        decode: (String) -> T,
    ): ApiResult<T> {
        val response =
            try {
                transport.execute(request)
            } catch (e: IOException) {
                return ApiResult.Failed(ApiFailure.Network(e.message ?: e.javaClass.simpleName))
            }
        val text = response.body.toString(Charsets.UTF_8)
        return try {
            if (response.status in HTTP_OK_MIN..HTTP_OK_MAX) {
                ApiResult.Ok(decode(text))
            } else {
                val err = runCatching { VladJson.decodeFromString<ErrorResponse>(text).error }.getOrNull()
                ApiResult.Failed(ApiFailure.Http(response.status, err))
            }
        } catch (e: SerializationException) {
            ApiResult.Failed(ApiFailure.BadResponse(e.message ?: "yanıt çözülemedi"))
        } catch (e: IllegalArgumentException) {
            ApiResult.Failed(ApiFailure.BadResponse(e.message ?: "yanıt çözülemedi"))
        }
    }

    companion object {
        fun isServerError(f: ApiFailure): Boolean = f is ApiFailure.Http && f.status >= SERVER_ERROR
    }
}

private val secureRandom = SecureRandom()

/** `X-Vlad-Nonce`: 16 rastgele bayt, dolgusuz base64url (22 karakter). */
fun randomNonce(): String = B64Url.encode(ByteArray(NONCE_BYTES).also { secureRandom.nextBytes(it) })
