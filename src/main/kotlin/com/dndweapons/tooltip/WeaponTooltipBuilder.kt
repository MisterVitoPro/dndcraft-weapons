package com.dndweapons.tooltip

import com.dndweapons.catalog.DamageType
import com.dndweapons.catalog.Property
import com.dndweapons.catalog.WeaponSpec

/**
 * Builds the DnD tooltip lines for a WeaponSpec. Pure: no MC types in the API.
 *
 * Output shape:
 *   1. Always one stat-block line.
 *   2. Zero or more conditional-bonus lines (Light, Finesse, Versatile, Lance Special),
 *      emitted in Property-enum declaration order.
 *
 * Heavy/Reach/TwoHanded/Thrown/Ammunition/Loading do NOT contribute bonus lines:
 *   - Heavy's effect is the ATTACK_KNOCKBACK attribute itself (visible in vanilla attribute block).
 *   - Reach/TwoHanded/Thrown/Ammunition/Loading are out of Phase 3 scope.
 */
object WeaponTooltipBuilder {

    fun build(spec: WeaponSpec): List<TooltipLine> {
        val out = mutableListOf<TooltipLine>()
        out += statBlock(spec)
        if (Property.LIGHT in spec.properties) {
            out += TooltipLine("tooltip.dndweapons.bonus.light_dual")
        }
        if (Property.FINESSE in spec.properties) {
            out += TooltipLine("tooltip.dndweapons.bonus.finesse_sprint")
        }
        if (Property.VERSATILE in spec.properties) {
            out += TooltipLine("tooltip.dndweapons.bonus.versatile_empty", listOf(spec.versatileBonus))
        }
        if (Property.SPECIAL_LANCE in spec.properties) {
            out += TooltipLine("tooltip.dndweapons.bonus.lance_foot")
        }
        return out
    }

    private fun statBlock(spec: WeaponSpec): TooltipLine {
        return TooltipLine(
            translationKey = "tooltip.dndweapons.stat_block",
            args = listOf(
                spec.diceText,
                damageTypeKey(spec.damageType),
                propertyTrailing(spec),
            ),
        )
    }

    private fun damageTypeKey(t: DamageType): String = when (t) {
        DamageType.SLASHING -> "tooltip.dndweapons.damage_type.slashing"
        DamageType.PIERCING -> "tooltip.dndweapons.damage_type.piercing"
        DamageType.BLUDGEONING -> "tooltip.dndweapons.damage_type.bludgeoning"
    }

    // Renders " · Versatile (1d10) · Finesse · Heavy · ..." style trailing segment.
    // Empty string if the spec has no properties; otherwise begins with " · ".
    private fun propertyTrailing(spec: WeaponSpec): String {
        val parts = mutableListOf<String>()
        for (prop in Property.values()) {
            if (prop !in spec.properties) continue
            parts += when (prop) {
                Property.VERSATILE -> {
                    val vd = spec.versatileDice
                    if (vd != null) "Versatile ($vd)" else "Versatile"
                }
                Property.TWO_HANDED -> "Two-Handed"
                Property.SPECIAL_LANCE -> "Special"
                else -> prop.name.lowercase().replaceFirstChar { it.uppercaseChar() }
            }
        }
        return if (parts.isEmpty()) "" else " · " + parts.joinToString(" · ")
    }
}
