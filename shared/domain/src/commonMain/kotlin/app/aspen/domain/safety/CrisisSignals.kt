package app.aspen.domain.safety

/**
 * Per-language explicit crisis phrases plus a script-neutral universal set. Runtime mirror of the
 * canonical `config/safety/crisis_signals.json`, pinned by `CrisisSignalLexiconParityTest` (JVM) —
 * the same single-source pattern as [ForbiddenLexicon]. The authoritative lists are advisor-supplied
 * trust-and-safety content (docs/12 §3); what ships here is a reviewed starter (en/de/ur).
 */
data class CrisisSignalLexicon(
    val byLanguage: Map<String, List<String>>,
    val universal: List<String>,
) {
    /** All phrases across every language plus the universal set (deduped, lowercased). */
    fun allPhrases(): List<String> =
        (byLanguage.values.flatten() + universal).map { it.lowercase() }.distinct()
}

/**
 * The crisis-sign INPUT check (docs/06 §5, CLAUDE.md #8): before any AI sees a user's words, this
 * decides one thing only — should the app offer a warm hand-off to Flow C and a human instead of
 * replying. Design constraints, in order:
 *
 * - **Conservative and heuristic, never diagnostic.** Explicit phrases only; the output is a single
 *   hand-off boolean — no severity, no category, no label, nothing stored (CLAUDE.md #9).
 * - **Doesn't over-trigger** (docs/06 §6.3): ordinary distress is what the notebook is FOR; treating
 *   every hard sentence as an emergency is its own harm. Under-triggering is the accepted trade —
 *   the human exit is permanently ≤2 taps away regardless of this check (CLAUDE.md #6).
 * - Whole-word/phrase Unicode matching (Latin + non-Latin scripts), same matcher as [SafetyRules].
 */
class CrisisSignals(private val lexicon: CrisisSignalLexicon) {

    private fun wholePhraseRegex(phrase: String): Regex =
        Regex("(?<!\\p{L})" + Regex.escape(phrase.trim()) + "(?!\\p{L})", RegexOption.IGNORE_CASE)

    /** True if [text] contains an explicit crisis phrase → the caller offers the warm hand-off. */
    fun suggestsCrisis(text: String): Boolean {
        if (text.isBlank()) return false
        return lexicon.allPhrases().any { it.isNotBlank() && wholePhraseRegex(it).containsMatchIn(text) }
    }
}

/**
 * Runtime mirror of `config/safety/crisis_signals.json`. Hand-edit BOTH together; the parity test is
 * the guard. Phrases are deliberately explicit-only — see [CrisisSignals] for why.
 */
object DefaultCrisisSignalLexicon {

    val lexicon: CrisisSignalLexicon = CrisisSignalLexicon(
        byLanguage = mapOf(
            "en" to listOf(
                "kill myself", "killing myself", "suicide", "suicidal",
                "end my life", "ending my life", "want to die", "wanted to die",
                "hurt myself", "hurting myself", "harm myself", "harming myself",
                "self-harm", "self harm", "don't want to be alive", "do not want to be alive",
                "better off dead", "end it all",
            ),
            "de" to listOf(
                "umbringen", "selbstmord", "suizid", "nicht mehr leben",
                "mir das leben nehmen", "mich verletzen",
            ),
            "ur" to listOf(
                "خودکشی", "مرنا چاہتا", "مرنا چاہتی", "خود کو نقصان",
            ),
        ),
        universal = emptyList(),
    )
}
