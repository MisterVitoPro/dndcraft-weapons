package com.dndweapons.item

import com.dndweapons.catalog.WeaponSpec
import com.dndweapons.compat.AttributeCompat
import net.minecraft.item.Item

/**
 * Generic melee weapon. Carries a back-reference to its spec so combat hooks
 * (Phase 3) can look up properties at runtime. Phase 2a does not implement
 * any combat hooks - this is just the item identity.
 *
 * On Epoch A (1.20.1), DndWeaponItem.getAttributeModifiers is overridden via
 * Stonecutter //? directive (added in Task 12) to read from AttributeCompat.
 * On Epoch C (1.21+), attributes come from the data component baked into
 * Item.Settings by AttributeCompat.applyTo - no override needed.
 */
open class DndWeaponItem(val spec: WeaponSpec, settings: Settings) : Item(settings) {
    init {
        AttributeCompat.storeFor(spec)
    }
}
