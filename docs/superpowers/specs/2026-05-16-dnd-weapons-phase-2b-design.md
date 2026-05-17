# DnD Weapons — Phase 2b Design Specification

**Date:** 2026-05-16
**Status:** Approved (brainstorm complete, ready for implementation planning)
**Author:** brainstorm session with the user
**Parent design:** [2026-05-16-dnd-weapons-design.md](2026-05-16-dnd-weapons-design.md) (sections 4 and 7 are the authoritative catalog and recipe pattern sources)
**Predecessor phase:** [phase-2a-3-mojang-naming](phase-2a-3-mojang-naming-design.md) — tagged `phase-2a-3-mojang-naming` on origin/main
**Implementation plan:** TBD (next step — invoke `superpowers:writing-plans`)

---

## 1. Goal & Scope

**Goal:** Expand the weapon catalog from 1 weapon (Longsword) to 38 — 34 newly-registered weapons + 4 vanilla-mapped role specs. Ship across all 5 MC versions (1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2) with crafting recipes, language strings, item models, role tags, and AI-generated 16x16 textures for each registered weapon.

### In scope

The master design's section-4 catalog tables are the authoritative source for which weapons are registered vs vanilla-mapped. Section 6 lists exactly 4 vanilla mappings: **Shortsword, Shortbow, Light Crossbow, Trident**. Reconciled per-category counts (matching master design's "34 registered + 4 vanilla-mapped = 38" total):

| Category | Total | Registered | Vanilla-mapped | Already in Phase 1 | New in Phase 2b |
|---|---|---|---|---|---|
| Simple Melee | 10 | 10 | 0 | 0 | 10 registered |
| Simple Ranged | 4 | 2 (Dart, Sling) | 2 (Shortbow, Light Crossbow) | 0 | 2 registered + 2 vanilla-mapped |
| Martial Melee | 18 | 16 (incl. Longsword) | 2 (Shortsword, Trident) | 1 (Longsword) | 15 registered + 2 vanilla-mapped |
| Martial Ranged | 6 | 6 | 0 | 0 | 6 registered |
| **Total** | **38** | **34** | **4** | **1** | **33 registered + 4 vanilla-mapped = 37 new specs** |

Concrete in-scope deliverables:

- 37 new `WeaponSpec` entries in `Weapons.kt`: 33 registered + 4 vanilla-mapped (Shortsword, Shortbow, Light Crossbow, Trident)
- 4 vanilla-mapped specs with `vanillaRoleTag` set to `"dndweapons:role/<id>"`
- 4 role tag JSONs at `data/dndweapons/tags/items/role/{shortsword,shortbow,light_crossbow,trident}.json`
- 33 new crafting recipes (`data/dndweapons/recipe/<id>.json` — 1.20.5+ schema)
- 33 corresponding overlay recipes in `versions/1.20.1/src/main/resources/data/dndweapons/recipes/<id>.json` (1.20.1 schema: plural folder, `"item"` key)
- 33 new lang entries in `assets/dndweapons/lang/en_us.json` (plus 4 entries for the vanilla-mapped role weapons for Phase 3 tooltip reuse)
- 33 new item models in `assets/dndweapons/models/item/<id>.json` parented to `minecraft:item/generated`
- 33 new 16x16 textures generated via the `minecraft-asset-generator` agent (Gemini AI Studio), output to `assets/dndweapons/textures/item/<id>.png`
- `WeaponsTest.kt` expanded with assertions for full-catalog integrity (count, category counts, vanilla-mapped invariants)
- All 5 MC versions pass `:build`, `:test`, `:runGametest` after the expansion
- `chiseledBuild` produces 5 jars

### Out of scope (deferred)

| Concern | Deferred to |
|---|---|
| `SpecRegistry` / role-tag runtime lookup | Phase 3 (combat hooks exercise it) |
| Combat hooks (Versatile, Finesse, Light, Heavy, Lance Special) | Phase 3 |
| Tooltip stat-block injection | Phase 3 |
| Smithing upgrade ladder (iron → diamond → netherite) | Phase 4 |
| Loot tables / villager trades / mob drops | Phase 5 |
| Wiki generator | Phase 6 |
| Recipe-ingredient schema fix (`fabric:tag` warning observed in Phase 2a-3) | Focused recipe-schema phase (not gating; warning is non-fatal across all 5 MC versions) |
| Real (non-placeholder) art | A future art-pass phase, or via re-running the generator agent with refined prompts |

---

## 2. Decision Log

| # | Decision | Choice | Rationale |
|---|---|---|---|
| 1 | Phase 2 split point | Phase 2a (versions) + Phase 2a-3 (Mojang naming) + Phase 2b (catalog) | Each phase plan-runner-cycle-sized; per-epoch + naming churn shouldn't mix with content churn |
| 2 | Vanilla-mapped specs in this phase | Data + tag JSONs only; no `SpecRegistry`/runtime infra | Without combat hooks (Phase 3) the lookup has no observable effect; ship the inert data now so Phase 3 can wire it |
| 3 | Texture strategy | Generate 33 textures via `minecraft-asset-generator` agent (Gemini AI Studio) | Real art, not placeholder copies; user has the agent ready |
| 4 | Texture wave ordering | Final wave (after all logic/data/tests pass on all 5 versions) | If data is broken, generating art is wasted; placeholder art is acceptable interim state |
| 5 | Per-MC recipe JSON | Shared `recipe/` for 1.20.5+; per-version overlay `recipes/` for 1.20.1 | Master design section 7 dictates per-epoch schema differences |
| 6 | Testing scope | Expand `WeaponsTest` only; keep existing single-weapon `RegistrationGametest` | Per-spec gametests are overkill; catalog data correctness is what could break |
| 7 | Lang entries for vanilla-mapped role weapons | Include them in `en_us.json` (4 extra entries) | Phase 3's tooltip injection will read these |

---

## 3. Architecture

### File touches (~140 total)

```
src/main/kotlin/com/dndweapons/catalog/Weapons.kt          # 1 file, +37 entries
src/test/kotlin/com/dndweapons/catalog/WeaponsTest.kt      # 1 file, expanded assertions

src/main/resources/data/dndweapons/recipe/<id>.json        # 33 new recipe JSONs (1.20.5+ format)
src/main/resources/data/dndweapons/tags/items/role/        # 4 role tag JSONs
src/main/resources/assets/dndweapons/lang/en_us.json       # +33 registered + 4 vanilla-mapped entries
src/main/resources/assets/dndweapons/models/item/<id>.json # 33 model JSONs
src/main/resources/assets/dndweapons/textures/item/<id>.png # 33 generated PNGs

versions/1.20.1/src/main/resources/data/dndweapons/recipes/<id>.json  # 33 new overlays (1.20.1 format)
```

### Key design choices

**One big `Weapons.kt` edit, not per-weapon files.** Master design puts all specs in a single `object Weapons`. Phase 2b keeps that — appends ~37 `val` declarations (33 registered + 4 vanilla-mapped) and updates the `ALL: List<WeaponSpec>` to include all 38. ~250 lines added. Single-file edit keeps the catalog easy to scan.

**Recipe pattern templates, not 33 unique designs.** Master design section 7 defines 8-10 reusable shape patterns. Most weapons fit one of: sword-vertical, polearm-2x3, throwable-3x3, heavy-2x2/1x3, throwable-small-1x2, bow, crossbow, blowgun, sling, firearm. The implementation plan will name the pattern per weapon; the dev agent writes the recipe from the pattern.

**Vanilla-mapped specs are data only.** They skip recipe + lang + model + texture. The `WeaponRegistrarImpl` already early-returns on `isVanillaMapped` (Phase 2a-3 verified this works on all 5 versions). The 4 role tag JSONs are inert metadata until Phase 3.

**1.20.1 recipe overlays are mechanical doubles.** For each shared `data/dndweapons/recipe/<id>.json`, a near-duplicate at `versions/1.20.1/src/main/resources/data/dndweapons/recipes/<id>.json` swaps the result-key spelling (`"id"` → `"item"`) and lives in the plural folder. Plan task auto-generates these from the 1.20.5+ source.

**Asset generation parallelism.** 33 textures via Gemini = 33 API calls. Plan-runner waves cap at 6 agents, so split into 6 parallel waves of ~5-6 weapons each. Worst case: ~6 minutes wall-clock if API responds in ~60s per call.

---

## 4. Catalog Specifics

### Full catalog count (resolved against master design section 6 as authoritative for vanilla mappings)

| Category | Total | Registered | Vanilla-mapped |
|---|---|---|---|
| Simple Melee | 10 | 10 | 0 |
| Simple Ranged | 4 | 2 (Dart, Sling) | 2 (Shortbow, Light Crossbow) |
| Martial Melee | 18 | 16 (incl. Longsword) | 2 (Shortsword, Trident) |
| Martial Ranged | 6 | 6 | 0 |
| **Total** | **38** | **34** | **4** |

### `WeaponSpec` value sources

For each weapon, master design section 4 specifies:
- `id` (lowercase snake_case, matches design's "Weapon" column)
- `displayName`
- `category`
- `damageType`
- `diceText`, `versatileDice` (nullable)
- `attackDamage`, `versatileBonus` (per design's "MC dmg" column + "+1" convention)
- `attackSpeed` (per "Speed" column)
- `reachBonus` (0.0 except polearms/whip which use 1.0)
- `knockbackBonus` (0 except Heavy weapons = 1)
- `properties: Set<Property>` (per design's "DnD" property tags translated to our `Property` enum)
- `ranged: RangeKind` (NONE for melee, mapped for ranged per the design's Range column)
- `baseDurability` (250 default per design — iron-tier baseline)
- `vanillaRoleTag` (nullable; `"dndweapons:role/<id>"` for the 4 vanilla mappings, null otherwise)

The writing-plans agent will tabulate every spec from the master design and inline the exact `WeaponSpec(...)` constructor call per weapon.

### Property enum coverage check

Phase 1's `Property.kt` declared 10 constants: LIGHT, HEAVY, FINESSE, REACH, TWO_HANDED, VERSATILE, THROWN, AMMUNITION, LOADING, SPECIAL_LANCE. Master design's "DnD" column references all 10 across the 38 weapons. No new `Property` constants needed.

### Vanilla mappings — role tag contents

| Tag file | Vanilla items |
|---|---|
| `tags/items/role/shortsword.json` | `minecraft:wooden_sword`, `minecraft:stone_sword`, `minecraft:iron_sword`, `minecraft:golden_sword`, `minecraft:diamond_sword`, `minecraft:netherite_sword` |
| `tags/items/role/shortbow.json` | `minecraft:bow` |
| `tags/items/role/light_crossbow.json` | `minecraft:crossbow` |
| `tags/items/role/trident.json` | `minecraft:trident` |

JSON format (vanilla item tag schema):
```json
{ "values": ["minecraft:wooden_sword", "minecraft:stone_sword", ...] }
```

---

## 5. Data Flow

### Catalog construction

`Weapons.kt` keeps its current `object Weapons` shape:
```kotlin
object Weapons {
    val LONGSWORD = WeaponSpec(...)    // existing
    val CLUB = WeaponSpec(id = "club", displayName = "Club", ...)
    val DAGGER = WeaponSpec(...)
    // ... 37 more vals
    val ALL: List<WeaponSpec> = listOf(LONGSWORD, CLUB, DAGGER, ..., TRIDENT)
}
```

`WeaponRegistrarImpl.register(spec)` iterates `ALL`. For each:
- `isVanillaMapped` → early return (no item, no compile-time tag dependency)
- otherwise → existing Phase 2a-3 registration pipeline (per-epoch attribute compat, registry key + setId, Registry.register)

### Recipe loading

Per MC version, vanilla's recipe loader picks up the matching `data/dndweapons/recipe/` (or `recipes/`) folder. The mod doesn't touch recipe loading code — recipes are pure data. Vanilla shows them in the recipe book once an `item` group is populated (Phase 1's creative tab already lists all registered items; recipes show up when a player has a crafting table open and matching ingredients).

### Asset loading

`assets/dndweapons/models/item/<id>.json` references `assets/dndweapons/textures/item/<id>.png` via `"textures": {"layer0": "dndweapons:item/<id>"}`. Vanilla resource manager loads them at world load. The 4 vanilla-mapped weapons skip this — they piggyback on vanilla item models/textures.

### Test flow

- `WeaponsTest` validates the catalog data at build time (JVM-only, no MC runtime).
- `RegistrationGametest` continues to verify only `longswordIsRegistered` — a single registration smoke confirms the whole pipeline still works; per-weapon gametests would be excessive.

---

## 6. Testing Strategy

### Per-version testing matrix

| Layer | Per-version artifact |
|---|---|
| Unit (JUnit Kotlin) | `WeaponSpecTest` (4 tests, unchanged) + `WeaponsTest` (10+ tests, expanded for full catalog) |
| Integration (Fabric Gametest) | `RegistrationGametest.longswordIsRegistered` — unchanged |
| Build | `:build` produces jar; `chiseledBuild` produces 5 jars |

### Acceptance criteria

| # | Criterion | Verification |
|---|---|---|
| 1 | `Weapons.ALL.size == 38` | `WeaponsTest.allHasExpectedCount` |
| 2 | 34 registered specs (`isVanillaMapped == false`) | `WeaponsTest.registeredCount` |
| 3 | 4 vanilla-mapped specs (`isVanillaMapped == true`) | `WeaponsTest.vanillaMappedCount` |
| 4 | All IDs unique | `WeaponsTest.allIdsAreUnique` (existing) |
| 5 | Per-category counts match (resolved at plan time) | `WeaponsTest.categoryCountsMatch` |
| 6 | Each vanilla-mapped spec's `vanillaRoleTag` points to one of the 4 known role-tag paths | `WeaponsTest.vanillaMappedTagsValid` |
| 7 | All 38 expected weapon IDs present | `WeaponsTest.allExpectedIdsPresent` |
| 8 | 33 recipe JSONs at `data/dndweapons/recipe/<id>.json` | filesystem |
| 9 | 33 recipe overlays at `versions/1.20.1/src/main/resources/data/dndweapons/recipes/<id>.json` | filesystem |
| 10 | 4 role tag JSONs at `data/dndweapons/tags/items/role/<name>.json` | filesystem |
| 11 | 33 model JSONs at `assets/dndweapons/models/item/<id>.json` | filesystem |
| 12 | 33 texture PNGs at `assets/dndweapons/textures/item/<id>.png`, non-zero size | filesystem |
| 13 | `en_us.json` has `item.dndweapons.<id>` entries for all 34 registered + 4 vanilla-mapped (38 entries total, plus `itemGroup.dndweapons.main`) | grep |
| 14 | All 5 MC versions pass `:build` | `chiseledBuild` |
| 15 | All 5 MC versions pass `:test` (now 11+ tests) | per-version sweep |
| 16 | All 5 MC versions pass `:runGametest` | per-version sweep |
| 17 | `chiseledBuild` produces 5 jars | filesystem |

### Plan-runner risk surfaces (early-warning tasks the plan will include)

1. **First task — `Weapons.kt` + `WeaponsTest.kt` + 1.21.4 unit-test smoke.** Pure data + Kotlin. If catalog data is malformed (validation rejects an ID, properties set wrong, count mismatch), tests catch it immediately. No JSON or texture work wasted.
2. **Recipe JSON wave — verify world-load on 1.21.4 via gametest.** Catches schema issues in one place. Pre-existing `fabric:tag` warning is acknowledged out-of-scope.
3. **Per-version smoke before texture wave.** Validate 5/5 versions green on the data/JSON/tag/lang side first. Textures are visual; if data is broken, generating art is wasted spend.
4. **Texture wave is final.** 33 Gemini calls in parallel-bounded waves. Even if some fail, the mod still works (purple checker); failures regenerate as a focused follow-up.

---

## 7. Resolved at Plan Time

These are intentionally not pinned here; the plan-writing step resolves them:

- Exact per-weapon `WeaponSpec(...)` constructor values — sourced from master design section 4's full catalog table. The writing-plans agent tabulates and inlines each.
- Exact recipe shape per weapon — sourced from master design section 7 patterns + the per-category table above. The writing-plans agent assigns a pattern per weapon and writes the resulting JSON.
- Exact texture prompts per weapon for the Gemini agent — the writing-plans agent drafts per-weapon prompts grounded in master design's "Notes" column (offhand ok, throwable, polearm, etc.) and the weapon's general DnD theme.
