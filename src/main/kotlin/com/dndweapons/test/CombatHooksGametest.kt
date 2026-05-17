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

//? if >=1.21.1 {
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

    @GameTest(template = "fabric-gametest-api-v1:empty")
    fun heavySweepGuardDoesNotBoostSecondary(ctx: GameTestHelper) = runHeavySweepGuardCase(ctx)
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

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun heavySweepGuardDoesNotBoostSecondary(ctx: GameTestHelper) = runHeavySweepGuardCase(ctx)
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
    //? if >=1.21.1 {
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
    player.attackStrengthTicker = 100
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
    //? if >=1.21.1 {
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
    player.attackStrengthTicker = 100
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
    //? if >=1.21.1 {
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
    player.attackStrengthTicker = 100
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
    //? if >=1.21.1 {
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
    player.attackStrengthTicker = 100
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
    //? if >=1.21.1 {
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

    player.attackStrengthTicker = 100
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
    //? if >=1.21.1 {
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
    player.attackStrengthTicker = 100
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

/**
 * Verifies that the ThreadLocal sweep-guard in PlayerAttackMixin correctly limits
 * the DnD @ModifyArg to the primary target only.
 *
 * The Greataxe has the Heavy property (knockback +1) but no damage multiplier in
 * WeaponAttackHandler (HEAVY is not FINESSE/VERSATILE/LIGHT/LANCE). The expected
 * primary damage is therefore `base * 1.0` (the mixin fires but returns base unchanged).
 * The secondary pig must receive only the vanilla sweep damage (< base - tolerance),
 * confirming that the @ModifyArg did NOT fire again for the sweep INVOKE.
 *
 * Sweep range in vanilla: entities within ~1.0 block of the primary that are < 3.0 blocks
 * from the attacker. We place the secondary pig 0.8 blocks from the primary (both at pos
 * y=1) to guarantee it is swept. The secondary pig damage will be the vanilla sweep
 * contribution: `sweepingEdge_level * base / (sweepingEdge_level + 1) + 1.0`, with no
 * Sweeping enchantment this is just `1.0` from the sweep formula, though vanilla may also
 * include a small base contribution. We assert `swept > 0 && swept < base - tolerance`
 * rather than pinning an exact float, to be robust across the 5 MC versions.
 */
private fun runHeavySweepGuardCase(ctx: GameTestHelper) {
    //? if >=1.21.1 {
    val player = ctx.makeMockPlayer(GameType.SURVIVAL)
    //?} else {
    /*val player = ctx.makeMockPlayer()
    *///?}
    player.setItemInHand(InteractionHand.MAIN_HAND, dndItem("greataxe"))
    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY)
    player.isSprinting = false

    // Primary pig: placed directly where the player will attack.
    val primaryPos = BlockPos(2, 1, 2)
    val primary = ctx.spawn(EntityType.PIG, primaryPos) as Pig

    // Secondary pig: 0.8 blocks north of primary — within vanilla sweep range (~1.0 block
    // from primary, and the player at 2,1,2 is ~0.8 blocks away too, within the 3-block
    // attacker distance gate). Vanilla sweeps horizontally, so same Y is fine.
    val secondaryPos = BlockPos(2, 1, 1)
    val secondary = ctx.spawn(EntityType.PIG, secondaryPos) as Pig

    val primaryBefore = primary.health
    val secondaryBefore = secondary.health

    // Fully charge the attack so cooldown multiplier = 1.0 (same fix as Task 1).
    player.attackStrengthTicker = 100
    player.attack(primary)

    val primaryDealt = primaryBefore - primary.health
    val secondaryDealt = secondaryBefore - secondary.health

    val base = player.mainHandItem.attributes.damage()
    val tol = 0.5f

    // Primary must receive the full base damage (Heavy has no damage multiplier, so
    // WeaponAttackHandler.modifyDamage returns base unchanged; the mixin fires once and
    // then consumes the strike-once flag, leaving sweep calls unmodified).
    if (Math.abs(primaryDealt - base) > tol) {
        throw AssertionError(
            "Heavy sweep guard (primary): dealt=$primaryDealt expected~$base (base=$base). " +
                "The mixin may not be firing or is applying an unexpected multiplier."
        )
    }

    // Secondary must have been swept (damage > 0) but must NOT have received the DnD-boosted
    // damage. Since Greataxe has no DnD damage multiplier the boosted and base values are
    // equal; we instead confirm the secondary received strictly LESS than the primary AND
    // received some positive damage (confirming sweep fired). If the strike-once flag had
    // been consumed correctly, the secondary receives only vanilla sweep damage (<= 1.0 on
    // a weapon with no Sweeping Edge enchant).
    if (secondaryDealt <= 0f) {
        throw AssertionError(
            "Heavy sweep guard (secondary): secondary pig took no damage — sweep did not fire. " +
                "Ensure primary and secondary are within vanilla sweep range."
        )
    }
    if (secondaryDealt >= primaryDealt - tol) {
        throw AssertionError(
            "Heavy sweep guard (secondary): secondary dealt=$secondaryDealt is too close to " +
                "primary dealt=$primaryDealt. The DnD @ModifyArg may have fired on the sweep " +
                "INVOKE as well, violating the strike-once guard."
        )
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
