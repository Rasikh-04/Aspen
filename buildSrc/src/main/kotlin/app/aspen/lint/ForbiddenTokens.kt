package app.aspen.lint

import groovy.json.JsonSlurper
import java.io.File

/** A category of banned copy, tied to the non-negotiables (CLAUDE.md #1/#2/#5, docs/01 §6). */
enum class TokenCategory { NUMBERS_FOOD_BODY, SHAME, APPEARANCE }

/**
 * Forbidden tokens for one language. Per docs/09 §2.3 these are detection heuristics for the
 * build-time copy lint, and per docs/12 §3 the lists are PER LANGUAGE — a numberless app must be
 * numberless in every language.
 */
data class TokenSet(
    val language: String,
    val numbersFoodBody: List<String>,
    val shame: List<String>,
    val appearance: List<String>,
) {
    fun byCategory(category: TokenCategory): List<String> = when (category) {
        TokenCategory.NUMBERS_FOOD_BODY -> numbersFoodBody
        TokenCategory.SHAME -> shame
        TokenCategory.APPEARANCE -> appearance
    }
}

/**
 * Loads the forbidden-token lexicon from the canonical `config/safety/forbidden_tokens.json` — the
 * SINGLE SOURCE OF TRUTH shared with the runtime (app.aspen.domain.safety.DefaultForbiddenLexicon).
 * buildSrc can't depend on :shared:domain, so the source is shared via this file, not a class
 * (decision #3). The runtime mirror is pinned to the same file by a parity test.
 *
 * The lexicon's `eating_advice` category is ignored here — it is a runtime-only AI-output concern;
 * the copy-lint scans numbers/shame/appearance.
 */
object ForbiddenTokens {

    private val lexicon: Lexicon by lazy { load(findCanonicalFile()) }

    private data class Lexicon(val byLanguage: Map<String, TokenSet>, val universal: TokenSet)

    /** Per-language token sets, from the canonical JSON. */
    fun defaults(): Map<String, TokenSet> = lexicon.byLanguage

    /** Tokens banned in EVERY language (language code "*"), from the canonical JSON. */
    fun universal(): TokenSet = lexicon.universal

    /** Walk up from the working directory to locate the repo's canonical lexicon file. */
    private fun findCanonicalFile(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val candidate = File(dir, "config/safety/forbidden_tokens.json")
            if (candidate.isFile) return candidate
            dir = dir.parentFile
        }
        error("Canonical lexicon config/safety/forbidden_tokens.json not found from ${System.getProperty("user.dir")}")
    }

    @Suppress("UNCHECKED_CAST")
    private fun load(file: File): Lexicon {
        val root = JsonSlurper().parse(file) as Map<String, Any?>
        val languages = (root["languages"] as Map<String, Any?>)
        val byLanguage = languages.mapValues { (lang, node) ->
            val cats = node as Map<String, Any?>
            TokenSet(
                language = lang,
                numbersFoodBody = strList(cats["numbers_food_body"]),
                shame = strList(cats["shame"]),
                appearance = strList(cats["appearance"]),
            )
        }
        val u = root["universal"] as Map<String, Any?>
        val universal = TokenSet(
            language = "*",
            numbersFoodBody = strList(u["numbers_food_body"]),
            shame = strList(u["shame"]),
            appearance = strList(u["appearance"]),
        )
        return Lexicon(byLanguage, universal)
    }

    @Suppress("UNCHECKED_CAST")
    private fun strList(node: Any?): List<String> =
        (node as? List<Any?>).orEmpty().map { it.toString() }
}
