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
 * Pins the runtime [DefaultForbiddenLexicon] to the canonical config/safety/forbidden_tokens.json
 * (decision #3 — buildSrc and the runtime share the JSON as the single source of truth, enforced by
 * parity tests since buildSrc can't be a runtime dependency). If this fails, the two have drifted:
 * edit BOTH the JSON and DefaultForbiddenLexicon together.
 */
class ForbiddenLexiconParityTest {

    private val categoryKeys = mapOf(
        "numbers_food_body" to TokenCategory.NUMBERS_FOOD_BODY,
        "shame" to TokenCategory.SHAME,
        "appearance" to TokenCategory.APPEARANCE,
        "eating_advice" to TokenCategory.EATING_ADVICE,
    )

    @Test
    fun runtimeLexiconMatchesCanonicalJson() {
        val fromJson = parseCanonical()
        assertEquals(
            fromJson,
            DefaultForbiddenLexicon.lexicon,
            "DefaultForbiddenLexicon has drifted from config/safety/forbidden_tokens.json",
        )
    }

    private fun parseCanonical(): ForbiddenLexicon {
        val file = findCanonicalFile()
        val root = Json.parseToJsonElement(file.readText()).jsonObject

        val byLanguage = root.getValue("languages").jsonObject.mapValues { (_, langNode) ->
            val cats = langNode.jsonObject
            categoryKeys.entries.associate { (key, cat) ->
                cat to cats.getValue(key).jsonArray.map { it.jsonPrimitive.content }
            }
        }
        val universalNode = root.getValue("universal").jsonObject
        val universal = categoryKeys.entries.associate { (key, cat) ->
            cat to universalNode.getValue(key).jsonArray.map { it.jsonPrimitive.content }
        }
        return ForbiddenLexicon(byLanguage = byLanguage, universal = universal)
    }

    private fun findCanonicalFile(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val candidate = File(dir, "config/safety/forbidden_tokens.json")
            if (candidate.isFile) return candidate
            dir = dir.parentFile
        }
        fail("config/safety/forbidden_tokens.json not found from ${System.getProperty("user.dir")}")
    }
}
