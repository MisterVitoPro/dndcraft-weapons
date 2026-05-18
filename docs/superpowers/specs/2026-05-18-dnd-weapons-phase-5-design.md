# DnD Weapons — Phase 5 Design Specification

**Date:** 2026-05-18
**Status:** Approved (brainstorm complete, ready for implementation planning)
**Author:** brainstorm session with the user
**Parent design:** [2026-05-16-dnd-weapons-design.md](2026-05-16-dnd-weapons-design.md)
**Predecessor phase:** [2026-05-17-dnd-weapons-phase-4-design.md](2026-05-17-dnd-weapons-phase-4-design.md) — Phase 4 shipped the smithing ladder (Tier enum, WeaponSpec.atTier(), 54 smithing recipes, stronghold/bastion template loot, wandering-trader template trades). Final verification at [phase-4-verification-final.md](../plans/phase-4-verification-final.md).
**Implementation plan:** TBD (next step — invoke `superpowers:writing-plans`)

---

## 1. Goal & Scope

**Goal:** Place the 27 base (iron-tier) DnD weapons into the natural acquisition surface — structure loot chests, villager trades, and mob drops — and add a small set of rare diamond/netherite-tier drops to reward endgame play and high-difficulty mobs.

The bulk of the work follows the master design's §8 "Acquisition (Loot, Trades, Mob Drops)" tables. The novelties are: a single declarative `AcquisitionCatalog` that's the source of truth for all three surfaces, a per-tier policy (iron everywhere by default, diamond only in endgame structures, netherite only as rare mob drops), and a JSON-codegen path for villager trades on 26.1.2 (which dropped the Fabric `TradeOfferHelper` API).

### In scope

| Item | Count | Source |
|---|---|---|
| Structure loot pools (13 vanilla loot tables) | 13 | catalog + runtime hook |
| Mob drop entries (7 mob types) | 7 | catalog + runtime hook |
| Wither boss trophy (100% random netherite weapon) | 1 | event hook |
| Villager trades — Weaponsmith (5 levels) | ~18 trades | catalog + 2 paths |
| Villager trades — Fletcher (4 levels) | ~7 trades | catalog + 2 paths |
| Villager trade JSONs (26.1.2 data pack) | ~10 files | codegen-emitted |
| Gametests covering loot + trades + mob drops | 3 | new |
| Unit test for catalog integrity | 1 | new |
| **Total new Kotlin LOC (rough)** | **~950** | |

No new items, no new textures, no new models, no new recipes. Phase 5 only wires existing Phase 4 items into the world.

### Explicitly out of scope

- **Wandering trader changes** — Phase 4 already shipped the 2 smithing-template trades. Phase 5 does not modify them.
- **Diamond-tier mob drops** — diamond appears only in endgame structure loot, not on any mob.
- **Netherite-tier structure loot** — netherite appears only as mob drops (rare) and the Wither trophy. Structures cap at diamond.
- **Pillager Captain as a distinct drop source** — it's flagged on the same `Pillager` entity and we'd need a mixin or a damage-source check; out of scope for a smoke-test phase. Captain rolls the same iron heavy_crossbow as a regular pillager.
- **Drowned trident-holder spear substitution** — master design §8 had a 25% "swap trident for spear" rule. Requires a `LivingEntity` post-spawn equipment mixin; deferred to a future phase. Drowned drops are unchanged in Phase 5.
- **Trial Chambers vault enchantments / ominous variants** — we inject into the ominous unique loot table only; non-ominous trial chambers are not modified.
- **Per-mob-equipment** — mobs do NOT spawn pre-wielding DnD weapons. Drops only. Master design §8 explicitly calls this out.
- **Advancement triggers on first DnD weapon acquired** — Phase 6 (wiki phase) if at all.

---

## 2. Decisions

| # | Decision | Choice | Rationale |
|---|---|---|---|
| 1 | Scope | Full master design §8 (13 structures + 2 villager professions + 8 mob types) | User-confirmed in brainstorm. Phase 4 already shipped the smithing-template loot/trades; Phase 5 extends to weapons. |
| 2 | Tier policy | Iron default everywhere; diamond only in 4 endgame structures; netherite only as rare mob drops + Wither trophy | Preserves Phase 4's smithing-progression intent. Endgame structures reward the player without bypassing the crafting path; netherite-only-on-mobs incentivizes crafting netherite via smithing for predictable supply. |
| 3 | Endgame diamond structures | End City, Ancient City, Trial Chambers ominous vaults (1.21+), Bastion treasure | Highest-difficulty structures across the matrix. Bastion already has the Phase 4 netherite template; adding a diamond-tier weapon overlay keeps that chest thematic. |
| 4 | Netherite mob drops | Wither Skeleton (1%), Piglin Brute (2%), Warden (5%, random netherite weapon) | Three thematic high-difficulty Nether/Deep-Dark mobs. Warden's 5% is the highest single-mob rate because Warden encounters are deliberate. |
| 5 | Wither trophy | 100% drop of a random netherite weapon on Wither death | Guaranteed reward for killing the Wither (it doesn't have a meaningful vanilla weapon drop). Random across all 27 netherite variants — wider variety, encourages multiple kills. |
| 6 | Mob iron vs netherite | Stacked independent rolls | Wither Skeleton: 6% iron glaive AND 1% netherite glaive on the same kill (independent rolls). Most generous; both drops can occur. Two pool insertions, not a weighted choice. |
| 7 | Architecture | Single `AcquisitionCatalog` + thin per-concern registrars | Master design §8 recommends a "single declarative table … built from the catalog". Single source of truth simplifies the runtime hook AND the 26.1.2 codegen. |
| 8 | 26.1.2 trade strategy | Codegen emits `data/dndweapons/villager_trades/*.json` from the catalog; runtime hook is skipped on 26.1.2 | Fabric removed `TradeOfferHelper` from the 26.1.2 umbrella. The 26.1.2 vanilla data-pack format supports villager-trade rebalance JSONs directly. Codegen keeps the catalog as the single source of truth. |
| 9 | Wither hook | `ServerLivingEntityEvents.AFTER_DEATH` (Fabric event) | Wither doesn't have a vanilla mob-drop loot table that handles weapon drops in the same way as other mobs. A direct death-event hook is simpler and reliable across all 5 versions. |
| 10 | Test coverage | 3 gametests (structure loot, trade registration, mob drop) + 1 unit test (catalog integrity) | "Smoke tests only" — confirmed in brainstorm. Per-structure / per-mob exhaustive tests are deferred; declarative catalog + chiseledBuild JSON parse is enough static verification. |

---

## 3. Architecture

```
src/main/kotlin/com/dndweapons/
  acquisition/
    AcquisitionCatalog.kt          # source of truth (data only)
    StructureLoot.kt               # data class
    MobDrop.kt                     # data class
    VillagerTradeEntry.kt          # data class
    WeaponLookup.kt                # helper: (weapon_id, tier) -> Item
  loot/
    WeaponLootRegistrar.kt         # 1 MODIFY hook, handles 13 structures + 7 mobs
    WitherTrophyHandler.kt         # AFTER_DEATH hook for Wither
  trade/
    WeaponTradeRegistrar.kt        # registers Weaponsmith + Fletcher (1.20.1-1.21.11 only)
  codegen/
    Phase5TradeCodegen.kt          # emits 26.1.2 trade JSONs from the catalog
  test/
    AcquisitionGametest.kt         # 3 smoke gametests
```

```
src/main/resources/
  data/dndweapons/villager_trades/
    weaponsmith_level1.json        # 26.1.2 only; ignored on older versions
    weaponsmith_level2.json
    ... (9 more files total: 5 weaponsmith + 4 fletcher levels)
```

```
src/test/kotlin/com/dndweapons/acquisition/
  AcquisitionCatalogTest.kt        # catalog integrity (no JUnit MC dependency)
```

**Init wiring (in `DndWeaponsMod.kt`, after Phase 4 calls):**

```kotlin
// Phase 5
AcquisitionCatalog.validate()       // logs unknown weapon ids; never throws
WeaponLootRegistrar.register()      // all 5 versions
WitherTrophyHandler.register()      // all 5 versions
//? if <26.1.2 {
WeaponTradeRegistrar.register()     // 1.20.1-1.21.11; uses TradeOfferHelper
//?}
// On 26.1.2 the JSON data pack is loaded by Minecraft itself; no Kotlin hook needed.
```

**Version drift is confined to two files:**

1. `WeaponLootRegistrar.kt` — same multi-SAM stonecutter pattern as Phase 4's `SmithingTemplateLootInjector.kt`. Five forks: v2 module (1.20.1–1.21.11) vs v3 (26.1.2), 5/3/4-param `Modify` SAM, `location()`/`identifier()` accessor.
2. `WeaponTradeRegistrar.kt` — same multi-fork pattern as Phase 4's `SmithingTemplateTrades.kt`. Stonecutter-gated for `ItemCost` vs `ItemStack` cost wrappers (1.21.1+ vs 1.20.1) and `registerVillagerOffers` signature shift (no Int level param at 1.21.11+ for some signatures).

`WitherTrophyHandler.kt`, the catalog files, and the codegen are version-agnostic. `Phase5TradeCodegen.kt` emits JSON that targets the 26.1.2 schema only.

---

## 4. Catalog data shape

### `AcquisitionCatalog.kt`

```kotlin
object AcquisitionCatalog {

    // ───── Structure loot ────────────────────────────────────────────
    val STRUCTURE_LOOT: Map<String, StructureLoot> = mapOf(
        // ── Iron-tier structures (9 entries) ──
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
        "minecraft:chests/nether_bridge" to StructureLoot(   // nether fortress
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
            // Ocean Monument has no standard chest loot table in vanilla; the master
            // design's "Ocean Monument -> Spear" intent maps to underwater ruins, which
            // share the aquatic theme and DO have a chest table. Confirm at impl time.
            weapons = listOf("spear"),
            chancePct = 6, tier = Tier.IRON,
        ),
        "minecraft:chests/pillager_outpost" to StructureLoot(
            weapons = listOf("heavy_crossbow", "hand_crossbow"),
            chancePct = 5, tier = Tier.IRON,
        ),

        // ── Diamond-tier endgame structures (4 entries) ──
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
            minVersion = "1.21.1",   // trial chambers don't exist pre-1.21
        ),
        "minecraft:chests/bastion_treasure" to StructureLoot(
            weapons = listOf("glaive", "heavy_crossbow"),
            chancePct = 3, tier = Tier.DIAMOND,   // already has netherite template from Phase 4
        ),
    )

    // ───── Mob drops ─────────────────────────────────────────────────
    val MOB_DROPS: Map<String, MobDrop> = mapOf(
        "minecraft:entities/vindicator"      to MobDrop("battleaxe",       ironPct = 8),
        "minecraft:entities/pillager"        to MobDrop("heavy_crossbow",  ironPct = 4),
        "minecraft:entities/wither_skeleton" to MobDrop("glaive",          ironPct = 6, netheritePct = 1),
        "minecraft:entities/husk"            to MobDrop("club",            ironPct = 5),
        "minecraft:entities/piglin_brute"    to MobDrop("maul",            ironPct = 10, netheritePct = 2),
        "minecraft:entities/warden"          to MobDrop(null,              ironPct = 0,  netheritePct = 5),   // random netherite weapon
        "minecraft:entities/skeleton"        to MobDrop("dagger",          ironPct = 2),
    )

    // ───── Villager trades ───────────────────────────────────────────
    val VILLAGER_TRADES: Map<String, Map<Int, List<VillagerTradeEntry>>> = mapOf(
        "minecraft:weaponsmith" to mapOf(
            1 to listOf(   // Novice
                VillagerTradeEntry("mace",         emeralds = 3),
                VillagerTradeEntry("sickle",       emeralds = 4),
                VillagerTradeEntry("quarterstaff", emeralds = 5),
            ),
            2 to listOf(   // Apprentice
                VillagerTradeEntry("spear",        emeralds = 4),
                VillagerTradeEntry("club",         emeralds = 4),
                VillagerTradeEntry("greatclub",    emeralds = 5),
                VillagerTradeEntry("light_hammer", emeralds = 7),
            ),
            3 to listOf(   // Journeyman
                VillagerTradeEntry("longsword",    emeralds = 8),
                VillagerTradeEntry("battleaxe",    emeralds = 10),
                VillagerTradeEntry("warhammer",    emeralds = 10),
                VillagerTradeEntry("morningstar",  emeralds = 12),
            ),
            4 to listOf(   // Expert
                VillagerTradeEntry("glaive",       emeralds = 15),
                VillagerTradeEntry("halberd",      emeralds = 17),
                VillagerTradeEntry("pike",         emeralds = 18),
                VillagerTradeEntry("maul",         emeralds = 20),
            ),
            5 to listOf(   // Master
                VillagerTradeEntry("greatsword",   emeralds = 22),
                VillagerTradeEntry("greataxe",     emeralds = 24),
                VillagerTradeEntry("lance",        emeralds = 26),
                VillagerTradeEntry("rapier",       emeralds = 28),
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
     * Init-time sanity check. Logs (does not throw) on any unknown weapon id.
     * Intended for catching typos in this file; the gametest is the runtime check.
     */
    fun validate() { /* logs to DndWeaponsMod.LOGGER; see §6 */ }
}
```

### Data classes (`acquisition/`)

```kotlin
data class StructureLoot(
    val weapons: List<String>,         // base weapon ids; tier applied at lookup time
    val chancePct: Int,                // 0..100; pool weight = chancePct for weapon, 100-chancePct for empty
    val tier: Tier,                    // IRON for most; DIAMOND for endgame
    val minVersion: String? = null,    // e.g. "1.21.1"; null = all versions
)

data class MobDrop(
    val ironWeapon: String?,           // base weapon id, or null if netherite-only
    val ironPct: Int,                  // 0..100
    val netheritePct: Int = 0,         // 0..100; 0 = no netherite roll
)

data class VillagerTradeEntry(
    val weapon: String,                // base weapon id; always iron tier
    val emeralds: Int,                 // input emerald cost
    val outputCount: Int = 1,          // stack size of the traded weapon
    val maxUses: Int = 12,             // vanilla weaponsmith default
    val xp: Int = 5,                   // villager XP per trade
)
```

### `WeaponLookup.kt`

```kotlin
object WeaponLookup {
    /**
     * Returns the registered `Item` for the given base weapon id + tier, or null if either
     * the weapon id is unknown or no tier variant exists (e.g., requesting diamond for a
     * vanilla-mapped weapon like shortsword).
     */
    fun byId(weaponId: String, tier: Tier): Item? { ... }

    /**
     * Returns all registered netherite-tier weapons. Used by the Warden mob drop pool and
     * the Wither trophy random selection.
     */
    fun allNetherite(): List<Item> { ... }
}
```

Implementation reads from `Weapons.ALL_TIERED` (Phase 4) and the vanilla item registry.

---

## 5. Data flow

### Loot injection (1.20.1 — 26.1.2)

One `LootTableEvents.MODIFY` hook handles both structure chests and mob death tables, since Minecraft routes both through the loot-table API.

```
mod init
  └─ WeaponLootRegistrar.register()
       └─ LootTableEvents.MODIFY.register { key, builder, ... ->
             val tableId = resolveTableId(key)                 // stonecutter-gated
             // structure path
             AcquisitionCatalog.STRUCTURE_LOOT[tableId]?.let { entry ->
                 if (!entry.appliesOnThisVersion()) return@MODIFY
                 builder.withPool(buildStructurePool(entry))   // weapon weight=chancePct, empty=100-chancePct
             }
             // mob path
             AcquisitionCatalog.MOB_DROPS[tableId]?.let { drop ->
                 if (drop.ironPct > 0)       builder.withPool(buildMobPool(drop, Tier.IRON))
                 if (drop.netheritePct > 0)  builder.withPool(buildMobPool(drop, Tier.NETHERITE))
             }
         }
```

Two pool insertions for a netherite-eligible mob = two independent rolls (per decision #6). Each pool has a single weighted entry: `weaponWeight = pct`, `emptyWeight = 100 - pct`.

### Wither trophy

```
mod init
  └─ WitherTrophyHandler.register()
       └─ ServerLivingEntityEvents.AFTER_DEATH.register { entity, damageSource ->
             if (entity.type != EntityType.WITHER) return@AFTER_DEATH
             val netherite = WeaponLookup.allNetherite().random(entity.random)
             entity.spawnAtLocation(ItemStack(netherite))
         }
```

No loot-table involvement; the Wither's vanilla death drops (nether star) are unaffected.

### Villager trades (1.20.1 — 1.21.11, Kotlin path)

```
mod init (only on <26.1.2)
  └─ WeaponTradeRegistrar.register()
       └─ for ((profession, levels) in AcquisitionCatalog.VILLAGER_TRADES) {
             for ((level, trades) in levels) {
                 TradeOfferHelper.registerVillagerOffers(profession, level) { factories ->
                     for (trade in trades) factories.add(buildOffer(trade))
                 }
             }
         }
```

`buildOffer` is stonecutter-gated: 1.20.1 uses `ItemStack` cost wrappers, 1.21.1+ uses `ItemCost`. The `registerVillagerOffers` signature also differs by version (same pattern as Phase 4's `SmithingTemplateTrades.kt`).

### Villager trades (26.1.2, data-pack path)

```
build-time (manual run when catalog changes)
  └─ ./gradlew runPhase5TradeCodegen   # invokes Phase5TradeCodegen.main()
       └─ for ((profession, levels) in AcquisitionCatalog.VILLAGER_TRADES) {
             for ((level, trades) in levels) {
                 emit data/dndweapons/villager_trades/${profession}_level${level}.json
             }
         }

runtime on 26.1.2
  └─ Minecraft auto-loads the JSONs as a villager trade rebalance pack.
```

JSON schema follows 26.1.2's `villager_trade_rebalance` data format. Files are committed to the repo and ignored by older versions (no matching data-pack loader exists pre-26.1).

### Loot table ID accessor (5-fork stonecutter)

```kotlin
private fun resolveTableId(key: Any): String {
    //? if <1.21.1 {
    /* 1.20.1 — 5-param SAM, key is ResourceLocation */
    return (key as ResourceLocation).toString()
    //?} else if (>=1.21.11) {
    //?   if (<26.1.2) {
    /* 1.21.11 — 3-param SAM, ResourceKey, identifier() */
    return (key as ResourceKey<*>).identifier().toString()
    //?   } else {
    /* 26.1.2 — 4-param SAM, ResourceKey, identifier() */
    return (key as ResourceKey<*>).identifier().toString()
    //?   }
    //?} else {
    /* 1.21.1 / 1.21.4 — 3-param SAM, ResourceKey, location() */
    return (key as ResourceKey<*>).location().toString()
    //?}
}
```

Matches the pattern in `SmithingTemplateLootInjector.kt` from Phase 4.

---

## 6. Error handling

Three real failure modes; everything else trusts the data.

**Failure 1 — Catalog references an unknown weapon id (typo).**
- Detection: `WeaponLookup.byId(id, tier)` returns `null`.
- Handling: `LOGGER.error("Unknown weapon id '$id' in AcquisitionCatalog for <context>; skipping")` and skip that single entry.
- Rationale: One typo shouldn't break injection for all other entries. The smoke gametests catch missing entries at chiseledBuild time.

**Failure 2 — Loot table id matches catalog but vanilla table is renamed/removed on a target MC version.**
- Detection: The `minVersion` / `maxVersion` field on `StructureLoot` gates per-version validity. Same mechanism as Trial Chambers being 1.21.1+ only.
- Handling: Silently skip; no log.
- Rationale: Adding a version gate is a one-line catalog edit when a vanilla rename is discovered.

**Failure 3 — 26.1.2 trade JSON references an item that no longer exists (stale codegen output).**
- Detection: Minecraft's data-pack loader logs a parse warning and skips that trade.
- Handling: Don't add extra logging; rely on Minecraft's own loader output.
- Rationale: Only happens if someone hand-edits the JSON or runs codegen against a stale tree. Both are user errors.

**Init-time sanity check** (`AcquisitionCatalog.validate()`):

```kotlin
fun validate() {
    val errors = mutableListOf<String>()
    for ((tableId, loot) in STRUCTURE_LOOT) {
        for (w in loot.weapons) {
            if (WeaponLookup.byId(w, loot.tier) == null) errors += "structure $tableId: $w (${loot.tier})"
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
                if (WeaponLookup.byId(t.weapon, Tier.IRON) == null) errors += "$profession level $level: ${t.weapon}"
            }
        }
    }
    if (errors.isNotEmpty()) {
        DndWeaponsMod.LOGGER.error("AcquisitionCatalog references unknown weapons: $errors")
    }
}
```

Runs once at mod init. Never throws. The error log is enough to debug a typo.

**Things we deliberately do NOT validate:**
- `chancePct` outside 0..100 — hand-authored catalog; a single `require()` at registrar init is sufficient.
- Duplicate catalog entries — a structure listed twice will inject two pools; harmless.
- Missing texture/model for a tiered weapon — Phase 4 shipped all 62; a missing texture is a Phase 4 regression, not a Phase 5 concern.

---

## 7. Testing

### Gametests (3 smoke tests, in `AcquisitionGametest.kt`)

**Test 1 — `strongholdCorridorContainsModWeapon`**
- Roll the `minecraft:chests/stronghold_corridor` loot table 200 times against a seeded `RandomSource`.
- Assert ≥1 roll contains an item with descriptionId starting `item.dndweapons.`.
- Expected: ~24 hits per 200 rolls at 12% chance; 1+ is the minimum bar for "the injector wired up".

**Test 2 — `weaponsmithLevelOneTradesIncludeModWeapon`**
- On <26.1.2: query `VillagerTrades.TRADES[VillagerProfession.WEAPONSMITH]?[1]`, invoke each `ItemListing`, assert ≥1 offer's result item has descriptionId starting `item.dndweapons.`.
- On 26.1.2: check `ResourceManager.listResources("villager_trades")` contains at least one `dndweapons` JSON.
- Both paths verify "the Weaponsmith level-1 catalog row produced a real trade".

**Test 3 — `vindicatorBattleaxeDropsAtExpectedRate`**
- Spawn 100 vindicators in a 4×4 test region, kill each, count `item.dndweapons.battleaxe` drops.
- Assert drop count in `1..30` (expected ~8, but wide tolerance for RNG noise).
- Validates "mob drop injector fires, weapon resolves, item entity spawns".

**Test budget after Phase 5:** 13 (after Phase 4) + 3 = **16 mod gametests per version**.

### Unit test (`src/test/kotlin/com/dndweapons/acquisition/AcquisitionCatalogTest.kt`)

Pure JUnit 5, no MC dependency. Verifies:
1. Every `weaponId` in `STRUCTURE_LOOT`, `MOB_DROPS`, `VILLAGER_TRADES` resolves to a `WeaponSpec` in `Weapons.ALL`.
2. Every `chancePct` is in 0..100.
3. Every `tier` referenced in `STRUCTURE_LOOT` is valid for that weapon (iron always; diamond only for non-vanilla-mapped non-ranged).
4. Trial Chambers entry has `minVersion = "1.21.1"`.
5. No duplicate keys across the three maps.

### What we explicitly skip

- **Wither trophy gametest** — summoning the Wither in a gametest is slow and may exceed test timeouts. The handler is small and statically reviewable.
- **Per-structure / per-mob coverage** — declarative catalog + chiseledBuild JSON parse + the one smoke test per category covers the common-mode failure (injector not wired up).
- **Per-version trade ItemCost shape** — stonecutter forks already cover the build-time correctness; the smoke test verifies registration, not API shape.

---

## 8. Build matrix impact

| Version | Loot SAM/module/key | Trade path | Wither hook | Notes |
|---|---|---|---|---|
| 1.20.1 | v2, 5-param, ResourceLocation direct | TradeOfferHelper + ItemStack costs | shared (AFTER_DEATH) | Trial Chambers entry skipped (minVersion=1.21.1) |
| 1.21.1 | v2, 3-param, `location()` | TradeOfferHelper + ItemCost | shared | Trial Chambers entry active |
| 1.21.4 | v2, 3-param, `location()` | TradeOfferHelper + ItemCost | shared | matches 1.21.1 |
| 1.21.11 | v2, 3-param, `identifier()` | TradeOfferHelper (different signature) + ItemCost | shared | `location()` → `identifier()` rename |
| 26.1.2 | v3, 4-param, `identifier()` | data-pack JSON only (codegen-emitted) | shared | TradeOfferHelper absent; runtime Kotlin trade path skipped |

`ServerLivingEntityEvents.AFTER_DEATH` exists in Fabric API on all 5 target versions — single shared import, no stonecutter forks for the Wither handler.

---

## 9. File map

### New Kotlin source files

```
src/main/kotlin/com/dndweapons/
  acquisition/
    AcquisitionCatalog.kt           # ~250 lines (data tables + validate)
    StructureLoot.kt                # ~15 lines (data class + helpers)
    MobDrop.kt                      # ~15 lines
    VillagerTradeEntry.kt           # ~15 lines
    WeaponLookup.kt                 # ~50 lines
  loot/
    WeaponLootRegistrar.kt          # ~200 lines (stonecutter-gated MODIFY hook)
  trade/
    WeaponTradeRegistrar.kt         # ~200 lines (stonecutter-gated trades)
  codegen/
    Phase5TradeCodegen.kt           # ~150 lines (runnable main; emits 26.1.2 JSONs)
  test/
    AcquisitionGametest.kt          # ~150 lines (3 @GameTest methods + helpers)
```

`WitherTrophyHandler.kt` is folded into `loot/` for proximity to the rest of acquisition logic:

```
  loot/
    WitherTrophyHandler.kt          # ~40 lines
```

### New unit test

```
src/test/kotlin/com/dndweapons/acquisition/
  AcquisitionCatalogTest.kt         # ~100 lines
```

### New data files (codegen-emitted, committed to repo)

```
src/main/resources/data/dndweapons/villager_trades/
  weaponsmith_level1.json
  weaponsmith_level2.json
  weaponsmith_level3.json
  weaponsmith_level4.json
  weaponsmith_level5.json
  fletcher_level2.json
  fletcher_level3.json
  fletcher_level4.json
  fletcher_level5.json
```

(9 files; only used on 26.1.2.)

### Modified files

```
src/main/kotlin/com/dndweapons/DndWeaponsMod.kt   # add 4 init calls (3 always, 1 stonecutter-gated)
src/main/resources/fabric.mod.json                # register AcquisitionGametest entrypoint
src/main/resources/assets/dndweapons/lang/en_us.json   # no Phase 5 changes (no new items)
```

---

## 10. Assumptions to verify at implementation time

These are not blockers — they are calls-out for the implementing agent to confirm against the live MC + Fabric source for each target version.

1. **Vanilla loot table IDs** in `STRUCTURE_LOOT` are written from memory. The implementing agent must confirm each against the actual `data/minecraft/loot_tables/chests/` in the 5 target MC versions, paying particular attention to:
   - `minecraft:chests/nether_bridge` (Nether Fortress chest)
   - `minecraft:chests/underwater_ruin_big` (the Ocean-Monument substitute)
   - `minecraft:chests/trial_chambers/reward_ominous_unique` (1.21.1+ only; the exact path under `trial_chambers/` may have shifted)
   - `minecraft:chests/ancient_city` (the path may include a subdirectory)
2. **`ServerLivingEntityEvents.AFTER_DEATH`** is part of `fabric-lifecycle-events-v1`. Confirm the import path on each version and apply stonecutter forks if the package moved (it did at 1.21.x at least once).
3. **26.1.2 villager_trade_rebalance JSON schema** — the codegen output format must match what 26.1.2 actually loads. Confirm against a vanilla example trade-rebalance JSON before emitting.
4. **`TradeOfferHelper.registerVillagerOffers`** signature drift across 1.20.1 / 1.21.1 / 1.21.11. Phase 4's `SmithingTemplateTrades.kt` already maps the wandering-trader equivalent; Phase 5 needs the villager-trade equivalents which may have a different fork pattern.
5. **Pillager-Captain check** is excluded (out of scope), but the implementing agent should confirm that the standard `minecraft:entities/pillager` table is fired for both captain and non-captain deaths so the regular iron heavy_crossbow drop covers both cases. If captain has a separate table on any version, the catalog entry handles that naturally — just add the row.

---

## 11. Cross-references

- **Master design** [`2026-05-16-dnd-weapons-design.md`](2026-05-16-dnd-weapons-design.md) §8 "Acquisition (Loot, Trades, Mob Drops)" — Phase 5 implements this section.
- **Phase 4 design** [`2026-05-17-dnd-weapons-phase-4-design.md`](2026-05-17-dnd-weapons-phase-4-design.md) — provides the `Tier` enum, `WeaponSpec.atTier()`, and `Weapons.ALL_TIERED` used here. Also provides reference patterns: `SmithingTemplateLootInjector.kt` (loot SAM forks) and `SmithingTemplateTrades.kt` (trade forks).
- **Phase 4 verification** [`phase-4-verification-final.md`](../plans/phase-4-verification-final.md) — Phase 5 work begins after this verification flips to GREEN.

---

**End of spec.**
