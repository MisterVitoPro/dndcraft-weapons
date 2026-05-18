package com.dndweapons.acquisition

/**
 * One Phase 5 catalog entry for a villager trade offer.
 *
 *  - [weapon] is the base weapon id; trades always sell iron-tier weapons.
 *  - [emeralds] is the input emerald cost; the output is one ItemStack of the
 *    weapon (or [outputCount] for stackables like darts).
 *  - [maxUses] is the trade's per-spawn use cap; vanilla weaponsmith defaults
 *    are 8-12, so 12 is the catalog default.
 *  - [xp] is the villager XP awarded per trade.
 *
 *  All trades are always-available within their level (no random selection
 *  subset); the per-level lists in `AcquisitionCatalog.VILLAGER_TRADES` are
 *  the complete trade pool for that profession-level combination.
 */
data class VillagerTradeEntry(
    val weapon: String,
    val emeralds: Int,
    val outputCount: Int = 1,
    val maxUses: Int = 12,
    val xp: Int = 5,
)
