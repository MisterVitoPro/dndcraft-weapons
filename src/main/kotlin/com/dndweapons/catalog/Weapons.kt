package com.dndweapons.catalog

/**
 * Source of truth for the entire weapon catalog. Phase 2a contains only the
 * Longsword; Phase 2b expands to the remaining 33 registered weapons plus 4
 * vanilla-mapped role specs.
 */
object Weapons {

    val LONGSWORD = WeaponSpec(
        id = "longsword", displayName = "Longsword",
        category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
        diceText = "1d8", versatileDice = "1d10",
        attackDamage = 6, versatileBonus = 1,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.VERSATILE),
        ranged = RangeKind.NONE, baseDurability = 250,
        vanillaRoleTag = null,
    )

    val ALL: List<WeaponSpec> = listOf(LONGSWORD)
}
