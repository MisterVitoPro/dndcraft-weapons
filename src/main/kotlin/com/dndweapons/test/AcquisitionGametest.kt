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
}
//?} else {
/*class AcquisitionGametest {

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun strongholdCorridorContainsModWeapon(ctx: GameTestHelper) {
        runStrongholdCorridorCheck(ctx)
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
