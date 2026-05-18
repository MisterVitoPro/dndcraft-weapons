// src/main/kotlin/com/dndweapons/loot/WitherTrophyHandler.kt
package com.dndweapons.loot

import com.dndweapons.DndWeaponsMod
import com.dndweapons.acquisition.WeaponLookup
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack

/**
 * Phase 5: drops a single random netherite-tier DnD weapon when the Wither
 * boss dies. 100% guaranteed drop on every Wither death. Uses Fabric's
 * `ServerLivingEntityEvents.AFTER_DEATH` (available on all 5 target versions).
 *
 * The Wither's vanilla drops (nether star + experience) are unaffected; this
 * handler only adds the trophy item.
 *
 * Version drift handled here:
 *  - `ServerLivingEntityEvents` lives in `net.fabricmc.fabric.api.entity.event.v1`
 *    (NOT in `event.lifecycle.v1`). Same path on all 5 versions, so a single
 *    import covers everything.
 *  - `Entity.spawnAtLocation(ItemStack)` was renamed across versions:
 *      * 1.20.1, 1.21.1               : spawnAtLocation(ItemStack)
 *      * 1.21.4, 1.21.11, 26.1.2      : spawnAtLocation(ServerLevel, ItemStack)
 *    The newer signature requires a ServerLevel; obtained by casting
 *    `entity.level()` to ServerLevel (AFTER_DEATH fires server-side only).
 */
object WitherTrophyHandler {

    fun register() {
        ServerLivingEntityEvents.AFTER_DEATH.register { entity, _ ->
            if (entity.type != EntityType.WITHER) return@register
            val netheriteItems = WeaponLookup.allNetherite()
            if (netheriteItems.isEmpty()) {
                DndWeaponsMod.LOGGER.warn("WitherTrophyHandler: no netherite weapons registered; trophy not dropped.")
                return@register
            }
            val chosen = netheriteItems[entity.random.nextInt(netheriteItems.size)]
            val stack = ItemStack(chosen)
            //? if <1.21.4 {
            /*entity.spawnAtLocation(stack)
            *///?} else {
            entity.spawnAtLocation(entity.level() as net.minecraft.server.level.ServerLevel, stack)
            //?}
        }
        DndWeaponsMod.LOGGER.info("Registered Wither trophy handler (100% random netherite weapon).")
    }
}
