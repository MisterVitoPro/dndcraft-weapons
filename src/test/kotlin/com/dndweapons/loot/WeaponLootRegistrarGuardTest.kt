// src/test/kotlin/com/dndweapons/loot/WeaponLootRegistrarGuardTest.kt
package com.dndweapons.loot

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * P1-006 / P2-010 / P2-011: WeaponLootRegistrar.buildStructurePool must:
 *  - guard against `entry.weapons.isEmpty()` (current code: chancePct / 0 -> throws)
 *  - resolve items FIRST, then compute per-weapon weight from `items.size`
 *  - log a WARN per skipped null resolution (P2-010)
 *
 * We can't easily exercise the runtime path here because instantiating LootPool
 * triggers a chain of MC bootstrap initializers (SharedConstants version, Blocks,
 * Items, EntityType) that aren't available in plain JVM unit tests. Instead we
 * verify the source text contains the three required mitigations.
 */
class WeaponLootRegistrarGuardTest {

    private val src: String by lazy {
        projectRoot().resolve("src/main/kotlin/com/dndweapons/loot/WeaponLootRegistrar.kt").readText()
    }

    @Test
    fun buildStructurePoolGuardsEmptyWeaponsList() {
        // Look for an isEmpty() check on entry.weapons inside buildStructurePool.
        val bodyStart = src.indexOf("private fun buildStructurePool")
        assertTrue(bodyStart > 0, "buildStructurePool function body not found")
        val bodyEnd = src.indexOf("private fun ", bodyStart + 1).let {
            if (it < 0) src.length else it
        }
        val body = src.substring(bodyStart, bodyEnd)
        assertTrue(
            body.contains("entry.weapons.isEmpty()") ||
                body.contains("weapons.isEmpty()"),
            "buildStructurePool must guard `entry.weapons.isEmpty()` to avoid chancePct / 0 (P1-006)"
        )
    }

    @Test
    fun buildStructurePoolResolvesItemsBeforeComputingWeight() {
        // After the fix, perWeaponWeight is computed from the resolved item list's
        // size, not the raw entry.weapons.size. Verify by source ordering: the
        // mapNotNull / WeaponLookup resolution must precede the perWeaponWeight
        // expression in the function body.
        val bodyStart = src.indexOf("private fun buildStructurePool")
        val bodyEnd = src.indexOf("private fun ", bodyStart + 1).let {
            if (it < 0) src.length else it
        }
        val body = src.substring(bodyStart, bodyEnd)
        val resolveIdx = body.indexOf("WeaponLookup.byId")
        val weightIdx = body.indexOf("perWeaponWeight")
        assertTrue(resolveIdx > 0, "WeaponLookup.byId call not found in buildStructurePool body")
        assertTrue(weightIdx > 0, "perWeaponWeight not found in buildStructurePool body")
        assertTrue(
            resolveIdx < weightIdx,
            "WeaponLookup.byId resolution must precede perWeaponWeight computation (P2-011)"
        )
    }

    @Test
    fun buildStructurePoolLogsWarnOnSkippedWeapon() {
        // P2-010: every null resolution must produce a LOGGER.warn entry.
        val bodyStart = src.indexOf("private fun buildStructurePool")
        val bodyEnd = src.indexOf("private fun ", bodyStart + 1).let {
            if (it < 0) src.length else it
        }
        val body = src.substring(bodyStart, bodyEnd)
        assertTrue(
            body.contains("LOGGER.warn") || body.contains("LOGGER.warn("),
            "buildStructurePool must LOGGER.warn on null WeaponLookup.byId resolution (P2-010)"
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
