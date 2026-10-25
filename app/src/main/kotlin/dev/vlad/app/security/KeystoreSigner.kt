package dev.vlad.app.security

import dev.vlad.core.api.DeviceSigner
import dev.vlad.core.crypto.B64Url
import dev.vlad.core.crypto.Der
import java.security.Signature

/**
 * Keystore P-256 imzalayıcısı. **Her imza `Der.normalizeLowS`'ten geçer** (bulgu F-vlad-017): Android Keystore imzalarının
 * S değeri rastgeledir, normalize edilmezse sunucu (katı DER + low-S) imzaların yaklaşık yarısını 401 ile reddeder.
 *
 * İki anahtar ayrı takma adlarla tutulur ve türleri de ayrıdır: [SessionSigner] API isteklerini ([DeviceSigner]),
 * [ApprovalSigner] yalnız onay/devir/iptal baytlarını imzalar. `ApprovalSigner` [DeviceSigner] değildir, bu yüzden
 * istek imzalama yoluna yanlışlıkla verilemez.
 */
sealed class KeystoreSigner(
    private val backend: KeyBackend,
    val alias: String,
    val requiresUserAuth: Boolean,
) {
    /** Anahtar yoksa üretir; yalnız açık anahtarı okuyan eşleştirme akışı da bunu çağırır. */
    fun ensureKey() = backend.ensureKey(alias, requiresUserAuth)

    fun publicKeyB64u(): String {
        ensureKey()
        return B64Url.encode(backend.publicKeySpki(alias))
    }

    /** `initSign` edilmiş imza nesnesi. Kullanıcı doğrulamalı anahtarda BiometricPrompt `CryptoObject`'ine verilir (S9c). */
    fun newSignature(): Signature {
        ensureKey()
        return backend.newSignature(alias)
    }

    /** [signature] (ör. biyometrik doğrulanmış) ile [message]'ı imzalar; çıktı katı DER + low-S. */
    fun finish(
        signature: Signature,
        message: ByteArray,
    ): ByteArray {
        signature.update(message)
        return Der.normalizeLowS(signature.sign())
    }

    fun deleteKey() = backend.delete(alias)

    companion object {
        const val SESSION_ALIAS = "vlad.session.v1"
        const val APPROVAL_ALIAS = "vlad.approval.v1"
    }
}

/** `session` anahtarı: kullanıcı doğrulaması yok, okuma/sohbet istekleri. */
class SessionSigner(backend: KeyBackend) : KeystoreSigner(backend, SESSION_ALIAS, requiresUserAuth = false), DeviceSigner {
    override fun sign(message: ByteArray): ByteArray = finish(newSignature(), message)
}

/**
 * `approval` anahtarı: her kullanımda kullanıcı doğrulaması ister (`setUserAuthenticationRequired(true)`).
 * BiometricPrompt akışı S9c'de; bu fazda yalnız anahtar üretilir ve açık anahtarı eşleştirmede gönderilir.
 */
class ApprovalSigner(backend: KeyBackend) : KeystoreSigner(backend, APPROVAL_ALIAS, requiresUserAuth = true)
