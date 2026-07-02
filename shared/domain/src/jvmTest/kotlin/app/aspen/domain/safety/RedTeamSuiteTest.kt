package app.aspen.domain.safety

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The red-team release gate (docs/03 SR-3, docs/06 §6.5, docs/07 Phase 4 DoD): every adversarial
 * entry in config/safety/redteam/corpus.json must be caught — `guard_blocks` by [SafetyRules]
 * (the output guard withholds + replaces), `crisis_handoff` by [CrisisSignals] (warm hand-off before
 * any AI call) — and every `passes` entry must trip NEITHER (an over-blocking support app is its own
 * harm). Runs in `check`, so a regression fails the build. Grow the corpus, never shrink it.
 */
class RedTeamSuiteTest {

    private val rules = SafetyRules(DefaultForbiddenLexicon.lexicon)
    private val signals = CrisisSignals(DefaultCrisisSignalLexicon.lexicon)

    private data class Entry(val text: String, val language: String, val expectation: String)

    private fun corpus(): List<Entry> {
        val root = Json.parseToJsonElement(findCorpusFile().readText()).jsonObject
        return root.getValue("entries").jsonArray.map { node ->
            val obj = node.jsonObject
            Entry(
                text = obj.getValue("text").jsonPrimitive.content,
                language = obj.getValue("language").jsonPrimitive.content,
                expectation = obj.getValue("expectation").jsonPrimitive.content,
            )
        }
    }

    @Test
    fun corpusIsNonTrivial() {
        val entries = corpus()
        assertTrue(entries.count { it.expectation == "guard_blocks" } >= 15, "adversarial guard coverage shrank")
        assertTrue(entries.count { it.expectation == "crisis_handoff" } >= 5, "crisis coverage shrank")
        assertTrue(entries.count { it.expectation == "passes" } >= 5, "benign (anti-over-blocking) coverage shrank")
    }

    @Test
    fun everyAdversarialOutputIsBlockedByTheGuard() {
        corpus().filter { it.expectation == "guard_blocks" }.forEach { entry ->
            assertTrue(
                rules.violatesAny(entry.text),
                "[${entry.language}] guard must block: ${entry.text}",
            )
        }
    }

    @Test
    fun everyCrisisInputTriggersTheHandOff() {
        corpus().filter { it.expectation == "crisis_handoff" }.forEach { entry ->
            assertTrue(
                signals.suggestsCrisis(entry.text),
                "[${entry.language}] crisis hand-off must trigger for: ${entry.text}",
            )
        }
    }

    @Test
    fun benignSupportLanguagePassesBothChecks() {
        corpus().filter { it.expectation == "passes" }.forEach { entry ->
            assertTrue(!rules.violatesAny(entry.text), "[${entry.language}] guard over-blocks: ${entry.text}")
            assertTrue(!signals.suggestsCrisis(entry.text), "[${entry.language}] crisis over-triggers: ${entry.text}")
        }
    }

    private fun findCorpusFile(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val candidate = File(dir, "config/safety/redteam/corpus.json")
            if (candidate.isFile) return candidate
            dir = dir.parentFile
        }
        fail("config/safety/redteam/corpus.json not found from ${System.getProperty("user.dir")}")
    }
}
