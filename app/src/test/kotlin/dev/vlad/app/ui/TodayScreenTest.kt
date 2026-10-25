package dev.vlad.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import dev.vlad.app.ui.theme.VladTheme
import dev.vlad.app.ui.today.TAG_ASK_CARD
import dev.vlad.app.ui.today.TAG_CHAT_DOCK
import dev.vlad.app.ui.today.TAG_LIMIT_BANNER
import dev.vlad.app.ui.today.TAG_OFFLINE_BANNER
import dev.vlad.app.ui.today.TAG_WORKER_ROW
import dev.vlad.app.ui.today.TodayScreen
import dev.vlad.core.reduce.Connection
import dev.vlad.core.reduce.HomeState
import dev.vlad.core.repo.TodaySnapshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class TodayScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(snapshot: TodaySnapshot) {
        rule.setContent {
            VladTheme(darkTheme = false) {
                TodayScreen(snapshot, onRetry = {}, onRepair = {}, formatTime = { "14:02" })
            }
        }
    }

    @Test
    fun `mobile-app today shows one card per ask and one row per worker`() {
        show(TodayFixtures.snapshot(TodayFixtures.home()))
        rule.onAllNodesWithTag(TAG_ASK_CARD).assertCountEquals(2)
        rule.onAllNodesWithTag(TAG_WORKER_ROW).assertCountEquals(1)
        rule.onNodeWithTag(TAG_CHAT_DOCK).assertIsDisplayed()
    }

    @Test
    fun `mobile-app today status labels per card kind and worker state`() {
        show(TodayFixtures.snapshot(TodayFixtures.home()))
        rule.onAllNodesWithText("Onay bekliyor", useUnmergedTree = true).assertCountEquals(1)
        rule.onAllNodesWithText("VLAD soruyor", useUnmergedTree = true).assertCountEquals(1)
        rule.onAllNodesWithText("Çalışıyor", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun `mobile-app today offline banner shows last update and keeps cached cards`() {
        show(TodayFixtures.snapshot(TodayFixtures.home(), connection = Connection.OFFLINE))
        rule.onNodeWithTag(TAG_OFFLINE_BANNER).assertIsDisplayed()
        rule.onNodeWithText("Sunucuya ulaşılamıyor. Son güncelleme 14:02.", useUnmergedTree = true).assertIsDisplayed()
        rule.onAllNodesWithTag(TAG_ASK_CARD).assertCountEquals(2)
    }

    @Test
    fun `mobile-app today no offline banner while reachable`() {
        show(TodayFixtures.snapshot(TodayFixtures.home()))
        rule.onAllNodesWithTag(TAG_OFFLINE_BANNER).assertCountEquals(0)
    }

    @Test
    fun `mobile-app today red ci merge button is passive with talkback reason`() {
        val home = TodayFixtures.home { it.replace("\"ci\":\"green\"", "\"ci\":\"red\"") }
        show(TodayFixtures.snapshot(home))
        rule.onAllNodesWithText("CI kırmızı", useUnmergedTree = true).assertCountEquals(1)
        rule.onNode(hasContentDescription("Merge et…, pasif: CI kırmızı")).assertIsNotEnabled()
    }

    @Test
    fun `mobile-app today all action buttons are passive in this slice`() {
        show(TodayFixtures.snapshot(TodayFixtures.home()))
        rule.onNode(hasContentDescription("Merge et…, pasif: onay akışı bu sürümde yok")).assertIsNotEnabled()
        rule.onNode(hasContentDescription("Cevapla, pasif: onay akışı bu sürümde yok")).assertIsNotEnabled()
        rule.onNode(hasContentDescription("Gönder, pasif: sohbet bu sürümde yok")).assertIsNotEnabled()
    }

    @Test
    fun `mobile-app today limit waiting worker and banner`() {
        val home =
            TodayFixtures.home {
                it.replace("\"limited\":false", "\"limited\":true").replace("\"state\":\"running\"", "\"state\":\"queued\"")
            }
        show(TodayFixtures.snapshot(home))
        rule.onAllNodesWithText("Sınır nedeniyle bekliyor", useUnmergedTree = true).assertCountEquals(1)
        rule.onNodeWithTag(TAG_LIMIT_BANNER).assertIsDisplayed()
    }

    @Test
    fun `mobile-app today unknown card kind and worker state are shown as unknown`() {
        val home =
            TodayFixtures.home {
                it.replace(
                    "\"kind\":\"vlad_question\"",
                    "\"kind\":\"hologram\"",
                ).replace("\"state\":\"running\"", "\"state\":\"teleporting\"")
            }
        show(TodayFixtures.snapshot(home))
        rule.onAllNodesWithText("Bilinmeyen", useUnmergedTree = true).assertCountEquals(2)
        rule.onAllNodesWithTag(TAG_ASK_CARD).assertCountEquals(2)
    }

    @Test
    fun `mobile-app today empty state`() {
        show(TodayFixtures.snapshot(TodayFixtures.empty))
        rule.onAllNodesWithTag(TAG_ASK_CARD).assertCountEquals(0)
        rule.onNodeWithText("Senden bekleyen yok").assertIsDisplayed()
    }

    @Test
    fun `mobile-app today revoked device asks to pair again`() {
        val state =
            HomeState(home = TodayFixtures.home(), connection = Connection.POLLING, lastError = dev.vlad.core.api.ErrorCode.DEVICE_REVOKED)
        show(TodaySnapshot(state, 1L, null, false))
        rule.onNodeWithText("Bu cihazın eşleşmesi kaldırıldı.").assertIsDisplayed()
        rule.onNodeWithText("Yeniden eşleştir").assertIsDisplayed()
    }

    @Test
    fun `mobile-app today server error keeps cache and offers retry`() {
        show(TodayFixtures.snapshot(TodayFixtures.home(), serverStatus = 503))
        rule.onNodeWithText("Durum alınamadı").assertIsDisplayed()
        rule.onAllNodesWithTag(TAG_ASK_CARD).assertCountEquals(2)
    }
}
