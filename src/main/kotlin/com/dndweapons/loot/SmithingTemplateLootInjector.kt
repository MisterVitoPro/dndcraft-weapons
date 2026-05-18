// src/main/kotlin/com/dndweapons/loot/SmithingTemplateLootInjector.kt
package com.dndweapons.loot

import com.dndweapons.DndWeaponsMod
import com.dndweapons.item.SmithingTemplateItems
import net.fabricmc.fabric.api.loot.v2.LootTableEvents
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue

//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation
//?}
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier as ResourceLocation
*///?}

/**
 * Injects the diamond and netherite smithing-upgrade templates into vanilla loot
 * tables so players can discover them outside of the crafting path.
 *
 *  - Stronghold library (low chance, weight 5) -> diamond template
 *  - Bastion treasure (lower chance, weight 3) -> netherite template
 *
 * Uses the Fabric Loot API v2 LootTableEvents.MODIFY hook, available since 1.20.x
 * on all our target versions. Resource locations differ per MC version for table
 * identifiers; the table strings below are stable in all 5 targets.
 */
object SmithingTemplateLootInjector {

    private const val STRONGHOLD_LIBRARY = "minecraft:chests/stronghold_library"
    private const val BASTION_TREASURE = "minecraft:chests/bastion_treasure"

    fun register() {
        LootTableEvents.MODIFY.register { key, builder, source, _ ->
            //? if >=1.21.1 {
            val tableId = key.location().toString()
            //?} else {
            /*val tableId = key.toString()
            *///?}

            when (tableId) {
                STRONGHOLD_LIBRARY -> builder.pool(
                    net.minecraft.world.level.storage.loot.LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(SmithingTemplateItems.DIAMOND)
                            .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0f)))
                            .setWeight(5))
                        .add(net.minecraft.world.level.storage.loot.entries.EmptyLootItem.emptyItem().setWeight(95))
                        .build()
                )
                BASTION_TREASURE -> builder.pool(
                    net.minecraft.world.level.storage.loot.LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(SmithingTemplateItems.NETHERITE)
                            .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0f)))
                            .setWeight(3))
                        .add(net.minecraft.world.level.storage.loot.entries.EmptyLootItem.emptyItem().setWeight(97))
                        .build()
                )
            }
        }
        DndWeaponsMod.LOGGER.info("Registered smithing-template loot injectors (stronghold + bastion).")
    }
}
