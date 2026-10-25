package dev.vlad.app.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.vlad.app.ui.common.StatusTag
import dev.vlad.app.ui.theme.Spacing
import dev.vlad.core.api.Quota
import dev.vlad.core.api.Worker

const val TAG_WORKER_ROW = "worker-row"

private const val PCT_MAX = 100f

/** Çalışan satırı (kart değil): ad · faz, durum etiketi, son adım, ilerleme varsa çubuk. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkerRow(
    worker: Worker,
    quota: Quota,
    modifier: Modifier = Modifier,
) {
    val tag = workerTag(worker, quota)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = Spacing.minTouch)
                .padding(horizontal = Spacing.s4, vertical = Spacing.s3)
                .testTag(TAG_WORKER_ROW),
        verticalArrangement = Arrangement.spacedBy(Spacing.s1),
    ) {
        Text(
            if (worker.phase != null) "${worker.title} · ${worker.phase}" else worker.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            StatusTag(tag.label, tag.icon, tag.tone)
        }
        if (isLimitWaiting(worker, quota)) {
            Text(
                TodayText.LIMIT_BANNER,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        worker.progress?.let { p ->
            Text(
                p.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            p.pct?.let { pct ->
                LinearProgressIndicator(
                    progress = { pct / PCT_MAX },
                    modifier =
                        Modifier.fillMaxWidth().semantics {
                            progressBarRangeInfo = ProgressBarRangeInfo(pct.toFloat(), 0f..PCT_MAX)
                            contentDescription = "İlerleme yüzde $pct"
                        },
                )
            }
        }
    }
}
