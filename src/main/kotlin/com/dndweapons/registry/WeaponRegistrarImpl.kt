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

class WeaponRegistrarImpl : WeaponRegistrar {

    override fun register(spec: WeaponSpec) {
        if (spec.isVanillaMapped) return

        val itemId = Identifier.of(DndWeaponsMod.MOD_ID, spec.id)
        val itemKey = RegistryKey.of(RegistryKeys.ITEM, itemId)
        val settings = AttributeCompat.applyTo(Item.Settings().registryKey(itemKey), spec)
        val item = DndWeaponItem(spec, settings)

        Registry.register(Registries.ITEM, itemKey, item)
        DndWeaponsMod.LOGGER.info("Registered weapon: {}", itemId)
    }
}
