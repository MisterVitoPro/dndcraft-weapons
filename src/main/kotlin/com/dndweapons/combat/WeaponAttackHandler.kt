package com.dndweapons.combat

import com.dndweapons.catalog.Property
import com.dndweapons.catalog.WeaponSpec

/**
 * Pure damage modifier for the 5 Phase-3 combat properties. Fully unit-testable
 * in plain JVM (no MC types in the signature).
 *
 * Stacking order is load-bearing: additive bonuses (Versatile, Light) apply BEFORE
 * multiplicative scales (Finesse, Lance) so a sprinting Scimitar with a Dagger offhand
 * yields (base + 1) x 1.20, not base x 1.20 + 1.
 */
object WeaponAttackHandler {

    data class Context(
        val attackerSprinting: Boolean,
        val attackerHasVehicle: Boolean,
        val offhandIsEmpty: Boolean,
        val offhandSpec: WeaponSpec?,
    )

    fun modifyDamage(base: Float, mainhand: WeaponSpec, ctx: Context): Float {
        var dmg = base
        // Additives first.
        if (Property.VERSATILE in mainhand.properties && ctx.offhandIsEmpty) {
            dmg += mainhand.versatileBonus
        }
        if (Property.LIGHT in mainhand.properties &&
            ctx.offhandSpec != null && Property.LIGHT in ctx.offhandSpec.properties
        ) {
            dmg += 1.0f
        }
        // Multiplicatives second.
        if (Property.FINESSE in mainhand.properties && ctx.attackerSprinting) {
            dmg *= 1.20f
        }
        if (Property.SPECIAL_LANCE in mainhand.properties && !ctx.attackerHasVehicle) {
            dmg *= 0.50f
        }
        return dmg
    }
}
