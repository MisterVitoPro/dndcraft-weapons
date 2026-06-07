// src/test/kotlin/com/dndweapons/codegen/AcquisitionLookupNetheriteRandomTest.kt
package com.dndweapons.codegen

import com.dndweapons.codegen.wiki.AcquisitionFact
import com.dndweapons.codegen.wiki.AcquisitionLookup
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * P3-006: AcquisitionLookup must emit a Warden netherite-random drop fact for
 * every netherite-tier weapon. Without this fix, no netherite weapon page
 * mentions the Warden 5% drop because the current loop skips entries with
 * `ironWeapon == null`.
 *
 * After fix, `factsFor("longsword_netherite")` (or any other netherite weapon
 * id) must include a `NetheriteRandomDrop` (or similar) fact mentioning Warden.
 */
class AcquisitionLookupNetheriteRandomTest {

    @Test
    fun netheriteWeaponPagesIncludeWardenRandomDrop() {
        val lookup = AcquisitionLookup.build()
        // Pick a netherite-tier weapon id that's NOT a per-mob drop.
        // longsword_netherite has no specific iron/netherite mob drop entry,
        // so without P3-006 it would have only crafting/structure facts.
        val facts = lookup.factsFor("longsword_netherite")
        val mentionsWarden = facts.any { fact ->
            when (fact) {
                is AcquisitionFact.MobDrop -> fact.mobLabel.contains("Warden", ignoreCase = true)
                // After P3-006 a new variant is added; assert any fact references Warden.
                else -> fact.toString().contains("Warden", ignoreCase = true)
            }
        }
        assertTrue(
            mentionsWarden,
            "Every netherite-tier weapon page must include the Warden random-drop fact (P3-006). " +
                "Facts found for longsword_netherite: $facts"
        )
    }
}
