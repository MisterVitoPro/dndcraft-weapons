package com.dndweapons.registry

import com.dndweapons.catalog.WeaponSpec

interface WeaponRegistrar {
    fun register(spec: WeaponSpec)

    fun registerAll(specs: List<WeaponSpec>) {
        specs.forEach(::register)
    }
}
