package com.dndweapons.acquisition

import com.dndweapons.DndWeaponsMod
import com.dndweapons.catalog.Tier

/**
 * Single declarative source of truth for Phase 5 acquisition surfaces.
 *
 * - [STRUCTURE_LOOT]: 13 vanilla loot table IDs -> StructureLoot entry. Iron
 *   for 9 entries; Diamond for 4 endgame entries (End City, Ancient City,
 *   Trial Chambers ominous vaults, Bastion treasure).
 * - [MOB_DROPS]: 7 mob loot table IDs -> MobDrop entry. Iron-only for 5;
 *   stacked iron + netherite for Wither Skeleton (6%/1%) and Piglin Brute
 *   (10%/2%); netherite-only random selection for Warden (5%).
 * - [VILLAGER_TRADES]: 2 professions x 4-5 levels = ~25 trade entries.
 *
 * The Wither boss trophy (100% random netherite drop) is handled by
 * [com.dndweapons.loot.WitherTrophyHandler] directly via the Fabric death
 * event, not via loot tables, because the Wither has no straightforward
 * weapon-bearing vanilla mob loot table.
 *
 * Wandering trader (Phase 4) is unmodified by Phase 5.
 */
object AcquisitionCatalog {

    // ============================================================
    // Structure loot
    // ============================================================
    val STRUCTURE_LOOT: Map<String, StructureLoot> = mapOf(
        // Iron-tier structures (9 entries)
        "minecraft:chests/stronghold_library" to StructureLoot(
            weapons = listOf("quarterstaff", "whip", "sickle", "rapier"),
            chancePct = 10, tier = Tier.IRON,
        ),
        "minecraft:chests/stronghold_corridor" to StructureLoot(
            weapons = listOf("longsword", "mace", "battleaxe", "warhammer"),
            chancePct = 12, tier = Tier.IRON,
        ),
        "minecraft:chests/woodland_mansion" to StructureLoot(
            weapons = listOf("greatsword", "halberd", "rapier", "longsword"),
            chancePct = 8, tier = Tier.IRON,
        ),
        "minecraft:chests/nether_bridge" to StructureLoot(
            weapons = listOf("pike", "maul", "greataxe", "flail"),
            chancePct = 10, tier = Tier.IRON,
        ),
        "minecraft:chests/desert_pyramid" to StructureLoot(
            weapons = listOf("scimitar", "spear", "dart"),
            chancePct = 12, tier = Tier.IRON,
        ),
        "minecraft:chests/jungle_temple" to StructureLoot(
            weapons = listOf("blowgun", "sling", "dart"),
            chancePct = 10, tier = Tier.IRON,
        ),
        "minecraft:chests/shipwreck_treasure" to StructureLoot(
            weapons = listOf("pistol", "musket", "hand_crossbow", "sickle"),
            chancePct = 8, tier = Tier.IRON,
        ),
        "minecraft:chests/underwater_ruin_big" to StructureLoot(
            weapons = listOf("spear"),
            chancePct = 6, tier = Tier.IRON,
        ),
        "minecraft:chests/pillager_outpost" to StructureLoot(
            weapons = listOf("heavy_crossbow", "hand_crossbow"),
            chancePct = 5, tier = Tier.IRON,
        ),

        // Diamond-tier endgame structures (4 entries)
        "minecraft:chests/end_city_treasure" to StructureLoot(
            weapons = listOf("greatsword", "longsword"),
            chancePct = 5, tier = Tier.DIAMOND,
        ),
        "minecraft:chests/ancient_city" to StructureLoot(
            weapons = listOf("morningstar", "war_pick", "whip"),
            chancePct = 6, tier = Tier.DIAMOND,
        ),
        "minecraft:chests/trial_chambers/reward_ominous_unique" to StructureLoot(
            weapons = listOf("rapier", "longsword", "pike"),
            chancePct = 8, tier = Tier.DIAMOND,
            minVersion = "1.21.1",
        ),
        "minecraft:chests/bastion_treasure" to StructureLoot(
            weapons = listOf("glaive", "heavy_crossbow"),
            chancePct = 3, tier = Tier.DIAMOND,
        ),
    )

    // ============================================================
    // Mob drops (iron tier; netherite is a separate stacked roll)
    // ============================================================
    val MOB_DROPS: Map<String, MobDrop> = mapOf(
        "minecraft:entities/vindicator"      to MobDrop("battleaxe",      ironPct = 8),
        "minecraft:entities/pillager"        to MobDrop("heavy_crossbow", ironPct = 4),
        "minecraft:entities/wither_skeleton" to MobDrop("glaive",         ironPct = 6, netheritePct = 1),
        "minecraft:entities/husk"            to MobDrop("club",           ironPct = 5),
        "minecraft:entities/piglin_brute"    to MobDrop("maul",           ironPct = 10, netheritePct = 2),
        "minecraft:entities/warden"          to MobDrop(null,             ironPct = 0,  netheritePct = 5),
        "minecraft:entities/skeleton"        to MobDrop("dagger",         ironPct = 2),
    )

    // ============================================================
    // Villager trades (Weaponsmith + Fletcher)
    // ============================================================
    val VILLAGER_TRADES: Map<String, Map<Int, List<VillagerTradeEntry>>> = mapOf(
        "minecraft:weaponsmith" to mapOf(
            1 to listOf(
                VillagerTradeEntry("mace",         emeralds = 3),
                VillagerTradeEntry("sickle",       emeralds = 4),
                VillagerTradeEntry("quarterstaff", emeralds = 5),
            ),
            2 to listOf(
                VillagerTradeEntry("spear",        emeralds = 4),
                VillagerTradeEntry("club",         emeralds = 4),
                VillagerTradeEntry("greatclub",    emeralds = 5),
                VillagerTradeEntry("light_hammer", emeralds = 7),
            ),
            3 to listOf(
                VillagerTradeEntry("longsword",   emeralds = 8),
                VillagerTradeEntry("battleaxe",   emeralds = 10),
                VillagerTradeEntry("warhammer",   emeralds = 10),
                VillagerTradeEntry("morningstar", emeralds = 12),
            ),
            4 to listOf(
                VillagerTradeEntry("glaive",  emeralds = 15),
                VillagerTradeEntry("halberd", emeralds = 17),
                VillagerTradeEntry("pike",    emeralds = 18),
                VillagerTradeEntry("maul",    emeralds = 20),
            ),
            5 to listOf(
                VillagerTradeEntry("greatsword", emeralds = 22),
                VillagerTradeEntry("greataxe",   emeralds = 24),
                VillagerTradeEntry("lance",      emeralds = 26),
                VillagerTradeEntry("rapier",     emeralds = 28),
            ),
        ),
        "minecraft:fletcher" to mapOf(
            2 to listOf(VillagerTradeEntry("dart", emeralds = 5, outputCount = 8)),
            3 to listOf(VillagerTradeEntry("hand_crossbow", emeralds = 10)),
            4 to listOf(
                VillagerTradeEntry("heavy_crossbow", emeralds = 15),
                VillagerTradeEntry("longbow",        emeralds = 22),
            ),
            5 to listOf(
                VillagerTradeEntry("blowgun", emeralds = 30),
                VillagerTradeEntry("musket",  emeralds = 35),
                VillagerTradeEntry("pistol",  emeralds = 35),
            ),
        ),
    )

    /**
     * Init-time integrity check. Logs (does not throw) on any unknown weapon id.
     * Intended for catching typos in this file; the gametest is the runtime check.
     */
    fun validate() {
        val errors = mutableListOf<String>()
        for ((tableId, loot) in STRUCTURE_LOOT) {
            // P1-006: empty weapons lists would crash buildStructurePool via /0;
            // catch them up front in validate().
            if (loot.weapons.isEmpty()) {
                errors += "structure $tableId: empty weapons list"
                continue
            }
            for (w in loot.weapons) {
                if (WeaponLookup.byId(w, loot.tier) == null) {
                    errors += "structure $tableId: $w (${loot.tier})"
                }
            }
        }
        for ((mobId, drop) in MOB_DROPS) {
            drop.ironWeapon?.let { w ->
                if (WeaponLookup.byId(w, Tier.IRON) == null) errors += "mob $mobId iron: $w"
                // P2-008: also verify the netherite-tier item exists when this mob
                // has a netherite drop (Wither Skeleton, Piglin Brute). Without this
                // check a missing netherite registration only surfaces when the mob
                // actually rolls a netherite drop at runtime.
                if (drop.netheritePct > 0 && WeaponLookup.byId(w, Tier.NETHERITE) == null) {
                    errors += "mob $mobId netherite: ${w}_netherite"
                }
            }
        }
        for ((profession, levels) in VILLAGER_TRADES) {
            for ((level, trades) in levels) {
                for (t in trades) {
                    if (WeaponLookup.byId(t.weapon, Tier.IRON) == null) {
                        errors += "$profession level $level: ${t.weapon}"
                    }
                }
            }
        }
        if (errors.isNotEmpty()) {
            DndWeaponsMod.LOGGER.error("AcquisitionCatalog references unknown weapons: $errors")
        } else {
            DndWeaponsMod.LOGGER.info(
                "AcquisitionCatalog validated: ${STRUCTURE_LOOT.size} structures, " +
                "${MOB_DROPS.size} mobs, " +
                "${VILLAGER_TRADES.values.sumOf { it.values.sumOf { l -> l.size } }} trades."
            )
        }
    }
}
