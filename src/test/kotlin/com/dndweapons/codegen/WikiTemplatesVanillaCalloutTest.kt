// src/test/kotlin/com/dndweapons/codegen/WikiTemplatesVanillaCalloutTest.kt
package com.dndweapons.codegen

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * P3-007: WikiTemplates.renderVanillaCallout's `else -> "vanilla item"` silent
 * fallback must be replaced with `else -> error(...)` so future vanilla-mapped
 * specs cannot silently render an embarrassing placeholder.
 */
class WikiTemplatesVanillaCalloutTest {

    @Test
    fun renderVanillaCalloutHasNoSilentVanillaItemFallback() {
        val src = projectRoot().resolve("src/main/kotlin/com/dndweapons/codegen/wiki/WikiTemplates.kt").readText()
        // The fallback string `"vanilla item"` should no longer appear as an else arm.
        // Allow it in comments/strings elsewhere only as the explicit `error()` message.
        val offending = Regex("else\\s*->\\s*\"vanilla item\"").containsMatchIn(src)
        assertTrue(
            !offending,
            "renderVanillaCallout's `else -> \"vanilla item\"` must be replaced with `else -> error(...)` per P3-007"
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
