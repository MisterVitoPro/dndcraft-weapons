// src/test/kotlin/com/dndweapons/tooltip/PropertyLangKeyTest.kt
package com.dndweapons.tooltip

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * P1-003: en_us.json must contain 10 property label keys
 * (`tooltip.dndweapons.property.<lowercase_property_name>`) for the i18n-ready
 * stat-block tooltip Phase 3 §7 criterion 13 requires.
 *
 * We don't pull in a JSON parser - a regex `"key": "value"` check is sufficient
 * for the present file shape (one key per line).
 */
class PropertyLangKeyTest {

    private val requiredKeys = listOf(
        "tooltip.dndweapons.property.light",
        "tooltip.dndweapons.property.heavy",
        "tooltip.dndweapons.property.finesse",
        "tooltip.dndweapons.property.reach",
        "tooltip.dndweapons.property.two_handed",
        "tooltip.dndweapons.property.versatile",
        "tooltip.dndweapons.property.thrown",
        "tooltip.dndweapons.property.ammunition",
        "tooltip.dndweapons.property.loading",
        "tooltip.dndweapons.property.special_lance",
    )

    @Test
    fun enUsJsonHasAllTenPropertyLabels() {
        val file = projectRoot().resolve("src/main/resources/assets/dndweapons/lang/en_us.json")
        assertTrue(file.exists(), "en_us.json missing at $file")
        val text = file.readText()
        val missing = mutableListOf<String>()
        for (key in requiredKeys) {
            if (!text.contains("\"$key\"")) missing += key
        }
        assertTrue(
            missing.isEmpty(),
            "Missing tooltip property lang keys (Phase 3 §7 criterion 13): $missing"
        )
    }

    private fun projectRoot(): File {
        var dir: File? = File("").absoluteFile
        repeat(8) {
            if (dir == null) return@repeat
            if (File(dir, "settings.gradle.kts").exists()) return dir!!
            dir = dir!!.parentFile
        }
        error("Could not locate project root")
    }
}
