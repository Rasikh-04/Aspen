package app.aspen.lint

import groovy.json.JsonSlurper
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Pure logic for the crisis-registry release gates (docs/09 §2.5, docs/10 §7), kept Gradle-free so
 * it is unit-testable in `buildSrc/src/test` exactly like [CopyLint]. Two independent checks:
 *
 *  - **SR-1 / NEDA-deny:** the literal string "NEDA" must appear in NO resource content, in ANY
 *    locale. The US ED line is the National Alliance for Eating Disorders; NEDA is disconnected
 *    (CLAUDE.md #7). The `_comment` meta field is excluded — it documents the ban itself.
 *  - **SR-2 / freshness:** for every LAUNCH locale (PK/DE/UK now; US when enabled) no resource may
 *    ship with a placeholder/blank/stale `verifiedOn`/`verifiedBy` or a placeholder contact value.
 *    INTL is the always-on fallback and is intentionally NOT gated.
 *
 * By design this gate FAILS today: the registry ships with every value as `TODO-VERIFY`, so
 * unverified content physically cannot reach a launch locale until advisors verify it (docs/10 §7).
 */
object CrisisRegistryLint {

    /** Values that mean "not yet advisor-verified" — any of these fails the freshness gate. */
    val PLACEHOLDERS: Set<String> = setOf("", "TODO", "TODO-VERIFY", "⚠VERIFY", "VERIFY", "TBD")

    /** Default staleness threshold: a verified resource older than this must be re-checked. */
    const val DEFAULT_MAX_AGE_DAYS: Long = 365L

    data class ContactData(val method: String, val label: String, val value: String)

    data class ResourceData(
        val id: String,
        val name: String,
        val purpose: String,
        val contacts: List<ContactData>,
        val notes: String?,
        val verifiedOn: String,
        val verifiedBy: String,
    )

    data class FileData(val source: String, val locale: String, val resources: List<ResourceData>)

    /** A single gate failure, with enough context to fix it. */
    data class Finding(val gate: String, val source: String, val message: String)

    @Suppress("UNCHECKED_CAST")
    fun parse(source: String, json: String): FileData {
        val root = JsonSlurper().parseText(json) as Map<String, Any?>
        val locale = (root["locale"] as? String).orEmpty()
        val resources = (root["resources"] as? List<Any?>).orEmpty().map { node ->
            val r = node as Map<String, Any?>
            ResourceData(
                id = (r["id"] as? String).orEmpty(),
                name = (r["name"] as? String).orEmpty(),
                purpose = (r["purpose"] as? String).orEmpty(),
                contacts = (r["contacts"] as? List<Any?>).orEmpty().map { c ->
                    val co = c as Map<String, Any?>
                    ContactData(
                        method = (co["method"] as? String).orEmpty(),
                        label = (co["label"] as? String).orEmpty(),
                        value = (co["value"] as? String).orEmpty(),
                    )
                },
                notes = r["notes"] as? String,
                verifiedOn = (r["verifiedOn"] as? String).orEmpty(),
                verifiedBy = (r["verifiedBy"] as? String).orEmpty(),
            )
        }
        return FileData(source, locale, resources)
    }

    /** SR-1: "NEDA" anywhere in resource content (id/name/notes/contact label+value) is a hard fail. */
    fun nedaViolations(files: List<FileData>): List<Finding> {
        val out = mutableListOf<Finding>()
        for (f in files) {
            for (r in f.resources) {
                val haystack = buildString {
                    append(r.id).append(' ').append(r.name).append(' ').append(r.notes ?: "")
                    r.contacts.forEach { append(' ').append(it.label).append(' ').append(it.value) }
                }
                if (haystack.uppercase().contains("NEDA")) {
                    out += Finding("NEDA-deny", f.source, "resource \"${r.id}\" mentions NEDA — forbidden (CLAUDE.md #7)")
                }
            }
        }
        return out
    }

    /**
     * SR-2: launch-locale resources must be verified, fresh, and free of placeholder contacts.
     *
     * [strict] controls how PROVISIONAL content (verifiedBy beginning "PROVISIONAL") is treated:
     *  - **non-strict (dev `crisisGate`):** provisionally-verified resources are ACCEPTED so local
     *    development isn't halted before advisors sign off. Truly-unmarked (`TODO-VERIFY`) content
     *    still fails — provisional is a deliberate, documented marker, not the absence of one.
     *  - **strict (release `crisisGateStrict`):** provisional is REJECTED; every launch resource must
     *    carry real advisor `verifiedBy`, a fresh `verifiedOn`, and real (non-placeholder) contacts.
     *
     * Provisional content deliberately keeps `TODO-VERIFY` contact values, so the UI still renders
     * them non-actionable — no unverified number can be dialled (docs/STATUS.md, CLAUDE.md #7).
     */
    fun freshnessViolations(
        files: List<FileData>,
        launchLocales: Set<String>,
        today: LocalDate,
        maxAgeDays: Long = DEFAULT_MAX_AGE_DAYS,
        strict: Boolean = true,
    ): List<Finding> {
        val launch = launchLocales.map { it.uppercase() }.toSet()
        val out = mutableListOf<Finding>()
        for (f in files) {
            if (f.locale.uppercase() !in launch) continue
            for (r in f.resources) {
                val provisional = isProvisional(r.verifiedBy)
                if (isPlaceholder(r.verifiedBy)) {
                    out += Finding("freshness", f.source, "[${f.locale}] resource \"${r.id}\" has no verifiedBy")
                } else if (provisional && strict) {
                    out += Finding(
                        "freshness",
                        f.source,
                        "[${f.locale}] resource \"${r.id}\" is only PROVISIONALLY verified " +
                            "(verifiedBy=\"${r.verifiedBy}\") — real advisor verification required before release",
                    )
                }
                // Date + contact checks run for everything EXCEPT provisional content in dev mode.
                if (!provisional || strict) {
                    when (ageState(r.verifiedOn, today, maxAgeDays)) {
                        AgeState.PLACEHOLDER ->
                            out += Finding("freshness", f.source, "[${f.locale}] resource \"${r.id}\" has unverified verifiedOn=\"${r.verifiedOn}\"")
                        AgeState.STALE ->
                            out += Finding("freshness", f.source, "[${f.locale}] resource \"${r.id}\" verifiedOn=\"${r.verifiedOn}\" is older than $maxAgeDays days")
                        AgeState.FRESH -> Unit
                    }
                    r.contacts.filter { isPlaceholder(it.value) }.forEach { c ->
                        out += Finding("freshness", f.source, "[${f.locale}] resource \"${r.id}\" contact \"${c.label}\" has placeholder value")
                    }
                }
            }
        }
        return out
    }

    /** True when verifiedBy is a deliberate provisional marker (dev-accepted, release-rejected). */
    fun isProvisional(value: String): Boolean = value.trim().uppercase().startsWith("PROVISIONAL")

    private enum class AgeState { PLACEHOLDER, STALE, FRESH }

    private fun ageState(verifiedOn: String, today: LocalDate, maxAgeDays: Long): AgeState {
        if (isPlaceholder(verifiedOn)) return AgeState.PLACEHOLDER
        val date = try {
            LocalDate.parse(verifiedOn.trim())
        } catch (e: DateTimeParseException) {
            return AgeState.PLACEHOLDER
        }
        return if (date.plusDays(maxAgeDays).isBefore(today)) AgeState.STALE else AgeState.FRESH
    }

    private fun isPlaceholder(value: String): Boolean =
        value.trim().uppercase() in PLACEHOLDERS.map { it.uppercase() }

    /** Convenience: parse every `*.json` in a directory (skips files whose name starts with `_`). */
    fun parseDir(dir: File): List<FileData> =
        dir.listFiles { f -> f.isFile && f.extension == "json" && !f.name.startsWith("_") }
            .orEmpty()
            .sortedBy { it.name }
            .map { parse(it.path, it.readText()) }
}
