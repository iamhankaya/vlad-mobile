package dev.vlad.app.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.vlad.app.ui.common.CodeChip
import dev.vlad.app.ui.common.StatusTag
import dev.vlad.app.ui.common.Tone
import dev.vlad.app.ui.common.colors
import dev.vlad.app.ui.theme.Spacing
import dev.vlad.core.api.Card
import dev.vlad.core.api.CardKind

const val TAG_ASK_CARD = "ask-card"

/**
 * "Senden bekleniyor" kartı. Sol 4 dp şerit durum rengi. Bu dilimde eylem düğmeleri **pasif**; her pasif düğme
 * TalkBack'e "<ad>, pasif: <neden>" okur (örn. kırmızı CI'da "Merge et…, pasif: CI kırmızı").
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DecisionCard(
    card: Card,
    modifier: Modifier = Modifier,
) {
    val tag = cardTag(card.kind)
    val failing = card.kind == CardKind.CI_RED || card.kind == CardKind.CONFLICT || ciTag(card) != null
    val accent = (if (failing) Tone.Error else tag.tone).colors().accent
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .drawBehind { drawRect(accent, size = Size(STRIPE_DP.dp.toPx(), size.height)) }
                .testTag(TAG_ASK_CARD),
    ) {
        Column(
            modifier =
                Modifier.weight(
                    1f,
                ).padding(start = Spacing.s4 + STRIPE_DP.dp, end = Spacing.s4, top = Spacing.s3, bottom = Spacing.s3),
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                StatusTag(tag.label, tag.icon, tag.tone)
                ciTag(card)?.let { StatusTag(it.label, it.icon, it.tone) }
            }
            val phase = card.gate?.phase
            Text(
                if (phase != null) "${card.project} · $phase" else card.project,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(card.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            cardSummary(card)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            card.gate?.let { g ->
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                    g.pr?.let { CodeChip("PR #${it.number} → ${it.base}") }
                    CodeChip(g.headSha.take(SHORT_SHA))
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.touchGap),
                verticalArrangement = Arrangement.spacedBy(Spacing.touchGap),
            ) {
                cardActions(card).forEach { PassiveButton(it) }
            }
        }
    }
}

@Composable
private fun PassiveButton(action: ActionSpec) {
    val mod =
        Modifier.heightIn(min = Spacing.minTouch).semantics {
            contentDescription = "${action.label}, pasif: ${action.disabledReason}"
            stateDescription = "pasif: ${action.disabledReason}"
        }
    if (action.primary) {
        Button(onClick = {}, enabled = false, modifier = mod) { Text(action.label) }
    } else {
        OutlinedButton(onClick = {}, enabled = false, modifier = mod) { Text(action.label) }
    }
}

private const val SHORT_SHA = 7
private const val STRIPE_DP = 4
