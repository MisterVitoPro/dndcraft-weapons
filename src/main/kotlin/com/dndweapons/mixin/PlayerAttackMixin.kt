package com.dndweapons.mixin

import com.dndweapons.combat.WeaponAttackHandler
import com.dndweapons.registry.SpecRegistry
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.ModifyVariable

/**
 * Injects into Player.attack(Entity) to route the local damage float through
 * WeaponAttackHandler when the player's mainhand item has a WeaponSpec
 * (registered DnD item OR vanilla item carrying a role tag).
 *
 * Target: the call to LivingEntity#hurt(DamageSource, float) inside Player.attack.
 * Shift BEFORE the hurt call so vanilla applies the modified damage.
 *
 * Per-version risk: the local variable name 'f' is not guaranteed by Mojang
 * mappings. If a version fails to find it, swap to ordinal-based selection
 * via //? per-epoch fork (see Task 14).
 */
@Mixin(Player::class)
abstract class PlayerAttackMixin {

    @ModifyVariable(
        method = ["attack(Lnet/minecraft/world/entity/Entity;)V"],
        at = At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            shift = At.Shift.BEFORE,
        ),
        name = ["f"],
    )
    private fun dndweapons_modifyAttackDamage(damage: Float, target: Entity): Float {
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
