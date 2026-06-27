package app.aspen.lint

import java.io.File

/** Minimal Android/Compose strings.xml reader: extracts (name, value) pairs and the folder locale. */
object StringResourceParser {

    private val STRING_RE =
        Regex("<string\\s+name=\"([^\"]+)\"[^>]*>(.*?)</string>", setOf(RegexOption.DOT_MATCHES_ALL))

    fun parse(text: String): List<Pair<String, String>> =
        STRING_RE.findAll(text).map { it.groupValues[1] to it.groupValues[2] }.toList()

    /** values -> "en"; values-ur -> "ur"; values-zh-rCN -> "zh". */
    fun languageOf(stringsFile: File): String {
        val folder = stringsFile.parentFile?.name ?: "values"
        if (folder == "values") return "en"
        val suffix = folder.removePrefix("values-")
        return suffix.substringBefore('-').lowercase()
    }
}
