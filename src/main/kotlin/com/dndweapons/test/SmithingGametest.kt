// src/main/kotlin/com/dndweapons/test/SmithingGametest.kt
package com.dndweapons.test

import com.dndweapons.DndWeaponsMod
import com.dndweapons.catalog.Property
import com.dndweapons.catalog.Tier
import com.dndweapons.catalog.Weapons
import com.dndweapons.registry.SpecRegistry
import net.minecraft.core.registries.BuiltInRegistries

//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation
//?}
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier as ResourceLocation
*///?}

//? if <1.21.5 {
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper

class SmithingGametest : FabricGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty")
    fun smithingDiamondUpgradePreservesSpec(ctx: GameTestHelper) = runDiamondPreservesSpec(ctx)

    @GameTest(template = "fabric-gametest-api-v1:empty")
    fun netheriteFireImmunityFires(ctx: GameTestHelper) = runNetheriteFireImmunity(ctx)
}
//?}

//? if >=1.21.5 {

/*import net.fabricmc.fabric.api.gametest.v1.GameTest
import net.minecraft.gametest.framework.GameTestHelper

class SmithingGametest {

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun smithingDiamondUpgradePreservesSpec(ctx: GameTestHelper) = runDiamondPreservesSpec(ctx)

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun netheriteFireImmunityFires(ctx: GameTestHelper) = runNetheriteFireImmunity(ctx)
}

*///?}

private fun runDiamondPreservesSpec(ctx: GameTestHelper) {
    val baseSpec = Weapons.LONGSWORD
    //? if >=1.21 {
    val itemId = ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, "longsword_diamond")
    //?} else {
    /*val itemId = ResourceLocation(DndWeaponsMod.MOD_ID, "longsword_diamond")
    *///?}
    //? if >=1.21.2 {
    val item = BuiltInRegistries.ITEM.get(itemId).orElseThrow {
        AssertionError("longsword_diamond not registered")
    }.value()
    //?} else {
    /*val item = BuiltInRegistries.ITEM.get(itemId)
        ?: throw AssertionError("longsword_diamond not registered")
    *///?}

    val spec = SpecRegistry.lookup(item)
        ?: throw AssertionError("longsword_diamond has no WeaponSpec")

    if (spec.id != "longsword_diamond")
        throw AssertionError("Expected id 'longsword_diamond', got '${spec.id}'")
    if (spec.attackDamage != baseSpec.attackDamage + Tier.DIAMOND.damageBonus)
        throw AssertionError("Expected attackDamage ${baseSpec.attackDamage + 1}, got ${spec.attackDamage}")
    if (spec.baseDurability != Tier.DIAMOND.durability)
        throw AssertionError("Expected durability ${Tier.DIAMOND.durability}, got ${spec.baseDurability}")
    if (spec.properties != baseSpec.properties)
        throw AssertionError("Properties mismatch: ${spec.properties} vs ${baseSpec.properties}")
    if (Property.VERSATILE !in spec.properties)
        throw AssertionError("Diamond longsword should still be VERSATILE")
    ctx.succeed()
}

private fun runNetheriteFireImmunity(ctx: GameTestHelper) {
    // Spawn a netherite-tier item entity in mid-air over a lava block. Wait 60 ticks.
    // Assert the entity is still alive. Then repeat with iron-tier and assert it
    // burns (sanity check that the fire-immune flag is what's keeping netherite alive).
    val world = ctx.level

    // Place a lava block at structure-relative (2, 1, 2).
    val lavaPos = net.minecraft.core.BlockPos(2, 1, 2)
    ctx.setBlock(lavaPos, net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState())

    val abs = ctx.absolutePos(lavaPos)
    val nx = abs.x + 0.5; val ny = abs.y + 0.5; val nz = abs.z + 0.5

    //? if >=1.21 {
    val netheriteId = ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, "longsword_netherite")
    val ironId      = ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, "longsword")
    //?} else {
    /*val netheriteId = ResourceLocation(DndWeaponsMod.MOD_ID, "longsword_netherite")
    val ironId      = ResourceLocation(DndWeaponsMod.MOD_ID, "longsword")
    *///?}
    //? if >=1.21.2 {
    val netheriteItem = BuiltInRegistries.ITEM.get(netheriteId).orElseThrow().value()
    val ironItem      = BuiltInRegistries.ITEM.get(ironId).orElseThrow().value()
    //?} else {
    /*val netheriteItem = BuiltInRegistries.ITEM.get(netheriteId) ?: throw AssertionError("longsword_netherite missing")
    val ironItem      = BuiltInRegistries.ITEM.get(ironId)      ?: throw AssertionError("longsword missing")
    *///?}

    val netheriteEntity = net.minecraft.world.entity.item.ItemEntity(
        world, nx, ny, nz, net.minecraft.world.item.ItemStack(netheriteItem),
    )
    world.addFreshEntity(netheriteEntity)

    ctx.runAfterDelay(60L) {
        if (netheriteEntity.isRemoved)
            throw AssertionError("Netherite longsword burned up in lava — fireResistant flag not applied")

        val ironEntity = net.minecraft.world.entity.item.ItemEntity(
            world, nx, ny, nz, net.minecraft.world.item.ItemStack(ironItem),
        )
        world.addFreshEntity(ironEntity)

        ctx.runAfterDelay(30L) {
            if (!ironEntity.isRemoved)
                throw AssertionError("Iron longsword did NOT burn in lava — sanity check failed; the fire-immune test is meaningless")
            ctx.succeed()
        }
    }
}
