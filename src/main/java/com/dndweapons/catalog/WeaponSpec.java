package com.dndweapons.catalog;

import java.util.Set;

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
public record WeaponSpec(
    String id,
    String displayName,
    Category category,
    DamageType damageType,
    String diceText,
    String versatileDice,
    int attackDamage,
    int versatileBonus,
    float attackSpeed,
    float reachBonus,
    int knockbackBonus,
    Set<Property> properties,
    RangeKind ranged,
    int baseDurability,
    String vanillaRoleTag
) {
    public WeaponSpec {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("WeaponSpec id must be non-blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("WeaponSpec displayName must be non-blank");
        }
        if (properties == null) {
            throw new IllegalArgumentException("WeaponSpec properties must be non-null");
        }
        // defensive copy to immutable set
        properties = Set.copyOf(properties);
    }

    public boolean isVanillaMapped() {
        return vanillaRoleTag != null;
    }
}
