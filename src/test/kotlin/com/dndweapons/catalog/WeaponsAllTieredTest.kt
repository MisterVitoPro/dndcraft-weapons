package com.dndweapons.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WeaponsAllTieredTest {

    @Test
    fun allTieredExcludesVanillaMappedAndRangedNonThrown() {
        for ((spec, _) in Weapons.ALL_TIERED) {
            assertTrue(spec.vanillaRoleTag == null) { "${spec.id} is vanilla-mapped" }
            assertTrue(spec.ranged == RangeKind.NONE || spec.ranged == RangeKind.THROWN) {
                "${spec.id} has ranged=${spec.ranged}, not NONE/THROWN"
            }
        }
    }

    @Test
    fun allTieredHasThreeEntriesPerBaseWeapon() {
        val ids = Weapons.ALL_TIERED.map { (spec, _) -> spec.id }.toSet()
        val baseIds = Weapons.ALL.filter { it.vanillaRoleTag == null && it.ranged in setOf(RangeKind.NONE, RangeKind.THROWN) }.map { it.id }
        for (baseId in baseIds) {
            assertTrue(baseId in ids) { "$baseId missing from ALL_TIERED" }
            assertTrue("${baseId}_diamond" in ids) { "${baseId}_diamond missing" }
            assertTrue("${baseId}_netherite" in ids) { "${baseId}_netherite missing" }
        }
    }

    @Test
    fun allTieredHasExpectedSize() {
        val baseCount = Weapons.ALL.count { it.vanillaRoleTag == null && it.ranged in setOf(RangeKind.NONE, RangeKind.THROWN) }
        assertEquals(baseCount * 3, Weapons.ALL_TIERED.size)
    }

    @Test
    fun allTieredIdsAreUnique() {
        val ids = Weapons.ALL_TIERED.map { (spec, _) -> spec.id }
        assertEquals(ids.size, ids.toSet().size) { "Duplicate ids in ALL_TIERED" }
    }

    @Test
    fun longswordDiamondHasCorrectDamageAndDurability() {
        val (spec, tier) = Weapons.ALL_TIERED.first { (s, t) -> s.id == "longsword_diamond" && t == Tier.DIAMOND }
        assertEquals(Weapons.LONGSWORD.attackDamage + 1, spec.attackDamage)
        assertEquals(1561, spec.baseDurability)
        assertNotNull(tier)
    }
}
