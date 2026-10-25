package dev.vlad.app.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.ProviderException
import java.security.Signature
import java.security.spec.ECGenParameterSpec

/** Anahtar deposu soyutlaması: üretimde Android Keystore, birim testte yazılım sağlayıcısı (Robolectric'te Keystore yok). */
interface KeyBackend {
    /** Yoksa P-256 imza anahtarı üretir. [requireUserAuth]: her kullanımda kullanıcı doğrulaması (zaman penceresi yok). */
    fun ensureKey(
        alias: String,
        requireUserAuth: Boolean,
    )

    fun has(alias: String): Boolean

    /** X.509 SubjectPublicKeyInfo (DER). */
    fun publicKeySpki(alias: String): ByteArray

    /** `SHA256withECDSA`, `initSign` edilmiş. Kullanıcı doğrulaması gereken anahtar için BiometricPrompt `CryptoObject`'i bunu sarar. */
    fun newSignature(alias: String): Signature

    fun delete(alias: String)
}

class AndroidKeyBackend : KeyBackend {
    private val store: KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    override fun ensureKey(
        alias: String,
        requireUserAuth: Boolean,
    ) {
        if (store.containsAlias(alias)) return
        val strongBox = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        try {
            generate(alias, requireUserAuth, strongBox)
        } catch (e: ProviderException) {
            // StrongBox yok (API 28+ `StrongBoxUnavailableException`, bunun alt sınıfı): yazılım destekli TEE'ye düş.
            if (!strongBox) throw e
            generate(alias, requireUserAuth, strongBox = false)
        }
    }

    private fun generate(
        alias: String,
        requireUserAuth: Boolean,
        strongBox: Boolean,
    ) {
        val spec =
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setUserAuthenticationRequired(requireUserAuth)
                .apply {
                    if (requireUserAuth) {
                        // Cihaz kilitliyken anahtar kullanılamaz (API 28+).
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) setUnlockedDeviceRequired(true)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            // Zaman penceresi 0 = her imzada doğrulama (BiometricPrompt CryptoObject).
                            setUserAuthenticationParameters(
                                0,
                                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            setUserAuthenticationValidityDurationSeconds(-1)
                        }
                    }
                    if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) setIsStrongBoxBacked(true)
                }
                .build()
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, PROVIDER).run {
            initialize(spec)
            generateKeyPair()
        }
    }

    override fun has(alias: String): Boolean = store.containsAlias(alias)

    override fun publicKeySpki(alias: String): ByteArray =
        requireNotNull(store.getCertificate(alias)) {
            "anahtar yok: $alias"
        }.publicKey.encoded

    override fun newSignature(alias: String): Signature {
        val key = store.getKey(alias, null) as? PrivateKey ?: error("anahtar yok: $alias")
        return Signature.getInstance(ALGORITHM).apply { initSign(key) }
    }

    override fun delete(alias: String) {
        if (store.containsAlias(alias)) store.deleteEntry(alias)
    }

    companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val ALGORITHM = "SHA256withECDSA"
    }
}
