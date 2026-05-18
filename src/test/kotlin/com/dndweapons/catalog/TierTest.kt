package com.dndweapons.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TierTest {

    @Test
    fun ironTierIsBaseline() {
        assertEquals("", Tier.IRON.suffix)
        assertEquals("", Tier.IRON.displayPrefix)
        assertEquals(0, Tier.IRON.damageBonus)
        assertEquals(250, Tier.IRON.durability)
        assertFalse(Tier.IRON.fireImmune)
    }

    @Test
    fun diamondTierAddsOneDamageAndVanillaDurability() {
        assertEquals("_diamond", Tier.DIAMOND.suffix)
        assertEquals("Diamond ", Tier.DIAMOND.displayPrefix)
        assertEquals(1, Tier.DIAMOND.damageBonus)
        assertEquals(1561, Tier.DIAMOND.durability)
        assertFalse(Tier.DIAMOND.fireImmune)
    }

    @Test
    fun netheriteTierAddsTwoDamageAndIsFireImmune() {
        assertEquals("_netherite", Tier.NETHERITE.suffix)
        assertEquals("Netherite ", Tier.NETHERITE.displayPrefix)
        assertEquals(2, Tier.NETHERITE.damageBonus)
        assertEquals(2031, Tier.NETHERITE.durability)
        assertTrue(Tier.NETHERITE.fireImmune)
    }
}
