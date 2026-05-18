// src/main/kotlin/com/dndweapons/loot/SmithingTemplateLootInjector.kt
package com.dndweapons.loot

import com.dndweapons.DndWeaponsMod
import com.dndweapons.item.SmithingTemplateItems
//? if >=26.1.2 {
/*import net.fabricmc.fabric.api.loot.v3.LootTableEvents
*///?} else {
import net.fabricmc.fabric.api.loot.v2.LootTableEvents
//?}
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue

/**
 * Injects the diamond and netherite smithing-upgrade templates into vanilla loot
 * tables so players can discover them outside of the crafting path.
 *
 *  - Stronghold library (low chance, weight 5)  -> diamond template
 *  - Bastion treasure   (lower chance, weight 3) -> netherite template
 *
 * Uses Fabric's LootTableEvents.MODIFY hook. Three independent dimensions of
 * version drift have to be handled here:
 *
 * 1. Fabric module path
 *    - 1.20.1 .. 1.21.11 : net.fabricmc.fabric.api.loot.v2.LootTableEvents
 *    - 26.1.2            : net.fabricmc.fabric.api.loot.v3.LootTableEvents
 *      (the v2 module was dropped from the fabric-api umbrella at 26.1)
 *
 * 2. Modify SAM arity
 *    - v2 / 1.20.1                : 5 params (ResourceManager, LootManager,
 *                                   ResourceLocation, LootTable.Builder,
 *                                   LootTableSource)
 *    - v2 / 1.21.1 .. 1.21.11     : 3 params (ResourceKey<LootTable>,
 *                                   LootTable.Builder, LootTableSource)
 *    - v3 / 26.1.2                : 4 params (ResourceKey<LootTable>,
 *                                   LootTable.Builder, LootTableSource,
 *                                   HolderLookup.Provider)
 *
 * 3. ResourceKey accessor for the loot-table id
 *    - 1.20.1                     : table id supplied directly as the 3rd param
 *    - 1.21.1, 1.21.4             : key.location().toString()
 *    - 1.21.11, 26.1.2            : key.identifier().toString()  (Mojang renamed it)
 *
 * Loot pool insertion uses LootTable.Builder.withPool(LootPool.Builder) on every
 * version; the older v1-style `pool(LootPool)` method does not exist on any of
 * our targets.
 */
object SmithingTemplateLootInjector {

    private const val STRONGHOLD_LIBRARY = "minecraft:chests/stronghold_library"
    private const val BASTION_TREASURE = "minecraft:chests/bastion_treasure"

    fun register() {
        //? if >=26.1.2 {
        /*// v3 hook on 26.1.2: SAM is (key, builder, source, registries)
        LootTableEvents.MODIFY.register { key, builder, _, _ ->
            injectFor(key.identifier().toString(), builder)
        }
        *///?}
        //? if >=1.21.11 {
        //? if <26.1.2 {
        /*// v2 hook on 1.21.11: 3-param SAM, key.identifier() replaces location()
        LootTableEvents.MODIFY.register { key, builder, _ ->
            injectFor(key.identifier().toString(), builder)
        }
        *///?}
        //?}
        //? if >=1.21.1 {
        //? if <1.21.11 {
        // v2 hook on 1.21.1, 1.21.4: 3-param SAM, key.location() still present
        LootTableEvents.MODIFY.register { key, builder, _ ->
            injectFor(key.location().toString(), builder)
        }
        //?}
        //?}
        //? if <1.21.1 {
        /*// v2 hook on 1.20.1: 5-param SAM, table id is the 3rd arg (ResourceLocation)
        LootTableEvents.MODIFY.register { _, _, tableId, builder, _ ->
            injectFor(tableId.toString(), builder)
        }
        *///?}
        DndWeaponsMod.LOGGER.info("Registered smithing-template loot injectors (stronghold + bastion).")
    }

    private fun injectFor(tableId: String, builder: net.minecraft.world.level.storage.loot.LootTable.Builder) {
        when (tableId) {
            STRONGHOLD_LIBRARY -> builder.withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(
                        LootItem.lootTableItem(SmithingTemplateItems.DIAMOND)
                            .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0f)))
                            .setWeight(5)
                    )
                    .add(EmptyLootItem.emptyItem().setWeight(95))
            )
            BASTION_TREASURE -> builder.withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(
                        LootItem.lootTableItem(SmithingTemplateItems.NETHERITE)
                            .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0f)))
                            .setWeight(3)
                    )
                    .add(EmptyLootItem.emptyItem().setWeight(97))
            )
        }
    }
}
