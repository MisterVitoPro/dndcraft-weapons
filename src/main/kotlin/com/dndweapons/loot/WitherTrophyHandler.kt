// src/main/kotlin/com/dndweapons/loot/WitherTrophyHandler.kt
package com.dndweapons.loot

import com.dndweapons.DndWeaponsMod
import com.dndweapons.acquisition.WeaponLookup
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLivingEntityEvents
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack

/**
 * Phase 5: drops a single random netherite-tier DnD weapon when the Wither
 * boss dies. 100% guaranteed drop on every Wither death. Uses Fabric's
 * `ServerLivingEntityEvents.AFTER_DEATH` (available on all 5 target versions).
 *
 * The Wither's vanilla drops (nether star + experience) are unaffected; this
 * handler only adds the trophy item.
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
            entity.spawnAtLocation(ItemStack(chosen))
        }
        DndWeaponsMod.LOGGER.info("Registered Wither trophy handler (100% random netherite weapon).")
    }
}
