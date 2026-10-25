package dev.vlad.app.ui.today

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import dev.vlad.app.ui.common.Tone
import dev.vlad.core.api.Card
import dev.vlad.core.api.CardKind
import dev.vlad.core.api.CiState
import dev.vlad.core.api.Quota
import dev.vlad.core.api.TaskState
import dev.vlad.core.api.Worker

/** Etiket = ikon + metin + ton. Bilinmeyen değerler "Bilinmeyen" olarak gösterilir (yeni sunucu enum'u çökertmez). */
data class TagSpec(val label: String, val icon: ImageVector, val tone: Tone)

object TodayText {
    const val TITLE = "Bugün"
    const val ASKS_TITLE = "Senden bekleniyor"
    const val WORKERS_TITLE = "Çalışanlar"
    const val EMPTY_ASKS = "Senden bekleyen yok"
    const val EMPTY_ASKS_BODY = "Karar gerektiğinde bildirim alırsın."
    const val EMPTY_WORKERS = "Şu an çalışan yok."
    const val RETRY = "Yeniden dene"
    const val LIMIT_BANNER = "Kullanım sınırı yakın; yeni faz başlamadı."
    const val LIMIT_WAIT = "Sınır nedeniyle bekliyor"
    const val SERVER_ERROR_TITLE = "Durum alınamadı"
    const val AUTH_LOST_TITLE = "Bu cihazın eşleşmesi kaldırıldı."
    const val AUTH_LOST_BODY = "Sunucu bu cihazı tanımıyor. Yeniden eşleştirince kaldığın yerden devam edersin."
    const val REPAIR = "Yeniden eşleştir"
    const val NO_ACTIONS_YET = "onay akışı bu sürümde yok"

    fun offlineBanner(lastUpdated: String?): String =
        if (lastUpdated == null) "Sunucuya ulaşılamıyor." else "Sunucuya ulaşılamıyor. Son güncelleme $lastUpdated."

    fun serverError(status: Int): String =
        "Sunucu $status döndü. VLAD oturumu yeniden başlıyor olabilir. Hiçbir şey kaybolmadı: durum kb/'de."
}

fun cardTag(kind: CardKind): TagSpec =
    when (kind) {
        CardKind.GATE, CardKind.ACTION_REQUEST -> TagSpec("Onay bekliyor", Icons.Filled.Lock, Tone.Tertiary)
        CardKind.CI_RED -> TagSpec("Kırmızı CI", Icons.Filled.Warning, Tone.Error)
        CardKind.CONFLICT -> TagSpec("Çakışma", Icons.Filled.Warning, Tone.Error)
        CardKind.KB_CONFLICT -> TagSpec("KB çakışması", Icons.Filled.Warning, Tone.Error)
        CardKind.VLAD_QUESTION -> TagSpec("VLAD soruyor", Icons.Filled.Info, Tone.Tertiary)
        CardKind.UNKNOWN -> TagSpec("Bilinmeyen", Icons.Filled.Info, Tone.Neutral)
    }

/** Kapı kartında CI kırmızıysa ikinci etiket. */
fun ciTag(card: Card): TagSpec? = if (card.gate?.ci == CiState.RED) TagSpec("CI kırmızı", Icons.Filled.Warning, Tone.Error) else null

/** Çalışan sınır nedeniyle mi bekliyor? Varsayım: sınır varken sırada/engelli olan çalışan (bkz. rapor, açık nokta). */
fun isLimitWaiting(
    worker: Worker,
    quota: Quota,
): Boolean = quota.limited && (worker.state == TaskState.QUEUED || worker.state == TaskState.BLOCKED)

fun workerTag(
    worker: Worker,
    quota: Quota,
): TagSpec =
    if (isLimitWaiting(worker, quota)) {
        TagSpec(TodayText.LIMIT_WAIT, Icons.Filled.DateRange, Tone.Tertiary)
    } else {
        when (worker.state) {
            TaskState.RUNNING -> TagSpec("Çalışıyor", Icons.Filled.Refresh, Tone.Primary)
            TaskState.STALLED -> TagSpec("Takıldı", Icons.Filled.Warning, Tone.Error)
            TaskState.BLOCKED -> TagSpec("Bekliyor", Icons.Filled.DateRange, Tone.Tertiary)
            TaskState.AWAITING_APPROVAL -> TagSpec("Onay bekliyor", Icons.Filled.Lock, Tone.Tertiary)
            TaskState.QUEUED -> TagSpec("Sırada", Icons.Filled.DateRange, Tone.Neutral)
            TaskState.DONE -> TagSpec("Bitti", Icons.Filled.CheckCircle, Tone.Success)
            TaskState.FAILED -> TagSpec("Başarısız", Icons.Filled.Warning, Tone.Error)
            TaskState.CANCELLED -> TagSpec("İptal edildi", Icons.Filled.Info, Tone.Neutral)
            TaskState.UNKNOWN -> TagSpec("Bilinmeyen", Icons.Filled.Info, Tone.Neutral)
        }
    }

/** Bu dilimde bütün eylemler pasiftir; düğme adı + neden TalkBack'e okunur. */
data class ActionSpec(val label: String, val primary: Boolean, val disabledReason: String)

fun cardActions(card: Card): List<ActionSpec> {
    val gateReason =
        when {
            card.gate?.ci == CiState.RED -> "CI kırmızı"
            card.gate?.mergeable == false -> "çakışma var"
            else -> TodayText.NO_ACTIONS_YET
        }
    val generic = TodayText.NO_ACTIONS_YET
    return when (card.kind) {
        CardKind.GATE ->
            listOf(
                ActionSpec("Merge et…", true, gateReason),
                ActionSpec("Rapor", false, generic),
                ActionSpec("Değişiklik iste", false, generic),
            )
        CardKind.ACTION_REQUEST -> listOf(ActionSpec("Onayla…", true, generic), ActionSpec("Vazgeç", false, generic))
        CardKind.VLAD_QUESTION -> listOf(ActionSpec("Cevapla", true, generic))
        CardKind.CI_RED, CardKind.CONFLICT -> listOf(ActionSpec("Ayrıntı", true, generic), ActionSpec("VLAD'a sor", false, generic))
        CardKind.KB_CONFLICT, CardKind.UNKNOWN -> listOf(ActionSpec("Ayrıntı", true, generic))
    }
}

/** Rapor markdown'ından kart özeti: başlık/madde imlerini atıp ilk 3 satır. */
fun cardSummary(card: Card): String? =
    card.gate?.reportMd?.lineSequence()
        ?.map { it.trim().trimStart('#', '-', '*', ' ') }
        ?.filter { it.isNotEmpty() }
        ?.take(SUMMARY_LINES)
        ?.joinToString("\n")
        ?.takeIf { it.isNotEmpty() }

private const val SUMMARY_LINES = 3
