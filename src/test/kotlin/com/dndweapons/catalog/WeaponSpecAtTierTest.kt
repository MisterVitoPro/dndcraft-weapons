// src/test/kotlin/com/dndweapons/catalog/WeaponSpecAtTierTest.kt
package com.dndweapons.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WeaponSpecAtTierTest {

    @Test
    fun atTierIronReturnsIdenticalSpec() {
        val base = Weapons.LONGSWORD
        val iron = base.atTier(Tier.IRON)
        assertEquals(base.id, iron.id)
        assertEquals(base.displayName, iron.displayName)
        assertEquals(base.attackDamage, iron.attackDamage)
        assertEquals(base.baseDurability, iron.baseDurability)
    }

    @Test
    fun atTierDiamondSuffixesIdAndAddsOneDamage() {
        val diamond = Weapons.LONGSWORD.atTier(Tier.DIAMOND)
        assertEquals("longsword_diamond", diamond.id)
        assertEquals("Diamond Longsword", diamond.displayName)
        assertEquals(Weapons.LONGSWORD.attackDamage + 1, diamond.attackDamage)
        assertEquals(1561, diamond.baseDurability)
    }

    @Test
    fun atTierNetheriteSuffixesIdAndAddsTwoDamage() {
        val netherite = Weapons.GREATAXE.atTier(Tier.NETHERITE)
        assertEquals("greataxe_netherite", netherite.id)
        assertEquals("Netherite Greataxe", netherite.displayName)
        assertEquals(Weapons.GREATAXE.attackDamage + 2, netherite.attackDamage)
        assertEquals(2031, netherite.baseDurability)
        assertTrue(Tier.NETHERITE.fireImmune)
    }

    @Test
    fun atTierPreservesPropertiesAndAttackSpeed() {
        val rapier = Weapons.RAPIER
        val diamondRapier = rapier.atTier(Tier.DIAMOND)
        assertEquals(rapier.properties, diamondRapier.properties)
        assertEquals(rapier.attackSpeed, diamondRapier.attackSpeed)
        assertEquals(rapier.reachBonus, diamondRapier.reachBonus)
        assertEquals(rapier.knockbackBonus, diamondRapier.knockbackBonus)
        assertEquals(rapier.damageType, diamondRapier.damageType)
        assertEquals(rapier.category, diamondRapier.category)
    }
}
