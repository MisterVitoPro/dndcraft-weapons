package com.dndweapons.catalog

/**
 * Material tier of a registered DnD weapon. Iron is the baseline produced by the
 * existing Phase 2b WeaponSpec catalog; Diamond and Netherite are produced by
 * applying [WeaponSpec.atTier] and registered as separate items in Phase 4.
 *
 * Durability values mirror vanilla MC's `Tiers.IRON/DIAMOND/NETHERITE.getUses()`
 * (pinned here to avoid a per-version `Tiers` class lookup). `damageBonus` is
 * additive on top of the base spec's `attackDamage` and feeds directly into
 * `AttributeCompat` -> `Attributes.ATTACK_DAMAGE` modifier value.
 */
enum class Tier(
    val suffix: String,
    val displayPrefix: String,
    val damageBonus: Int,
    val durability: Int,
    val fireImmune: Boolean,
) {
    IRON(suffix = "", displayPrefix = "", damageBonus = 0, durability = 250, fireImmune = false),
    DIAMOND(suffix = "_diamond", displayPrefix = "Diamond ", damageBonus = 1, durability = 1561, fireImmune = false),
    NETHERITE(suffix = "_netherite", displayPrefix = "Netherite ", damageBonus = 2, durability = 2031, fireImmune = true),
}
