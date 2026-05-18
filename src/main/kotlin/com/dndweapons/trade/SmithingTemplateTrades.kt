// src/main/kotlin/com/dndweapons/trade/SmithingTemplateTrades.kt
package com.dndweapons.trade

import com.dndweapons.DndWeaponsMod
import com.dndweapons.item.SmithingTemplateItems
//? if <26.1.2 {
import net.fabricmc.fabric.api.`object`.builder.v1.trade.TradeOfferHelper
//?}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
//? if >=1.21.1 {
import net.minecraft.world.item.trading.ItemCost
//?}
import net.minecraft.world.item.trading.MerchantOffer
//? if >=1.21.11 {
//? if <26.1.2 {
/*import net.minecraft.world.entity.npc.villager.VillagerTrades
*///?}
//?}
//? if <1.21.11 {
import net.minecraft.world.entity.npc.VillagerTrades
//?}
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier as ResourceLocation
*///?} else {
import net.minecraft.resources.ResourceLocation
//?}

/**
 * Adds rare wandering-trader trades for the 2 smithing-upgrade templates. Each trade
 * is uses-capped at 1 per trader spawn so it doesn't dominate the trader's listings.
 *
 *  - 32 emeralds + 1 emerald block -> 1 diamond_weapon_upgrade_template
 *  - 48 emeralds + 1 diamond block -> 1 netherite_weapon_upgrade_template
 *
 * The trade API has drifted significantly across our 5 targets:
 *
 *  - 1.20.1                 : TradeOfferHelper.registerWanderingTraderOffers(int, Consumer<List<ItemListing>>);
 *                             VillagerTrades.ItemListing(Entity, RandomSource) -> MerchantOffer;
 *                             MerchantOffer(ItemStack, ItemStack, ItemStack, uses, max, xp, mult)  (no ItemCost)
 *  - 1.21.1 / 1.21.4        : same TradeOfferHelper signature; ItemCost wraps the input stacks;
 *                             MerchantOffer(ItemCost, Optional<ItemCost>, ItemStack, uses, max, xp, mult)
 *  - 1.21.11                : TradeOfferHelper.registerWanderingTraderOffers(Consumer<WanderingTraderOffersBuilder>);
 *                             VillagerTrades moved package to net.minecraft.world.entity.npc.villager;
 *                             ItemListing now takes (ServerLevel, Entity, RandomSource)
 *  - 26.1.2                 : Fabric trade API removed from the umbrella entirely.
 *                             Trader trades are unavailable on this version; we log a notice
 *                             and skip registration. (Templates remain obtainable via crafting and loot.)
 */
object SmithingTemplateTrades {

    //? if >=1.21.11 {
    //? if <26.1.2 {
    /*private val RARE_POOL: ResourceLocation =
        TradeOfferHelper.WanderingTraderOffersBuilder.SELL_SPECIAL_ITEMS_POOL
    *///?}
    //?}

    fun register() {
        //? if >=26.1.2 {
        /*// Fabric removed the TradeOfferHelper API at 26.1; smithing templates remain
        // discoverable via crafting and stronghold/bastion loot.
        DndWeaponsMod.LOGGER.info(
            "Skipped wandering-trader trades for smithing templates (Fabric trade API unavailable on 26.1.2+)."
        )
        return
        *///?}

        //? if >=1.21.11 {
        //? if <26.1.2 {
        /*TradeOfferHelper.registerWanderingTraderOffers { builder ->
            builder.pool(
                RARE_POOL,
                1,
                VillagerTrades.ItemListing { _, _, _ ->
                    MerchantOffer(
                        ItemCost(Items.EMERALD, 32),
                        java.util.Optional.of(ItemCost(Items.EMERALD_BLOCK, 1)),
                        ItemStack(SmithingTemplateItems.DIAMOND, 1),
                        0, 1, 30, 0.05f,
                    )
                },
                VillagerTrades.ItemListing { _, _, _ ->
                    MerchantOffer(
                        ItemCost(Items.EMERALD, 48),
                        java.util.Optional.of(ItemCost(Items.DIAMOND_BLOCK, 1)),
                        ItemStack(SmithingTemplateItems.NETHERITE, 1),
                        0, 1, 40, 0.05f,
                    )
                },
            )
        }
        DndWeaponsMod.LOGGER.info("Registered wandering-trader trades for 2 smithing templates.")
        *///?}
        //?}
        //? if >=1.21.1 {
        //? if <1.21.11 {
        TradeOfferHelper.registerWanderingTraderOffers(2) { factories ->
            factories.add(VillagerTrades.ItemListing { _, _ ->
                MerchantOffer(
                    ItemCost(Items.EMERALD, 32),
                    java.util.Optional.of(ItemCost(Items.EMERALD_BLOCK, 1)),
                    ItemStack(SmithingTemplateItems.DIAMOND, 1),
                    0, 1, 30, 0.05f,
                )
            })
            factories.add(VillagerTrades.ItemListing { _, _ ->
                MerchantOffer(
                    ItemCost(Items.EMERALD, 48),
                    java.util.Optional.of(ItemCost(Items.DIAMOND_BLOCK, 1)),
                    ItemStack(SmithingTemplateItems.NETHERITE, 1),
                    0, 1, 40, 0.05f,
                )
            })
        }
        DndWeaponsMod.LOGGER.info("Registered wandering-trader trades for 2 smithing templates.")
        //?}
        //?}
        //? if <1.21.1 {
        /*TradeOfferHelper.registerWanderingTraderOffers(2) { factories ->
            factories.add(VillagerTrades.ItemListing { _, _ ->
                MerchantOffer(
                    ItemStack(Items.EMERALD, 32),
                    ItemStack(Items.EMERALD_BLOCK, 1),
                    ItemStack(SmithingTemplateItems.DIAMOND, 1),
                    0, 1, 30, 0.05f,
                )
            })
            factories.add(VillagerTrades.ItemListing { _, _ ->
                MerchantOffer(
                    ItemStack(Items.EMERALD, 48),
                    ItemStack(Items.DIAMOND_BLOCK, 1),
                    ItemStack(SmithingTemplateItems.NETHERITE, 1),
                    0, 1, 40, 0.05f,
                )
            })
        }
        DndWeaponsMod.LOGGER.info("Registered wandering-trader trades for 2 smithing templates.")
        *///?}
    }
}
