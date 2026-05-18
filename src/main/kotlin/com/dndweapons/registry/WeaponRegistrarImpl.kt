package com.dndweapons.registry

import com.dndweapons.DndWeaponsMod
import com.dndweapons.catalog.Tier
import com.dndweapons.catalog.WeaponSpec
import com.dndweapons.catalog.Weapons
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

    /**
     * Override the default [WeaponRegistrar.registerAll] so that vanilla-mapped specs
     * (Shortsword, Shortbow, Light Crossbow, Trident) are also routed through
     * [SpecRegistry.bindRoleTag]. `Weapons.ALL_TIERED` filters them OUT (only the
     * 27 non-vanilla-mapped melee + thrown specs are tiered), so if we relied solely
     * on the caller-supplied list those four specs would never reach [register] and
     * `SpecRegistry` would not learn about the IRON_SWORD -> SHORTSWORD mapping that
     * the finesse hook and tooltip injection both depend on.
     *
     * This method is idempotent: vanilla-mapped specs are registered exactly once
     * (at IRON tier) regardless of how many tiers the input list contains.
     */
    override fun registerAll(entries: List<Pair<WeaponSpec, Tier>>) {
        // First bind every vanilla-mapped spec from the canonical catalog. We use
        // Weapons.ALL (not the input list) because ALL_TIERED is filtered to exclude
        // vanilla-mapped specs; binding from ALL guarantees coverage regardless of
        // what the caller passes.
        for (spec in Weapons.ALL) {
            if (spec.isVanillaMapped) register(spec, Tier.IRON)
        }
        // Then register the tiered (non-vanilla-mapped) specs normally.
        for ((spec, tier) in entries) register(spec, tier)
    }

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
