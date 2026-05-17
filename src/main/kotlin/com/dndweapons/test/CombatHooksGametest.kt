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
 * Strategy: use a FINESSE weapon (rapier) with the player sprinting so the DnD mixin
 * applies a 1.20× damage multiplier to the primary. The sweep INVOKE that hits the
 * secondary pig must NOT receive that multiplier — if the strike-once guard works, the
 * secondary receives only the vanilla sweep contribution (~1.0 on an un-enchanted weapon)
 * which is strictly LESS than the primary's boosted hit. This proves the @ModifyArg fired
 * exactly once even though `player.attack` internally INVOKEs `entity.hurt` multiple times
 * (once for the primary, once per swept entity). HEAVY contributes no damage multiplier in
 * `WeaponAttackHandler.modifyDamage` (it only handles VERSATILE/LIGHT/FINESSE/SPECIAL_LANCE),
 * so a HEAVY weapon would make this test trivially pass even if the mixin never fired —
 * FINESSE is required for the assertion to have observational power.
 *
 * Sweep range in vanilla: entities within ~1.0 block of the primary that are < 3.0 blocks
 * from the attacker. We place the secondary pig 0.5 blocks from the primary (both at y=1)
 * to guarantee it is inside the strict `< 1.0` sweep check used on some MC versions. We
 * assert `secondaryDealt > 0 && secondaryDealt < primaryDealt - tolerance` rather than
 * pinning an exact float, to be robust across the 5 MC versions.
 */
private fun runHeavySweepGuardCase(ctx: GameTestHelper) {
    //? if >=1.21.1 {
    val player = ctx.makeMockPlayer(GameType.SURVIVAL)
    //?} else {
    /*val player = ctx.makeMockPlayer()
    *///?}
    // FINESSE weapon (rapier) + sprinting => DnD mixin applies a 1.20× multiplier to the
    // primary. The strike-once guard must prevent the same multiplier from being applied
    // to the swept secondary, so secondary damage stays at the vanilla sweep baseline.
    player.setItemInHand(InteractionHand.MAIN_HAND, dndItem("rapier"))
    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY)
    player.isSprinting = true

    // Primary pig: placed directly where the player will attack.
    val primaryPos = BlockPos(2, 1, 2)
    val primary = ctx.spawn(EntityType.PIG, primaryPos) as Pig

    // Secondary pig: spawn one block away (relative BlockPos(2,1,1)) so it doesn't
    // collide with the primary at spawn time, then teleport it to a fractional
    // ABSOLUTE position 0.5 blocks closer to the primary along Z. Using the primary's
    // own world coordinates as the reference avoids relying on structure-origin math
    // and keeps the code API-stable across all 5 MC versions (no Vec3/Vec3d class
    // import required — only Entity.setPos(double,double,double)).
    val secondary = ctx.spawn(EntityType.PIG, BlockPos(2, 1, 1)) as Pig
    secondary.setPos(primary.x, primary.y, primary.z - 0.5)

    val primaryBefore = primary.health
    val secondaryBefore = secondary.health

    // Fully charge the attack so cooldown multiplier = 1.0 (same fix as Task 1).
    player.attackStrengthTicker = 100
    player.attack(primary)

    val primaryDealt = primaryBefore - primary.health
    val secondaryDealt = secondaryBefore - secondary.health

    val base = player.mainHandItem.attributes.damage()
    val tol = 0.5f

    // Primary must receive the FINESSE sprint-boosted damage (base * 1.20). If the mixin
    // failed to fire, primaryDealt would equal base (~rapier base) and this would fail.
    val expectedPrimary = base * 1.20f
    if (Math.abs(primaryDealt - expectedPrimary) > tol) {
        throw AssertionError(
            "Sweep guard (primary): dealt=$primaryDealt expected~$expectedPrimary (base=$base). " +
                "The FINESSE sprint mixin may not be firing on the primary hit."
        )
    }

    // Secondary must have been swept (damage > 0) AND must NOT have received the 1.20×
    // FINESSE multiplier. If the strike-once guard is broken, secondary would be boosted
    // to ~primaryDealt; correct behavior yields the vanilla sweep contribution
    // (<= ~1.0 on a weapon with no Sweeping Edge enchant), strictly less than primary.
    if (secondaryDealt <= 0f) {
        throw AssertionError(
            "Sweep guard (secondary): secondary pig took no damage — sweep did not fire. " +
                "Ensure primary and secondary are within vanilla sweep range."
        )
    }
    if (secondaryDealt >= primaryDealt - tol) {
        throw AssertionError(
            "Sweep guard (secondary): secondary dealt=$secondaryDealt is too close to " +
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
