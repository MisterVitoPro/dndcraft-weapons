package com.dndweapons.registry

import com.dndweapons.catalog.Tier
import com.dndweapons.catalog.WeaponSpec

interface WeaponRegistrar {
    fun register(spec: WeaponSpec, tier: Tier)

    /**
     * Phase 4 §4 backward-compat shim. Callers outside the tier-aware loop
     * (e.g. older unit tests, mod-compat shims) can still register a spec at
     * the implicit IRON tier without threading [Tier.IRON] through every call.
     */
    fun register(spec: WeaponSpec): Unit = register(spec, Tier.IRON)

    fun registerAll(entries: List<Pair<WeaponSpec, Tier>>) {
        for ((spec, tier) in entries) register(spec, tier)
    }
}
