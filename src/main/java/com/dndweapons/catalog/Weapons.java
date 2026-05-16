package com.dndweapons.catalog;

import java.util.List;
import java.util.Set;

/**
 * Source of truth for the entire weapon catalog. Phase 1 contains only the
 * Longsword; Phase 2 adds the remaining 33 registered weapons plus 4
 * vanilla-mapped role specs.
 */
public final class Weapons {
    private Weapons() {}

    public static final WeaponSpec LONGSWORD = new WeaponSpec(
        "longsword", "Longsword",
        Category.MARTIAL_MELEE, DamageType.SLASHING,
        "1d8", "1d10",
        6, 1, 1.5f, 0.0f, 0,
        Set.of(Property.VERSATILE),
        RangeKind.NONE, 250, null
    );

    public static final List<WeaponSpec> ALL = List.of(LONGSWORD);
}
