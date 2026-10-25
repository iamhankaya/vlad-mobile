package dev.vlad.app.security

import dev.vlad.core.crypto.B64Url
import dev.vlad.core.crypto.Der
import dev.vlad.core.crypto.Signatures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Provider
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureSpi
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.ECGenParameterSpec

/** Yazılım P-256 anahtarlı sahte depo (Keystore Robolectric/JVM'de yok). [forceHighS]: her ham imzayı yüksek-S'e çevirir. */
private class SoftwareKeyBackend(private val forceHighS: Boolean = false) : KeyBackend {
    private val keys = mutableMapOf<String, java.security.KeyPair>()
    val authRequired = mutableMapOf<String, Boolean>()

    override fun ensureKey(
        alias: String,
        requireUserAuth: Boolean,
    ) {
        if (alias in keys) return
        keys[alias] = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        authRequired[alias] = requireUserAuth
    }

    override fun publicKeySpki(alias: String): ByteArray = keys.getValue(alias).public.encoded

    override fun newSignature(alias: String): Signature {
        val key = keys.getValue(alias).private
        val sig = if (forceHighS) Signature.getInstance(HighSProvider.ALG, HighSProvider) else Signature.getInstance("SHA256withECDSA")
        return sig.apply { initSign(key) }
    }

    override fun delete(alias: String) {
        keys.remove(alias)
    }
}

/** Ham imzayı kasıtlı yüksek-S yapan imza sağlayıcısı: `Der.normalizeLowS` atlanırsa test düşer. */
private object HighSProvider : Provider("HighS", 1.0, "test") {
    const val ALG = "HIGHSECDSA"

    init {
        put("Signature.$ALG", HighSSpi::class.java.name)
    }

    class HighSSpi : SignatureSpi() {
        private val delegate = Signature.getInstance("SHA256withECDSA")

        override fun engineInitVerify(publicKey: PublicKey) = delegate.initVerify(publicKey)

        override fun engineInitSign(privateKey: PrivateKey) = delegate.initSign(privateKey)

        override fun engineUpdate(b: Byte) = delegate.update(b)

        override fun engineUpdate(
            b: ByteArray,
            off: Int,
            len: Int,
        ) = delegate.update(b, off, len)

        override fun engineVerify(sigBytes: ByteArray): Boolean = delegate.verify(sigBytes)

        override fun engineSign(): ByteArray {
            val rs = Der.parseStrict(delegate.sign())!!
            val high = if (rs.s > Der.P256_HALF_N) rs.s else Der.P256_N.subtract(rs.s)
            return Der.encode(rs.r, high)
        }

        @Deprecated("eski API")
        override fun engineSetParameter(
            param: String?,
            value: Any?,
        ) = Unit

        @Deprecated("eski API")
        override fun engineGetParameter(param: String?): Any? = null

        override fun engineSetParameter(params: AlgorithmParameterSpec?) = Unit
    }
}

class KeystoreSignerTest {
    private val message =
        (
            "v1\nGET\n/v1/home\n1761350400000\nAAECAwQFBgcICQoLDA0ODw\n" +
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        ).toByteArray()

    @Test
    fun `mobile-app signer 300 signatures are strict der low-s and verify with core`() {
        val signer = SessionSigner(SoftwareKeyBackend())
        val pub = signer.publicKeyB64u()
        repeat(300) {
            val sig = signer.sign(message)
            assertTrue("katı DER + low-S", Der.isCanonical(sig))
            assertTrue("Signatures.verify", Signatures.verify(pub, message, B64Url.encode(sig)))
        }
    }

    @Test
    fun `mobile-app signer normalizes a provider that returns high-S`() {
        val backend = SoftwareKeyBackend(forceHighS = true)
        val signer = SessionSigner(backend)
        // Sağlayıcının ham çıktısı gerçekten yüksek-S (testin kendisini doğrular).
        val raw = signer.newSignature().apply { update(message) }.sign()
        assertFalse("ham imza yüksek-S olmalı", Der.isCanonical(raw))
        repeat(50) {
            val sig = signer.sign(message)
            assertTrue(Der.isCanonical(sig))
            assertTrue(Signatures.verify(signer.publicKeyB64u(), message, B64Url.encode(sig)))
        }
    }

    @Test
    fun `mobile-app approval key is distinct and requires user authentication`() {
        val backend = SoftwareKeyBackend()
        val session = SessionSigner(backend)
        val approval = ApprovalSigner(backend)
        assertNotEquals(session.alias, approval.alias)
        assertNotEquals(session.publicKeyB64u(), approval.publicKeyB64u())
        assertEquals(false, backend.authRequired[session.alias])
        assertEquals(true, backend.authRequired[approval.alias])
        // Biyometrik akışın (S9c) kullanacağı yol da low-S üretir.
        val sig = approval.finish(approval.newSignature(), message)
        assertTrue(Der.isCanonical(sig))
        assertTrue(Signatures.verify(approval.publicKeyB64u(), message, B64Url.encode(sig)))
        // Session anahtarıyla approval imzası doğrulanmaz.
        assertFalse(Signatures.verify(session.publicKeyB64u(), message, B64Url.encode(sig)))
    }

    @Test
    fun `mobile-app delete key then new key is generated`() {
        val backend = SoftwareKeyBackend()
        val signer = SessionSigner(backend)
        val first = signer.publicKeyB64u()
        signer.deleteKey()
        assertNotEquals(first, signer.publicKeyB64u())
    }
}
