package com.dndweapons.registry

import com.dndweapons.DndWeaponsMod
import com.dndweapons.catalog.WeaponSpec
import com.dndweapons.compat.AttributeCompat
import com.dndweapons.item.DndWeaponItem
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
//? if <1.21.11 {
/*import net.minecraft.resources.ResourceLocation
*///?}
//? if >=1.21.11 {
import net.minecraft.resources.Identifier as ResourceLocation
//?}
import net.minecraft.world.item.Item

class WeaponRegistrarImpl : WeaponRegistrar {

    override fun register(spec: WeaponSpec) {
        if (spec.isVanillaMapped) return

        //? if >=1.21 {
        val itemId = ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, spec.id)
        //?} else {
        /*val itemId = ResourceLocation(DndWeaponsMod.MOD_ID, spec.id)
        *///?}
        val itemKey = ResourceKey.create(Registries.ITEM, itemId)

        //? if >=1.21.2 {
        val settings = AttributeCompat.applyTo(Item.Properties().setId(itemKey), spec)
        //?} else {
        /*val settings = AttributeCompat.applyTo(Item.Properties(), spec)
        *///?}

        val item = DndWeaponItem(spec, settings)
        Registry.register(BuiltInRegistries.ITEM, itemKey, item)
        DndWeaponsMod.LOGGER.info("Registered weapon: {}", itemId)
    }
}
