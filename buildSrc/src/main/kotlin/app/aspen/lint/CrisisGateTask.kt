package app.aspen.lint

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.time.LocalDate

/**
 * Build-time crisis-registry release gate (docs/09 §2.5, docs/10 §7). Runs both checks from
 * [CrisisRegistryLint] over `config/safety/crisis/` JSON files and FAILS the build on any finding:
 *  - **NEDA-deny** — "NEDA" anywhere in resource content (every locale).
 *  - **freshness** — any launch-locale resource still unverified / stale / placeholder-contacted.
 *
 * This gate is EXPECTED to fail until the trust-and-safety team verifies the registry content
 * (every value currently `TODO-VERIFY`). That is the safety backstop, not a regression: unverified
 * crisis content cannot ship to a launch locale.
 */
abstract class CrisisGateTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val crisisDir: DirectoryProperty

    /** Launch locales subject to the freshness gate (INTL is the always-on fallback, never gated). */
    @get:Input
    abstract val launchLocales: SetProperty<String>

    /** Strict = release mode (provisional content fails). Non-strict = dev mode (provisional accepted). */
    @get:Input
    abstract val strict: Property<Boolean>

    @TaskAction
    fun check() {
        val dir = crisisDir.get().asFile
        if (!dir.isDirectory) {
            throw GradleException("Crisis registry directory not found: ${dir.path}")
        }
        val files = CrisisRegistryLint.parseDir(dir)
        val neda = CrisisRegistryLint.nedaViolations(files)
        val freshness = CrisisRegistryLint.freshnessViolations(
            files = files,
            launchLocales = launchLocales.get(),
            today = LocalDate.now(),
            strict = strict.get(),
        )
        val findings = neda + freshness

        if (findings.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Crisis-registry gate FAILED: ${findings.size} finding(s) (docs/09 §2.5).")
                    appendLine("NEDA-deny: ${neda.size}; freshness: ${freshness.size}.")
                    findings.forEach { appendLine("  [${it.gate}] ${it.source}: ${it.message}") }
                    appendLine(
                        "Until the trust-and-safety team verifies each launch-locale resource " +
                            "(verifiedOn/verifiedBy + real contacts) this gate stays red — by design.",
                    )
                },
            )
        }
        logger.lifecycle("Crisis-registry gate passed: ${files.size} locale file(s), no findings.")
    }
}
