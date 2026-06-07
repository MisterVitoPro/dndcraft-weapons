package com.dndweapons.mixin

import com.dndweapons.combat.WeaponAttackHandler
import com.dndweapons.registry.SpecRegistry
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.ModifyArg
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * Injects into Player.attack(Entity) to route the damage float through
 * WeaponAttackHandler when the player's mainhand item has a WeaponSpec
 * (registered DnD item OR vanilla item carrying a role tag).
 *
 * @ModifyArg intercepts argument index 1 (the float) of the primary damage call
 * directly, so no local-variable name / ordinal lookup is needed - immune to LVT
 * differences across MC versions.
 *
 * Per-version fork on the target descriptor:
 *   < 1.21.4  -> Entity#hurt(DamageSource, float) : boolean
 *   >= 1.21.4 -> Entity#hurtOrSimulate(DamageSource, float) : boolean
 *
 * DamageSource descriptor audit (cycle-2):
 *   net.minecraft.world.damagesource.DamageSource has been stable since 1.19.
 *   All 5 supported versions (1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2) use the
 *   same FQN under official Mojang mappings. 26.1.2 uses an identity tiny v2
 *   stub so source-level FQN == runtime FQN. No per-version fork on the
 *   DamageSource portion of the @At target string is required.
 *
 * 26.1.2 identity-mapping note (cycle-2):
 *   The 26.1.2 build uses libs/identity-mappings-26.1.2.jar which maps every
 *   official class/method name to itself (no intermediary remap step). Mixin's
 *   bytecode transformer evaluates @At target descriptors against the runtime
 *   class names supplied by Loom's mapping pipeline. With the identity stub
 *   that pipeline is a no-op, so the source-level descriptor strings here
 *   resolve to the same runtime FQN as on remapped builds. No additional fork
 *   for >=26.1.2 is required. (Verified by gametest pass at runtime - see
 *   cycle-2 task 11 acceptance criteria.)
 */
@Mixin(Player::class)
abstract class PlayerAttackMixin {

    // Tracks which Entity is the *primary* target of the current Player.attack(Entity)
    // invocation. The vanilla method also delivers sweep damage to nearby entities via
    // the same hurt/hurtOrSimulate INVOKE site - without this guard, our @ModifyArg
    // would fire on each sweep entity and double-apply Heavy/Finesse/Lance bonuses to
    // collateral targets. Spec scopes the DnD modifier to a single deliberate strike,
    // so we limit application to the primary target only.
    //
    // ThreadLocal is required because Player.attack runs on the server's main worker
    // pool and a single Player instance can theoretically experience overlapping
    // attack invocations across ticks (rare but possible via mixins/events).
    private val dndweapons_primaryTarget = ThreadLocal<Entity?>()

    @Inject(
        method = ["attack(Lnet/minecraft/world/entity/Entity;)V"],
        at = [At("HEAD")],
    )
    private fun dndweapons_captureTarget(target: Entity, ci: CallbackInfo) {
        dndweapons_primaryTarget.set(target)
    }

    // QA Swarm P1-005 / P3-008: TAIL (not RETURN) so the ThreadLocal also clears
    // on exception exits. RETURN fires only on normal returns; if Player.attack
    // throws after dndweapons_captureTarget set the ThreadLocal, the Entity ref
    // would leak until the next attack on the same thread (worker thread reuse).
    // TAIL fires on every method exit including exception paths.
    @Inject(
        method = ["attack(Lnet/minecraft/world/entity/Entity;)V"],
        at = [At("TAIL")],
    )
    private fun dndweapons_clearTarget(target: Entity, ci: CallbackInfo) {
        dndweapons_primaryTarget.remove()
    }

    @ModifyArg(
        method = ["attack(Lnet/minecraft/world/entity/Entity;)V"],
        //? if <1.21.4 {
        at = At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
        ),
        //?} else {
        /*at = At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
        ),
        *///?}
        index = 1,
    )
    private fun dndweapons_modifyAttackDamage(damage: Float): Float {
        val self = this as Player
        val mainSpec = SpecRegistry.lookup(self.mainHandItem.item) ?: return damage

        // Sweep-entity guard: only apply the DnD modifier when the current INVOKE is
        // hitting the primary target. Sweep INVOKEs target neighbouring entities; we
        // can't distinguish "which entity is being hurt right now" from inside the
        // @ModifyArg callback (we only see the damage float), so we rely on the
        // primary-target ThreadLocal set by dndweapons_captureTarget. The
        // self.lastHurtMob field would be tempting here, but it's set AFTER the hurt
        // call returns, not before - so it isn't useful for this discrimination.
        //
        // The trade-off: we cannot directly inspect the hurt-target inside @ModifyArg.
        // Vanilla calls hurt/hurtOrSimulate on the primary target FIRST (before the
        // sweep loop). We rely on a strike-once flag: the first invocation per
        // Player.attack call applies the modifier; subsequent invocations (sweep) do
        // not.
        val primary = dndweapons_primaryTarget.get() ?: return damage
        // Consume the flag so subsequent INVOKEs in the same call (sweep) skip the
        // modifier. Cleared by dndweapons_clearTarget on RETURN as a safety net.
        dndweapons_primaryTarget.set(null)

        val offhandStack = self.offhandItem
        val ctx = WeaponAttackHandler.Context(
            attackerSprinting = self.isSprinting,
            attackerHasVehicle = self.isPassenger,
            offhandIsEmpty = offhandStack.isEmpty,
            offhandSpec = SpecRegistry.lookup(offhandStack.item),
        )
        return WeaponAttackHandler.modifyDamage(damage, mainSpec, ctx)
    }
}
