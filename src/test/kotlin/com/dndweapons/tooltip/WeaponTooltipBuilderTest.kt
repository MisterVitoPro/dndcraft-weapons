package com.dndweapons.tooltip

import com.dndweapons.catalog.Weapons
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WeaponTooltipBuilderTest {

    @Test
    fun maceProducesOnlyStatBlockNoBonusLine() {
        val lines = WeaponTooltipBuilder.build(Weapons.MACE)
        assertEquals(1, lines.size, "Mace should have stat block only")
        assertEquals("tooltip.dndweapons.stat_block", lines[0].translationKey)
        // args = [diceText, damageTypeLabel, propertyList]
        assertEquals("1d6", lines[0].args[0])
        assertEquals("tooltip.dndweapons.damage_type.bludgeoning", lines[0].args[1])
        assertEquals("", lines[0].args[2], "no properties -> empty trailing")
    }

    @Test
    fun rapierProducesStatBlockPlusFinesseBonus() {
        val lines = WeaponTooltipBuilder.build(Weapons.RAPIER)
        assertEquals(2, lines.size)
        assertEquals("tooltip.dndweapons.stat_block", lines[0].translationKey)
        assertEquals("1d8", lines[0].args[0])
        assertEquals("tooltip.dndweapons.damage_type.piercing", lines[0].args[1])
        // Trailing property segment carries the dot-prefixed property tag(s).
        assertTrue(
            (lines[0].args[2] as String).contains("Finesse"),
            "Rapier stat block should mention Finesse"
        )
        assertEquals("tooltip.dndweapons.bonus.finesse_sprint", lines[1].translationKey)
    }

    @Test
    fun longswordVersatileStatBlockShowsVersatileDice() {
        val lines = WeaponTooltipBuilder.build(Weapons.LONGSWORD)
        assertEquals(2, lines.size)
        val statBlockArgs = lines[0].args
        assertEquals("1d8", statBlockArgs[0])
        assertTrue(
            (statBlockArgs[2] as String).contains("Versatile (1d10)"),
            "Longsword stat block should mention Versatile (1d10)"
        )
        assertEquals("tooltip.dndweapons.bonus.versatile_empty", lines[1].translationKey)
        // versatileBonus arg threaded into the bonus line for "+N damage..."
        assertEquals(1, lines[1].args[0])
    }

    @Test
    fun lanceStatBlockShowsAllPropertiesAndBonusLineForSpecial() {
        val lines = WeaponTooltipBuilder.build(Weapons.LANCE)
        // Lance: Heavy + Reach + TwoHanded + SPECIAL_LANCE. Bonus line is for SPECIAL_LANCE only;
        // Heavy/Reach/TwoHanded are silent in Phase 3 (Heavy is the knockback attribute, R/TH are out of scope).
        assertEquals(2, lines.size)
        val statBlockTail = lines[0].args[2] as String
        assertTrue(statBlockTail.contains("Heavy"))
        assertTrue(statBlockTail.contains("Reach"))
        assertTrue(statBlockTail.contains("Two-Handed"))
        assertTrue(statBlockTail.contains("Special"))
        assertEquals("tooltip.dndweapons.bonus.lance_foot", lines[1].translationKey)
    }

    @Test
    fun daggerProducesLightBonusLine() {
        val lines = WeaponTooltipBuilder.build(Weapons.DAGGER)
        // Dagger: Light + Thrown. Light is in-scope; Thrown is silent in Phase 3.
        assertEquals(2, lines.size)
        assertEquals("tooltip.dndweapons.bonus.light_dual", lines[1].translationKey)
    }

    @Test
    fun scimitarProducesLightAndFinesseBonusLinesInDeclarationOrder() {
        // Property declaration order: LIGHT, HEAVY, FINESSE, REACH, TWO_HANDED, VERSATILE, ...
        // Scimitar has Finesse + Light. Bonus lines should be LIGHT first then FINESSE.
        val lines = WeaponTooltipBuilder.build(Weapons.SCIMITAR)
        assertEquals(3, lines.size, "stat block + light bonus + finesse bonus")
        assertEquals("tooltip.dndweapons.bonus.light_dual", lines[1].translationKey)
        assertEquals("tooltip.dndweapons.bonus.finesse_sprint", lines[2].translationKey)
    }

    @Test
    fun whipProducesOnlyFinesseBonusLineNotReach() {
        // Whip: Finesse + Reach. Reach is silent in Phase 3 scope (no attribute applied yet).
        val lines = WeaponTooltipBuilder.build(Weapons.WHIP)
        assertEquals(2, lines.size, "stat block + finesse only; Reach is silent")
        assertEquals("tooltip.dndweapons.bonus.finesse_sprint", lines[1].translationKey)
    }

    @Test
    fun spearProducesVersatileBonusLineThrownIsSilent() {
        // Spear: Thrown + Versatile. Thrown is silent in Phase 3 (custom item subclasses deferred).
        val lines = WeaponTooltipBuilder.build(Weapons.SPEAR)
        assertEquals(2, lines.size, "stat block + versatile only; Thrown is silent")
        assertEquals("tooltip.dndweapons.bonus.versatile_empty", lines[1].translationKey)
    }

    @Test
    fun greataxeStatBlockShowsHeavyButHasNoBonusLine() {
        // Greataxe: Heavy + TwoHanded. Heavy's effect is the attribute; no bonus line.
        val lines = WeaponTooltipBuilder.build(Weapons.GREATAXE)
        assertEquals(1, lines.size, "stat block only; Heavy is silent (attribute), TwoHanded out of scope")
        val tail = lines[0].args[2] as String
        assertTrue(tail.contains("Heavy"))
        assertTrue(tail.contains("Two-Handed"))
    }

    @Test
    fun vanillaMappedShortswordProducesSameAsRegistered() {
        // Vanilla-mapped specs build the same tooltip as registered ones (the injector decides
        // which Items the tooltip attaches to; the builder is spec-agnostic).
        val lines = WeaponTooltipBuilder.build(Weapons.SHORTSWORD)
        assertEquals(3, lines.size, "stat block + light bonus + finesse bonus (same as Scimitar shape)")
        assertEquals("tooltip.dndweapons.bonus.light_dual", lines[1].translationKey)
        assertEquals("tooltip.dndweapons.bonus.finesse_sprint", lines[2].translationKey)
    }
}
