package dev.vlad.app.ui.pairing

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vlad.app.AppContainer
import dev.vlad.app.data.PairedDevice
import dev.vlad.core.api.ApiFailure
import dev.vlad.core.api.ApiResult
import dev.vlad.core.api.ErrorCode
import dev.vlad.core.api.PairRequest
import dev.vlad.core.api.ServerAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.ProviderException

enum class PairingError(val message: String) {
    BAD_ADDRESS("Adres geçersiz. https:// ile başlayan sunucu adresini yaz (yol ve parola olmadan)."),
    BAD_CODE("Kodu yaz (sunucuda `vlad-server pair` ile üretilir)."),
    CODE_REJECTED("Kod süresi doldu ya da kullanıldı. Sunucuda yeni kod üret."),
    UNREACHABLE("Sunucuya ulaşılamıyor. Adresi ve internetini kontrol et."),
    RATE_LIMITED("Çok fazla deneme. Biraz bekleyip tekrar dene."),
    KEY_REJECTED("Sunucu bu cihaz anahtarını kabul etmedi."),
    NO_SECURE_LOCK("Onay anahtarı için ekran kilidi (PIN, desen ya da parmak izi) gerekli. Cihaz kilidini ayarla."),
    UNEXPECTED("Eşleştirme tamamlanamadı."),
}

data class PairingUiState(
    val address: String = "",
    val code: String = "",
    val busy: Boolean = false,
    val error: PairingError? = null,
)

class PairingViewModel(private val container: AppContainer, private val onPaired: (PairedDevice) -> Unit) : ViewModel() {
    private val _state = MutableStateFlow(PairingUiState())
    val state: StateFlow<PairingUiState> = _state.asStateFlow()

    fun setAddress(v: String) = _state.update { it.copy(address = v, error = null) }

    fun setCode(v: String) = _state.update { it.copy(code = v, error = null) }

    fun submit() {
        val s = _state.value
        if (s.busy) return
        val server = ServerAddress.parse(s.address)
        val code = s.code.trim().uppercase()
        val early =
            when {
                server == null -> PairingError.BAD_ADDRESS
                code.isEmpty() -> PairingError.BAD_CODE
                else -> null
            }
        if (early != null || server == null) {
            _state.update { it.copy(error = early) }
            return
        }
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) { pair(server, code) }
            when (outcome) {
                is Outcome.Paired -> onPaired(outcome.device)
                is Outcome.Failed -> _state.update { it.copy(busy = false, error = outcome.error) }
            }
        }
    }

    private sealed interface Outcome {
        data class Paired(val device: PairedDevice) : Outcome

        data class Failed(val error: PairingError) : Outcome
    }

    @Suppress("TooGenericExceptionCaught")
    private fun pair(
        server: String,
        code: String,
    ): Outcome {
        val request =
            try {
                // Telefon kendi anahtarlarını üretir; özel anahtar Keystore'dan çıkmaz, yalnız açık anahtarlar gider.
                PairRequest(
                    code = code,
                    deviceName = Build.MODEL.take(MAX_NAME),
                    sessionPub = container.sessionSigner.publicKeyB64u(),
                    approvalPub = container.approvalSigner.publicKeyB64u(),
                )
            } catch (e: Exception) {
                return Outcome.Failed(
                    if (e is IllegalStateException || e is ProviderException) PairingError.NO_SECURE_LOCK else PairingError.UNEXPECTED,
                )
            }
        val before = System.currentTimeMillis()
        return when (val r = container.api(server).pair(request)) {
            is ApiResult.Ok -> {
                val device = PairedDevice(server, r.value.deviceId, r.value.serverTime - before)
                container.store.save(device)
                Outcome.Paired(device)
            }
            is ApiResult.Failed -> Outcome.Failed(map(r.failure))
        }
    }

    private fun map(f: ApiFailure): PairingError =
        when (f) {
            is ApiFailure.Network -> PairingError.UNREACHABLE
            is ApiFailure.BadResponse -> PairingError.UNEXPECTED
            is ApiFailure.Http ->
                when (f.error?.code) {
                    ErrorCode.UNAUTHORIZED -> PairingError.CODE_REJECTED
                    ErrorCode.RATE_LIMITED -> PairingError.RATE_LIMITED
                    ErrorCode.BAD_REQUEST -> PairingError.KEY_REJECTED
                    else -> if (f.status == HTTP_TOO_MANY) PairingError.RATE_LIMITED else PairingError.UNEXPECTED
                }
        }

    private companion object {
        const val MAX_NAME = 40
        const val HTTP_TOO_MANY = 429
    }
}
