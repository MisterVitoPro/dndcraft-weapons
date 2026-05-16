package com.dndweapons.registry;

import com.dndweapons.catalog.WeaponSpec;

import java.util.List;

public interface WeaponRegistrar {
    void register(WeaponSpec spec);

    default void registerAll(List<WeaponSpec> specs) {
        for (WeaponSpec spec : specs) register(spec);
    }
}
