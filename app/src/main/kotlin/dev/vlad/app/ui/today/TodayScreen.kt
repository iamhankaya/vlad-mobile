package dev.vlad.app.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.vlad.app.ui.common.Banner
import dev.vlad.app.ui.common.EmptyState
import dev.vlad.app.ui.common.ErrorCard
import dev.vlad.app.ui.common.SkeletonCard
import dev.vlad.app.ui.common.Tone
import dev.vlad.app.ui.theme.Spacing
import dev.vlad.core.repo.TodaySnapshot
import dev.vlad.core.repo.sortAsks
import dev.vlad.core.reduce.Banner as BannerKind

const val TAG_OFFLINE_BANNER = "offline-banner"
const val TAG_LIMIT_BANNER = "limit-banner"

/**
 * "Bugün" ekranı, salt okunur (S7b). Durumsuzdur: [snapshot] her şeyi belirler (Compose testi ve Roborazzi aynı girdiyle çizer).
 * [formatTime] son güncelleme saatini yazar; testte sabit verilir.
 */
@Composable
fun TodayScreen(
    snapshot: TodaySnapshot,
    onRetry: () -> Unit,
    onRepair: () -> Unit,
    modifier: Modifier = Modifier,
    formatTime: (Long) -> String = { it.toString() },
) {
    val home = snapshot.state.home
    val banner = snapshot.state.banner
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            TodayText.TITLE,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s3).semantics { heading() },
        )
        if (banner == BannerKind.AUTH_LOST) {
            AuthLost(onRepair)
            return@Column
        }
        if (banner == BannerKind.OFFLINE) {
            Banner(
                text = TodayText.offlineBanner(snapshot.lastUpdatedMs?.let(formatTime)),
                icon = Icons.Filled.Warning,
                tone = Tone.Neutral,
                modifier = Modifier.testTag(TAG_OFFLINE_BANNER),
                actionLabel = TodayText.RETRY,
                onAction = onRetry,
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = Spacing.contentMaxWidth).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.s2),
            ) {
                if (home == null) {
                    if (snapshot.serverStatus != null) {
                        item { ServerError(snapshot.serverStatus!!, onRetry) }
                    } else if (snapshot.loading) {
                        items(2) { SkeletonCard(Modifier.padding(horizontal = Spacing.s4)) }
                    } else if (banner == BannerKind.NONE) {
                        item { EmptyState(Icons.Filled.CheckCircle, TodayText.EMPTY_ASKS, TodayText.EMPTY_ASKS_BODY) }
                    }
                } else {
                    if (snapshot.serverStatus != null) item { ServerError(snapshot.serverStatus!!, onRetry) }
                    item { SectionTitle("${TodayText.ASKS_TITLE} (${home.asks.size})") }
                    if (home.asks.isEmpty()) {
                        item { EmptyState(Icons.Filled.CheckCircle, TodayText.EMPTY_ASKS, TodayText.EMPTY_ASKS_BODY) }
                    } else {
                        items(sortAsks(home.asks), key = { it.id }) { DecisionCard(it, Modifier.padding(horizontal = Spacing.s4)) }
                    }
                    item { SectionTitle(TodayText.WORKERS_TITLE) }
                    if (home.workers.isEmpty()) {
                        item {
                            Text(
                                TodayText.EMPTY_WORKERS,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = Spacing.s4),
                            )
                        }
                    } else {
                        items(home.workers, key = { it.taskId }) { w ->
                            WorkerRow(w, home.quota)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                    if (home.quota.limited) {
                        item {
                            Banner(
                                text = TodayText.LIMIT_BANNER,
                                icon = Icons.Filled.DateRange,
                                tone = Tone.Tertiary,
                                modifier = Modifier.testTag(TAG_LIMIT_BANNER),
                            )
                        }
                    }
                }
            }
        }
        ChatDock()
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2).semantics { heading() },
    )
}

@Composable
private fun ServerError(
    status: Int,
    onRetry: () -> Unit,
) {
    ErrorCard(
        title = TodayText.SERVER_ERROR_TITLE,
        body = TodayText.serverError(status),
        retryLabel = TodayText.RETRY,
        onRetry = onRetry,
        modifier = Modifier.padding(horizontal = Spacing.s4),
    )
}

@Composable
private fun AuthLost(onRepair: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.s4),
        verticalArrangement = Arrangement.spacedBy(Spacing.s3, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(TodayText.AUTH_LOST_TITLE, style = MaterialTheme.typography.titleMedium)
        Text(TodayText.AUTH_LOST_BODY, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onRepair) { Text(TodayText.REPAIR) }
    }
}
