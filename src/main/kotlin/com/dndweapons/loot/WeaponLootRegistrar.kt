// src/main/kotlin/com/dndweapons/loot/WeaponLootRegistrar.kt
package com.dndweapons.loot

import com.dndweapons.DndWeaponsMod
import com.dndweapons.acquisition.AcquisitionCatalog
import com.dndweapons.acquisition.MobDrop
import com.dndweapons.acquisition.StructureLoot
import com.dndweapons.acquisition.WeaponLookup
import com.dndweapons.catalog.Tier
//? if >=26.1.2 {
/*import net.fabricmc.fabric.api.loot.v3.LootTableEvents
*///?} else {
import net.fabricmc.fabric.api.loot.v2.LootTableEvents
//?}
import net.minecraft.world.item.Item
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue

/**
 * Phase 5 loot injector. Reads [AcquisitionCatalog] and adds loot pools to the
 * relevant vanilla tables. Structure chests get one pool per matching entry
 * (weapon vs empty, weighted by `chancePct`). Mob entities get up to two
 * INDEPENDENT pools - one iron, one netherite - that roll separately on each
 * kill.
 *
 * Three dimensions of version drift (mirrors Phase 4's SmithingTemplateLootInjector):
 *  1. Fabric module path: v2 (1.20.1..1.21.11) vs v3 (26.1.2).
 *  2. Modify SAM arity:
 *     - v2 / 1.20.1                : 5 params (RM, LM, ResourceLocation, Builder, Source)
 *     - v2 / 1.21.1..1.21.11       : 3 params (ResourceKey<LootTable>, Builder, Source)
 *     - v3 / 26.1.2                : 4 params (ResourceKey<LootTable>, Builder, Source, Registries)
 *  3. ResourceKey accessor for the table id:
 *     - 1.20.1                     : ResourceLocation supplied as the 3rd SAM arg directly
 *     - 1.21.1, 1.21.4             : key.location().toString()
 *     - 1.21.11, 26.1.2            : key.identifier().toString()
 */
object WeaponLootRegistrar {

    private const val MOD_VERSION_STRING: String =
        //? if >=1.21.11 {
        /*"1.21.11"*/
        //?} else if >=1.21.4 {
        /*"1.21.4"*/
        //?} else if >=1.21.1 {
        /*"1.21.1"*/
        //?} else {
        "1.20.1"
        //?}

    fun register() {
        //? if >=26.1.2 {
        /*// v3 hook on 26.1.2: SAM is (key, builder, source, registries)
        LootTableEvents.MODIFY.register { key, builder, _, _ ->
            injectFor(key.identifier().toString(), builder)
        }
        *///?}
        //? if >=1.21.11 {
        //? if <26.1.2 {
        /*// v2 hook on 1.21.11: 3-param SAM, identifier() rename
        LootTableEvents.MODIFY.register { key, builder, _ ->
            injectFor(key.identifier().toString(), builder)
        }
        *///?}
        //?}
        //? if >=1.21.1 {
        //? if <1.21.11 {
        // v2 hook on 1.21.1, 1.21.4: 3-param SAM, location() still present
        LootTableEvents.MODIFY.register { key, builder, _ ->
            injectFor(key.location().toString(), builder)
        }
        //?}
        //?}
        //? if <1.21.1 {
        /*// v2 hook on 1.20.1: 5-param SAM, tableId is the 3rd arg (ResourceLocation)
        LootTableEvents.MODIFY.register { _, _, tableId, builder, _ ->
            injectFor(tableId.toString(), builder)
        }
        *///?}
        DndWeaponsMod.LOGGER.info(
            "Registered Phase 5 loot injectors " +
            "(${AcquisitionCatalog.STRUCTURE_LOOT.size} structures, " +
            "${AcquisitionCatalog.MOB_DROPS.size} mob tables)."
        )
    }

    private fun injectFor(tableId: String, builder: LootTable.Builder) {
        AcquisitionCatalog.STRUCTURE_LOOT[tableId]?.let { entry ->
            if (versionApplies(entry)) {
                builder.withPool(buildStructurePool(entry))
            }
        }
        AcquisitionCatalog.MOB_DROPS[tableId]?.let { drop ->
            if (drop.ironPct > 0)      builder.withPool(buildMobPool(drop, Tier.IRON))
            if (drop.netheritePct > 0) builder.withPool(buildMobPool(drop, Tier.NETHERITE))
        }
    }

    private fun versionApplies(entry: StructureLoot): Boolean {
        if (entry.minVersion == null) return true
        // Simple lexical compare works because our version strings use the same
        // segmented MAJOR.MINOR.PATCH form throughout (1.20.1 < 1.21.1 < 1.21.4 etc.).
        return compareVersionStrings(MOD_VERSION_STRING, entry.minVersion) >= 0
    }

    /** Returns negative if a < b, 0 if equal, positive if a > b. */
    private fun compareVersionStrings(a: String, b: String): Int {
        val ap = a.split('.').map { it.toIntOrNull() ?: 0 }
        val bp = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(ap.size, bp.size)) {
            val ai = ap.getOrNull(i) ?: 0
            val bi = bp.getOrNull(i) ?: 0
            if (ai != bi) return ai - bi
        }
        return 0
    }

    private fun buildStructurePool(entry: StructureLoot): LootPool.Builder {
        val pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
        // Weighted weapon entries: each weapon shares the chancePct weight equally.
        val perWeaponWeight = entry.chancePct / entry.weapons.size
        for (weaponId in entry.weapons) {
            val item: Item = WeaponLookup.byId(weaponId, entry.tier) ?: continue
            pool.add(
                LootItem.lootTableItem(item)
                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0f)))
                    .setWeight(maxOf(perWeaponWeight, 1))
            )
        }
        pool.add(EmptyLootItem.emptyItem().setWeight(maxOf(100 - entry.chancePct, 1)))
        return pool
    }

    private fun buildMobPool(drop: MobDrop, tier: Tier): LootPool.Builder {
        val pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
        val pct = if (tier == Tier.IRON) drop.ironPct else drop.netheritePct
        if (tier == Tier.NETHERITE && drop.ironWeapon == null) {
            // Warden case: random selection across all 27 netherite-tier weapons.
            // Each weapon entry has weight 1; empty entry weight balances the (100-pct).
            val netheriteItems = WeaponLookup.allNetherite()
            val perItemWeight = maxOf(pct / netheriteItems.size, 1)
            for (item in netheriteItems) {
                pool.add(
                    LootItem.lootTableItem(item)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0f)))
                        .setWeight(perItemWeight)
                )
            }
            pool.add(EmptyLootItem.emptyItem().setWeight(maxOf(100 - pct, 1)))
        } else {
            // Iron OR netherite for a mob that has a single thematic weapon id.
            val weaponId = drop.ironWeapon ?: return pool   // defensive; netherite-only handled above
            val item = WeaponLookup.byId(weaponId, tier) ?: return pool
            pool.add(
                LootItem.lootTableItem(item)
                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0f)))
                    .setWeight(pct)
            )
            pool.add(EmptyLootItem.emptyItem().setWeight(maxOf(100 - pct, 1)))
        }
        return pool
    }
}
