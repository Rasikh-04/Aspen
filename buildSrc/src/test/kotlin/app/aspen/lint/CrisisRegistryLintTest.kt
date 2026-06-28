package app.aspen.lint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Proves the crisis-registry gates actually fire (docs/09 §2.5). Uses synthetic JSON only — no
 * real registry content — so the test states the rule independently of the shipped data.
 */
class CrisisRegistryLintTest {

    private val today = LocalDate.of(2026, 6, 28)

    private fun file(locale: String, json: String) = CrisisRegistryLint.parse("$locale.json", json)

    private fun resourceJson(
        id: String = "r1",
        name: String = "Some Org",
        purpose: String = "ED_SUPPORT",
        contactValue: String = "+10000000000",
        notes: String = "a note",
        verifiedOn: String = "2026-06-01",
        verifiedBy: String = "advisor-AB",
    ) = """
        { "locale": "%LOCALE%", "version": "2026-06-01", "resources": [
          { "id": "$id", "name": "$name", "purpose": "$purpose",
            "contacts": [{ "method": "PHONE", "label": "Helpline", "value": "$contactValue" }],
            "hours": null, "languages": ["en"], "notes": "$notes",
            "verifiedOn": "$verifiedOn", "verifiedBy": "$verifiedBy" } ] }
    """.trimIndent()

    private fun json(locale: String, body: String) = body.replace("%LOCALE%", locale)

    // ---- NEDA-deny (SR-1) ----

    @Test
    fun nedaInResourceContentIsFlaggedInAnyLocale() {
        val f = file("US", json("US", resourceJson(name = "NEDA Helpline")))
        val findings = CrisisRegistryLint.nedaViolations(listOf(f))
        assertEquals(1, findings.size)
        assertEquals("NEDA-deny", findings.first().gate)
    }

    @Test
    fun nedaInNotesIsAlsoFlagged() {
        val f = file("UK", json("UK", resourceJson(notes = "do not use the neda line")))
        assertTrue("NEDA in notes must be caught (case-insensitive)", CrisisRegistryLint.nedaViolations(listOf(f)).isNotEmpty())
    }

    @Test
    fun cleanRegistryHasNoNedaFindings() {
        val f = file("UK", json("UK", resourceJson(name = "National Alliance for Eating Disorders")))
        assertTrue(CrisisRegistryLint.nedaViolations(listOf(f)).isEmpty())
    }

    // ---- Freshness (SR-2) ----

    private val launch = setOf("PK", "DE", "UK")

    @Test
    fun unverifiedLaunchLocaleResourceFailsFreshness() {
        val f = file("UK", json("UK", resourceJson(contactValue = "TODO-VERIFY", verifiedOn = "TODO-VERIFY", verifiedBy = "TODO-VERIFY")))
        val findings = CrisisRegistryLint.freshnessViolations(listOf(f), launch, today)
        // verifiedBy + verifiedOn + contact value all placeholders → three findings.
        assertEquals("expected verifiedBy, verifiedOn and contact placeholders all flagged", 3, findings.size)
        assertTrue(findings.all { it.gate == "freshness" })
    }

    @Test
    fun fullyVerifiedFreshLaunchLocaleResourcePasses() {
        val f = file("DE", json("DE", resourceJson()))
        assertTrue(CrisisRegistryLint.freshnessViolations(listOf(f), launch, today).isEmpty())
    }

    @Test
    fun staleVerifiedResourceFailsFreshness() {
        val f = file("DE", json("DE", resourceJson(verifiedOn = "2020-01-01")))
        val findings = CrisisRegistryLint.freshnessViolations(listOf(f), launch, today)
        assertTrue("a year-old verification must be flagged stale", findings.any { it.message.contains("older than") })
    }

    @Test
    fun intlFallbackIsNotGatedForFreshness() {
        // INTL ships with TODO-VERIFY but is the always-on fallback — it must NOT fail the gate.
        val f = file("INTL", json("INTL", resourceJson(contactValue = "TODO-VERIFY", verifiedOn = "TODO-VERIFY", verifiedBy = "TODO-VERIFY")))
        assertTrue("INTL is not a launch locale and is not freshness-gated", CrisisRegistryLint.freshnessViolations(listOf(f), launch, today).isEmpty())
    }

    @Test
    fun provisionalLaunchLocalePassesInDevModeButFailsInStrictMode() {
        // Provisional provenance + still-placeholder contacts: accepted by the dev gate, rejected by release.
        val f = file("UK", json("UK", resourceJson(contactValue = "TODO-VERIFY", verifiedOn = "2026-06-28", verifiedBy = "PROVISIONAL-UNVERIFIED")))
        assertTrue(
            "provisional content must pass the non-strict dev gate",
            CrisisRegistryLint.freshnessViolations(listOf(f), launch, today, strict = false).isEmpty(),
        )
        assertTrue(
            "provisional content must fail the strict release gate",
            CrisisRegistryLint.freshnessViolations(listOf(f), launch, today, strict = true).isNotEmpty(),
        )
    }

    @Test
    fun trulyUnmarkedContentFailsEvenInDevMode() {
        // TODO-VERIFY is the ABSENCE of a marker — it must fail even the lenient dev gate.
        val f = file("UK", json("UK", resourceJson(verifiedOn = "TODO-VERIFY", verifiedBy = "TODO-VERIFY")))
        assertTrue(CrisisRegistryLint.freshnessViolations(listOf(f), launch, today, strict = false).isNotEmpty())
    }

    @Test
    fun disabledUsIsNotGatedUntilAddedToLaunchLocales() {
        // US is present-but-disabled: not in launch set yet, so its TODO-VERIFY content is not gated.
        val f = file("US", json("US", resourceJson(verifiedOn = "TODO-VERIFY", verifiedBy = "TODO-VERIFY")))
        assertTrue(CrisisRegistryLint.freshnessViolations(listOf(f), launch, today).isEmpty())
        // …but once US is enabled, the same content fails.
        assertTrue(CrisisRegistryLint.freshnessViolations(listOf(f), launch + "US", today).isNotEmpty())
    }
}
