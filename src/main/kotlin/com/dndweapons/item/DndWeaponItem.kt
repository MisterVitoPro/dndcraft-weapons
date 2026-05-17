package com.dndweapons.item

import com.dndweapons.catalog.WeaponSpec
import com.dndweapons.compat.AttributeCompat
import net.minecraft.world.item.Item

open class DndWeaponItem(val spec: WeaponSpec, settings: Properties) : Item(settings) {

    init {
        AttributeCompat.storeFor(spec)
    }

    //? if <1.20.5 {
    /*
    override fun getAttributeModifiers(
        slot: net.minecraft.world.entity.EquipmentSlot,
    ): com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> {
        return AttributeCompat.modifiersFor(spec, slot)
    }
    */
    //?}
}
