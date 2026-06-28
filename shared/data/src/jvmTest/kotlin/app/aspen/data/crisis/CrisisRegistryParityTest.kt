package app.aspen.data.crisis

import app.aspen.domain.safety.model.Contact
import app.aspen.domain.safety.model.ContactMethod
import app.aspen.domain.safety.model.CrisisResource
import app.aspen.domain.safety.model.LocaleKey
import app.aspen.domain.safety.model.Purpose
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Pins the in-code [CrisisRegistry] to the canonical, advisor-editable `config/safety/crisis/`
 * JSON files (the same single-source pattern as the token lexicon — buildSrc's gate reads the JSON,
 * the app reads the in-code mirror, this test forbids drift). If this fails, edit BOTH together.
 */
class CrisisRegistryParityTest {

    @Test
    fun inCodeRegistryMatchesCanonicalJsonForEveryLocale() {
        val dir = findCrisisDir()
        // Every JSON file must have an in-code locale, and vice versa.
        val jsonLocales = dir.listFiles { f -> f.extension == "json" }.orEmpty()
            .map { it.nameWithoutExtension.uppercase() }
            .map { LocaleKey.valueOf(it) }
            .toSet()
        assertEquals(
            CrisisRegistry.byLocale.keys,
            jsonLocales,
            "Locales in config/safety/crisis/*.json differ from CrisisRegistry.byLocale keys",
        )

        for ((locale, inCode) in CrisisRegistry.byLocale) {
            val fromJson = parseLocale(File(dir, locale.name.lowercase() + ".json"))
            assertEquals(
                fromJson,
                inCode,
                "CrisisRegistry has drifted from config/safety/crisis/${locale.name.lowercase()}.json",
            )
        }
    }

    private fun parseLocale(file: File): List<CrisisResource> {
        val root = Json.parseToJsonElement(file.readText()).jsonObject
        val locale = LocaleKey.valueOf(root.getValue("locale").jsonPrimitive.content)
        return root.getValue("resources").jsonArray.map { node ->
            val r = node.jsonObject
            CrisisResource(
                id = r.getValue("id").jsonPrimitive.content,
                locale = locale,
                name = r.getValue("name").jsonPrimitive.content,
                purpose = Purpose.valueOf(r.getValue("purpose").jsonPrimitive.content),
                contacts = r.getValue("contacts").jsonArray.map { c ->
                    val co = c.jsonObject
                    Contact(
                        method = ContactMethod.valueOf(co.getValue("method").jsonPrimitive.content),
                        label = co.getValue("label").jsonPrimitive.content,
                        value = co.getValue("value").jsonPrimitive.content,
                    )
                },
                hours = r.getValue("hours").jsonPrimitive.let { if (it.toString() == "null") null else it.content },
                languages = r.getValue("languages").jsonArray.map { it.jsonPrimitive.content },
                notes = r.getValue("notes").jsonPrimitive.let { if (it.toString() == "null") null else it.content },
                verifiedOn = r.getValue("verifiedOn").jsonPrimitive.content,
                verifiedBy = r.getValue("verifiedBy").jsonPrimitive.content,
            )
        }
    }

    private fun findCrisisDir(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val candidate = File(dir, "config/safety/crisis")
            if (candidate.isDirectory) return candidate
            dir = dir.parentFile
        }
        fail("config/safety/crisis not found from ${System.getProperty("user.dir")}")
    }
}
