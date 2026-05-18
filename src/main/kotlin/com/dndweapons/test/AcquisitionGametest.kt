// src/main/kotlin/com/dndweapons/test/AcquisitionGametest.kt
package com.dndweapons.test

//? if <1.21.5 {
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
//?}
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper

/**
 * Phase 5 acquisition gametests. Three smoke tests:
 *  1. Stronghold corridor loot table produces a mod weapon over N rolls.
 *  2. Weaponsmith level-1 trades include a mod weapon.
 *  3. Vindicator drops a mod battleaxe in a small kill batch.
 *
 * Class declaration is version-gated like CombatHooksGametest and
 * SmithingGametest: pre-1.21.5 extends FabricGameTest with `template`,
 * post-1.21.5 is a bare class with `structure`.
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

private fun runStrongholdCorridorCheck(ctx: GameTestHelper) {
    val server = ctx.level.server
    val table = run {
        //? if <1.21.5 {
        val lootData = server.lootData
        val location = net.minecraft.resources.ResourceLocation.parse("minecraft:chests/stronghold_corridor")
        lootData.getLootTable(location)
        //?} else {
        /*val lootData = server.reloadableRegistries().lookup()
        val key = net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.LOOT_TABLE,
            net.minecraft.resources.ResourceLocation.parse("minecraft:chests/stronghold_corridor"),
        )
        lootData.lookup(net.minecraft.core.registries.Registries.LOOT_TABLE).orElseThrow().get(key).orElseThrow()
        *///?}
    }
    val rng = net.minecraft.util.RandomSource.create(42L)
    val context = net.minecraft.world.level.storage.loot.LootParams.Builder(ctx.level)
        .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST)
    var hits = 0
    repeat(200) {
        val items = table.getRandomItems(context, rng)
        if (items.any { it.descriptionId.startsWith("item.dndweapons.") }) hits++
    }
    if (hits == 0) {
        throw AssertionError(
            "Stronghold corridor produced no dndweapons items in 200 rolls (12% chance, expected ~24)."
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
    *///?} else {
    // 1.20.1-1.21.11 path: query VillagerTrades.TRADES for the profession + level 1.
    val profession = net.minecraft.world.entity.npc.VillagerProfession.WEAPONSMITH
    val trades = net.minecraft.world.entity.npc.VillagerTrades.TRADES[profession]
        ?.get(1) ?: emptyArray()
    val rng = net.minecraft.util.RandomSource.create(0L)
    // Use the villager that gametest helpers spawn at the test origin, then
    // probe each ItemListing for a mod weapon result.
    val villager = ctx.spawnWithNoFreeWill(
        net.minecraft.world.entity.EntityType.VILLAGER,
        net.minecraft.core.BlockPos(2, 2, 2),
    )
    val hasModWeapon = trades.any { listing ->
        //? if <1.21.5 {
        val offer = listing.getOffer(villager, rng)
        //?} else {
        /*val offer = listing.getOffer(ctx.level, villager, rng)
        *///?}
        offer?.result?.descriptionId?.startsWith("item.dndweapons.") == true
    }
    if (!hasModWeapon) {
        throw AssertionError(
            "Weaponsmith level 1 has no dndweapons trades (Phase 5 WeaponTradeRegistrar failed)."
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
            if (e.item.descriptionId == "item.dndweapons.battleaxe") battleaxeDrops++
            e.discard()
        }
    }
    if (battleaxeDrops !in 1..30) {
        throw AssertionError(
            "Vindicator battleaxe drops out of expected range (got $battleaxeDrops/100; expected ~8, accepted 1-30)."
        )
    }
    ctx.succeed()
}
