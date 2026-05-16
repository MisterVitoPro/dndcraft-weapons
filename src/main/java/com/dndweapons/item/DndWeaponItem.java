package com.dndweapons.item;

import com.dndweapons.catalog.WeaponSpec;
import net.minecraft.item.Item;

/**
 * Generic melee weapon. Carries a back-reference to its spec so combat hooks
 * (Phase 3) can look up properties at runtime. Phase 1 does not implement
 * any combat hooks — this is just the item identity.
 */
public class DndWeaponItem extends Item {
    private final WeaponSpec spec;

    public DndWeaponItem(WeaponSpec spec, Settings settings) {
        super(settings);
        this.spec = spec;
    }

    public WeaponSpec spec() {
        return spec;
    }
}
