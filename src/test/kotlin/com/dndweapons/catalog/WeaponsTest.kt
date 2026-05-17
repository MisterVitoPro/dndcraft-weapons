package com.dndweapons.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WeaponsTest {

    @Test
    fun allIsNonEmpty() {
        assertFalse(Weapons.ALL.isEmpty())
    }

    @Test
    fun allHasExpectedCount() {
        assertEquals(38, Weapons.ALL.size, "expected 38 PHB weapons in catalog")
    }

    @Test
    fun registeredCount() {
        val registered = Weapons.ALL.filter { !it.isVanillaMapped }
        assertEquals(34, registered.size, "expected 34 registered (non-vanilla-mapped) specs")
    }

    @Test
    fun vanillaMappedCount() {
        val mapped = Weapons.ALL.filter { it.isVanillaMapped }
        assertEquals(4, mapped.size, "expected 4 vanilla-mapped specs")
    }

    @Test
    fun allIdsAreUnique() {
        val seen = mutableSetOf<String>()
        for (spec in Weapons.ALL) {
            assertTrue(seen.add(spec.id), "duplicate spec id: ${spec.id}")
        }
    }

    @Test
    fun categoryCountsMatch() {
        val byCategory = Weapons.ALL.groupBy { it.category }
        assertEquals(10, byCategory[Category.SIMPLE_MELEE]?.size, "simple_melee count")
        assertEquals(4, byCategory[Category.SIMPLE_RANGED]?.size, "simple_ranged count")
        assertEquals(18, byCategory[Category.MARTIAL_MELEE]?.size, "martial_melee count")
        assertEquals(6, byCategory[Category.MARTIAL_RANGED]?.size, "martial_ranged count")
    }

    @Test
    fun vanillaMappedTagsValid() {
        val expected = setOf(
            "dndweapons:role/shortsword",
            "dndweapons:role/shortbow",
            "dndweapons:role/light_crossbow",
            "dndweapons:role/trident",
        )
        val actual = Weapons.ALL
            .filter { it.isVanillaMapped }
            .map { it.vanillaRoleTag }
            .toSet()
        assertEquals(expected, actual)
    }

    @Test
    fun allExpectedIdsPresent() {
        val expected = setOf(
            // simple melee
            "club", "dagger", "greatclub", "handaxe", "javelin",
            "light_hammer", "mace", "quarterstaff", "sickle", "spear",
            // simple ranged
            "dart", "sling", "shortbow", "light_crossbow",
            // martial melee
            "longsword", "battleaxe", "flail", "glaive", "greataxe", "greatsword",
            "halberd", "lance", "maul", "morningstar", "pike", "rapier", "scimitar",
            "shortsword", "trident", "war_pick", "warhammer", "whip",
            // martial ranged
            "blowgun", "hand_crossbow", "heavy_crossbow", "longbow", "musket", "pistol",
        )
        val actual = Weapons.ALL.map { it.id }.toSet()
        assertEquals(expected, actual)
    }

    @Test
    fun longswordIsPresentAndCorrect() {
        val longsword = Weapons.ALL.first { it.id == "longsword" }
        assertEquals("Longsword", longsword.displayName)
        assertEquals(Category.MARTIAL_MELEE, longsword.category)
        assertEquals(DamageType.SLASHING, longsword.damageType)
        assertEquals(6, longsword.attackDamage)
        assertEquals(1, longsword.versatileBonus)
        assertEquals(1.5f, longsword.attackSpeed)
        assertTrue(longsword.properties.contains(Property.VERSATILE))
        assertFalse(longsword.isVanillaMapped)
    }
}
