package dev.vlad.core.crypto

import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/** Dolgusuz base64url (sözleşmedeki tüm ikili değerler). */
object B64Url {
    fun encode(b: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(b)

    fun decode(s: String): ByteArray = Base64.getUrlDecoder().decode(s)
}

object Signatures {
    fun publicKey(spkiB64u: String): PublicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(B64Url.decode(spkiB64u)))

    /**
     * Yalnız doğrulama yönü: katı minimal DER + low-S, sonra ECDSA P-256 SHA-256. Bozuk anahtar/imza/base64
     * çökmez, "geçersiz" (false) döner.
     */
    fun verify(
        spkiB64u: String,
        message: ByteArray,
        sigB64u: String,
    ): Boolean =
        try {
            val sig = B64Url.decode(sigB64u)
            Der.isCanonical(sig) &&
                Signature.getInstance("SHA256withECDSA").run {
                    initVerify(publicKey(spkiB64u))
                    update(message)
                    verify(sig)
                }
        } catch (_: java.security.GeneralSecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }

    /** İmzalama yönü (testler ve cihaz katmanı için): JCA imzasını low-S'e normalize eder. */
    fun signLowS(
        privateKey: java.security.PrivateKey,
        message: ByteArray,
    ): ByteArray {
        val raw =
            Signature.getInstance("SHA256withECDSA").run {
                initSign(privateKey)
                update(message)
                sign()
            }
        return Der.normalizeLowS(raw)
    }
}
