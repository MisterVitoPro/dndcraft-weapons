// src/main/kotlin/com/dndweapons/trade/WeaponTradeRegistrar.kt
package com.dndweapons.trade

import com.dndweapons.DndWeaponsMod
import com.dndweapons.acquisition.AcquisitionCatalog
import com.dndweapons.acquisition.VillagerTradeEntry
import com.dndweapons.acquisition.WeaponLookup
import com.dndweapons.catalog.Tier
//? if <26.1.2 {
import net.fabricmc.fabric.api.`object`.builder.v1.trade.TradeOfferHelper
//?}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
//? if >=1.21.1 {
import net.minecraft.world.item.trading.ItemCost
//?}
import net.minecraft.world.item.trading.MerchantOffer
//? if <1.21.11 {
import net.minecraft.world.entity.npc.VillagerProfession
import net.minecraft.world.entity.npc.VillagerTrades
//?}
//? if >=1.21.11 {
//? if <26.1.2 {
/*import net.minecraft.world.entity.npc.villager.VillagerProfession
import net.minecraft.world.entity.npc.villager.VillagerTrades
*///?}
//?}
//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation
//?}
//? if >=1.21.11 {
//? if <26.1.2 {
/*import net.minecraft.resources.Identifier as ResourceLocation
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.Registries
*///?}
//?}

/**
 * Phase 5: registers Weaponsmith + Fletcher trades on 1.20.1, 1.21.1, 1.21.4,
 * 1.21.11. 26.1.2 uses data-pack JSON trades (emitted by Phase5TradeCodegen)
 * since Fabric removed TradeOfferHelper from the umbrella at 26.1.
 *
 * Trade signature drift across versions:
 *  - 1.20.1                 : registerVillagerOffers(VillagerProfession, level, factories)
 *                             MerchantOffer takes ItemStack costs (no ItemCost wrapper).
 *                             ItemListing SAM is (Entity, RandomSource) -> MerchantOffer.
 *  - 1.21.1, 1.21.4         : registerVillagerOffers(VillagerProfession, level, factories)
 *                             MerchantOffer takes ItemCost.
 *                             ItemListing SAM is (Entity, RandomSource) -> MerchantOffer.
 *  - 1.21.11                : registerVillagerOffers(ResourceKey<VillagerProfession>, level, factories)
 *                             VillagerProfession lives in npc.villager package.
 *                             ItemListing SAM gains ServerLevel param (3-arg).
 *  - 26.1.2                 : Fabric API removed; this file's register() is stonecutter-stubbed.
 *
 * The version-specific build is in [buildOffer]; the per-profession dispatch
 * is forked per version (1.20.1-1.21.4 vs 1.21.11) so the profession argument
 * has a concrete type at the registerVillagerOffers call site.
 */
object WeaponTradeRegistrar {

    fun register() {
        //? if >=26.1.2 {
        /*// Fabric TradeOfferHelper API removed at 26.1; trades ship as data-pack JSON.
        DndWeaponsMod.LOGGER.info(
            "Skipped WeaponTradeRegistrar Kotlin path on 26.1.2 (using data-pack JSON trades)."
        )
        return
        *///?}

        //? if <26.1.2 {
        var registered = 0
        for ((professionId, levels) in AcquisitionCatalog.VILLAGER_TRADES) {
            //? if <1.21.11 {
            val profession: VillagerProfession = resolveProfessionOld(professionId) ?: continue
            //?}
            //? if >=1.21.11 {
            //? if <26.1.2 {
            /*val profession: ResourceKey<VillagerProfession> = resolveProfessionNew(professionId)
            *///?}
            //?}
            for ((level, trades) in levels) {
                //? if <1.21.5 {
                TradeOfferHelper.registerVillagerOffers(profession, level) { factories ->
                    for (trade in trades) {
                        factories.add(VillagerTrades.ItemListing { _, _ -> buildOffer(trade) })
                    }
                }
                //?} else {
                /*TradeOfferHelper.registerVillagerOffers(profession, level) { factories ->
                    for (trade in trades) {
                        factories.add(VillagerTrades.ItemListing { _, _, _ -> buildOffer(trade) })
                    }
                }
                *///?}
                registered += trades.size
            }
        }
        DndWeaponsMod.LOGGER.info(
            "Registered $registered Phase 5 villager trades (Weaponsmith + Fletcher)."
        )
        //?}
    }

    //? if <1.21.11 {
    private fun resolveProfessionOld(id: String): VillagerProfession? {
        return when (id) {
            "minecraft:weaponsmith" -> VillagerProfession.WEAPONSMITH
            "minecraft:fletcher"    -> VillagerProfession.FLETCHER
            else -> null
        }
    }
    //?}

    //? if >=1.21.11 {
    //? if <26.1.2 {
    /*private fun resolveProfessionNew(id: String): ResourceKey<VillagerProfession> {
        return when (id) {
            "minecraft:weaponsmith" -> VillagerProfession.WEAPONSMITH
            "minecraft:fletcher"    -> VillagerProfession.FLETCHER
            else -> {
                val location = ResourceLocation.parse(id)
                ResourceKey.create(Registries.VILLAGER_PROFESSION, location)
            }
        }
    }
    *///?}
    //?}

    //? if <26.1.2 {
    private fun buildOffer(trade: VillagerTradeEntry): MerchantOffer? {
        val result = WeaponLookup.byId(trade.weapon, Tier.IRON) ?: return null
        val resultStack = ItemStack(result, trade.outputCount)
        //? if >=1.21.1 {
        return MerchantOffer(
            ItemCost(Items.EMERALD, trade.emeralds),
            java.util.Optional.empty(),
            resultStack,
            0,                  // uses
            trade.maxUses,
            trade.xp,
            0.05f,              // price multiplier
        )
        //?} else {
        /*return MerchantOffer(
            ItemStack(Items.EMERALD, trade.emeralds),
            ItemStack.EMPTY,
            resultStack,
            0,                  // uses
            trade.maxUses,
            trade.xp,
            0.05f,
        )
        *///?}
    }
    //?}
}
