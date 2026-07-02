package com.dndweapons.codegen.wiki

import com.dndweapons.catalog.Property

/**
 * P1-008: Centralized repository for weapon property descriptions used in wiki generation.
 * This replaces hardcoded strings in WikiTemplates and enables easier localization and
 * maintenance of property documentation.
 */
object PropertyDescriptions {

    private val descriptions = mapOf(
        Property.LIGHT to "+1 damage when offhand also holds a Light weapon (dual-wield)",
        Property.HEAVY to "+1 knockback level on hit",
        Property.FINESSE to "+20% damage when attacker is sprinting",
        Property.VERSATILE to "+versatile damage bonus when wielded two-handed",
        Property.TWO_HANDED to "Requires both hands; offhand items prevent attack",
        Property.REACH to "+1 block attack range",
        Property.THROWN to "Right-click to throw as a ranged projectile",
        Property.AMMUNITION to "Requires the appropriate ammo item",
        Property.LOADING to "Reload animation between shots",
        Property.SPECIAL_LANCE to "Lance special: see weapon-specific notes (mounted bonus, off-hand restriction)",
    )

    fun summaryFor(property: Property): String =
        descriptions[property] ?: error("No description found for property $property")
}
