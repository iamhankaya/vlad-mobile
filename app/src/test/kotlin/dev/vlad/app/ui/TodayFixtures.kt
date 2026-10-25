package dev.vlad.app.ui

import dev.vlad.core.api.Home
import dev.vlad.core.api.VladJson
import dev.vlad.core.reduce.Connection
import dev.vlad.core.reduce.HomeState
import dev.vlad.core.repo.TodaySnapshot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File

/** Sözleşme fixture'ından (sentetik, bkz. contract/SOURCE.md) `Home`; testler `mutate` ile tek alanı değiştirir. */
object TodayFixtures {
    private val dir = File(requireNotNull(System.getProperty("vlad.contract.dir")))

    private fun body(name: String): String {
        val root = Json.parseToJsonElement(File(dir, "fixtures/api/$name").readText()).jsonObject
        return root.getValue("response").jsonObject.getValue("body").toString()
    }

    fun home(
        name: String = "get-home.200.json",
        mutate: (String) -> String = { it },
    ): Home = VladJson.decodeFromString<Home>(mutate(body(name)))

    const val UPDATED_MS = 1_000L

    fun snapshot(
        home: Home?,
        connection: Connection = Connection.POLLING,
        loading: Boolean = false,
        serverStatus: Int? = null,
    ) = TodaySnapshot(HomeState(home = home, connection = connection), UPDATED_MS, serverStatus, loading)

    val empty: Home get() = home().copy(asks = emptyList(), workers = emptyList())
}
