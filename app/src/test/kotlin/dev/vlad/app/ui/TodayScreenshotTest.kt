@file:OptIn(ExperimentalRoborazziApi::class)

package dev.vlad.app.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import dev.vlad.app.ui.theme.VladTheme
import dev.vlad.app.ui.today.TodayScreen
import dev.vlad.core.reduce.Connection
import dev.vlad.core.repo.TodaySnapshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Roborazzi ekran görüntüleri. Referanslar yalnız CI'da üretilir (`recordRoborazziDebug`), karşılaştırma toleransı %0.5
 * piksel farkıdır (`changeThreshold = 0.005`). Düz `testDebugUnitTest` görüntü yazmaz/karşılaştırmaz.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class TodayScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    private val options = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.005f))

    private fun shot(
        name: String,
        snapshot: TodaySnapshot,
        dark: Boolean = false,
        fontScale: Float = 1f,
    ) {
        rule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) {
                VladTheme(darkTheme = dark) {
                    TodayScreen(snapshot, onRetry = {}, onRepair = {}, formatTime = { "14:02" })
                }
            }
        }
        rule.onRoot().captureRoboImage("src/test/screenshots/today_$name.png", roborazziOptions = options)
    }

    @Test
    fun empty() = shot("empty", TodayFixtures.snapshot(TodayFixtures.empty))

    @Test
    fun full() = shot("full", TodayFixtures.snapshot(TodayFixtures.home()))

    @Test
    fun fullDark() = shot("full_dark", TodayFixtures.snapshot(TodayFixtures.home()), dark = true)

    @Test
    fun limitWaiting() {
        val home =
            TodayFixtures.home {
                it.replace("\"limited\":false", "\"limited\":true").replace("\"state\":\"running\"", "\"state\":\"queued\"")
            }
        shot("limit_waiting", TodayFixtures.snapshot(home))
    }

    @Test
    fun offline() = shot("offline", TodayFixtures.snapshot(TodayFixtures.home(), connection = Connection.OFFLINE))

    @Test
    fun redCi() = shot("red_ci", TodayFixtures.snapshot(TodayFixtures.home { it.replace("\"ci\":\"green\"", "\"ci\":\"red\"") }))

    @Test
    fun font2x() = shot("font_2x", TodayFixtures.snapshot(TodayFixtures.home()), fontScale = 2f)
}
