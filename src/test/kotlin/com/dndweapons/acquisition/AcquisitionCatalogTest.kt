package com.dndweapons.acquisition

import com.dndweapons.catalog.Tier
import com.dndweapons.catalog.Weapons
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AcquisitionCatalogTest {

    @Test
    fun structureLootWeaponIdsAreAllRegistered() {
        for ((tableId, loot) in AcquisitionCatalog.STRUCTURE_LOOT) {
            for (w in loot.weapons) {
                val spec = Weapons.ALL.firstOrNull { it.id == w }
                assertNotNull(spec, "structure '$tableId' references unknown weapon id '$w'")
            }
        }
    }

    @Test
    fun mobDropIronWeaponIdsAreAllRegistered() {
        for ((mobId, drop) in AcquisitionCatalog.MOB_DROPS) {
            drop.ironWeapon?.let { w ->
                val spec = Weapons.ALL.firstOrNull { it.id == w }
                assertNotNull(spec, "mob '$mobId' references unknown iron weapon id '$w'")
            }
        }
    }

    @Test
    fun villagerTradeWeaponIdsAreAllRegistered() {
        for ((profession, levels) in AcquisitionCatalog.VILLAGER_TRADES) {
            for ((level, trades) in levels) {
                for (t in trades) {
                    val spec = Weapons.ALL.firstOrNull { it.id == t.weapon }
                    assertNotNull(spec, "$profession level $level: unknown weapon '${t.weapon}'")
                }
            }
        }
    }

    @Test
    fun structureLootChancePctIsInRange() {
        for ((tableId, loot) in AcquisitionCatalog.STRUCTURE_LOOT) {
            assertTrue(loot.chancePct in 1..100, "structure '$tableId' has out-of-range chancePct ${loot.chancePct}")
        }
    }

    @Test
    fun mobDropPctIsInRange() {
        for ((mobId, drop) in AcquisitionCatalog.MOB_DROPS) {
            assertTrue(drop.ironPct in 0..100, "mob '$mobId' ironPct ${drop.ironPct}")
            assertTrue(drop.netheritePct in 0..100, "mob '$mobId' netheritePct ${drop.netheritePct}")
            // At least one of the two must be non-zero or there's no point in the entry.
            assertTrue(drop.ironPct > 0 || drop.netheritePct > 0, "mob '$mobId' has both pcts at zero")
        }
    }

    @Test
    fun diamondTierStructuresAreEndgameOnly() {
        val diamondTables = AcquisitionCatalog.STRUCTURE_LOOT
            .filter { (_, loot) -> loot.tier == Tier.DIAMOND }
            .keys
        val expected = setOf(
            "minecraft:chests/end_city_treasure",
            "minecraft:chests/ancient_city",
            "minecraft:chests/trial_chambers/reward_ominous_unique",
            "minecraft:chests/bastion_treasure",
        )
        assertEquals(expected, diamondTables, "diamond-tier structure set drifted from spec")
    }

    @Test
    fun trialChambersIsGatedTo121Plus() {
        val entry = AcquisitionCatalog.STRUCTURE_LOOT["minecraft:chests/trial_chambers/reward_ominous_unique"]
        assertNotNull(entry)
        assertEquals("1.21.1", entry!!.minVersion, "trial chambers must be gated to 1.21.1+")
    }
}
