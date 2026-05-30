// src/test/kotlin/com/dndweapons/acquisition/AcquisitionCatalogNetheriteValidationTest.kt
package com.dndweapons.acquisition

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * P2-008: AcquisitionCatalog.validate() must check NETHERITE-tier weapons exist
 * for mobs with netheritePct > 0. We verify by source-text inspection that the
 * validate() body references Tier.NETHERITE inside the MOB_DROPS loop (it does
 * not today). Pure structural test - avoids depending on MC registries.
 */
class AcquisitionCatalogNetheriteValidationTest {

    @Test
    fun validateChecksNetheriteTierForMobsWithNetheritePct() {
        val src = projectRoot().resolve("src/main/kotlin/com/dndweapons/acquisition/AcquisitionCatalog.kt").readText()
        // Look for a netherite check inside validate() - any reference to Tier.NETHERITE
        // is sufficient (the current code only references Tier.IRON in the mob loop).
        val validateStart = src.indexOf("fun validate()")
        assertTrue(validateStart > 0, "AcquisitionCatalog.validate() not found")
        val validateBody = src.substring(validateStart)
        assertTrue(
            validateBody.contains("Tier.NETHERITE") || validateBody.contains("netheritePct"),
            "validate() must reference Tier.NETHERITE or netheritePct to catch missing netherite-tier registrations"
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
