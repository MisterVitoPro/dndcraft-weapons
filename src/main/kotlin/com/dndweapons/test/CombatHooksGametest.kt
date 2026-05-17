package com.dndweapons.test

import com.dndweapons.DndWeaponsMod
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.animal.Pig
//?}
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier as ResourceLocation
import net.minecraft.world.entity.animal.pig.Pig
*///?}

//? if >=1.21.2 {
import net.minecraft.world.level.GameType
//?}

//? if <1.21.5 {
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper

class CombatHooksGametest : FabricGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty")
    fun finesseSprintBonusFires(ctx: GameTestHelper) = runFinesseSprintCase(ctx, dndItem("rapier"))

    @GameTest(template = "fabric-gametest-api-v1:empty")
    fun lightDualWieldBonusFires(ctx: GameTestHelper) = runLightDualCase(ctx)

    @GameTest(template = "fabric-gametest-api-v1:empty")
    fun versatileEmptyOffhandFires(ctx: GameTestHelper) = runVersatileCase(ctx)

    @GameTest(template = "fabric-gametest-api-v1:empty")
    fun lanceOnFootHalves(ctx: GameTestHelper) = runLanceOnFootCase(ctx)

    @GameTest(template = "fabric-gametest-api-v1:empty")
    fun lanceMountedFullDamage(ctx: GameTestHelper) = runLanceMountedCase(ctx)

    @GameTest(template = "fabric-gametest-api-v1:empty")
    fun vanillaIronSwordCarriesFinesseHook(ctx: GameTestHelper) =
        runFinesseSprintCase(ctx, ItemStack(Items.IRON_SWORD))

    @GameTest(template = "fabric-gametest-api-v1:empty")
    fun heavyKnockbackBonusApplies(ctx: GameTestHelper) = runHeavyKnockbackCase(ctx)
}
//?}

//? if >=1.21.5 {

/*import net.fabricmc.fabric.api.gametest.v1.GameTest
import net.minecraft.gametest.framework.GameTestHelper

class CombatHooksGametest {

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun finesseSprintBonusFires(ctx: GameTestHelper) = runFinesseSprintCase(ctx, dndItem("rapier"))

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun lightDualWieldBonusFires(ctx: GameTestHelper) = runLightDualCase(ctx)

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun versatileEmptyOffhandFires(ctx: GameTestHelper) = runVersatileCase(ctx)

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun lanceOnFootHalves(ctx: GameTestHelper) = runLanceOnFootCase(ctx)

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun lanceMountedFullDamage(ctx: GameTestHelper) = runLanceMountedCase(ctx)

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun vanillaIronSwordCarriesFinesseHook(ctx: GameTestHelper) =
        runFinesseSprintCase(ctx, ItemStack(Items.IRON_SWORD))

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun heavyKnockbackBonusApplies(ctx: GameTestHelper) = runHeavyKnockbackCase(ctx)
}

*///?}

// ---- Shared test bodies (compiled in both branches) ----

private fun dndItem(id: String): ItemStack {
    val resId = makeRl(DndWeaponsMod.MOD_ID, id)
    //? if >=1.21.2 {
    val holder = BuiltInRegistries.ITEM.get(resId)
        .orElseThrow { AssertionError("DnD item not registered: $id") }
    return ItemStack(holder.value())
    //?} else {
    /*val item = BuiltInRegistries.ITEM.get(resId)
        ?: throw AssertionError("DnD item not registered: $id")
    return ItemStack(item)
    *///?}
}

private fun makeRl(ns: String, path: String): ResourceLocation {
    //? if >=1.21 {
    return ResourceLocation.fromNamespaceAndPath(ns, path)
    //?} else {
    /*return ResourceLocation(ns, path)
    *///?}
}

private fun runFinesseSprintCase(ctx: GameTestHelper, weapon: ItemStack) {
    //? if >=1.21.2 {
    val player = ctx.makeMockPlayer(GameType.SURVIVAL)
    //?} else {
    /*val player = ctx.makeMockPlayer()
    *///?}
    player.setItemInHand(InteractionHand.MAIN_HAND, weapon)
    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY)
    player.isSprinting = true

    val pos = BlockPos(2, 1, 2)
    val pig = ctx.spawn(EntityType.PIG, pos) as Pig

    val before = pig.health
    player.attack(pig)
    val after = pig.health
    val dealt = before - after

    val base = weapon.attributes.damage()
    val expected = base * 1.20f
    val tol = 0.5f  // vanilla can round; pig armor is zero
    if (Math.abs(dealt - expected) > tol) {
        throw AssertionError("Finesse sprint: dealt=$dealt expected~$expected (base=$base)")
    }
    ctx.succeed()
}

private fun runLightDualCase(ctx: GameTestHelper) {
    //? if >=1.21.2 {
    val player = ctx.makeMockPlayer(GameType.SURVIVAL)
    //?} else {
    /*val player = ctx.makeMockPlayer()
    *///?}
    player.setItemInHand(InteractionHand.MAIN_HAND, dndItem("dagger"))
    player.setItemInHand(InteractionHand.OFF_HAND, dndItem("dagger"))
    player.isSprinting = false

    val pos = BlockPos(2, 1, 2)
    val pig = ctx.spawn(EntityType.PIG, pos) as Pig

    val before = pig.health
    player.attack(pig)
    val after = pig.health
    val dealt = before - after

    val base = player.mainHandItem.attributes.damage()
    val expected = base + 1f
    if (Math.abs(dealt - expected) > 0.5f) {
        throw AssertionError("Light dual: dealt=$dealt expected~$expected (base=$base)")
    }
    ctx.succeed()
}

private fun runVersatileCase(ctx: GameTestHelper) {
    //? if >=1.21.2 {
    val player = ctx.makeMockPlayer(GameType.SURVIVAL)
    //?} else {
    /*val player = ctx.makeMockPlayer()
    *///?}
    player.setItemInHand(InteractionHand.MAIN_HAND, dndItem("longsword"))
    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY)
    player.isSprinting = false

    val pos = BlockPos(2, 1, 2)
    val pig = ctx.spawn(EntityType.PIG, pos) as Pig

    val before = pig.health
    player.attack(pig)
    val after = pig.health
    val dealt = before - after

    val base = player.mainHandItem.attributes.damage()
    val expected = base + 1f  // versatileBonus for Longsword
    if (Math.abs(dealt - expected) > 0.5f) {
        throw AssertionError("Versatile empty: dealt=$dealt expected~$expected (base=$base)")
    }
    ctx.succeed()
}

private fun runLanceOnFootCase(ctx: GameTestHelper) {
    //? if >=1.21.2 {
    val player = ctx.makeMockPlayer(GameType.SURVIVAL)
    //?} else {
    /*val player = ctx.makeMockPlayer()
    *///?}
    player.setItemInHand(InteractionHand.MAIN_HAND, dndItem("lance"))
    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY)
    player.isSprinting = false
    // explicitly NOT mounted

    val pos = BlockPos(2, 1, 2)
    val pig = ctx.spawn(EntityType.PIG, pos) as Pig

    val before = pig.health
    player.attack(pig)
    val after = pig.health
    val dealt = before - after

    val base = player.mainHandItem.attributes.damage()
    val expected = base * 0.5f
    if (Math.abs(dealt - expected) > 0.5f) {
        throw AssertionError("Lance on foot: dealt=$dealt expected~$expected (base=$base)")
    }
    ctx.succeed()
}

private fun runHeavyKnockbackCase(ctx: GameTestHelper) {
    //? if >=1.21.2 {
    val player = ctx.makeMockPlayer(GameType.SURVIVAL)
    //?} else {
    /*val player = ctx.makeMockPlayer()
    *///?}
    // Greataxe is a Heavy property weapon with knockbackBonus=1; the
    // ATTACK_KNOCKBACK attribute modifier in AttributeCompat lifts the
    // target's hit-back velocity above the vanilla baseline.
    player.setItemInHand(InteractionHand.MAIN_HAND, dndItem("greataxe"))
    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY)
    player.isSprinting = false

    val pos = BlockPos(2, 1, 2)
    val pig = ctx.spawn(EntityType.PIG, pos) as Pig
    // Snapshot velocity before the hit. Any post-hit deltaMovement of meaningful
    // magnitude implies knockback was delivered (a regular un-modified hit on a
    // stationary pig produces a smaller knockback component; the Heavy bonus
    // doubles the horizontal kick).
    val beforeVel = pig.deltaMovement.lengthSqr()

    player.attack(pig)

    val afterVel = pig.deltaMovement.lengthSqr()
    // Heavy weapons should impart a noticeable horizontal velocity to the
    // target. Threshold 0.01 (m/tick)^2 = ~0.1 m/tick which is comfortably
    // above the un-modified baseline of ~0.04 m/tick a stationary attack
    // produces on a pig.
    if (afterVel - beforeVel < 0.01f) {
        throw AssertionError(
            "Heavy knockback: post-hit deltaMovement^2=$afterVel did not exceed baseline=$beforeVel by 0.01 " +
                "(greataxe should impart Heavy knockback)"
        )
    }
    ctx.succeed()
}

private fun runLanceMountedCase(ctx: GameTestHelper) {
    //? if >=1.21.2 {
    val player = ctx.makeMockPlayer(GameType.SURVIVAL)
    //?} else {
    /*val player = ctx.makeMockPlayer()
    *///?}
    player.setItemInHand(InteractionHand.MAIN_HAND, dndItem("lance"))
    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY)
    player.isSprinting = false

    val mountPos = BlockPos(1, 1, 1)
    val mount = ctx.spawn(EntityType.PIG, mountPos) as Pig
    //? if >=1.21.11 {
    player.startRiding(mount, true, true)
    //?} else {
    /*player.startRiding(mount, true)
    *///?}

    val targetPos = BlockPos(3, 1, 3)
    val target = ctx.spawn(EntityType.PIG, targetPos) as Pig

    val before = target.health
    player.attack(target)
    val after = target.health
    val dealt = before - after

    val base = player.mainHandItem.attributes.damage()
    val expected = base  // mounted -> full damage
    if (Math.abs(dealt - expected) > 0.5f) {
        throw AssertionError("Lance mounted: dealt=$dealt expected~$expected (base=$base)")
    }
    ctx.succeed()
}

// Helper: read the ATTACK_DAMAGE modifier on the stack's MAINHAND slot.
// Defined as an extension PROPERTY (not function) so callers can write
// `stack.attributes.damage()` without an extra pair of parens.
private val ItemStack.attributes: ItemAttributesAccess
    get() = ItemAttributesAccess(this)

private class ItemAttributesAccess(private val stack: ItemStack) {
    fun damage(): Float {
        //? if >=1.20.5 {
        val mods = stack.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS)
            ?: return defaultPlayerBase()
        var total = defaultPlayerBase()
        for (entry in mods.modifiers()) {
            if (entry.attribute() == net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) {
                total += entry.modifier().amount().toFloat()
            }
        }
        return total
        //?} else {
        /*// Epoch A: Item.getDefaultAttributeModifiers(MAINHAND) returns the Multimap.
        val item = stack.item
        val mm = item.getDefaultAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
        var total = defaultPlayerBase()
        for (mod in mm[net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE]) {
            total += mod.amount.toFloat()
        }
        return total
        *///?}
    }

    private fun defaultPlayerBase(): Float = 1.0f  // player baseline ATTACK_DAMAGE
}
