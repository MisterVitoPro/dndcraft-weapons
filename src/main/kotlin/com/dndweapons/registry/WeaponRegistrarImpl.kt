package com.dndweapons.registry

import com.dndweapons.DndWeaponsMod
import com.dndweapons.catalog.Tier
import com.dndweapons.catalog.WeaponSpec
import com.dndweapons.compat.AttributeCompat
import com.dndweapons.item.DndWeaponItem
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation
//?}
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier as ResourceLocation
*///?}
import net.minecraft.world.item.Item

class WeaponRegistrarImpl : WeaponRegistrar {

    override fun register(spec: WeaponSpec, tier: Tier) {
        if (spec.isVanillaMapped) {
            // Vanilla-mapped specs are only registered at IRON tier; the spec routes
            // through SpecRegistry.bindRoleTag and no Item is created.
            if (tier == Tier.IRON) SpecRegistry.bindRoleTag(spec)
            return
        }

        //? if >=1.21 {
        val itemId = ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, spec.id)
        //?} else {
        /*val itemId = ResourceLocation(DndWeaponsMod.MOD_ID, spec.id)
        *///?}
        val itemKey = ResourceKey.create(Registries.ITEM, itemId)

        //? if >=1.21.2 {
        var props = Item.Properties().setId(itemKey)
        //?} else {
        /*var props = Item.Properties()
        *///?}
        props = AttributeCompat.applyTo(props, spec)
        if (tier.fireImmune) props = props.fireResistant()

        val item = DndWeaponItem(spec, props)
        Registry.register(BuiltInRegistries.ITEM, itemKey, item)
        SpecRegistry.bindRegistered(item, spec)
        DndWeaponsMod.LOGGER.info("Registered weapon: {} (tier={})", itemId, tier)
    }
}
