import app.aspen.lint.CopyLintTask
import app.aspen.lint.CrisisGateTask

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.ktlint) apply false
}

// ---------------------------------------------------------------------------------------------
// Copy-lint release gate (docs/06 §6.4, docs/09 §2.5).
// Scans every user-facing string resource (per language) for forbidden number/shame/appearance
// tokens and fails the build on any violation. Backstops CLAUDE.md non-negotiables #1/#2/#5.
// ---------------------------------------------------------------------------------------------
val copyLint = tasks.register<CopyLintTask>("copyLint") {
    group = "verification"
    description = "Scan string resources for forbidden number/shame/appearance tokens (per language)."
    stringFiles.from(
        fileTree(rootDir) {
            include("**/src/**/composeResources/values*/strings.xml")
            include("**/src/main/res/values*/strings.xml")
            exclude("**/build/**")
        },
    )
    allowListFile.set(layout.projectDirectory.file("config/copylint/allowlist.txt"))
}

// ---------------------------------------------------------------------------------------------
// Crisis-registry release gate (docs/09 §2.5 SR-1/SR-2, docs/10 §7).
// Fails the build if "NEDA" appears anywhere in crisis data, or if any LAUNCH-locale (PK/DE/UK)
// resource is unverified/stale/placeholder. EXPECTED to fail until advisors verify the registry —
// that is the safety backstop: unverified crisis content physically cannot ship (CLAUDE.md #7).
// ---------------------------------------------------------------------------------------------
// Dev gate (wired into `check`): NEDA-deny + freshness, but PROVISIONALLY-verified launch content is
// accepted so local work isn't halted before advisors sign off. TODO-VERIFY content still fails.
val crisisGate = tasks.register<CrisisGateTask>("crisisGate") {
    group = "verification"
    description = "Dev crisis gate: NEDA-deny + freshness; provisionally-verified launch content allowed."
    crisisDir.set(layout.projectDirectory.dir("config/safety/crisis"))
    launchLocales.set(setOf("PK", "DE", "UK"))
    strict.set(false)
}

// Release gate (NOT wired into `check` — run before shipping): rejects provisional content; every
// launch-locale resource must carry REAL advisor verification + fresh date + real contacts.
// See docs/PRE_SHIP_VERIFICATION.md. Expected RED until advisors verify the registry.
val crisisGateStrict = tasks.register<CrisisGateTask>("crisisGateStrict") {
    group = "verification"
    description = "Release crisis gate: requires real advisor verification (provisional content fails)."
    crisisDir.set(layout.projectDirectory.dir("config/safety/crisis"))
    launchLocales.set(setOf("PK", "DE", "UK"))
    strict.set(true)
}

// Make every module's `check` depend on the dev gates, so `./gradlew check` enforces them.
subprojects {
    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(copyLint)
        dependsOn(crisisGate)
    }
}
