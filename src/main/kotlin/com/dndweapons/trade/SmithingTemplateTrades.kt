// src/main/kotlin/com/dndweapons/trade/SmithingTemplateTrades.kt
package com.dndweapons.trade

import com.dndweapons.DndWeaponsMod
import com.dndweapons.item.SmithingTemplateItems
import net.fabricmc.fabric.api.`object`.builder.v1.trade.TradeOfferHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * Adds rare wandering-trader trades for the 2 smithing-upgrade templates. Each trade
 * is uses-capped at 1 per trader spawn so it doesn't dominate the trader's listings.
 *
 *  - 32 emeralds + 1 emerald block -> 1 diamond_weapon_upgrade_template
 *  - 48 emeralds + 1 diamond block -> 1 netherite_weapon_upgrade_template
 *
 * Uses Fabric's TradeOfferHelper.registerWanderingTraderOffers at level 2 (rare).
 */
object SmithingTemplateTrades {

    fun register() {
        //? if >=1.21.1 {
        TradeOfferHelper.registerWanderingTraderOffers(2) { factories ->
            factories.add { _, _ ->
                net.minecraft.world.item.trading.MerchantOffer(
                    net.minecraft.world.item.trading.ItemCost(Items.EMERALD, 32),
                    java.util.Optional.of(net.minecraft.world.item.trading.ItemCost(Items.EMERALD_BLOCK, 1)),
                    ItemStack(SmithingTemplateItems.DIAMOND, 1),
                    /* uses        = */ 0,
                    /* maxUses     = */ 1,
                    /* xp          = */ 30,
                    /* priceMult   = */ 0.05f,
                )
            }
            factories.add { _, _ ->
                net.minecraft.world.item.trading.MerchantOffer(
                    net.minecraft.world.item.trading.ItemCost(Items.EMERALD, 48),
                    java.util.Optional.of(net.minecraft.world.item.trading.ItemCost(Items.DIAMOND_BLOCK, 1)),
                    ItemStack(SmithingTemplateItems.NETHERITE, 1),
                    0, 1, 40, 0.05f,
                )
            }
        }
        //?} else {
        /*TradeOfferHelper.registerWanderingTraderOffers(2) { factories ->
            factories.add(net.minecraft.world.entity.npc.VillagerTrades.TradeListing { _, _ ->
                net.minecraft.world.item.trading.MerchantOffer(
                    ItemStack(Items.EMERALD, 32),
                    ItemStack(Items.EMERALD_BLOCK, 1),
                    ItemStack(SmithingTemplateItems.DIAMOND, 1),
                    0, 1, 30, 0.05f
                )
            })
            factories.add(net.minecraft.world.entity.npc.VillagerTrades.TradeListing { _, _ ->
                net.minecraft.world.item.trading.MerchantOffer(
                    ItemStack(Items.EMERALD, 48),
                    ItemStack(Items.DIAMOND_BLOCK, 1),
                    ItemStack(SmithingTemplateItems.NETHERITE, 1),
                    0, 1, 40, 0.05f
                )
            })
        }
        *///?}
        DndWeaponsMod.LOGGER.info("Registered wandering-trader trades for 2 smithing templates.")
    }
}
