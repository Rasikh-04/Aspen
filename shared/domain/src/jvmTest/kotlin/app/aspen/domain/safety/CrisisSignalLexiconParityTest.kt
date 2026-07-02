package app.aspen.domain.safety

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Pins the runtime [DefaultCrisisSignalLexicon] to the canonical config/safety/crisis_signals.json —
 * same single-source-by-parity pattern as [ForbiddenLexiconParityTest]. If this fails, the two have
 * drifted: edit BOTH together.
 */
class CrisisSignalLexiconParityTest {

    @Test
    fun runtimeLexiconMatchesCanonicalJson() {
        val root = Json.parseToJsonElement(findCanonicalFile().readText()).jsonObject

        val byLanguage = root.getValue("languages").jsonObject.mapValues { (_, phrases) ->
            phrases.jsonArray.map { it.jsonPrimitive.content }
        }
        val universal = root.getValue("universal").jsonArray.map { it.jsonPrimitive.content }

        assertEquals(
            CrisisSignalLexicon(byLanguage = byLanguage, universal = universal),
            DefaultCrisisSignalLexicon.lexicon,
            "DefaultCrisisSignalLexicon has drifted from config/safety/crisis_signals.json",
        )
    }

    private fun findCanonicalFile(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val candidate = File(dir, "config/safety/crisis_signals.json")
            if (candidate.isFile) return candidate
            dir = dir.parentFile
        }
        fail("config/safety/crisis_signals.json not found from ${System.getProperty("user.dir")}")
    }
}
