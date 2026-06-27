package app.aspen.lint

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Build-time copy-lint release gate (docs/06 §6.4, docs/09 §2.5). Scans user-facing string
 * resources for forbidden number/shame/appearance tokens, PER LANGUAGE (docs/12 §3), and FAILS
 * the build on any violation. This is a backstop for the non-negotiables (CLAUDE.md #1/#2/#5).
 */
abstract class CopyLintTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val stringFiles: ConfigurableFileCollection

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val allowListFile: RegularFileProperty

    @TaskAction
    fun lint() {
        val allowList = loadAllowList()
        val tokens = ForbiddenTokens.defaults()
        val universal = ForbiddenTokens.universal()

        val report = StringBuilder()
        var violationCount = 0
        var scanned = 0

        stringFiles.files.filter { it.exists() }.sortedBy { it.path }.forEach { file ->
            scanned++
            val language = StringResourceParser.languageOf(file)
            val strings = StringResourceParser.parse(file.readText())
            val violations = CopyLint.scanStrings(language, strings, tokens, universal, allowList)
            violations.forEach { v ->
                violationCount++
                report.appendLine(
                    "  ${file.path} [${v.language}] string=\"${v.stringName}\" " +
                        "token=\"${v.token}\" category=${v.category}",
                )
            }
        }

        if (violationCount > 0) {
            throw GradleException(
                buildString {
                    appendLine(
                        "Copy-lint FAILED: $violationCount forbidden token(s) in user-facing strings.",
                    )
                    appendLine(
                        "No numbers about food/body, no appearance comments, no shame language " +
                            "(CLAUDE.md #1/#2/#5).",
                    )
                    append(report)
                    appendLine(
                        "Fix the copy, or add a reviewed allow-list entry to " +
                            "config/copylint/allowlist.txt as `token|reviewer note`.",
                    )
                },
            )
        }
        logger.lifecycle("Copy-lint passed: no forbidden tokens across $scanned string file(s).")
    }

    private fun loadAllowList(): Set<String> {
        val file = allowListFile.orNull?.asFile ?: return emptySet()
        if (!file.exists()) return emptySet()
        return file.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { it.substringBefore('|').trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}
