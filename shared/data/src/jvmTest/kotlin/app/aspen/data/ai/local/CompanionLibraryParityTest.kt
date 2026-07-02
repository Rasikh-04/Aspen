package app.aspen.data.ai.local

import app.aspen.domain.ai.CompanionLibrary
import app.aspen.domain.ai.CompanionLine
import app.aspen.domain.ai.CompanionMoment
import app.aspen.domain.ai.LineReviewStatus
import app.aspen.domain.onboarding.model.CompanionTone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Pins the runtime [DefaultCompanionLibrary] to the canonical config/companion/library.json — the
 * advisors' review surface. Same single-source-by-parity pattern as the crisis registry and the
 * forbidden-token lexicon. If this fails, the two have drifted: edit BOTH together.
 */
class CompanionLibraryParityTest {

    @Test
    fun runtimeLibraryMatchesCanonicalJson() {
        val root = Json.parseToJsonElement(findCanonicalFile().readText()).jsonObject

        val allTones = root.getValue("all_tones").jsonArray.map { CompanionTone.valueOf(it.jsonPrimitive.content) }.toSet()
        val lines = root.getValue("lines").jsonArray.map { node ->
            val obj = node.jsonObject
            val tonesNode = obj.getValue("tones")
            CompanionLine(
                key = obj.getValue("key").jsonPrimitive.content,
                moment = CompanionMoment.valueOf(obj.getValue("moment").jsonPrimitive.content),
                tones = if (tonesNode is JsonArray) {
                    tonesNode.map { CompanionTone.valueOf(it.jsonPrimitive.content) }.toSet()
                } else {
                    check(tonesNode.jsonPrimitive.content == "all") { "tones must be a list or \"all\"" }
                    allTones
                },
                rankingHint = obj.getValue("ranking_hint").jsonPrimitive.content,
                review = obj.getValue("review").jsonObject.mapValues { (_, status) ->
                    LineReviewStatus.valueOf(status.jsonPrimitive.content)
                },
            )
        }

        assertEquals(
            CompanionLibrary(lines),
            DefaultCompanionLibrary.library,
            "DefaultCompanionLibrary has drifted from config/companion/library.json",
        )
    }

    private fun findCanonicalFile(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val candidate = File(dir, "config/companion/library.json")
            if (candidate.isFile) return candidate
            dir = dir.parentFile
        }
        fail("config/companion/library.json not found from ${System.getProperty("user.dir")}")
    }
}
