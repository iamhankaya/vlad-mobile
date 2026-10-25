package dev.vlad.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File

/** `contract/` dizini (vlad reposundan kopya, bkz. `contract/SOURCE.md`); yolu Gradle `vlad.contract.dir` ile verir. */
object ContractFiles {
    private val dir = File(requireNotNull(System.getProperty("vlad.contract.dir")) { "vlad.contract.dir tanımlı değil" })

    private fun json(file: File): JsonElement = Json.parseToJsonElement(file.readText(Charsets.UTF_8))

    val vectors: JsonObject by lazy { json(File(dir, "signing-vectors.json")).jsonObject }

    val apiFixtures: List<Pair<String, JsonObject>> by lazy {
        File(dir, "fixtures/api").listFiles { f -> f.extension == "json" }!!
            .sortedBy { it.name }
            .map { it.name to json(it).jsonObject }
    }
}
