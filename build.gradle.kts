import app.aspen.lint.CopyLintTask

plugins {
    alias(libs.plugins.android.application) apply false
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

// Make every module's `check` depend on the copy-lint gate, so `./gradlew check` enforces it.
subprojects {
    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(copyLint)
    }
}
