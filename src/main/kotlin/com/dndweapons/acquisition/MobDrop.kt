package com.dndweapons.acquisition

/**
 * One Phase 5 catalog entry for a vanilla mob loot table.
 *
 *  - [ironWeapon] is the base weapon id dropped at iron tier (e.g. "battleaxe").
 *    Null means this mob has no iron-tier drop (used for Warden, which drops a
 *    random netherite weapon only).
 *  - [ironPct] and [netheritePct] are pool weights for two INDEPENDENT rolls:
 *    one iron pool and one netherite pool inserted into the same mob loot table.
 *    Both rolls can produce a drop on the same kill (the "stacked" policy from
 *    the spec, decision #6).
 *  - When [ironWeapon] is null, the iron roll is skipped entirely; the netherite
 *    pool emits a random selection across all 27 netherite-tier weapons.
 *  - When [ironWeapon] is non-null and [netheritePct] > 0, the netherite pool
 *    emits the SAME weapon at netherite tier (e.g. Wither Skeleton drops glaive
 *    at both iron and netherite tiers).
 */
data class MobDrop(
    val ironWeapon: String?,
    val ironPct: Int,
    val netheritePct: Int = 0,
)
