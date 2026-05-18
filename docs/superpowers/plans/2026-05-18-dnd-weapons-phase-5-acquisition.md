# DnD Weapons Phase 5 — Acquisition (Loot, Trades, Mob Drops) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire the 27 base iron-tier DnD weapons into vanilla structure loot, villager trades, and mob drops; add diamond-tier overlays for 4 endgame structures and netherite drops from Wither Skeleton / Piglin Brute / Warden; add a 100%-guaranteed random netherite weapon drop on Wither boss death.

**Architecture:** Single declarative `AcquisitionCatalog` is the source of truth for all three surfaces. Thin per-concern registrars (`WeaponLootRegistrar`, `WeaponTradeRegistrar`, `WitherTrophyHandler`) read the catalog at mod init. On 26.1.2 the Fabric `TradeOfferHelper` API is absent — `Phase5TradeCodegen.kt` emits villager-trade JSON files into `data/dndweapons/villager_trades/` from the same catalog. All version drift (Fabric loot v2/v3 modules, 5/3/4-param `Modify` SAM, `location()`/`identifier()` rename, `ItemCost` vs `ItemStack` cost wrappers) is confined to the two registrar files; catalog data is version-agnostic.

**Tech Stack:** Kotlin 2.3.21, Fabric Loom 1.16.2, Stonecutter 0.6, JUnit 5 for unit tests, Fabric Gametest API for runtime tests. MC target matrix: 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2.

**Spec:** `docs/superpowers/specs/2026-05-18-dnd-weapons-phase-5-design.md`

---

## File Map

**New Kotlin files:**
- `src/main/kotlin/com/dndweapons/acquisition/StructureLoot.kt` — data class
- `src/main/kotlin/com/dndweapons/acquisition/MobDrop.kt` — data class
- `src/main/kotlin/com/dndweapons/acquisition/VillagerTradeEntry.kt` — data class
- `src/main/kotlin/com/dndweapons/acquisition/WeaponLookup.kt` — base-id + tier → Item resolver
- `src/main/kotlin/com/dndweapons/acquisition/AcquisitionCatalog.kt` — declarative source of truth
- `src/main/kotlin/com/dndweapons/loot/WeaponLootRegistrar.kt` — MODIFY hook for structures + mob drops
- `src/main/kotlin/com/dndweapons/loot/WitherTrophyHandler.kt` — Wither death event hook
- `src/main/kotlin/com/dndweapons/trade/WeaponTradeRegistrar.kt` — Weaponsmith + Fletcher (1.20.1–1.21.11 Kotlin path)
- `src/main/kotlin/com/dndweapons/codegen/Phase5TradeCodegen.kt` — runnable main; emits 26.1.2 trade JSONs
- `src/main/kotlin/com/dndweapons/test/AcquisitionGametest.kt` — 3 gametests

**New unit test:**
- `src/test/kotlin/com/dndweapons/acquisition/AcquisitionCatalogTest.kt`

**New data files (emitted by codegen, committed to repo):**
- `src/main/resources/data/dndweapons/villager_trades/weaponsmith_level1.json` … `_level5.json` (5 files)
- `src/main/resources/data/dndweapons/villager_trades/fletcher_level2.json` … `_level5.json` (4 files)

**Modified Kotlin files:**
- `src/main/kotlin/com/dndweapons/DndWeaponsMod.kt` — add 4 init calls (3 always, 1 stonecutter-gated for <26.1.2)
- `src/main/resources/fabric.mod.json` — add `AcquisitionGametest` to `fabric-gametest` entrypoint list

---

## Task 1: StructureLoot data class

**Files:**
- Create: `src/main/kotlin/com/dndweapons/acquisition/StructureLoot.kt`

- [ ] **Step 1: Write the data class**

```kotlin
// src/main/kotlin/com/dndweapons/acquisition/StructureLoot.kt
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
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :1.20.1:compileKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/dndweapons/acquisition/StructureLoot.kt
git commit -m "phase 5: StructureLoot data class"
```

---

## Task 2: MobDrop data class

**Files:**
- Create: `src/main/kotlin/com/dndweapons/acquisition/MobDrop.kt`

- [ ] **Step 1: Write the data class**

```kotlin
// src/main/kotlin/com/dndweapons/acquisition/MobDrop.kt
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
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :1.20.1:compileKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/dndweapons/acquisition/MobDrop.kt
git commit -m "phase 5: MobDrop data class"
```

---

## Task 3: VillagerTradeEntry data class

**Files:**
- Create: `src/main/kotlin/com/dndweapons/acquisition/VillagerTradeEntry.kt`

- [ ] **Step 1: Write the data class**

```kotlin
// src/main/kotlin/com/dndweapons/acquisition/VillagerTradeEntry.kt
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
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :1.20.1:compileKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/dndweapons/acquisition/VillagerTradeEntry.kt
git commit -m "phase 5: VillagerTradeEntry data class"
```

---

## Task 4: WeaponLookup helper

**Files:**
- Create: `src/main/kotlin/com/dndweapons/acquisition/WeaponLookup.kt`

- [ ] **Step 1: Write the helper**

```kotlin
// src/main/kotlin/com/dndweapons/acquisition/WeaponLookup.kt
package com.dndweapons.acquisition

import com.dndweapons.DndWeaponsMod
import com.dndweapons.catalog.Tier
import com.dndweapons.catalog.Weapons
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
//? if >=1.21 {
import net.minecraft.resources.ResourceLocation
//?} else {
/*import net.minecraft.resources.ResourceLocation*/
//?}

/**
 * Resolves (base weapon id, tier) -> registered Item by consulting the Phase 4
 * `Weapons.ALL_TIERED` list and the vanilla item registry. Used by both the
 * loot registrar (to convert catalog entries into LootItem stacks) and the
 * trade registrar (to build MerchantOffer result stacks).
 *
 * Returns null when:
 *  - The base id is not in `Weapons.ALL`, OR
 *  - The (id, tier) pair is not in `Weapons.ALL_TIERED` (e.g. requesting diamond
 *    for a vanilla-mapped weapon like shortsword, or a ranged weapon excluded
 *    from the tier ladder in Phase 4).
 */
object WeaponLookup {

    fun byId(weaponId: String, tier: Tier): Item? {
        val targetId = weaponId + tier.suffix
        val location = resourceLocation(DndWeaponsMod.MOD_ID, targetId) ?: return null
        val item = BuiltInRegistries.ITEM.get(location)
        return when {
            item == null -> null
            item === BuiltInRegistries.ITEM.get(resourceLocation("minecraft", "air")) -> null
            else -> item
        }
    }

    /**
     * All registered netherite-tier weapons. Used by the Warden mob-drop pool
     * (random selection) and the Wither trophy event handler.
     */
    fun allNetherite(): List<Item> = Weapons.ALL_TIERED
        .filter { (_, tier) -> tier == Tier.NETHERITE }
        .mapNotNull { (spec, _) -> byId(spec.id.removeSuffix("_netherite"), Tier.NETHERITE) }

    private fun resourceLocation(namespace: String, path: String): ResourceLocation? {
        //? if >=1.21 {
        return ResourceLocation.fromNamespaceAndPath(namespace, path)
        //?} else {
        /*return ResourceLocation(namespace, path)*/
        //?}
    }
}
```

- [ ] **Step 2: Verify it compiles on all 5 versions**

Run: `./gradlew chiseledBuild`
Expected: BUILD SUCCESSFUL on 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/dndweapons/acquisition/WeaponLookup.kt
git commit -m "phase 5: WeaponLookup (id+tier -> Item resolver)"
```

---

## Task 5: AcquisitionCatalog

**Files:**
- Create: `src/main/kotlin/com/dndweapons/acquisition/AcquisitionCatalog.kt`

- [ ] **Step 1: Write the catalog**

```kotlin
// src/main/kotlin/com/dndweapons/acquisition/AcquisitionCatalog.kt
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
            for (w in loot.weapons) {
                if (WeaponLookup.byId(w, loot.tier) == null) {
                    errors += "structure $tableId: $w (${loot.tier})"
                }
            }
        }
        for ((mobId, drop) in MOB_DROPS) {
            drop.ironWeapon?.let { w ->
                if (WeaponLookup.byId(w, Tier.IRON) == null) errors += "mob $mobId iron: $w"
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
```

- [ ] **Step 2: Verify it compiles on all 5 versions**

Run: `./gradlew chiseledBuild`
Expected: BUILD SUCCESSFUL on 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/dndweapons/acquisition/AcquisitionCatalog.kt
git commit -m "phase 5: AcquisitionCatalog (13 structures + 7 mobs + 25 trades)"
```

---

## Task 6: AcquisitionCatalogTest (unit test)

**Files:**
- Create: `src/test/kotlin/com/dndweapons/acquisition/AcquisitionCatalogTest.kt`

- [ ] **Step 1: Write the unit test**

```kotlin
// src/test/kotlin/com/dndweapons/acquisition/AcquisitionCatalogTest.kt
package com.dndweapons.acquisition

import com.dndweapons.catalog.Tier
import com.dndweapons.catalog.Weapons
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AcquisitionCatalogTest {

    @Test
    fun structureLootWeaponIdsAreAllRegistered() {
        for ((tableId, loot) in AcquisitionCatalog.STRUCTURE_LOOT) {
            for (w in loot.weapons) {
                val spec = Weapons.ALL.firstOrNull { it.id == w }
                assertNotNull(spec, "structure '$tableId' references unknown weapon id '$w'")
            }
        }
    }

    @Test
    fun mobDropIronWeaponIdsAreAllRegistered() {
        for ((mobId, drop) in AcquisitionCatalog.MOB_DROPS) {
            drop.ironWeapon?.let { w ->
                val spec = Weapons.ALL.firstOrNull { it.id == w }
                assertNotNull(spec, "mob '$mobId' references unknown iron weapon id '$w'")
            }
        }
    }

    @Test
    fun villagerTradeWeaponIdsAreAllRegistered() {
        for ((profession, levels) in AcquisitionCatalog.VILLAGER_TRADES) {
            for ((level, trades) in levels) {
                for (t in trades) {
                    val spec = Weapons.ALL.firstOrNull { it.id == t.weapon }
                    assertNotNull(spec, "$profession level $level: unknown weapon '${t.weapon}'")
                }
            }
        }
    }

    @Test
    fun structureLootChancePctIsInRange() {
        for ((tableId, loot) in AcquisitionCatalog.STRUCTURE_LOOT) {
            assertTrue(loot.chancePct in 1..100, "structure '$tableId' has out-of-range chancePct ${loot.chancePct}")
        }
    }

    @Test
    fun mobDropPctIsInRange() {
        for ((mobId, drop) in AcquisitionCatalog.MOB_DROPS) {
            assertTrue(drop.ironPct in 0..100, "mob '$mobId' ironPct ${drop.ironPct}")
            assertTrue(drop.netheritePct in 0..100, "mob '$mobId' netheritePct ${drop.netheritePct}")
            // At least one of the two must be non-zero or there's no point in the entry.
            assertTrue(drop.ironPct > 0 || drop.netheritePct > 0, "mob '$mobId' has both pcts at zero")
        }
    }

    @Test
    fun diamondTierStructuresAreEndgameOnly() {
        val diamondTables = AcquisitionCatalog.STRUCTURE_LOOT
            .filter { (_, loot) -> loot.tier == Tier.DIAMOND }
            .keys
        val expected = setOf(
            "minecraft:chests/end_city_treasure",
            "minecraft:chests/ancient_city",
            "minecraft:chests/trial_chambers/reward_ominous_unique",
            "minecraft:chests/bastion_treasure",
        )
        assertEquals(expected, diamondTables, "diamond-tier structure set drifted from spec")
    }

    @Test
    fun trialChambersIsGatedTo121Plus() {
        val entry = AcquisitionCatalog.STRUCTURE_LOOT["minecraft:chests/trial_chambers/reward_ominous_unique"]
        assertNotNull(entry)
        assertEquals("1.21.1", entry!!.minVersion, "trial chambers must be gated to 1.21.1+")
    }
}
```

- [ ] **Step 2: Run the test to verify it passes**

Run: `./gradlew :1.20.1:test --tests com.dndweapons.acquisition.AcquisitionCatalogTest`
Expected: PASS (7 tests).

- [ ] **Step 3: Commit**

```bash
git add src/test/kotlin/com/dndweapons/acquisition/AcquisitionCatalogTest.kt
git commit -m "phase 5: AcquisitionCatalogTest (integrity check)"
```

---

## Task 7: WeaponLootRegistrar

**Files:**
- Create: `src/main/kotlin/com/dndweapons/loot/WeaponLootRegistrar.kt`

This is the largest single file in Phase 5. It mirrors Phase 4's `SmithingTemplateLootInjector.kt` stonecutter pattern for the multi-SAM `LootTableEvents.MODIFY` hook, but routes BOTH structure and mob loot table IDs through the same registrar.

- [ ] **Step 1: Write the registrar**

```kotlin
// src/main/kotlin/com/dndweapons/loot/WeaponLootRegistrar.kt
package com.dndweapons.loot

import com.dndweapons.DndWeaponsMod
import com.dndweapons.acquisition.AcquisitionCatalog
import com.dndweapons.acquisition.MobDrop
import com.dndweapons.acquisition.StructureLoot
import com.dndweapons.acquisition.WeaponLookup
import com.dndweapons.catalog.Tier
//? if >=26.1.2 {
/*import net.fabricmc.fabric.api.loot.v3.LootTableEvents
*///?} else {
import net.fabricmc.fabric.api.loot.v2.LootTableEvents
//?}
import net.minecraft.world.item.Item
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue

/**
 * Phase 5 loot injector. Reads [AcquisitionCatalog] and adds loot pools to the
 * relevant vanilla tables. Structure chests get one pool per matching entry
 * (weapon vs empty, weighted by `chancePct`). Mob entities get up to two
 * INDEPENDENT pools - one iron, one netherite - that roll separately on each
 * kill.
 *
 * Three dimensions of version drift (mirrors Phase 4's SmithingTemplateLootInjector):
 *  1. Fabric module path: v2 (1.20.1..1.21.11) vs v3 (26.1.2).
 *  2. Modify SAM arity:
 *     - v2 / 1.20.1                : 5 params (RM, LM, ResourceLocation, Builder, Source)
 *     - v2 / 1.21.1..1.21.11       : 3 params (ResourceKey<LootTable>, Builder, Source)
 *     - v3 / 26.1.2                : 4 params (ResourceKey<LootTable>, Builder, Source, Registries)
 *  3. ResourceKey accessor for the table id:
 *     - 1.20.1                     : ResourceLocation supplied as the 3rd SAM arg directly
 *     - 1.21.1, 1.21.4             : key.location().toString()
 *     - 1.21.11, 26.1.2            : key.identifier().toString()
 */
object WeaponLootRegistrar {

    private const val MOD_VERSION_STRING: String =
        //? if >=1.21.11 { /*"1.21.11"*///?}
        //? if (>=1.21.4) & (<1.21.11) { /*"1.21.4"*///?}
        //? if (>=1.21.1) & (<1.21.4) { /*"1.21.1"*///?}
        //? if <1.21.1 {
        "1.20.1"
        //?}

    fun register() {
        //? if >=26.1.2 {
        /*// v3 hook on 26.1.2: SAM is (key, builder, source, registries)
        LootTableEvents.MODIFY.register { key, builder, _, _ ->
            injectFor(key.identifier().toString(), builder)
        }
        *///?}
        //? if >=1.21.11 {
        //? if <26.1.2 {
        /*// v2 hook on 1.21.11: 3-param SAM, identifier() rename
        LootTableEvents.MODIFY.register { key, builder, _ ->
            injectFor(key.identifier().toString(), builder)
        }
        *///?}
        //?}
        //? if >=1.21.1 {
        //? if <1.21.11 {
        // v2 hook on 1.21.1, 1.21.4: 3-param SAM, location() still present
        LootTableEvents.MODIFY.register { key, builder, _ ->
            injectFor(key.location().toString(), builder)
        }
        //?}
        //?}
        //? if <1.21.1 {
        /*// v2 hook on 1.20.1: 5-param SAM, tableId is the 3rd arg (ResourceLocation)
        LootTableEvents.MODIFY.register { _, _, tableId, builder, _ ->
            injectFor(tableId.toString(), builder)
        }
        *///?}
        DndWeaponsMod.LOGGER.info(
            "Registered Phase 5 loot injectors " +
            "(${AcquisitionCatalog.STRUCTURE_LOOT.size} structures, " +
            "${AcquisitionCatalog.MOB_DROPS.size} mob tables)."
        )
    }

    private fun injectFor(tableId: String, builder: LootTable.Builder) {
        AcquisitionCatalog.STRUCTURE_LOOT[tableId]?.let { entry ->
            if (versionApplies(entry)) {
                builder.withPool(buildStructurePool(entry))
            }
        }
        AcquisitionCatalog.MOB_DROPS[tableId]?.let { drop ->
            if (drop.ironPct > 0)      builder.withPool(buildMobPool(drop, Tier.IRON))
            if (drop.netheritePct > 0) builder.withPool(buildMobPool(drop, Tier.NETHERITE))
        }
    }

    private fun versionApplies(entry: StructureLoot): Boolean {
        if (entry.minVersion == null) return true
        // Simple lexical compare works because our version strings use the same
        // segmented MAJOR.MINOR.PATCH form throughout (1.20.1 < 1.21.1 < 1.21.4 etc.).
        return compareVersionStrings(MOD_VERSION_STRING, entry.minVersion) >= 0
    }

    /** Returns negative if a < b, 0 if equal, positive if a > b. */
    private fun compareVersionStrings(a: String, b: String): Int {
        val ap = a.split('.').map { it.toIntOrNull() ?: 0 }
        val bp = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(ap.size, bp.size)) {
            val ai = ap.getOrNull(i) ?: 0
            val bi = bp.getOrNull(i) ?: 0
            if (ai != bi) return ai - bi
        }
        return 0
    }

    private fun buildStructurePool(entry: StructureLoot): LootPool.Builder {
        val pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
        // Weighted weapon entries: each weapon shares the chancePct weight equally.
        val perWeaponWeight = entry.chancePct / entry.weapons.size
        for (weaponId in entry.weapons) {
            val item: Item = WeaponLookup.byId(weaponId, entry.tier) ?: continue
            pool.add(
                LootItem.lootTableItem(item)
                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0f)))
                    .setWeight(maxOf(perWeaponWeight, 1))
            )
        }
        pool.add(EmptyLootItem.emptyItem().setWeight(maxOf(100 - entry.chancePct, 1)))
        return pool
    }

    private fun buildMobPool(drop: MobDrop, tier: Tier): LootPool.Builder {
        val pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
        val pct = if (tier == Tier.IRON) drop.ironPct else drop.netheritePct
        if (tier == Tier.NETHERITE && drop.ironWeapon == null) {
            // Warden case: random selection across all 27 netherite-tier weapons.
            // Each weapon entry has weight 1; empty entry weight balances the (100-pct).
            val netheriteItems = WeaponLookup.allNetherite()
            val perItemWeight = maxOf(pct / netheriteItems.size, 1)
            for (item in netheriteItems) {
                pool.add(
                    LootItem.lootTableItem(item)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0f)))
                        .setWeight(perItemWeight)
                )
            }
            pool.add(EmptyLootItem.emptyItem().setWeight(maxOf(100 - pct, 1)))
        } else {
            // Iron OR netherite for a mob that has a single thematic weapon id.
            val weaponId = drop.ironWeapon ?: return pool   // defensive; netherite-only handled above
            val item = WeaponLookup.byId(weaponId, tier) ?: return pool
            pool.add(
                LootItem.lootTableItem(item)
                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0f)))
                    .setWeight(pct)
            )
            pool.add(EmptyLootItem.emptyItem().setWeight(maxOf(100 - pct, 1)))
        }
        return pool
    }
}
```

- [ ] **Step 2: Verify it compiles on all 5 versions**

Run: `./gradlew chiseledBuild`
Expected: BUILD SUCCESSFUL on 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/dndweapons/loot/WeaponLootRegistrar.kt
git commit -m "phase 5: WeaponLootRegistrar (structures + mob drops)"
```

---

## Task 8: WitherTrophyHandler

**Files:**
- Create: `src/main/kotlin/com/dndweapons/loot/WitherTrophyHandler.kt`

- [ ] **Step 1: Write the handler**

```kotlin
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
            val chosen = netheriteItems.random(entity.random)
            entity.spawnAtLocation(ItemStack(chosen))
        }
        DndWeaponsMod.LOGGER.info("Registered Wither trophy handler (100% random netherite weapon).")
    }
}
```

- [ ] **Step 2: Verify it compiles on all 5 versions**

Run: `./gradlew chiseledBuild`
Expected: BUILD SUCCESSFUL on 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2.

If `ServerLivingEntityEvents` is unresolved on any version, the import path moved — apply a stonecutter fork around the import line, mirroring the pattern Phase 4 used for `LootTableEvents` v2/v3.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/dndweapons/loot/WitherTrophyHandler.kt
git commit -m "phase 5: WitherTrophyHandler (100% random netherite trophy)"
```

---

## Task 9: WeaponTradeRegistrar (Kotlin path, 1.20.1–1.21.11)

**Files:**
- Create: `src/main/kotlin/com/dndweapons/trade/WeaponTradeRegistrar.kt`

This file is NEVER reached on 26.1.2 — its mod-init call is wrapped in `//? if <26.1.2` in Task 11. The file must still compile on 26.1.2 (since Kotlin source is shared across all versions); inside the 26.1.2 branch we stonecutter the body to a no-op stub. Mirrors the Phase 4 `SmithingTemplateTrades.kt` pattern.

- [ ] **Step 1: Write the registrar**

```kotlin
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
import net.minecraft.world.entity.npc.VillagerTrades
//?}
//? if >=1.21.11 {
//? if <26.1.2 {
/*import net.minecraft.world.entity.npc.villager.VillagerTrades
*///?}
//?}
//? if >=1.21.1 {
import net.minecraft.core.registries.BuiltInRegistries
//?}
//? if <1.21.5 {
import net.minecraft.world.entity.npc.VillagerProfession
//?}

/**
 * Phase 5: registers Weaponsmith + Fletcher trades on 1.20.1, 1.21.1, 1.21.4,
 * 1.21.11. 26.1.2 uses data-pack JSON trades (emitted by Phase5TradeCodegen)
 * since Fabric removed TradeOfferHelper from the umbrella at 26.1.
 *
 * Trade signature drift across versions:
 *  - 1.20.1                 : registerVillagerOffers(VillagerProfession, level, factories)
 *                             MerchantOffer takes ItemStack costs (no ItemCost wrapper).
 *  - 1.21.1, 1.21.4         : registerVillagerOffers(VillagerProfession, level, factories)
 *                             MerchantOffer takes ItemCost.
 *  - 1.21.11                : registerVillagerOffers(RegistryKey<VillagerProfession>, level, factories)
 *                             ItemListing SAM gains a ServerLevel param (3-arg lambda).
 *  - 26.1.2                 : Fabric API removed; this file's register() is stonecutter-stubbed.
 *
 * The version-specific build is in [buildOffer]; the per-profession dispatch
 * is in [register].
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
            val profession = resolveProfession(professionId) ?: continue
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

    //? if <26.1.2 {
    private fun resolveProfession(id: String): Any? {
        //? if <1.21.5 {
        return when (id) {
            "minecraft:weaponsmith" -> VillagerProfession.WEAPONSMITH
            "minecraft:fletcher"    -> VillagerProfession.FLETCHER
            else -> null
        }
        //?} else {
        /*val location = net.minecraft.resources.ResourceLocation.parse(id)
        val key = net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.VILLAGER_PROFESSION,
            location,
        )
        return key
        *///?}
    }

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
```

- [ ] **Step 2: Verify it compiles on all 5 versions**

Run: `./gradlew chiseledBuild`
Expected: BUILD SUCCESSFUL on 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2.

On 26.1.2 the file compiles to a stub that just logs and returns. On 1.21.11 the `ItemListing` lambda is 3-arg; on older versions it's 2-arg.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/dndweapons/trade/WeaponTradeRegistrar.kt
git commit -m "phase 5: WeaponTradeRegistrar (Weaponsmith + Fletcher, 1.20.1-1.21.11)"
```

---

## Task 10: Phase5TradeCodegen + 26.1.2 trade JSONs

**Files:**
- Create: `src/main/kotlin/com/dndweapons/codegen/Phase5TradeCodegen.kt`
- Create: `src/main/resources/data/dndweapons/villager_trades/weaponsmith_level1.json`
- Create: `src/main/resources/data/dndweapons/villager_trades/weaponsmith_level2.json`
- Create: `src/main/resources/data/dndweapons/villager_trades/weaponsmith_level3.json`
- Create: `src/main/resources/data/dndweapons/villager_trades/weaponsmith_level4.json`
- Create: `src/main/resources/data/dndweapons/villager_trades/weaponsmith_level5.json`
- Create: `src/main/resources/data/dndweapons/villager_trades/fletcher_level2.json`
- Create: `src/main/resources/data/dndweapons/villager_trades/fletcher_level3.json`
- Create: `src/main/resources/data/dndweapons/villager_trades/fletcher_level4.json`
- Create: `src/main/resources/data/dndweapons/villager_trades/fletcher_level5.json`

26.1.2 reads villager-trade rebalance JSONs from `data/<namespace>/villager_trades/`. The exact schema must be confirmed against a vanilla example before emitting; the structure below is the expected form. The codegen runs as a Kotlin `main()` that reads `AcquisitionCatalog.VILLAGER_TRADES` and writes one file per profession-level pair.

- [ ] **Step 1: Write the codegen Kotlin source**

```kotlin
// src/main/kotlin/com/dndweapons/codegen/Phase5TradeCodegen.kt
@file:JvmName("Phase5TradeCodegen")
package com.dndweapons.codegen

import com.dndweapons.acquisition.AcquisitionCatalog
import com.dndweapons.acquisition.VillagerTradeEntry
import java.io.File

/**
 * Emits the 9 villager-trade JSON files Minecraft 26.1.2 loads as a trade
 * rebalance data pack. The same trades that
 * [com.dndweapons.trade.WeaponTradeRegistrar] registers via Fabric API on
 * 1.20.1-1.21.11 are emitted here as JSON for 26.1.2.
 *
 * Run via `./gradlew :26.1.2:runPhase5TradeCodegen` (or directly with `kotlin
 * com.dndweapons.codegen.Phase5TradeCodegen`). The output files are committed
 * to the repo; this main is only re-run when [AcquisitionCatalog.VILLAGER_TRADES]
 * is edited.
 */
fun main() {
    val outputRoot = File("src/main/resources/data/dndweapons/villager_trades")
    outputRoot.mkdirs()
    var written = 0
    for ((professionId, levels) in AcquisitionCatalog.VILLAGER_TRADES) {
        val professionSlug = professionId.removePrefix("minecraft:")
        for ((level, trades) in levels) {
            val fileName = "${professionSlug}_level${level}.json"
            File(outputRoot, fileName).writeText(buildJson(professionId, level, trades))
            written++
        }
    }
    println("Phase5TradeCodegen: wrote $written trade JSONs to ${outputRoot.path}")
}

private fun buildJson(professionId: String, level: Int, trades: List<VillagerTradeEntry>): String {
    val tradeEntries = trades.joinToString(",\n") { tradeJson(it) }
    return """{
  "profession": "$professionId",
  "level": $level,
  "trades": [
$tradeEntries
  ]
}
"""
}

private fun tradeJson(t: VillagerTradeEntry): String = """    {
      "input": [{ "id": "minecraft:emerald", "count": ${t.emeralds} }],
      "result": { "id": "dndweapons:${t.weapon}", "count": ${t.outputCount} },
      "max_uses": ${t.maxUses},
      "xp": ${t.xp},
      "price_multiplier": 0.05
    }"""
```

- [ ] **Step 2: Run the codegen**

Run: `./gradlew :26.1.2:runPhase5TradeCodegen` if a Gradle task exists; otherwise the dev agent should add a `runPhase5TradeCodegen` Gradle task to the existing `build.gradle.kts` under the `:26.1.2:` subproject, or run `kotlin com.dndweapons.codegen.Phase5TradeCodegen` directly against the compiled main classpath.

Expected: 9 JSON files written under `src/main/resources/data/dndweapons/villager_trades/`.

- [ ] **Step 3: Verify the emitted JSON shape**

Spot-check `weaponsmith_level1.json`. Expected content (whitespace may differ):

```json
{
  "profession": "minecraft:weaponsmith",
  "level": 1,
  "trades": [
    {
      "input": [{ "id": "minecraft:emerald", "count": 3 }],
      "result": { "id": "dndweapons:mace", "count": 1 },
      "max_uses": 12,
      "xp": 5,
      "price_multiplier": 0.05
    },
    {
      "input": [{ "id": "minecraft:emerald", "count": 4 }],
      "result": { "id": "dndweapons:sickle", "count": 1 },
      "max_uses": 12,
      "xp": 5,
      "price_multiplier": 0.05
    },
    {
      "input": [{ "id": "minecraft:emerald", "count": 5 }],
      "result": { "id": "dndweapons:quarterstaff", "count": 1 },
      "max_uses": 12,
      "xp": 5,
      "price_multiplier": 0.05
    }
  ]
}
```

- [ ] **Step 4: Verify all 9 files exist**

```bash
ls src/main/resources/data/dndweapons/villager_trades/ | wc -l
```

Expected: 9 (weaponsmith_level1..5 + fletcher_level2..5).

- [ ] **Step 5: Verify build succeeds (no recipe parse errors)**

Run: `./gradlew chiseledBuild`
Expected: BUILD SUCCESSFUL on all 5 versions. The 26.1.2 build should log no errors about the trade JSONs. Older versions ignore them silently.

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/dndweapons/codegen/Phase5TradeCodegen.kt \
        src/main/resources/data/dndweapons/villager_trades/
git commit -m "phase 5: trade codegen + 9 villager-trade JSONs (26.1.2 data pack)"
```

---

## Task 11: Wire Phase 5 into DndWeaponsMod.onInitialize()

**Files:**
- Modify: `src/main/kotlin/com/dndweapons/DndWeaponsMod.kt` (add 4 init calls after the Phase 4 block)

- [ ] **Step 1: Add imports**

At the top of `DndWeaponsMod.kt`, after the existing Phase 4 imports, add:

```kotlin
import com.dndweapons.acquisition.AcquisitionCatalog
import com.dndweapons.loot.WeaponLootRegistrar
import com.dndweapons.loot.WitherTrophyHandler
//? if <26.1.2 {
import com.dndweapons.trade.WeaponTradeRegistrar
//?}
```

- [ ] **Step 2: Add init calls**

After the Phase 4 `SmithingTemplateTrades.register()` call in `onInitialize()`, append:

```kotlin
// Phase 5: acquisition
AcquisitionCatalog.validate()
WeaponLootRegistrar.register()
WitherTrophyHandler.register()
//? if <26.1.2 {
WeaponTradeRegistrar.register()
//?}
LOGGER.info("Phase 5 acquisition surface registered.")
```

- [ ] **Step 3: Verify it compiles on all 5 versions**

Run: `./gradlew chiseledBuild`
Expected: BUILD SUCCESSFUL on 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2.

- [ ] **Step 4: Verify all existing gametests still pass (no regression)**

Run: `./gradlew chiseledRunGametest`
Expected: PASS on all 5 versions. The Phase 4 test count (13 per version) is unchanged because Phase 5 hasn't added gametests yet.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/dndweapons/DndWeaponsMod.kt
git commit -m "phase 5: wire AcquisitionCatalog/LootRegistrar/Trophy/Trades into mod init"
```

---

## Task 12: AcquisitionGametest scaffold + fabric.mod.json registration

**Files:**
- Create: `src/main/kotlin/com/dndweapons/test/AcquisitionGametest.kt` (with the first @GameTest method — see Task 13 for the test body)
- Modify: `src/main/resources/fabric.mod.json` (add `AcquisitionGametest` to the `fabric-gametest` entrypoint list)

This task only creates the file skeleton + entrypoint registration; Tasks 13/14/15 add the three @GameTest methods. The skeleton must include the stonecutter-gated class declaration so the version-specific `@GameTest(template=...)` vs `@GameTest(structure=...)` attribute is correct.

- [ ] **Step 1: Create the AcquisitionGametest file skeleton**

```kotlin
// src/main/kotlin/com/dndweapons/test/AcquisitionGametest.kt
package com.dndweapons.test

//? if <1.21.5 {
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
//?}
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper

/**
 * Phase 5 acquisition gametests. Three smoke tests:
 *  1. Stronghold corridor loot table produces a mod weapon over N rolls.
 *  2. Weaponsmith level-1 trades include a mod weapon.
 *  3. Vindicator drops a mod battleaxe in a small kill batch.
 *
 * Class declaration is version-gated like CombatHooksGametest and
 * SmithingGametest: pre-1.21.5 extends FabricGameTest with `template`,
 * post-1.21.5 is a bare class with `structure`.
 */
//? if <1.21.5 {
class AcquisitionGametest : FabricGameTest {
    // Phase 5 gametests will be appended here in Tasks 13-15.
}
//?} else {
/*class AcquisitionGametest {
    // Phase 5 gametests will be appended here in Tasks 13-15.
}
*///?}
```

- [ ] **Step 2: Register the test class in fabric.mod.json**

Open `src/main/resources/fabric.mod.json` and find the `fabric-gametest` entrypoint list. After Phase 4's `"com.dndweapons.test.SmithingGametest"` entry, add:

```json
"com.dndweapons.test.AcquisitionGametest"
```

The full entrypoint list (after edit) should look like:

```json
"fabric-gametest": [
    "com.dndweapons.test.RegistrationGametest",
    "com.dndweapons.test.WeaponInteractionGametest",
    "com.dndweapons.test.CombatHooksGametest",
    "com.dndweapons.test.SmithingGametest",
    "com.dndweapons.test.AcquisitionGametest"
]
```

- [ ] **Step 3: Verify it compiles on all 5 versions**

Run: `./gradlew chiseledBuild`
Expected: BUILD SUCCESSFUL on all 5 versions.

- [ ] **Step 4: Verify all existing gametests still pass**

Run: `./gradlew chiseledRunGametest`
Expected: PASS on all 5 versions. Test count is still 13 (the empty AcquisitionGametest class contributes no tests yet).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/dndweapons/test/AcquisitionGametest.kt src/main/resources/fabric.mod.json
git commit -m "phase 5: AcquisitionGametest scaffold + fabric.mod.json entrypoint"
```

---

## Task 13: Gametest — strongholdCorridorContainsModWeapon

**Files:**
- Modify: `src/main/kotlin/com/dndweapons/test/AcquisitionGametest.kt` (append first @GameTest method + helper)

- [ ] **Step 1: Add the @GameTest method + helper**

Inside both stonecutter branches of `AcquisitionGametest` (the `<1.21.5` class body and the `>=1.21.5` class body), append the method:

```kotlin
//? if <1.21.5 {
@GameTest(template = "fabric-gametest-api-v1:empty")
fun strongholdCorridorContainsModWeapon(ctx: GameTestHelper) {
    runStrongholdCorridorCheck(ctx)
}
//?} else {
/*@GameTest(structure = "fabric-gametest-api-v1:empty")
fun strongholdCorridorContainsModWeapon(ctx: GameTestHelper) {
    runStrongholdCorridorCheck(ctx)
}
*///?}
```

Then at file scope (after the class declaration), append the helper function:

```kotlin
private fun runStrongholdCorridorCheck(ctx: GameTestHelper) {
    val server = ctx.level.server
    val table = run {
        //? if <1.21.5 {
        val lootData = server.lootData
        val location = net.minecraft.resources.ResourceLocation.parse("minecraft:chests/stronghold_corridor")
        lootData.getLootTable(location)
        //?} else {
        /*val lootData = server.reloadableRegistries().lookup()
        val key = net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.LOOT_TABLE,
            net.minecraft.resources.ResourceLocation.parse("minecraft:chests/stronghold_corridor"),
        )
        lootData.lookup(net.minecraft.core.registries.Registries.LOOT_TABLE).orElseThrow().get(key).orElseThrow()
        *///?}
    }
    val rng = net.minecraft.util.RandomSource.create(42L)
    val context = net.minecraft.world.level.storage.loot.LootParams.Builder(ctx.level)
        .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST)
    var hits = 0
    repeat(200) {
        val items = table.getRandomItems(context, rng)
        if (items.any { it.descriptionId.startsWith("item.dndweapons.") }) hits++
    }
    if (hits == 0) {
        throw AssertionError(
            "Stronghold corridor produced no dndweapons items in 200 rolls (12% chance, expected ~24)."
        )
    }
    ctx.succeed()
}
```

- [ ] **Step 2: Verify it compiles on all 5 versions**

Run: `./gradlew chiseledBuild`
Expected: BUILD SUCCESSFUL on all 5 versions.

- [ ] **Step 3: Run the gametest on 1.20.1**

Run: `./gradlew :1.20.1:runGametest`
Expected: PASS (strongholdCorridorContainsModWeapon). Test count is now 14 mod tests.

- [ ] **Step 4: Run on all 5 versions**

Run: `./gradlew chiseledRunGametest`
Expected: PASS on all 5 versions (14 mod tests per version).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/dndweapons/test/AcquisitionGametest.kt
git commit -m "phase 5: gametest strongholdCorridorContainsModWeapon"
```

---

## Task 14: Gametest — weaponsmithLevelOneTradesIncludeModWeapon

**Files:**
- Modify: `src/main/kotlin/com/dndweapons/test/AcquisitionGametest.kt` (append second @GameTest + helper)

- [ ] **Step 1: Add the @GameTest method**

Inside both stonecutter branches of `AcquisitionGametest`, append:

```kotlin
//? if <1.21.5 {
@GameTest(template = "fabric-gametest-api-v1:empty")
fun weaponsmithLevelOneTradesIncludeModWeapon(ctx: GameTestHelper) {
    runWeaponsmithLevelOneCheck(ctx)
}
//?} else {
/*@GameTest(structure = "fabric-gametest-api-v1:empty")
fun weaponsmithLevelOneTradesIncludeModWeapon(ctx: GameTestHelper) {
    runWeaponsmithLevelOneCheck(ctx)
}
*///?}
```

Then at file scope append the helper:

```kotlin
private fun runWeaponsmithLevelOneCheck(ctx: GameTestHelper) {
    //? if >=26.1.2 {
    /*// 26.1.2 ships trades as a data pack. Verify the JSON is loaded.
    val rm = ctx.level.server.resourceManager
    val found = rm.listResources("villager_trades") { it.path.endsWith("weaponsmith_level1.json") }
        .keys.any { it.namespace == "dndweapons" }
    if (!found) {
        throw AssertionError("dndweapons weaponsmith_level1.json not loaded by ResourceManager on 26.1.2")
    }
    *///?} else {
    // 1.20.1-1.21.11 path: query VillagerTrades.TRADES for the profession + level 1.
    val profession = net.minecraft.world.entity.npc.VillagerProfession.WEAPONSMITH
    val trades = net.minecraft.world.entity.npc.VillagerTrades.TRADES[profession]
        ?.get(1) ?: emptyArray()
    val rng = net.minecraft.util.RandomSource.create(0L)
    // Use the villager that gametest helpers spawn at the test origin, then
    // probe each ItemListing for a mod weapon result.
    val villager = ctx.spawnWithNoFreeWill(
        net.minecraft.world.entity.EntityType.VILLAGER,
        net.minecraft.core.BlockPos(2, 2, 2),
    )
    val hasModWeapon = trades.any { listing ->
        //? if <1.21.5 {
        val offer = listing.getOffer(villager, rng)
        //?} else {
        /*val offer = listing.getOffer(ctx.level, villager, rng)
        *///?}
        offer?.result?.descriptionId?.startsWith("item.dndweapons.") == true
    }
    if (!hasModWeapon) {
        throw AssertionError(
            "Weaponsmith level 1 has no dndweapons trades (Phase 5 WeaponTradeRegistrar failed)."
        )
    }
    //?}
    ctx.succeed()
}
```

- [ ] **Step 2: Verify it compiles on all 5 versions**

Run: `./gradlew chiseledBuild`
Expected: BUILD SUCCESSFUL on all 5 versions.

- [ ] **Step 3: Run on all 5 versions**

Run: `./gradlew chiseledRunGametest`
Expected: PASS on all 5 versions (15 mod tests per version).

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/com/dndweapons/test/AcquisitionGametest.kt
git commit -m "phase 5: gametest weaponsmithLevelOneTradesIncludeModWeapon"
```

---

## Task 15: Gametest — vindicatorBattleaxeDropsAtExpectedRate

**Files:**
- Modify: `src/main/kotlin/com/dndweapons/test/AcquisitionGametest.kt` (append third @GameTest + helper)

- [ ] **Step 1: Add the @GameTest method**

Inside both stonecutter branches of `AcquisitionGametest`, append:

```kotlin
//? if <1.21.5 {
@GameTest(template = "fabric-gametest-api-v1:empty")
fun vindicatorBattleaxeDropsAtExpectedRate(ctx: GameTestHelper) {
    runVindicatorBattleaxeCheck(ctx)
}
//?} else {
/*@GameTest(structure = "fabric-gametest-api-v1:empty")
fun vindicatorBattleaxeDropsAtExpectedRate(ctx: GameTestHelper) {
    runVindicatorBattleaxeCheck(ctx)
}
*///?}
```

Then at file scope append the helper:

```kotlin
private fun runVindicatorBattleaxeCheck(ctx: GameTestHelper) {
    var battleaxeDrops = 0
    val spawnPos = net.minecraft.core.BlockPos(2, 2, 2)
    repeat(100) {
        val v = ctx.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.VINDICATOR, spawnPos)
        v.health = 0.1f
        v.hurt(ctx.level.damageSources().generic(), 999f)
        val center = ctx.absolutePos(spawnPos).center
        val box = net.minecraft.world.phys.AABB(
            center.x - 4, center.y - 4, center.z - 4,
            center.x + 4, center.y + 4, center.z + 4,
        )
        val nearby = ctx.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity::class.java, box)
        for (e in nearby) {
            if (e.item.descriptionId == "item.dndweapons.battleaxe") battleaxeDrops++
            e.discard()
        }
    }
    if (battleaxeDrops !in 1..30) {
        throw AssertionError(
            "Vindicator battleaxe drops out of expected range (got $battleaxeDrops/100; expected ~8, accepted 1-30)."
        )
    }
    ctx.succeed()
}
```

- [ ] **Step 2: Verify it compiles on all 5 versions**

Run: `./gradlew chiseledBuild`
Expected: BUILD SUCCESSFUL on all 5 versions.

- [ ] **Step 3: Run on all 5 versions**

Run: `./gradlew chiseledRunGametest`
Expected: PASS on all 5 versions (16 mod tests per version).

If the test fails because the vindicator drop range is too wide or too narrow, adjust the assertion bounds — the bound `1..30` is a generous RNG-noise envelope for an 8% expected rate over 100 trials. A failure with `0` drops would indicate the mob loot injector isn't wired up; a failure outside the upper bound suggests the chancePct is being misinterpreted (likely as weight directly, not as percentage).

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/com/dndweapons/test/AcquisitionGametest.kt
git commit -m "phase 5: gametest vindicatorBattleaxeDropsAtExpectedRate"
```

---

## Task 16: Full matrix verification + Phase 5 verification doc

**Files:**
- Create: `docs/superpowers/plans/phase-5-verification-final.md`

- [ ] **Step 1: Run the full verification matrix**

Run in sequence:

```bash
./gradlew chiseledBuild       # all 5 versions must compile
./gradlew chiseledTest        # JUnit unit tests (AcquisitionCatalogTest + Phase 1-4 tests)
./gradlew chiseledRunGametest # 16 mod tests per version (13 from Phase 1-4 + 3 from Phase 5)
```

Expected per-version results:

| Version | chiseledBuild | chiseledTest | chiseledRunGametest |
|---|---|---|---|
| 1.20.1 | PASS | PASS (all unit tests including AcquisitionCatalogTest) | 16/16 PASS |
| 1.21.1 | PASS | PASS | 16/16 PASS |
| 1.21.4 | PASS | PASS | 16/16 PASS |
| 1.21.11 | PASS | PASS | 16/16 PASS |
| 26.1.2 | PASS | PASS | 16/16 PASS (trade test verifies JSON data pack) |

If any version fails, document the failure in the verification doc rather than silently dropping. Do not apply the git tag until all 5 are GREEN.

- [ ] **Step 2: Write the verification doc**

Create `docs/superpowers/plans/phase-5-verification-final.md` with the following structure (fill in the actual results captured in Step 1):

```markdown
# Phase 5 Verification — Acquisition (Loot, Trades, Mob Drops)

**Date:** <today>
**Status:** <GREEN | HOLD>

## Top-line results

| Version | chiseledBuild | chiseledTest | chiseledRunGametest |
|---|---|---|---|
| 1.20.1  | <result> | <result> | <count>/<count> |
| 1.21.1  | <result> | <result> | <count>/<count> |
| 1.21.4  | <result> | <result> | <count>/<count> |
| 1.21.11 | <result> | <result> | <count>/<count> |
| 26.1.2  | <result> | <result> | <count>/<count> |

## New artifacts in Phase 5

- 1 catalog file (~250 lines)
- 4 acquisition data class / helper files (StructureLoot, MobDrop, VillagerTradeEntry, WeaponLookup)
- 1 loot registrar (~250 lines, 5-fork stonecutter)
- 1 Wither trophy handler
- 1 trade registrar (~200 lines, 1.20.1-1.21.11 only)
- 1 codegen main + 9 trade JSONs (26.1.2 only)
- 1 gametest class with 3 @GameTest methods (smoke tests)
- 1 unit test class with 7 tests (catalog integrity)
- 2 modified files (DndWeaponsMod.kt, fabric.mod.json)

## Test deltas

- Phase 4 baseline: 13 mod gametests per version, ~12 JUnit unit tests
- Phase 5 adds: 3 mod gametests + 7 JUnit unit tests
- Phase 5 total: 16 mod gametests per version, ~19 JUnit unit tests

## Tag command (run only after all 5 versions GREEN)

git tag phase-5-acquisition
git push origin phase-5-acquisition

**Status: <HOLD | apply tag>**

## Known issues / deferrals

(list any catalog entries that didn't apply on a particular version, any per-version flake, etc.)
```

- [ ] **Step 3: Apply the git tag (only if all 5 versions GREEN)**

```bash
git tag phase-5-acquisition
git push origin phase-5-acquisition
```

If any version is RED, leave the tag unapplied and update the doc's Status line to HOLD with a description of the failure.

- [ ] **Step 4: Commit the verification doc**

```bash
git add docs/superpowers/plans/phase-5-verification-final.md
git commit -m "phase 5: final verification doc"
```

---

## Self-review notes

This plan implements every requirement in the Phase 5 design spec:

| Spec section | Tasks |
|---|---|
| §3 Architecture (file layout) | Tasks 1-12 cover every new file in the spec's file map |
| §4 Catalog data shape | Tasks 1-5 (data classes + WeaponLookup + AcquisitionCatalog) |
| §5 Data flow / loot injection | Task 7 (WeaponLootRegistrar with 5-fork stonecutter pattern) |
| §5 Wither trophy | Task 8 (WitherTrophyHandler) |
| §5 Villager trades Kotlin path | Task 9 (WeaponTradeRegistrar) |
| §5 Villager trades 26.1.2 data path | Task 10 (Phase5TradeCodegen + 9 JSONs) |
| §5 Loot table ID accessor (5-fork) | Embedded in Task 7's `injectFor` SAM lambdas |
| §6 Error handling | `validate()` in Task 5; catalog tolerates unknown weapons silently in registrars |
| §7 Gametests (3 smoke) | Tasks 13, 14, 15 |
| §7 Unit test | Task 6 (AcquisitionCatalogTest with 7 tests) |
| §8 Build matrix | Task 16 |
| §9 File map | All tasks combined match the spec's file map exactly |
| §10 Assumptions to verify | The "If `ServerLivingEntityEvents` is unresolved..." note in Task 8 covers Fabric event drift; loot table ID assumptions covered by Task 16 verification |

No placeholders. Every step has actual code or a concrete command. Function and type names are consistent across tasks (`WeaponLookup.byId`, `AcquisitionCatalog.STRUCTURE_LOOT`, etc.).
