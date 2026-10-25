package dev.vlad.app

import android.app.Application
import android.content.Context
import dev.vlad.app.data.PairedDevice
import dev.vlad.app.data.PairingStore
import dev.vlad.app.security.AndroidKeyBackend
import dev.vlad.app.security.ApprovalSigner
import dev.vlad.app.security.KeyBackend
import dev.vlad.app.security.SessionSigner
import dev.vlad.core.api.HttpUrlTransport
import dev.vlad.core.api.SessionCredentials
import dev.vlad.core.api.Transport
import dev.vlad.core.api.VladApi
import dev.vlad.core.repo.HomeRepository

/** Elle bağlanan bağımlılıklar (DI çatısı yok). */
class AppContainer(context: Context) {
    val keyBackend: KeyBackend = AndroidKeyBackend()
    val sessionSigner = SessionSigner(keyBackend)
    val approvalSigner = ApprovalSigner(keyBackend)
    val store = PairingStore(context)
    val transport: Transport = HttpUrlTransport()

    fun api(
        serverUrl: String,
        clockOffsetMs: Long = 0L,
    ): VladApi = VladApi(serverUrl, transport, clock = { System.currentTimeMillis() + clockOffsetMs })

    fun homeRepository(device: PairedDevice): HomeRepository {
        val creds = SessionCredentials(device.deviceId, sessionSigner)
        return HomeRepository(api(device.serverUrl, device.clockOffsetMs), creds) { System.currentTimeMillis() }
    }

    /** "Bu cihazdan çık": kayıt silinir, iki Keystore anahtarı da silinir (yeniden eşleşmede yenileri üretilir). */
    fun unpair() {
        store.clear()
        sessionSigner.deleteKey()
        approvalSigner.deleteKey()
    }
}

class VladApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
