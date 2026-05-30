// src/test/kotlin/com/dndweapons/codegen/Phase5TradeCodegenValidationTest.kt
package com.dndweapons.codegen

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * P2-013: Phase5TradeCodegen.main() must validate every trade weapon id against
 * Weapons.ALL before emitting JSON. Structural source-text check: assert main()
 * body references `Weapons.ALL` (or `Weapons.ALL_TIERED`) before the emission
 * loop.
 */
class Phase5TradeCodegenValidationTest {

    @Test
    fun mainValidatesWeaponIdsBeforeEmission() {
        val src = projectRoot().resolve("src/main/kotlin/com/dndweapons/codegen/Phase5TradeCodegen.kt").readText()
        val mainIdx = src.indexOf("fun main()")
        assertTrue(mainIdx > 0, "Phase5TradeCodegen.main() not found")
        val body = src.substring(mainIdx)
        // After fix, main() must mention Weapons (for the id-validation loop) somewhere
        // before it writes JSON files.
        val weaponsRef = body.indexOf("Weapons.ALL")
        val writeText = body.indexOf("writeText")
        assertTrue(
            weaponsRef >= 0,
            "Phase5TradeCodegen.main() must validate trade weapon ids against Weapons.ALL"
        )
        assertTrue(
            weaponsRef in 0 until writeText,
            "Weapons.ALL validation must occur BEFORE writeText emission in main()"
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
