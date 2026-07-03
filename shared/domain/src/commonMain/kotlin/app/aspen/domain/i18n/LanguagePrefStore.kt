package app.aspen.domain.i18n

import app.aspen.core.i18n.SupportedLanguage

/**
 * Persistence port for the user's explicit UI-language choice (docs/12 §4: a user override always
 * wins; no choice saved means "follow the device"). Same port pattern as
 * [app.aspen.domain.companion.CompanionPrefsStore]; the encrypted implementation lives in `:shared:data`.
 *
 * This stores LANGUAGE ONLY. It must never be used to derive a crisis region (docs/12 §6) — the
 * safety registry's region is an independent, explicit user choice.
 *
 * **Fail-safe:** reads never throw. Missing or unreadable choice returns `null`, which callers treat
 * as "follow the device language" — a storage fault can only ever fall back to the system default.
 */
interface LanguagePrefStore {
    /** Persist an explicit choice, replacing any previous one. */
    fun save(language: SupportedLanguage)

    /** The saved choice, or null to follow the device (none saved, or unreadable). Never throws. */
    fun current(): SupportedLanguage?

    /** Remove the saved choice — back to following the device language. */
    fun clear()
}
