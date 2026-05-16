package com.dndweapons.item

import com.dndweapons.catalog.WeaponSpec
import com.dndweapons.compat.AttributeCompat
import net.minecraft.item.Item

open class DndWeaponItem(val spec: WeaponSpec, settings: Settings) : Item(settings) {

    init {
        AttributeCompat.storeFor(spec)
    }
}
