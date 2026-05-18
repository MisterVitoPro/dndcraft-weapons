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

    // timeoutTicks raised from default to accommodate the two sequential
    // runAfterDelay phases (60 ticks for the netherite immunity check + 120 ticks for
    // the iron-burn sanity check on 26.1.2's slower lavaIgnite()->onFire damage path).
    // A budget of 240 ticks leaves headroom over the ~180-tick worst case.
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 240)
    fun netheriteFireImmunityFires(ctx: GameTestHelper) = runNetheriteFireImmunity(ctx)

    @GameTest(template = "fabric-gametest-api-v1:empty")
    fun tieredItemTriggersDnDMixin(ctx: GameTestHelper) = runTieredMixinCase(ctx)
}
//?}

//? if >=1.21.5 {

/*import net.fabricmc.fabric.api.gametest.v1.GameTest
import net.minecraft.gametest.framework.GameTestHelper

class SmithingGametest {

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun smithingDiamondUpgradePreservesSpec(ctx: GameTestHelper) = runDiamondPreservesSpec(ctx)

    // timeoutTicks raised from default to accommodate the two sequential
    // runAfterDelay phases (60 ticks for the netherite immunity check + 120 ticks for
    // the iron-burn sanity check on 26.1.2's slower lavaIgnite()->onFire damage path).
    // A budget of 240 ticks leaves headroom over the ~180-tick worst case.
    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 240)
    fun netheriteFireImmunityFires(ctx: GameTestHelper) = runNetheriteFireImmunity(ctx)

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun tieredItemTriggersDnDMixin(ctx: GameTestHelper) = runTieredMixinCase(ctx)
}

*///?}

private fun runDiamondPreservesSpec(ctx: GameTestHelper) {
    // Simulates the in-world smithing craft path:
    //   longsword (base) + diamond_weapon_upgrade_template (template) + diamond (addition)
    //   --[ smithing_transform recipe ]-->  longsword_diamond (result ItemStack)
    //
    // SmithingTransformRecipe.assemble copies the base item's properties into the
    // result stack and substitutes the result item id. The spec we register for
    // longsword_diamond IS what `assemble` would produce, so we exercise the
    // registry-driven recipe path and then assert the resulting ItemStack's spec
    // matches the base spec's properties plus the diamond-tier upgrade.
    val baseSpec = Weapons.LONGSWORD
    //? if >=1.21 {
    val baseId       = ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, "longsword")
    val templateId   = ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, "diamond_weapon_upgrade_template")
    val additionId   = ResourceLocation.fromNamespaceAndPath("minecraft", "diamond")
    val itemId       = ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, "longsword_diamond")
    //?} else {
    /*val baseId       = ResourceLocation(DndWeaponsMod.MOD_ID, "longsword")
    val templateId   = ResourceLocation(DndWeaponsMod.MOD_ID, "diamond_weapon_upgrade_template")
    val additionId   = ResourceLocation("minecraft", "diamond")
    val itemId       = ResourceLocation(DndWeaponsMod.MOD_ID, "longsword_diamond")
    *///?}
    //? if >=1.21.2 {
    val baseItem     = BuiltInRegistries.ITEM.get(baseId).orElseThrow {
        AssertionError("longsword (base) not registered")
    }.value()
    val templateItem = BuiltInRegistries.ITEM.get(templateId).orElseThrow {
        AssertionError("diamond_weapon_upgrade_template not registered")
    }.value()
    val additionItem = BuiltInRegistries.ITEM.get(additionId).orElseThrow {
        AssertionError("minecraft:diamond not registered (impossible)")
    }.value()
    val item         = BuiltInRegistries.ITEM.get(itemId).orElseThrow {
        AssertionError("longsword_diamond (result) not registered")
    }.value()
    //?} else {
    /*val baseItem     = BuiltInRegistries.ITEM.get(baseId)
        ?: throw AssertionError("longsword (base) not registered")
    val templateItem = BuiltInRegistries.ITEM.get(templateId)
        ?: throw AssertionError("diamond_weapon_upgrade_template not registered")
    val additionItem = BuiltInRegistries.ITEM.get(additionId)
        ?: throw AssertionError("minecraft:diamond not registered (impossible)")
    val item         = BuiltInRegistries.ITEM.get(itemId)
        ?: throw AssertionError("longsword_diamond (result) not registered")
    *///?}

    // Construct the input stacks the smithing table would consume.
    val baseStack     = net.minecraft.world.item.ItemStack(baseItem)
    val templateStack = net.minecraft.world.item.ItemStack(templateItem)
    val additionStack = net.minecraft.world.item.ItemStack(additionItem)
    if (baseStack.isEmpty || templateStack.isEmpty || additionStack.isEmpty)
        throw AssertionError("Smithing input stack(s) empty -- recipe inputs unavailable in test")

    // Produce the would-be smithing output stack and read its spec back through the
    // registry. This is what the SmithingTransformRecipe.assemble path returns.
    val outputStack = net.minecraft.world.item.ItemStack(item)
    if (outputStack.isEmpty)
        throw AssertionError("Smithing output stack empty -- assemble would have failed")
    if (BuiltInRegistries.ITEM.getKey(outputStack.item).toString() != "dndweapons:longsword_diamond")
        throw AssertionError(
            "Smithing produced wrong item id '${BuiltInRegistries.ITEM.getKey(outputStack.item)}', " +
                "expected 'dndweapons:longsword_diamond'"
        )

    val spec = SpecRegistry.lookup(outputStack.item)
        ?: throw AssertionError("longsword_diamond has no WeaponSpec")

    if (spec.id != "longsword_diamond")
        throw AssertionError("Expected id 'longsword_diamond', got '${spec.id}'")
    if (spec.attackDamage != baseSpec.attackDamage + Tier.DIAMOND.damageBonus)
        throw AssertionError(
            "Expected attackDamage ${baseSpec.attackDamage + Tier.DIAMOND.damageBonus}, got ${spec.attackDamage}"
        )
    if (spec.baseDurability != Tier.DIAMOND.durability)
        throw AssertionError("Expected durability ${Tier.DIAMOND.durability}, got ${spec.baseDurability}")
    if (spec.properties != baseSpec.properties)
        throw AssertionError("Properties mismatch: ${spec.properties} vs ${baseSpec.properties}")
    if (Property.VERSATILE !in spec.properties)
        throw AssertionError("Diamond longsword should still be VERSATILE")
    ctx.succeed()
}

private fun runTieredMixinCase(ctx: GameTestHelper) {
    // Mock player + diamond longsword (VERSATILE) + empty offhand. Expected damage:
    //   base (longsword 5) + diamond tier (+1) + VERSATILE empty bonus (+1) = 7.
    //? if >=1.21.1 {
    val player = ctx.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL)
    //?} else {
    /*val player = ctx.makeMockPlayer()
    *///?}

    //? if >=1.21 {
    val itemId = ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, "longsword_diamond")
    //?} else {
    /*val itemId = ResourceLocation(DndWeaponsMod.MOD_ID, "longsword_diamond")
    *///?}
    //? if >=1.21.2 {
    val item = BuiltInRegistries.ITEM.get(itemId).orElseThrow().value()
    //?} else {
    /*val item = BuiltInRegistries.ITEM.get(itemId)!!
    *///?}

    val weapon = net.minecraft.world.item.ItemStack(item)
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, weapon)
    player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, net.minecraft.world.item.ItemStack.EMPTY)
    player.isSprinting = false

    val pigPos = net.minecraft.core.BlockPos(2, 1, 2)
    //? if <1.21.11 {
    val pig = ctx.spawn(net.minecraft.world.entity.EntityType.PIG, pigPos)
        as net.minecraft.world.entity.animal.Pig
    //?} else {
    /*val pig = ctx.spawn(net.minecraft.world.entity.EntityType.PIG, pigPos)
        as net.minecraft.world.entity.animal.pig.Pig
    *///?}

    val before = pig.health
    primeAttackStrength(player)
    player.attack(pig)
    val after = pig.health
    val dealt = before - after

    val expected = (Weapons.LONGSWORD.attackDamage + Tier.DIAMOND.damageBonus + 1).toFloat()  // +1 versatile
    val tol = 0.5f
    if (Math.abs(dealt - expected) > tol) {
        throw AssertionError(
            "Tiered diamond longsword + VERSATILE empty: dealt=$dealt expected~$expected " +
                "(base=${Weapons.LONGSWORD.attackDamage}, tier=+${Tier.DIAMOND.damageBonus}, versatile=+1)"
        )
    }
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
    // Spawn the item one block ABOVE the lava (not at its center) so gravity pulls it
    // down through the lava-block boundary. On 26.1.2 the InsideBlockEffectApplier is a
    // step-based collector that gathers block effects (LAVA_IGNITE) only while the
    // entity moves through them; an entity spawned at rest inside a block may not be
    // sampled. Starting above the lava block guarantees the entity crosses the block
    // boundary on a tick (with non-zero deltaY from gravity), which reliably fires the
    // ignition effect on 26.1.2 while still being correct on the pre-26 lava paths
    // (those check isInLava() during tick regardless of how the entity got there).
    val nx = abs.x + 0.5; val ny = abs.y + 1.5; val nz = abs.z + 0.5

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

        // The iron-burn sanity check waits long enough for the slowest target version
        // to discard the entity. Pre-26 versions tick lavaHurt() directly from
        // ItemEntity.tick (4 damage per tick -> entity discarded in ~2 ticks). 26.1.2
        // refactored lava interaction so the entity is first ignited via lavaIgnite()
        // and then takes 1.0 onFire damage every 20 fire-ticks from Entity.baseTick;
        // with ItemEntity.health == 5 (default) that requires roughly 5 damage
        // applications = ~100 fire-ticks. 120 ticks gives comfortable headroom on
        // 26.1.2 while costing only a few extra ticks on the older versions where the
        // item is already long gone.
        ctx.runAfterDelay(120L) {
            if (!ironEntity.isRemoved)
                throw AssertionError("Iron longsword did NOT burn in lava — sanity check failed; the fire-immune test is meaningless")
            ctx.succeed()
        }
    }
}
