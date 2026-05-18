// src/main/kotlin/com/dndweapons/test/AcquisitionGametest.kt
package com.dndweapons.test

//? if <1.21.5 {
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.gametest.framework.GameTest
//?} else {
/*import net.fabricmc.fabric.api.gametest.v1.GameTest
*///?}
import net.minecraft.gametest.framework.GameTestHelper

//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation
//?}
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier as ResourceLocation
*///?}

/**
 * Phase 5 acquisition gametests. Three smoke tests:
 *  1. Stronghold corridor loot table produces a mod weapon over N rolls.
 *  2. Weaponsmith level-1 trades include a mod weapon.
 *  3. Vindicator drops a mod battleaxe in a small kill batch.
 *
 * Class declaration is version-gated like CombatHooksGametest and
 * SmithingGametest: pre-1.21.5 extends FabricGameTest with `template`,
 * post-1.21.5 is a bare class with `structure` (Fabric GameTest annotation).
 *
 * Version-drift accommodations in the helpers below:
 *  - ResourceLocation -> Identifier rename at 1.21.11 (handled with import alias)
 *  - ResourceLocation.parse() does not exist on 1.20.1 (use tryParse + null check)
 *  - MinecraftServer.lootData (1.20.1 only) vs reloadableRegistries().getLootTable(key) (1.21.1+)
 *  - LootTable.getRandomItems return-list overloads:
 *      1.20.1                                : (LootParams, long seed) -> List
 *      1.21.1+                               : (LootParams, RandomSource) -> List
 *  - ItemStack.getDescriptionId() removed at 1.21.4; use stack.item.descriptionId everywhere.
 *  - VillagerProfession/VillagerTrades package moved to npc.villager at 1.21.11; TRADES key
 *    changed to ResourceKey<VillagerProfession>.
 *  - ItemListing SAM gains ServerLevel param at 1.21.5+.
 */
//? if <1.21.5 {
class AcquisitionGametest : FabricGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty")
    fun strongholdCorridorContainsModWeapon(ctx: GameTestHelper) {
        runStrongholdCorridorCheck(ctx)
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    fun weaponsmithLevelOneTradesIncludeModWeapon(ctx: GameTestHelper) {
        runWeaponsmithLevelOneCheck(ctx)
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    fun vindicatorBattleaxeDropsAtExpectedRate(ctx: GameTestHelper) {
        runVindicatorBattleaxeCheck(ctx)
    }
}
//?} else {
/*class AcquisitionGametest {

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun strongholdCorridorContainsModWeapon(ctx: GameTestHelper) {
        runStrongholdCorridorCheck(ctx)
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun weaponsmithLevelOneTradesIncludeModWeapon(ctx: GameTestHelper) {
        runWeaponsmithLevelOneCheck(ctx)
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun vindicatorBattleaxeDropsAtExpectedRate(ctx: GameTestHelper) {
        runVindicatorBattleaxeCheck(ctx)
    }
}
*///?}

private fun parseId(id: String): ResourceLocation {
    //? if >=1.21.1 {
    return ResourceLocation.parse(id)
    //?} else {
    /*return ResourceLocation.tryParse(id)
        ?: throw IllegalArgumentException("Invalid identifier: $id")
    *///?}
}

private fun runStrongholdCorridorCheck(ctx: GameTestHelper) {
    val server = ctx.level.server
    val location = parseId("minecraft:chests/stronghold_corridor")
    //? if <1.21.1 {
    /*val table = server.lootData.getLootTable(location)
    *///?}
    //? if >=1.21.1 {
    val key = net.minecraft.resources.ResourceKey.create(
        net.minecraft.core.registries.Registries.LOOT_TABLE,
        location,
    )
    val table = server.reloadableRegistries().getLootTable(key)
    //?}
    val rng = net.minecraft.util.RandomSource.create(42L)
    val context = net.minecraft.world.level.storage.loot.LootParams.Builder(ctx.level)
        .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST)
    var hits = 0
    repeat(200) {
        //? if <1.21.1 {
        /*val items = table.getRandomItems(context, rng.nextLong())
        *///?}
        //? if >=1.21.1 {
        val items = table.getRandomItems(context, rng)
        //?}
        if (items.any { it.item.descriptionId.startsWith("item.dndweapons.") }) hits++
    }
    if (hits == 0) {
        throw AssertionError(
            "Stronghold corridor produced no dndweapons items in 200 rolls (12% chance, expected ~24).",
        )
    }
    ctx.succeed()
}

private fun runWeaponsmithLevelOneCheck(ctx: GameTestHelper) {
    //? if >=26.1.2 {
    /*// 26.1.2 ships trades as a data pack. Verify the JSON is loaded.
    val rm = ctx.level.server.resourceManager
    val found = rm.listResources("villager_trades") { it.path.endsWith("weaponsmith_level1.json") }
        .keys.any { it.namespace == "dndweapons" }
    if (!found) {
        throw AssertionError("dndweapons weaponsmith_level1.json not loaded by ResourceManager on 26.1.2")
    }
    *///?}
    //? if <26.1.2 {
    // 1.20.1-1.21.11 path: query VillagerTrades.TRADES for the profession + level 1.
    //? if <1.21.11 {
    val professionKey: Any = net.minecraft.world.entity.npc.VillagerProfession.WEAPONSMITH
    val tradesMap = net.minecraft.world.entity.npc.VillagerTrades.TRADES
    //?}
    //? if >=1.21.11 {
    /*val professionKey: Any = net.minecraft.world.entity.npc.villager.VillagerProfession.WEAPONSMITH
    val tradesMap = net.minecraft.world.entity.npc.villager.VillagerTrades.TRADES
    *///?}
    @Suppress("UNCHECKED_CAST")
    val trades = (tradesMap as Map<Any, it.unimi.dsi.fastutil.ints.Int2ObjectMap<Array<Any>>>)[professionKey]
        ?.get(1) ?: emptyArray()
    val rng = net.minecraft.util.RandomSource.create(0L)
    // Use the villager that gametest helpers spawn at the test origin, then
    // probe each ItemListing for a mod weapon result.
    val villager = ctx.spawnWithNoFreeWill(
        net.minecraft.world.entity.EntityType.VILLAGER,
        net.minecraft.core.BlockPos(2, 2, 2),
    )
    //? if <1.21.11 {
    val hasModWeapon = trades.any { listing ->
        val l = listing as net.minecraft.world.entity.npc.VillagerTrades.ItemListing
        //? if <1.21.5 {
        val offer = l.getOffer(villager, rng)
        //?} else {
        /*val offer = l.getOffer(ctx.level, villager, rng)
        *///?}
        offer?.result?.item?.descriptionId?.startsWith("item.dndweapons.") == true
    }
    //?}
    //? if >=1.21.11 {
    /*val hasModWeapon = trades.any { listing ->
        val l = listing as net.minecraft.world.entity.npc.villager.VillagerTrades.ItemListing
        val offer = l.getOffer(ctx.level, villager, rng)
        offer?.result?.item?.descriptionId?.startsWith("item.dndweapons.") == true
    }
    *///?}
    if (!hasModWeapon) {
        throw AssertionError(
            "Weaponsmith level 1 has no dndweapons trades (Phase 5 WeaponTradeRegistrar failed).",
        )
    }
    //?}
    ctx.succeed()
}

private fun runVindicatorBattleaxeCheck(ctx: GameTestHelper) {
    var battleaxeDrops = 0
    val spawnPos = net.minecraft.core.BlockPos(2, 2, 2)
    repeat(100) {
        val v = ctx.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.VINDICATOR, spawnPos)
        v.health = 0.1f
        v.hurt(ctx.level.damageSources().generic(), 999f)
        val center = ctx.absolutePos(spawnPos).center
        val box = net.minecraft.world.phys.AABB(
            center.x - 4, center.y - 4, center.z - 4,
            center.x + 4, center.y + 4, center.z + 4,
        )
        val nearby = ctx.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity::class.java, box)
        for (e in nearby) {
            if (e.item.item.descriptionId == "item.dndweapons.battleaxe") battleaxeDrops++
            e.discard()
        }
    }
    if (battleaxeDrops !in 1..30) {
        throw AssertionError(
            "Vindicator battleaxe drops out of expected range (got $battleaxeDrops/100; expected ~8, accepted 1-30).",
        )
    }
    ctx.succeed()
}
