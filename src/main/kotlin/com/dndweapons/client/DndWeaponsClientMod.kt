package com.dndweapons.client

import com.dndweapons.tooltip.WeaponTooltipInjector
import net.fabricmc.api.ClientModInitializer

/**
 * Client-side entrypoint. Owns ItemTooltipCallback registration and any other
 * net.fabricmc.fabric.api.client.* hook so that the dedicated-server jar never
 * tries to load client-only classes during the main entrypoint.
 *
 * Wired into fabric.mod.json under entrypoints.client.
 */
object DndWeaponsClientMod : ClientModInitializer {
    override fun onInitializeClient() {
        WeaponTooltipInjector.register()
    }
}
