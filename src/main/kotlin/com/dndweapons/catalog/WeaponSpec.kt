package com.dndweapons.catalog

/**
 * Immutable description of one DnD weapon. Single source of truth for the
 * entire catalog. Safe to construct in static initializers on all target
 * Minecraft versions (does not reference Item or registry types).
 *
 * @param vanillaRoleTag identifier of the role tag (e.g. "dndweapons:role/shortsword")
 *                       when this spec is fulfilled by a vanilla item. When non-null,
 *                       no Item is registered for this spec; the tag's members carry
 *                       the DnD identity. Resolved lazily at runtime.
 */
data class WeaponSpec(
    val id: String,
    val displayName: String,
    val category: Category,
    val damageType: DamageType,
    val diceText: String,
    val versatileDice: String?,
    val attackDamage: Int,
    val versatileBonus: Int,
    val attackSpeed: Float,
    val reachBonus: Float,
    val knockbackBonus: Int,
    val properties: Set<Property>,
    val ranged: RangeKind,
    val baseDurability: Int,
    val vanillaRoleTag: String?,
) {
    init {
        require(id.isNotBlank()) { "WeaponSpec id must be non-blank" }
        require(displayName.isNotBlank()) { "WeaponSpec displayName must be non-blank" }
    }

    val isVanillaMapped: Boolean get() = vanillaRoleTag != null
}
