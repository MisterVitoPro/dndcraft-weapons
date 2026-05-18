package com.dndweapons.registry

import com.dndweapons.catalog.Tier
import com.dndweapons.catalog.WeaponSpec

interface WeaponRegistrar {
    fun register(spec: WeaponSpec, tier: Tier)

    fun registerAll(entries: List<Pair<WeaponSpec, Tier>>) {
        for ((spec, tier) in entries) register(spec, tier)
    }
}
