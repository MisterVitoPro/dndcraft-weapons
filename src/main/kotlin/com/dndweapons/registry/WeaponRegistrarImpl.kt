package com.dndweapons.registry

import com.dndweapons.DndWeaponsMod
import com.dndweapons.catalog.WeaponSpec
import com.dndweapons.compat.AttributeCompat
import com.dndweapons.item.DndWeaponItem
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier

/**
 * Thin registrar. Item.Settings.registryKey() was added in 1.21.2 and is
 * required by the 1.21.4+ runtime ("Item id not set" NPE otherwise). On
 * earlier versions the call doesn't exist, and the older
 * Registry.register(Registries.ITEM, Identifier, Item) overload suffices.
 */
class WeaponRegistrarImpl : WeaponRegistrar {

    override fun register(spec: WeaponSpec) {
        if (spec.isVanillaMapped) return

        val itemId = Identifier.of(DndWeaponsMod.MOD_ID, spec.id)

        //? if >=1.21.2 {
        val itemKey = RegistryKey.of(RegistryKeys.ITEM, itemId)
        val settings = AttributeCompat.applyTo(Item.Settings().registryKey(itemKey), spec)
        val item = DndWeaponItem(spec, settings)
        Registry.register(Registries.ITEM, itemKey, item)
        //?} else {
        
        /*val settings = AttributeCompat.applyTo(Item.Settings(), spec)
        val item = DndWeaponItem(spec, settings)
        Registry.register(Registries.ITEM, itemId, item)
        
        *///?}

        DndWeaponsMod.LOGGER.info("Registered weapon: {}", itemId)
    }
}
