package com.dndweapons.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WeaponSpecTest {

    @Test
    fun canConstructMinimalMeleeSpec() {
        val longsword = WeaponSpec(
            id = "longsword", displayName = "Longsword",
            category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
            diceText = "1d8", versatileDice = "1d10",
            attackDamage = 6, versatileBonus = 1,
            attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
            properties = setOf(Property.VERSATILE),
            ranged = RangeKind.NONE, baseDurability = 250,
            vanillaRoleTag = null,
        )
        assertEquals("longsword", longsword.id)
        assertEquals(6, longsword.attackDamage)
        assertTrue(longsword.properties.contains(Property.VERSATILE))
    }

    @Test
    fun blankIdIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            WeaponSpec(
                id = "", displayName = "Longsword",
                category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
                diceText = "1d8", versatileDice = null,
                attackDamage = 6, versatileBonus = 0,
                attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
                properties = emptySet(),
                ranged = RangeKind.NONE, baseDurability = 250,
                vanillaRoleTag = null,
            )
        }
    }

    @Test
    fun vanillaRoleTagWhenSetMakesItVanillaMapped() {
        val shortsword = WeaponSpec(
            id = "shortsword", displayName = "Shortsword",
            category = Category.MARTIAL_MELEE, damageType = DamageType.PIERCING,
            diceText = "1d6", versatileDice = null,
            attackDamage = 5, versatileBonus = 0,
            attackSpeed = 1.8f, reachBonus = 0.0f, knockbackBonus = 0,
            properties = setOf(Property.LIGHT, Property.FINESSE),
            ranged = RangeKind.NONE, baseDurability = 250,
            vanillaRoleTag = "dndweapons:role/shortsword",
        )
        assertNotNull(shortsword.vanillaRoleTag)
        assertTrue(shortsword.isVanillaMapped)
    }

    @Test
    fun nullVanillaRoleTagMeansRegisteredItem() {
        val longsword = WeaponSpec(
            id = "longsword", displayName = "Longsword",
            category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
            diceText = "1d8", versatileDice = "1d10",
            attackDamage = 6, versatileBonus = 1,
            attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
            properties = setOf(Property.VERSATILE),
            ranged = RangeKind.NONE, baseDurability = 250,
            vanillaRoleTag = null,
        )
        assertNull(longsword.vanillaRoleTag)
        assertFalse(longsword.isVanillaMapped)
    }
}
