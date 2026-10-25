package dev.vlad.app.data

import android.content.Context
import android.content.SharedPreferences

/** Eşleşme kaydı. Anahtar malzemesi burada değildir (Keystore'dan çıkmaz); sunucu adresi yalnız kullanıcıdan gelir. */
data class PairedDevice(
    val serverUrl: String,
    val deviceId: String,
    /** `server_time - cihaz saati` (ms): zarf zaman damgası sunucu saatine yakın kalsın (±120 sn penceresi). */
    val clockOffsetMs: Long,
)

class PairingStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(): PairedDevice? {
        val url = prefs.getString(KEY_URL, null)
        val id = prefs.getString(KEY_DEVICE, null)
        if (url == null || id == null) return null
        return PairedDevice(url, id, prefs.getLong(KEY_OFFSET, 0L))
    }

    fun save(device: PairedDevice) {
        prefs.edit()
            .putString(KEY_URL, device.serverUrl)
            .putString(KEY_DEVICE, device.deviceId)
            .putLong(KEY_OFFSET, device.clockOffsetMs)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val FILE = "vlad_pairing"
        const val KEY_URL = "server_url"
        const val KEY_DEVICE = "device_id"
        const val KEY_OFFSET = "clock_offset_ms"
    }
}
