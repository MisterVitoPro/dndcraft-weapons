package com.dndweapons

import com.dndweapons.catalog.Weapons
import com.dndweapons.registry.SpecRegistry
import com.dndweapons.registry.WeaponRegistrarImpl
import com.dndweapons.tooltip.WeaponTooltipInjector
import net.fabricmc.api.ModInitializer
//? if <26 {
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
//?} else {
/*import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput
*///?}
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation
//?}
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier as ResourceLocation
*///?}
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private fun rl(namespace: String, path: String): ResourceLocation {
    //? if >=1.21 {
    return ResourceLocation.fromNamespaceAndPath(namespace, path)
    //?} else {
    /*return ResourceLocation(namespace, path)
    *///?}
}

object DndWeaponsMod : ModInitializer {

    const val MOD_ID: String = "dndweapons"
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    val CREATIVE_TAB: ResourceKey<CreativeModeTab> =
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, rl(MOD_ID, "main"))

    override fun onInitialize() {
        LOGGER.info("DnD Weapons initializing...")

        val registrar = WeaponRegistrarImpl()
        registrar.registerAll(Weapons.ALL)

        SpecRegistry.init()
        WeaponTooltipInjector.register()

        //? if <26 {
        Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB, CREATIVE_TAB,
            FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.dndweapons.main"))
                .icon { iconStack() }
                .build(),
        )

        ItemGroupEvents.modifyEntriesEvent(CREATIVE_TAB).register { entries ->
            for (spec in Weapons.ALL) {
                if (spec.isVanillaMapped) continue
                addToEntriesLegacy(entries, spec)
            }
        }
        //?} else {
        /*Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB, CREATIVE_TAB,
            FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.dndweapons.main"))
                .icon { iconStack() }
                .build(),
        )

        CreativeModeTabEvents.modifyOutputEvent(CREATIVE_TAB).register { output ->
            for (spec in Weapons.ALL) {
                if (spec.isVanillaMapped) continue
                addToEntries26(output, spec)
            }
        }
        *///?}

        LOGGER.info("DnD Weapons initialized with {} weapons.", Weapons.ALL.size)
    }

    private fun iconStack(): ItemStack {
        //? if >=1.21.2 {
        return BuiltInRegistries.ITEM
            .get(rl(MOD_ID, "longsword"))
            .map { ItemStack(it) }
            .orElse(ItemStack.EMPTY)
        //?} else {
        /*return ItemStack(BuiltInRegistries.ITEM.get(rl(MOD_ID, "longsword")))
        *///?}
    }

    //? if <26 {
    private fun addToEntriesLegacy(entries: net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries, spec: com.dndweapons.catalog.WeaponSpec) {
        val itemId = rl(MOD_ID, spec.id)
        //? if >=1.21.2 {
        BuiltInRegistries.ITEM.get(itemId).ifPresent { entries.accept(it.value()) }
        //?} else {

        /*val item = BuiltInRegistries.ITEM.get(itemId)
        if (item != null) entries.accept(item)
        *///?}
    }
    //?} else {
    /*private fun addToEntries26(output: FabricCreativeModeTabOutput, spec: com.dndweapons.catalog.WeaponSpec) {
        val itemId = rl(MOD_ID, spec.id)
        BuiltInRegistries.ITEM.get(itemId).ifPresent { output.accept(it.value()) }
    }
    *///?}
}
