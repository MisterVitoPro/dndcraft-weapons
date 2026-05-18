package com.dndweapons.acquisition

import com.dndweapons.catalog.Tier

/**
 * One Phase 5 catalog entry for a vanilla structure loot table.
 *
 *  - [weapons] are base weapon ids (e.g. "longsword"); the resolved Item is looked up
 *    by combining the base id with [tier] at registration time via [WeaponLookup.byId].
 *  - [chancePct] is the pool weight for the weapon entry; the empty entry receives
 *    `100 - chancePct` so the pool's expected hit rate per chest opening matches.
 *  - [tier] is IRON for all base structures; DIAMOND for the 4 endgame structures
 *    (End City, Ancient City, Trial Chambers ominous vaults, Bastion treasure).
 *  - [minVersion] is `null` on most entries (apply to all 5 MC versions). Set to
 *    "1.21.1" for Trial Chambers (the structure does not exist on 1.20.1).
 */
data class StructureLoot(
    val weapons: List<String>,
    val chancePct: Int,
    val tier: Tier,
    val minVersion: String? = null,
)
