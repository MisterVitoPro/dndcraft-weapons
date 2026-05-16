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
    fun allIdsAreUnique() {
        val seen = mutableSetOf<String>()
        for (spec in Weapons.ALL) {
            assertTrue(seen.add(spec.id), "duplicate spec id: ${spec.id}")
        }
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
