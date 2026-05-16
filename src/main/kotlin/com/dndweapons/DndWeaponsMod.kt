package com.dndweapons

import com.dndweapons.catalog.Weapons
import com.dndweapons.registry.WeaponRegistrarImpl
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object DndWeaponsMod : ModInitializer {

    const val MOD_ID: String = "dndweapons"
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    val CREATIVE_TAB: RegistryKey<ItemGroup> =
        RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MOD_ID, "main"))

    override fun onInitialize() {
        LOGGER.info("DnD Weapons initializing...")

        val registrar = WeaponRegistrarImpl()
        registrar.registerAll(Weapons.ALL)

        Registry.register(
            Registries.ITEM_GROUP, CREATIVE_TAB,
            FabricItemGroup.builder()
                .displayName(Text.translatable("itemGroup.dndweapons.main"))
                .icon { ItemStack(Registries.ITEM.get(Identifier.of(MOD_ID, "longsword"))) }
                .build(),
        )

        ItemGroupEvents.modifyEntriesEvent(CREATIVE_TAB).register { entries ->
            for (spec in Weapons.ALL) {
                if (spec.isVanillaMapped) continue
                val item = Registries.ITEM.get(Identifier.of(MOD_ID, spec.id))
                if (item != null) entries.add(item)
            }
        }

        LOGGER.info("DnD Weapons initialized with {} weapons.", Weapons.ALL.size)
    }
}
