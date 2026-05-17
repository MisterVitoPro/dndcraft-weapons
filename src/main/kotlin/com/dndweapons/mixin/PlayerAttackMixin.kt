package com.dndweapons.mixin

import com.dndweapons.combat.WeaponAttackHandler
import com.dndweapons.registry.SpecRegistry
import net.minecraft.world.entity.player.Player
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.ModifyArg

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
 */
@Mixin(Player::class)
abstract class PlayerAttackMixin {

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
