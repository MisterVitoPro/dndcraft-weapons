package com.dndweapons.item

import com.dndweapons.catalog.WeaponSpec
import com.dndweapons.compat.AttributeCompat
import net.minecraft.item.Item

/**
 * Generic melee weapon. Carries a back-reference to its spec so combat hooks
 * (Phase 3) can look up properties at runtime. Phase 2a does not implement
 * any combat hooks - this is just the item identity.
 */
open class DndWeaponItem(val spec: WeaponSpec, settings: Settings) : Item(settings) {

    init {
        AttributeCompat.storeFor(spec)
    }

    //? if MC < 1.20.5 {
    /*
    override fun getAttributeModifiers(
        slot: net.minecraft.entity.EquipmentSlot,
    ): com.google.common.collect.Multimap<net.minecraft.entity.attribute.EntityAttribute, net.minecraft.entity.attribute.EntityAttributeModifier> {
        return AttributeCompat.modifiersFor(spec, slot)
    }
    */
    //?}
}
