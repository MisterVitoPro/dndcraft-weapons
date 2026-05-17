# DnD Weapons — Phase 2b: Catalog Expansion to 38 Weapons

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. This plan is also consumable by `/plan-runner:run` directly.

**Goal:** Expand `Weapons.kt` from 1 weapon (Longsword) to 38 — 34 registered + 4 vanilla-mapped role specs — and ship the corresponding crafting recipes, language strings, item models, vanilla role tags, and 16x16 textures (AI-generated via the `minecraft-asset-generator` agent) across all 5 MC versions.

**Architecture:** Append 37 `val` declarations to the `Weapons` Kotlin `object` (data-only edit; `WeaponRegistrarImpl` already handles `isVanillaMapped` via early-return). For each of 33 newly-registered weapons, ship one shared recipe JSON (1.20.5+ schema) under `data/dndweapons/recipe/<id>.json` plus a 1.20.1-flavored overlay under `versions/1.20.1/src/main/resources/data/dndweapons/recipes/<id>.json` (plural folder + `"item"` result key), one model JSON, and one Gemini-generated 16x16 PNG texture. The 4 vanilla-mapped specs ship only a `WeaponSpec` entry + role tag JSON + lang entry — no recipe/model/texture, since they piggyback on vanilla items. `WeaponsTest.kt` expands to assert the full-catalog invariants.

**Tech Stack:** Kotlin 2.3.21, Gradle 9.5.1, Fabric Loom 1.16.2, FLK 1.13.11+kotlin.2.3.21, JDK 25 daemon. No new dependencies; this phase is pure content + tests.

**Predecessor:** [phase-2a-3-mojang-naming](2026-05-16-dnd-weapons-phase-2a-3-mojang-naming.md) — tagged `phase-2a-3-mojang-naming` on `origin/main`. 5-version build pipeline confirmed green.

**Source-of-truth catalog:** [master design section 4](../specs/2026-05-16-dnd-weapons-design.md) — all `WeaponSpec(...)` values below are translated from that section's tables.

---

## File Inventory

**Modified:**
```
src/main/kotlin/com/dndweapons/catalog/Weapons.kt          # 37 new vals + ALL list
src/test/kotlin/com/dndweapons/catalog/WeaponsTest.kt      # +7 test methods
src/main/resources/assets/dndweapons/lang/en_us.json       # +37 entries (33 registered + 4 vanilla-mapped)
```

**Created (shared, 1.20.5+):**
```
src/main/resources/data/dndweapons/recipe/{club,dagger,greatclub,handaxe,javelin,light_hammer,mace,quarterstaff,sickle,spear,dart,sling,battleaxe,flail,glaive,greataxe,greatsword,halberd,lance,maul,morningstar,pike,rapier,scimitar,war_pick,warhammer,whip,blowgun,hand_crossbow,heavy_crossbow,longbow,musket,pistol}.json   # 33 recipes
src/main/resources/data/dndweapons/tags/items/role/{shortsword,shortbow,light_crossbow,trident}.json         # 4 role tags
src/main/resources/assets/dndweapons/models/item/{...33 ids...}.json                                          # 33 models
src/main/resources/assets/dndweapons/textures/item/{...33 ids...}.png                                         # 33 textures
```

**Created (1.20.1 overlay):**
```
versions/1.20.1/src/main/resources/data/dndweapons/recipes/{...33 ids...}.json   # 33 overlay recipes
```

**Total: ~140 file touches.**

---

## Weapon ID Reference (used throughout the plan)

| Category | IDs (33 newly-registered + 4 vanilla-mapped) |
|---|---|
| Simple Melee (10 registered) | `club`, `dagger`, `greatclub`, `handaxe`, `javelin`, `light_hammer`, `mace`, `quarterstaff`, `sickle`, `spear` |
| Simple Ranged (2 registered) | `dart`, `sling` |
| Simple Ranged (2 vanilla-mapped) | `shortbow`, `light_crossbow` |
| Martial Melee (15 newly-registered; `longsword` already exists) | `battleaxe`, `flail`, `glaive`, `greataxe`, `greatsword`, `halberd`, `lance`, `maul`, `morningstar`, `pike`, `rapier`, `scimitar`, `war_pick`, `warhammer`, `whip` |
| Martial Melee (2 vanilla-mapped) | `shortsword`, `trident` |
| Martial Ranged (6 registered) | `blowgun`, `hand_crossbow`, `heavy_crossbow`, `longbow`, `musket`, `pistol` |

---

## Task 1: Append 37 new WeaponSpec entries to `Weapons.kt`

**Files:**
- Modify: `src/main/kotlin/com/dndweapons/catalog/Weapons.kt`

- [ ] **Step 1: Replace `Weapons.kt` content with the full catalog**

The file currently has only `LONGSWORD` and `ALL = listOf(LONGSWORD)`. Replace with the expanded version below.

Full file content for `src/main/kotlin/com/dndweapons/catalog/Weapons.kt`:

```kotlin
package com.dndweapons.catalog

/**
 * Source of truth for the entire weapon catalog. 38 PHB weapons total:
 * 34 registered + 4 vanilla-mapped (Shortsword, Shortbow, Light Crossbow, Trident).
 *
 * Values translated from master design section 4
 * (docs/superpowers/specs/2026-05-16-dnd-weapons-design.md).
 *
 * Conventions:
 * - attackDamage = round(dice_average + 1.5); calibrated so 1d8 = 6 (vanilla iron sword).
 * - versatileBonus = attackDamage delta when wielded two-handed (versatile property).
 * - attackSpeed: 1.8 very fast / 1.6 fast / 1.5 normal / 1.0 polearm / 0.9 heavy.
 *   Ranged weapons use 1.5 placeholder; vanilla draw mechanics override at runtime.
 * - reachBonus: 1.0 for Reach property weapons (polearms + whip), 0.0 otherwise.
 * - knockbackBonus: 1 for Heavy property weapons, 0 otherwise.
 * - baseDurability: 250 (iron-tier baseline) for all.
 */
object Weapons {

    // ===== Simple Melee (10) =====

    val CLUB = WeaponSpec(
        id = "club", displayName = "Club",
        category = Category.SIMPLE_MELEE, damageType = DamageType.BLUDGEONING,
        diceText = "1d4", versatileDice = null,
        attackDamage = 4, versatileBonus = 0,
        attackSpeed = 1.6f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.LIGHT),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val DAGGER = WeaponSpec(
        id = "dagger", displayName = "Dagger",
        category = Category.SIMPLE_MELEE, damageType = DamageType.PIERCING,
        diceText = "1d4", versatileDice = null,
        attackDamage = 4, versatileBonus = 0,
        attackSpeed = 1.8f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.LIGHT, Property.THROWN),
        ranged = RangeKind.THROWN, baseDurability = 250, vanillaRoleTag = null,
    )

    val GREATCLUB = WeaponSpec(
        id = "greatclub", displayName = "Greatclub",
        category = Category.SIMPLE_MELEE, damageType = DamageType.BLUDGEONING,
        diceText = "1d8", versatileDice = null,
        attackDamage = 6, versatileBonus = 0,
        attackSpeed = 0.9f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.TWO_HANDED),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val HANDAXE = WeaponSpec(
        id = "handaxe", displayName = "Handaxe",
        category = Category.SIMPLE_MELEE, damageType = DamageType.SLASHING,
        diceText = "1d6", versatileDice = null,
        attackDamage = 5, versatileBonus = 0,
        attackSpeed = 1.6f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.LIGHT, Property.THROWN),
        ranged = RangeKind.THROWN, baseDurability = 250, vanillaRoleTag = null,
    )

    val JAVELIN = WeaponSpec(
        id = "javelin", displayName = "Javelin",
        category = Category.SIMPLE_MELEE, damageType = DamageType.PIERCING,
        diceText = "1d6", versatileDice = null,
        attackDamage = 5, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.THROWN),
        ranged = RangeKind.THROWN, baseDurability = 250, vanillaRoleTag = null,
    )

    val LIGHT_HAMMER = WeaponSpec(
        id = "light_hammer", displayName = "Light Hammer",
        category = Category.SIMPLE_MELEE, damageType = DamageType.BLUDGEONING,
        diceText = "1d4", versatileDice = null,
        attackDamage = 4, versatileBonus = 0,
        attackSpeed = 1.6f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.LIGHT, Property.THROWN),
        ranged = RangeKind.THROWN, baseDurability = 250, vanillaRoleTag = null,
    )

    val MACE = WeaponSpec(
        id = "mace", displayName = "Mace",
        category = Category.SIMPLE_MELEE, damageType = DamageType.BLUDGEONING,
        diceText = "1d6", versatileDice = null,
        attackDamage = 5, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = emptySet(),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val QUARTERSTAFF = WeaponSpec(
        id = "quarterstaff", displayName = "Quarterstaff",
        category = Category.SIMPLE_MELEE, damageType = DamageType.BLUDGEONING,
        diceText = "1d6", versatileDice = "1d8",
        attackDamage = 5, versatileBonus = 1,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.VERSATILE),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val SICKLE = WeaponSpec(
        id = "sickle", displayName = "Sickle",
        category = Category.SIMPLE_MELEE, damageType = DamageType.SLASHING,
        diceText = "1d4", versatileDice = null,
        attackDamage = 4, versatileBonus = 0,
        attackSpeed = 1.8f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.LIGHT),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val SPEAR = WeaponSpec(
        id = "spear", displayName = "Spear",
        category = Category.SIMPLE_MELEE, damageType = DamageType.PIERCING,
        diceText = "1d6", versatileDice = "1d8",
        attackDamage = 5, versatileBonus = 1,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.THROWN, Property.VERSATILE),
        ranged = RangeKind.THROWN, baseDurability = 250, vanillaRoleTag = null,
    )

    // ===== Simple Ranged (4: 2 registered, 2 vanilla-mapped) =====

    val DART = WeaponSpec(
        id = "dart", displayName = "Dart",
        category = Category.SIMPLE_RANGED, damageType = DamageType.PIERCING,
        diceText = "1d4", versatileDice = null,
        attackDamage = 4, versatileBonus = 0,
        attackSpeed = 1.8f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.FINESSE, Property.THROWN),
        ranged = RangeKind.THROWN, baseDurability = 250, vanillaRoleTag = null,
    )

    val SLING = WeaponSpec(
        id = "sling", displayName = "Sling",
        category = Category.SIMPLE_RANGED, damageType = DamageType.BLUDGEONING,
        diceText = "1d4", versatileDice = null,
        attackDamage = 4, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.AMMUNITION),
        ranged = RangeKind.SLING, baseDurability = 250, vanillaRoleTag = null,
    )

    val SHORTBOW = WeaponSpec(
        id = "shortbow", displayName = "Shortbow",
        category = Category.SIMPLE_RANGED, damageType = DamageType.PIERCING,
        diceText = "1d6", versatileDice = null,
        attackDamage = 5, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.TWO_HANDED),
        ranged = RangeKind.BOW, baseDurability = 250,
        vanillaRoleTag = "dndweapons:role/shortbow",
    )

    val LIGHT_CROSSBOW = WeaponSpec(
        id = "light_crossbow", displayName = "Light Crossbow",
        category = Category.SIMPLE_RANGED, damageType = DamageType.PIERCING,
        diceText = "1d8", versatileDice = null,
        attackDamage = 6, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.LOADING, Property.TWO_HANDED),
        ranged = RangeKind.CROSSBOW, baseDurability = 250,
        vanillaRoleTag = "dndweapons:role/light_crossbow",
    )

    // ===== Martial Melee (18: 16 registered (incl. existing Longsword), 2 vanilla-mapped) =====

    val LONGSWORD = WeaponSpec(
        id = "longsword", displayName = "Longsword",
        category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
        diceText = "1d8", versatileDice = "1d10",
        attackDamage = 6, versatileBonus = 1,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.VERSATILE),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val BATTLEAXE = WeaponSpec(
        id = "battleaxe", displayName = "Battleaxe",
        category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
        diceText = "1d8", versatileDice = "1d10",
        attackDamage = 6, versatileBonus = 1,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.VERSATILE),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val FLAIL = WeaponSpec(
        id = "flail", displayName = "Flail",
        category = Category.MARTIAL_MELEE, damageType = DamageType.BLUDGEONING,
        diceText = "1d8", versatileDice = null,
        attackDamage = 6, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = emptySet(),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val GLAIVE = WeaponSpec(
        id = "glaive", displayName = "Glaive",
        category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
        diceText = "1d10", versatileDice = null,
        attackDamage = 7, versatileBonus = 0,
        attackSpeed = 1.0f, reachBonus = 1.0f, knockbackBonus = 1,
        properties = setOf(Property.HEAVY, Property.REACH, Property.TWO_HANDED),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val GREATAXE = WeaponSpec(
        id = "greataxe", displayName = "Greataxe",
        category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
        diceText = "1d12", versatileDice = null,
        attackDamage = 8, versatileBonus = 0,
        attackSpeed = 0.9f, reachBonus = 0.0f, knockbackBonus = 1,
        properties = setOf(Property.HEAVY, Property.TWO_HANDED),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val GREATSWORD = WeaponSpec(
        id = "greatsword", displayName = "Greatsword",
        category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
        diceText = "2d6", versatileDice = null,
        attackDamage = 9, versatileBonus = 0,
        attackSpeed = 0.9f, reachBonus = 0.0f, knockbackBonus = 1,
        properties = setOf(Property.HEAVY, Property.TWO_HANDED),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val HALBERD = WeaponSpec(
        id = "halberd", displayName = "Halberd",
        category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
        diceText = "1d10", versatileDice = null,
        attackDamage = 7, versatileBonus = 0,
        attackSpeed = 1.0f, reachBonus = 1.0f, knockbackBonus = 1,
        properties = setOf(Property.HEAVY, Property.REACH, Property.TWO_HANDED),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val LANCE = WeaponSpec(
        id = "lance", displayName = "Lance",
        category = Category.MARTIAL_MELEE, damageType = DamageType.PIERCING,
        diceText = "1d10", versatileDice = null,
        attackDamage = 7, versatileBonus = 0,
        attackSpeed = 1.0f, reachBonus = 1.0f, knockbackBonus = 1,
        properties = setOf(Property.HEAVY, Property.REACH, Property.TWO_HANDED, Property.SPECIAL_LANCE),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val MAUL = WeaponSpec(
        id = "maul", displayName = "Maul",
        category = Category.MARTIAL_MELEE, damageType = DamageType.BLUDGEONING,
        diceText = "2d6", versatileDice = null,
        attackDamage = 9, versatileBonus = 0,
        attackSpeed = 0.9f, reachBonus = 0.0f, knockbackBonus = 1,
        properties = setOf(Property.HEAVY, Property.TWO_HANDED),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val MORNINGSTAR = WeaponSpec(
        id = "morningstar", displayName = "Morningstar",
        category = Category.MARTIAL_MELEE, damageType = DamageType.PIERCING,
        diceText = "1d8", versatileDice = null,
        attackDamage = 6, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = emptySet(),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val PIKE = WeaponSpec(
        id = "pike", displayName = "Pike",
        category = Category.MARTIAL_MELEE, damageType = DamageType.PIERCING,
        diceText = "1d10", versatileDice = null,
        attackDamage = 7, versatileBonus = 0,
        attackSpeed = 1.0f, reachBonus = 1.0f, knockbackBonus = 1,
        properties = setOf(Property.HEAVY, Property.REACH, Property.TWO_HANDED),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val RAPIER = WeaponSpec(
        id = "rapier", displayName = "Rapier",
        category = Category.MARTIAL_MELEE, damageType = DamageType.PIERCING,
        diceText = "1d8", versatileDice = null,
        attackDamage = 6, versatileBonus = 0,
        attackSpeed = 1.6f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.FINESSE),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val SCIMITAR = WeaponSpec(
        id = "scimitar", displayName = "Scimitar",
        category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
        diceText = "1d6", versatileDice = null,
        attackDamage = 5, versatileBonus = 0,
        attackSpeed = 1.8f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.FINESSE, Property.LIGHT),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val SHORTSWORD = WeaponSpec(
        id = "shortsword", displayName = "Shortsword",
        category = Category.MARTIAL_MELEE, damageType = DamageType.PIERCING,
        diceText = "1d6", versatileDice = null,
        attackDamage = 5, versatileBonus = 0,
        attackSpeed = 1.8f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.FINESSE, Property.LIGHT),
        ranged = RangeKind.NONE, baseDurability = 250,
        vanillaRoleTag = "dndweapons:role/shortsword",
    )

    val TRIDENT = WeaponSpec(
        id = "trident", displayName = "Trident",
        category = Category.MARTIAL_MELEE, damageType = DamageType.PIERCING,
        diceText = "1d8", versatileDice = "1d10",
        attackDamage = 6, versatileBonus = 1,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.THROWN, Property.VERSATILE),
        ranged = RangeKind.THROWN, baseDurability = 250,
        vanillaRoleTag = "dndweapons:role/trident",
    )

    val WAR_PICK = WeaponSpec(
        id = "war_pick", displayName = "War Pick",
        category = Category.MARTIAL_MELEE, damageType = DamageType.PIERCING,
        diceText = "1d8", versatileDice = null,
        attackDamage = 6, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = emptySet(),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val WARHAMMER = WeaponSpec(
        id = "warhammer", displayName = "Warhammer",
        category = Category.MARTIAL_MELEE, damageType = DamageType.BLUDGEONING,
        diceText = "1d8", versatileDice = "1d10",
        attackDamage = 6, versatileBonus = 1,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.VERSATILE),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val WHIP = WeaponSpec(
        id = "whip", displayName = "Whip",
        category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
        diceText = "1d4", versatileDice = null,
        attackDamage = 4, versatileBonus = 0,
        attackSpeed = 1.8f, reachBonus = 1.0f, knockbackBonus = 0,
        properties = setOf(Property.FINESSE, Property.REACH),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    // ===== Martial Ranged (6) =====

    val BLOWGUN = WeaponSpec(
        id = "blowgun", displayName = "Blowgun",
        category = Category.MARTIAL_RANGED, damageType = DamageType.PIERCING,
        diceText = "1", versatileDice = null,
        attackDamage = 2, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.LOADING),
        ranged = RangeKind.BLOWGUN, baseDurability = 250, vanillaRoleTag = null,
    )

    val HAND_CROSSBOW = WeaponSpec(
        id = "hand_crossbow", displayName = "Hand Crossbow",
        category = Category.MARTIAL_RANGED, damageType = DamageType.PIERCING,
        diceText = "1d6", versatileDice = null,
        attackDamage = 5, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.LIGHT, Property.LOADING),
        ranged = RangeKind.CROSSBOW, baseDurability = 250, vanillaRoleTag = null,
    )

    val HEAVY_CROSSBOW = WeaponSpec(
        id = "heavy_crossbow", displayName = "Heavy Crossbow",
        category = Category.MARTIAL_RANGED, damageType = DamageType.PIERCING,
        diceText = "1d10", versatileDice = null,
        attackDamage = 7, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 1,
        properties = setOf(Property.HEAVY, Property.LOADING, Property.TWO_HANDED),
        ranged = RangeKind.CROSSBOW, baseDurability = 250, vanillaRoleTag = null,
    )

    val LONGBOW = WeaponSpec(
        id = "longbow", displayName = "Longbow",
        category = Category.MARTIAL_RANGED, damageType = DamageType.PIERCING,
        diceText = "1d8", versatileDice = null,
        attackDamage = 6, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 1,
        properties = setOf(Property.HEAVY, Property.TWO_HANDED),
        ranged = RangeKind.BOW, baseDurability = 250, vanillaRoleTag = null,
    )

    val MUSKET = WeaponSpec(
        id = "musket", displayName = "Musket",
        category = Category.MARTIAL_RANGED, damageType = DamageType.PIERCING,
        diceText = "1d12", versatileDice = null,
        attackDamage = 8, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.LOADING, Property.TWO_HANDED),
        ranged = RangeKind.FIREARM, baseDurability = 250, vanillaRoleTag = null,
    )

    val PISTOL = WeaponSpec(
        id = "pistol", displayName = "Pistol",
        category = Category.MARTIAL_RANGED, damageType = DamageType.PIERCING,
        diceText = "1d10", versatileDice = null,
        attackDamage = 7, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.LOADING),
        ranged = RangeKind.FIREARM, baseDurability = 250, vanillaRoleTag = null,
    )

    // ===== Catalog list (38 entries) =====

    val ALL: List<WeaponSpec> = listOf(
        // Simple Melee (10)
        CLUB, DAGGER, GREATCLUB, HANDAXE, JAVELIN,
        LIGHT_HAMMER, MACE, QUARTERSTAFF, SICKLE, SPEAR,
        // Simple Ranged (4)
        DART, SLING, SHORTBOW, LIGHT_CROSSBOW,
        // Martial Melee (18)
        LONGSWORD, BATTLEAXE, FLAIL, GLAIVE, GREATAXE, GREATSWORD,
        HALBERD, LANCE, MAUL, MORNINGSTAR, PIKE, RAPIER, SCIMITAR,
        SHORTSWORD, TRIDENT, WAR_PICK, WARHAMMER, WHIP,
        // Martial Ranged (6)
        BLOWGUN, HAND_CROSSBOW, HEAVY_CROSSBOW, LONGBOW, MUSKET, PISTOL,
    )
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/kotlin/com/dndweapons/catalog/Weapons.kt
git commit -m "feat(phase-2b): expand Weapons catalog to 38 specs (34 registered + 4 vanilla-mapped)"
```

---

## Task 2: Expand `WeaponsTest.kt` with full-catalog assertions

**Files:**
- Modify: `src/test/kotlin/com/dndweapons/catalog/WeaponsTest.kt`

- [ ] **Step 1: Replace test file with expanded assertions**

Full file content for `src/test/kotlin/com/dndweapons/catalog/WeaponsTest.kt`:

```kotlin
package com.dndweapons.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WeaponsTest {

    @Test
    fun allIsNonEmpty() {
        assertFalse(Weapons.ALL.isEmpty())
    }

    @Test
    fun allHasExpectedCount() {
        assertEquals(38, Weapons.ALL.size, "expected 38 PHB weapons in catalog")
    }

    @Test
    fun registeredCount() {
        val registered = Weapons.ALL.filter { !it.isVanillaMapped }
        assertEquals(34, registered.size, "expected 34 registered (non-vanilla-mapped) specs")
    }

    @Test
    fun vanillaMappedCount() {
        val mapped = Weapons.ALL.filter { it.isVanillaMapped }
        assertEquals(4, mapped.size, "expected 4 vanilla-mapped specs")
    }

    @Test
    fun allIdsAreUnique() {
        val seen = mutableSetOf<String>()
        for (spec in Weapons.ALL) {
            assertTrue(seen.add(spec.id), "duplicate spec id: ${spec.id}")
        }
    }

    @Test
    fun categoryCountsMatch() {
        val byCategory = Weapons.ALL.groupBy { it.category }
        assertEquals(10, byCategory[Category.SIMPLE_MELEE]?.size, "simple_melee count")
        assertEquals(4, byCategory[Category.SIMPLE_RANGED]?.size, "simple_ranged count")
        assertEquals(18, byCategory[Category.MARTIAL_MELEE]?.size, "martial_melee count")
        assertEquals(6, byCategory[Category.MARTIAL_RANGED]?.size, "martial_ranged count")
    }

    @Test
    fun vanillaMappedTagsValid() {
        val expected = setOf(
            "dndweapons:role/shortsword",
            "dndweapons:role/shortbow",
            "dndweapons:role/light_crossbow",
            "dndweapons:role/trident",
        )
        val actual = Weapons.ALL
            .filter { it.isVanillaMapped }
            .map { it.vanillaRoleTag }
            .toSet()
        assertEquals(expected, actual)
    }

    @Test
    fun allExpectedIdsPresent() {
        val expected = setOf(
            // simple melee
            "club", "dagger", "greatclub", "handaxe", "javelin",
            "light_hammer", "mace", "quarterstaff", "sickle", "spear",
            // simple ranged
            "dart", "sling", "shortbow", "light_crossbow",
            // martial melee
            "longsword", "battleaxe", "flail", "glaive", "greataxe", "greatsword",
            "halberd", "lance", "maul", "morningstar", "pike", "rapier", "scimitar",
            "shortsword", "trident", "war_pick", "warhammer", "whip",
            // martial ranged
            "blowgun", "hand_crossbow", "heavy_crossbow", "longbow", "musket", "pistol",
        )
        val actual = Weapons.ALL.map { it.id }.toSet()
        assertEquals(expected, actual)
    }

    @Test
    fun longswordIsPresentAndCorrect() {
        val longsword = Weapons.ALL.first { it.id == "longsword" }
        assertEquals("Longsword", longsword.displayName)
        assertEquals(Category.MARTIAL_MELEE, longsword.category)
        assertEquals(DamageType.SLASHING, longsword.damageType)
        assertEquals(6, longsword.attackDamage)
        assertEquals(1, longsword.versatileBonus)
        assertEquals(1.5f, longsword.attackSpeed)
        assertTrue(longsword.properties.contains(Property.VERSATILE))
        assertFalse(longsword.isVanillaMapped)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/test/kotlin/com/dndweapons/catalog/WeaponsTest.kt
git commit -m "test(phase-2b): expand WeaponsTest assertions for 38-weapon catalog"
```

---

## Task 3: Smoke-test 1.21.4 unit tests pass

**Files:**
- (none — verification only)

- [ ] **Step 1: Run 1.21.4 unit tests**

```bash
./gradlew "Set active project to 1.21.4"
./gradlew :1.21.4:test
```

Expected: BUILD SUCCESSFUL. Tests run: `WeaponSpecTest` (4 tests, unchanged) + `WeaponsTest` (9 tests). 13 tests pass, 0 failures.

If any test fails, the catalog data in Task 1 has a bug. Investigate the failure message, fix `Weapons.kt`, re-run.

- [ ] **Step 2: Verify (no commit — this is a verification step)**

---

## Task 4: Write Simple Melee recipes (10) + 1.20.1 overlays

Recipe shape conventions (master design section 7):
- Sword-like vertical: `I` / `I` / `S` (handle bottom)
- Two-hander vertical: `I` / `I` / `S` for greatclub
- Throwable small: `I` / `S` for handaxe, light_hammer
- Throwable spear pattern (3x3): `. I .` / `. S .` / `. S .` for javelin, spear
- Tag references: `c:ingots/iron` = `I`, `c:rods/wooden` = `S`, `c:wood` = `W` (for greatclub head), `c:string` = `T`, `c:leather` = `L`

**Files:**
- Create: `src/main/resources/data/dndweapons/recipe/club.json`
- Create: `src/main/resources/data/dndweapons/recipe/dagger.json`
- Create: `src/main/resources/data/dndweapons/recipe/greatclub.json`
- Create: `src/main/resources/data/dndweapons/recipe/handaxe.json`
- Create: `src/main/resources/data/dndweapons/recipe/javelin.json`
- Create: `src/main/resources/data/dndweapons/recipe/light_hammer.json`
- Create: `src/main/resources/data/dndweapons/recipe/mace.json`
- Create: `src/main/resources/data/dndweapons/recipe/quarterstaff.json`
- Create: `src/main/resources/data/dndweapons/recipe/sickle.json`
- Create: `src/main/resources/data/dndweapons/recipe/spear.json`
- Create: corresponding `versions/1.20.1/src/main/resources/data/dndweapons/recipes/<id>.json` for each

- [ ] **Step 1: Write shared recipe JSONs**

For each weapon, create the file at the path above with the JSON content shown.

**`club.json`** — handle + handle (simple wooden weapon):
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["S", "S"],
  "key": { "S": { "tag": "c:rods/wooden" } },
  "result": { "id": "dndweapons:club", "count": 1 }
}
```

**`dagger.json`** — small blade + handle:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["I", "S"],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:dagger", "count": 1 }
}
```

**`greatclub.json`** — 2-block log + handle:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["W", "W", "S"],
  "key": {
    "W": { "tag": "c:wooden_logs" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:greatclub", "count": 1 }
}
```

**`handaxe.json`** — angled iron + handle:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["II", "IS", " S"],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:handaxe", "count": 1 }
}
```

**`javelin.json`** — spear-style (thrown 30/120):
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [" I ", " S ", " S "],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:javelin", "count": 1 }
}
```

**`light_hammer.json`** — small hammer head + handle:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["II", " S"],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:light_hammer", "count": 1 }
}
```

**`mace.json`** — knobbed iron head + handle:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["I", "I", "S"],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:mace", "count": 1 }
}
```

**`quarterstaff.json`** — long wooden rod:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["S", "S", "S"],
  "key": { "S": { "tag": "c:rods/wooden" } },
  "result": { "id": "dndweapons:quarterstaff", "count": 1 }
}
```

**`sickle.json`** — curved iron + handle:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [" I", "IS"],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:sickle", "count": 1 }
}
```

**`spear.json`** — spear-style (thrown 20/60, versatile):
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [" I ", " S ", " S "],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:spear", "count": 1 }
}
```

- [ ] **Step 2: Write 1.20.1 overlay recipes**

For each of the 10 weapons above, create the corresponding file at `versions/1.20.1/src/main/resources/data/dndweapons/recipes/<id>.json` with the SAME content but with `"result": { "id": ... }` replaced by `"result": { "item": ... }`. (Plural `recipes/` folder per 1.20.1 datapack schema; `"item"` key instead of `"id"`.)

Example for `versions/1.20.1/src/main/resources/data/dndweapons/recipes/club.json`:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["S", "S"],
  "key": { "S": { "tag": "c:rods/wooden" } },
  "result": { "item": "dndweapons:club", "count": 1 }
}
```

Repeat for `dagger`, `greatclub`, `handaxe`, `javelin`, `light_hammer`, `mace`, `quarterstaff`, `sickle`, `spear`.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/data/dndweapons/recipe/{club,dagger,greatclub,handaxe,javelin,light_hammer,mace,quarterstaff,sickle,spear}.json versions/1.20.1/src/main/resources/data/dndweapons/recipes/{club,dagger,greatclub,handaxe,javelin,light_hammer,mace,quarterstaff,sickle,spear}.json
git commit -m "feat(phase-2b): simple melee recipes (10 weapons) + 1.20.1 overlays"
```

---

## Task 5: Write Simple Ranged recipes (2) + 1.20.1 overlays

Only `dart` and `sling` need recipes — `shortbow` and `light_crossbow` are vanilla-mapped (no recipe).

**Files:**
- Create: `src/main/resources/data/dndweapons/recipe/dart.json`
- Create: `src/main/resources/data/dndweapons/recipe/sling.json`
- Create: `versions/1.20.1/src/main/resources/data/dndweapons/recipes/dart.json`
- Create: `versions/1.20.1/src/main/resources/data/dndweapons/recipes/sling.json`

- [ ] **Step 1: Write shared recipe JSONs**

**`dart.json`** — small thrown dart, count 4:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["I ", " S"],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:dart", "count": 4 }
}
```

**`sling.json`** — leather + string:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["T T", " L "],
  "key": {
    "T": { "tag": "c:string" },
    "L": { "tag": "c:leathers" }
  },
  "result": { "id": "dndweapons:sling", "count": 1 }
}
```

If `c:leathers` (plural common tag) doesn't exist on a given MC version, fall back to `c:leather` — at recipe-loading test time the agent verifies the tag resolves; if not, swap the singular form into both shared and overlay JSONs.

- [ ] **Step 2: Write 1.20.1 overlays**

For each: same content as shared, but `"result.id"` → `"result.item"`. Files at `versions/1.20.1/src/main/resources/data/dndweapons/recipes/{dart,sling}.json`.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/data/dndweapons/recipe/{dart,sling}.json versions/1.20.1/src/main/resources/data/dndweapons/recipes/{dart,sling}.json
git commit -m "feat(phase-2b): simple ranged recipes (dart, sling) + 1.20.1 overlays"
```

---

## Task 6: Write Martial Melee recipes (15) + 1.20.1 overlays

`longsword` already has a recipe (Phase 1). `shortsword` and `trident` are vanilla-mapped (no recipe). 15 new: battleaxe, flail, glaive, greataxe, greatsword, halberd, lance, maul, morningstar, pike, rapier, scimitar, war_pick, warhammer, whip.

**Files:**
- Create: 15 files under `src/main/resources/data/dndweapons/recipe/`
- Create: 15 files under `versions/1.20.1/src/main/resources/data/dndweapons/recipes/`

- [ ] **Step 1: Write shared recipe JSONs**

Each file uses one of three recipe shapes per master design section 7. Apply the patterns as follows:

| Weapon | Pattern | Notes |
|---|---|---|
| `battleaxe` | 2-handed axe head 2x2 + handle | sword-axe family |
| `flail` | mace head + chain (string) + handle | unique |
| `glaive` | polearm (1d10, reach) | polearm pattern |
| `greataxe` | greataxe pattern (heavy) | heavy two-hander |
| `greatsword` | greatsword pattern | heavy two-hander |
| `halberd` | polearm | polearm pattern |
| `lance` | polearm (longer) | polearm pattern |
| `maul` | huge hammer (heavy) | heavy two-hander |
| `morningstar` | spiked mace | mace-style |
| `pike` | polearm (longest) | polearm pattern |
| `rapier` | thin sword | sword-vertical |
| `scimitar` | curved sword | sword-vertical |
| `war_pick` | curved iron pick | mace-style |
| `warhammer` | hammer | mace-style |
| `whip` | leather strands | unique |

Recipe content per weapon:

**`battleaxe.json`**:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["II", "IS", " S"],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:battleaxe", "count": 1 }
}
```

**`flail.json`** — head + chain + handle:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["I", "T", "S"],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "T": { "tag": "c:string" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:flail", "count": 1 }
}
```

**`glaive.json`** — polearm:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["II ", " S ", " S "],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:glaive", "count": 1 }
}
```

**`greataxe.json`** — 2-handed axe:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["III", "IS ", " S "],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:greataxe", "count": 1 }
}
```

**`greatsword.json`** — 2-handed sword:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["I", "I", "I", "S"],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:greatsword", "count": 1 }
}
```

Note: 4-row patterns may not be supported by the vanilla 3x3 crafting table. If `:1.21.4:runGametest` at Task 11 reports recipe-loading failure for `greatsword`, swap the pattern to:
```json
"pattern": ["II", "II", " S"]
```

**`halberd.json`** — polearm:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["II ", "IS ", " S "],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:halberd", "count": 1 }
}
```

**`lance.json`** — polearm:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["  I", " S ", "S  "],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:lance", "count": 1 }
}
```

**`maul.json`** — huge hammer:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["III", "ISI", " S "],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:maul", "count": 1 }
}
```

**`morningstar.json`** — spiked mace:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["III", " I ", " S "],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:morningstar", "count": 1 }
}
```

**`pike.json`** — polearm:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [" I ", " S ", " S "],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:pike", "count": 1 }
}
```

**`rapier.json`** — thin sword:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["I", "I", "S"],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:rapier", "count": 1 }
}
```

**`scimitar.json`** — curved sword:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [" I", "I ", "S "],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:scimitar", "count": 1 }
}
```

**`war_pick.json`** — curved iron pick:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["II", "IS", " S"],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:war_pick", "count": 1 }
}
```

**`warhammer.json`** — hammer:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["III", " S ", " S "],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:warhammer", "count": 1 }
}
```

**`whip.json`** — leather strands:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["L ", "L ", "LS"],
  "key": {
    "L": { "tag": "c:leathers" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:whip", "count": 1 }
}
```

- [ ] **Step 2: Write 1.20.1 overlays**

For each of the 15 weapons, create the corresponding file at `versions/1.20.1/src/main/resources/data/dndweapons/recipes/<id>.json` with identical content except `"result.id"` → `"result.item"`.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/data/dndweapons/recipe/{battleaxe,flail,glaive,greataxe,greatsword,halberd,lance,maul,morningstar,pike,rapier,scimitar,war_pick,warhammer,whip}.json versions/1.20.1/src/main/resources/data/dndweapons/recipes/{battleaxe,flail,glaive,greataxe,greatsword,halberd,lance,maul,morningstar,pike,rapier,scimitar,war_pick,warhammer,whip}.json
git commit -m "feat(phase-2b): martial melee recipes (15 weapons) + 1.20.1 overlays"
```

---

## Task 7: Write Martial Ranged recipes (6) + 1.20.1 overlays

All 6 are registered. Patterns use bow strings + iron barrels per design.

**Files:**
- Create: 6 files under `src/main/resources/data/dndweapons/recipe/`
- Create: 6 files under `versions/1.20.1/src/main/resources/data/dndweapons/recipes/`

- [ ] **Step 1: Write shared recipe JSONs**

**`blowgun.json`** — long hollow reed:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["S", "S", "S"],
  "key": { "S": { "tag": "c:rods/wooden" } },
  "result": { "id": "dndweapons:blowgun", "count": 1 }
}
```

Note: this overlaps with `quarterstaff` (3 sticks vertical). To disambiguate, swap to `["S ", " S", "S "]` (diagonal) for blowgun.

Use the diagonal pattern for blowgun:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["S ", " S", "S "],
  "key": { "S": { "tag": "c:rods/wooden" } },
  "result": { "id": "dndweapons:blowgun", "count": 1 }
}
```

**`hand_crossbow.json`** — one-handed crossbow:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["IT ", "ST ", " S "],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "T": { "tag": "c:string" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:hand_crossbow", "count": 1 }
}
```

**`heavy_crossbow.json`** — large 2-handed crossbow:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["ITI", "STS", " I "],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "T": { "tag": "c:string" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:heavy_crossbow", "count": 1 }
}
```

**`longbow.json`** — large 2-handed bow:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [" ST", "S T", " ST"],
  "key": {
    "T": { "tag": "c:string" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:longbow", "count": 1 }
}
```

**`musket.json`** — iron barrel + wood stock:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["III", "SSI", "S  "],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:musket", "count": 1 }
}
```

**`pistol.json`** — shorter iron barrel + grip:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["II", "SI", "S "],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:pistol", "count": 1 }
}
```

- [ ] **Step 2: Write 1.20.1 overlays**

For each: identical content with `"result.id"` → `"result.item"`. Files at `versions/1.20.1/src/main/resources/data/dndweapons/recipes/{blowgun,hand_crossbow,heavy_crossbow,longbow,musket,pistol}.json`.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/data/dndweapons/recipe/{blowgun,hand_crossbow,heavy_crossbow,longbow,musket,pistol}.json versions/1.20.1/src/main/resources/data/dndweapons/recipes/{blowgun,hand_crossbow,heavy_crossbow,longbow,musket,pistol}.json
git commit -m "feat(phase-2b): martial ranged recipes (6 weapons) + 1.20.1 overlays"
```

---

## Task 8: Write 4 vanilla role tag JSONs

**Files:**
- Create: `src/main/resources/data/dndweapons/tags/items/role/shortsword.json`
- Create: `src/main/resources/data/dndweapons/tags/items/role/shortbow.json`
- Create: `src/main/resources/data/dndweapons/tags/items/role/light_crossbow.json`
- Create: `src/main/resources/data/dndweapons/tags/items/role/trident.json`

- [ ] **Step 1: Write tag files**

**`shortsword.json`** — all 6 vanilla swords:
```json
{
  "values": [
    "minecraft:wooden_sword",
    "minecraft:stone_sword",
    "minecraft:iron_sword",
    "minecraft:golden_sword",
    "minecraft:diamond_sword",
    "minecraft:netherite_sword"
  ]
}
```

**`shortbow.json`**:
```json
{ "values": ["minecraft:bow"] }
```

**`light_crossbow.json`**:
```json
{ "values": ["minecraft:crossbow"] }
```

**`trident.json`**:
```json
{ "values": ["minecraft:trident"] }
```

These tag JSONs apply to all 5 MC versions identically (item-tag schema is stable).

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/data/dndweapons/tags/items/role/{shortsword,shortbow,light_crossbow,trident}.json
git commit -m "feat(phase-2b): vanilla role tags (4 vanilla-mapped weapons)"
```

---

## Task 9: Update `en_us.json` with 37 new entries

**Files:**
- Modify: `src/main/resources/assets/dndweapons/lang/en_us.json`

- [ ] **Step 1: Replace `en_us.json` with the expanded version**

Full file content for `src/main/resources/assets/dndweapons/lang/en_us.json`:

```json
{
  "itemGroup.dndweapons.main": "DnD Weapons",

  "item.dndweapons.longsword": "Longsword",

  "item.dndweapons.club": "Club",
  "item.dndweapons.dagger": "Dagger",
  "item.dndweapons.greatclub": "Greatclub",
  "item.dndweapons.handaxe": "Handaxe",
  "item.dndweapons.javelin": "Javelin",
  "item.dndweapons.light_hammer": "Light Hammer",
  "item.dndweapons.mace": "Mace",
  "item.dndweapons.quarterstaff": "Quarterstaff",
  "item.dndweapons.sickle": "Sickle",
  "item.dndweapons.spear": "Spear",

  "item.dndweapons.dart": "Dart",
  "item.dndweapons.sling": "Sling",
  "item.dndweapons.shortbow": "Shortbow",
  "item.dndweapons.light_crossbow": "Light Crossbow",

  "item.dndweapons.battleaxe": "Battleaxe",
  "item.dndweapons.flail": "Flail",
  "item.dndweapons.glaive": "Glaive",
  "item.dndweapons.greataxe": "Greataxe",
  "item.dndweapons.greatsword": "Greatsword",
  "item.dndweapons.halberd": "Halberd",
  "item.dndweapons.lance": "Lance",
  "item.dndweapons.maul": "Maul",
  "item.dndweapons.morningstar": "Morningstar",
  "item.dndweapons.pike": "Pike",
  "item.dndweapons.rapier": "Rapier",
  "item.dndweapons.scimitar": "Scimitar",
  "item.dndweapons.shortsword": "Shortsword",
  "item.dndweapons.trident": "Trident",
  "item.dndweapons.war_pick": "War Pick",
  "item.dndweapons.warhammer": "Warhammer",
  "item.dndweapons.whip": "Whip",

  "item.dndweapons.blowgun": "Blowgun",
  "item.dndweapons.hand_crossbow": "Hand Crossbow",
  "item.dndweapons.heavy_crossbow": "Heavy Crossbow",
  "item.dndweapons.longbow": "Longbow",
  "item.dndweapons.musket": "Musket",
  "item.dndweapons.pistol": "Pistol"
}
```

38 `item.dndweapons.*` entries total: 1 existing (`longsword`) + 37 new. The `itemGroup.dndweapons.main` entry is preserved.

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/assets/dndweapons/lang/en_us.json
git commit -m "feat(phase-2b): en_us lang entries for 37 new weapons (38 total)"
```

---

## Task 10: Write 33 item model JSONs

Every registered weapon needs an item model. Vanilla-mapped weapons don't (they use the vanilla item's model).

**Files:**
- Create: 33 files under `src/main/resources/assets/dndweapons/models/item/<id>.json` for each registered weapon (excludes `longsword` which already has a model, and excludes the 4 vanilla-mapped specs)

Registered weapons needing models (33 total):
- Simple melee: club, dagger, greatclub, handaxe, javelin, light_hammer, mace, quarterstaff, sickle, spear (10)
- Simple ranged: dart, sling (2)
- Martial melee: battleaxe, flail, glaive, greataxe, greatsword, halberd, lance, maul, morningstar, pike, rapier, scimitar, war_pick, warhammer, whip (15)
- Martial ranged: blowgun, hand_crossbow, heavy_crossbow, longbow, musket, pistol (6)

- [ ] **Step 1: Write all 33 model JSONs**

Each model is identical in shape (parented to `minecraft:item/generated` with `layer0` referencing the weapon's texture). Template:

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "dndweapons:item/<id>"
  }
}
```

Write 33 files where `<id>` is replaced with the matching weapon id from the list above (e.g., `models/item/club.json` has `"layer0": "dndweapons:item/club"`).

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/assets/dndweapons/models/item/
git commit -m "feat(phase-2b): item models for 33 newly-registered weapons"
```

---

## Task 11: Per-version smoke before texture generation

This is the gate before texture wave: verify all 5 MC versions build + test + gametest pass with the expanded catalog data BUT before textures arrive. Missing-texture purple-checker is acceptable interim state.

**Files:**
- Create: `docs/plan-runner-out/phase-2b-smoke-pre-textures.md`

- [ ] **Step 1: Run chiseledBuild**

```bash
./gradlew chiseledBuild
```

Expected: 5 jars produced.

- [ ] **Step 2: Per-version test + gametest sweep**

```bash
for V in 1.20.1 1.21.1 1.21.4 1.21.11 26.1.2; do
  ./gradlew "Set active project to $V"
  ./gradlew ":$V:test" ":$V:runGametest" --rerun-tasks
done
```

Each invocation takes 30-90 seconds. Total: 5-10 minutes.

- [ ] **Step 3: Write smoke report**

Create `docs/plan-runner-out/phase-2b-smoke-pre-textures.md`:

```markdown
# Phase 2b Pre-Texture Smoke Test

**Date:** <YYYY-MM-DD HH:MM>

## Results

| Subproject | :build | :test (N/13) | :runGametest |
|---|---|---|---|
| 1.20.1  | PASS or FAIL | PASS or FAIL | PASS or FAIL |
| 1.21.1  | PASS or FAIL | PASS or FAIL | PASS or FAIL |
| 1.21.4  | PASS or FAIL | PASS or FAIL | PASS or FAIL |
| 1.21.11 | PASS or FAIL | PASS or FAIL | PASS or FAIL |
| 26.1.2  | PASS or FAIL | PASS or FAIL | PASS or FAIL |

## Notes
<paragraph: any recipe-loading warnings, missing-texture noise (expected), or per-version oddities>
```

Fill in actual results.

- [ ] **Step 4: Reset active to 1.21.4**

```bash
./gradlew "Set active project to 1.21.4"
```

- [ ] **Step 5: Commit**

```bash
git add docs/plan-runner-out/phase-2b-smoke-pre-textures.md
git commit -m "test(phase-2b): pre-texture smoke verification on 5 MC versions"
```

If any version fails build or test, STOP. Investigate before generating textures. Failures here usually mean malformed catalog data or recipe JSON — fix in place and re-run smoke.

---

## Tasks 12-17: Generate 33 textures via `minecraft-asset-generator` agent (6 parallel waves)

Split the 33 weapons into 6 batches of 5-6 weapons each. Each task dispatches the `minecraft-asset-generator` agent (per `D:/minecraft/.claude/agents/minecraft-asset-generator.md`) once per weapon in its batch. The agent uses Gemini AI Studio to produce a 16x16 PNG and writes it to the destination path.

For each weapon, the agent prompt should include:
- Weapon name and category (e.g., "Greatsword, Martial Melee weapon")
- Material hint (iron blade, wooden handle, etc., per recipe)
- Style: "Minecraft 16x16 pixel art item icon, transparent background, side view, slight isometric tilt matching vanilla Minecraft tools"
- Damage-type hint (slashing/piercing/bludgeoning maps to blade/point/blunt shape)

### Task 12: Batch 1 — 6 weapons

**Files (create):**
- `src/main/resources/assets/dndweapons/textures/item/club.png`
- `src/main/resources/assets/dndweapons/textures/item/dagger.png`
- `src/main/resources/assets/dndweapons/textures/item/greatclub.png`
- `src/main/resources/assets/dndweapons/textures/item/handaxe.png`
- `src/main/resources/assets/dndweapons/textures/item/javelin.png`
- `src/main/resources/assets/dndweapons/textures/item/light_hammer.png`

- [ ] **Step 1: Invoke `minecraft-asset-generator` agent for each weapon**

For each weapon in this batch, invoke the agent with a prompt like:

> Generate a 16x16 pixel art Minecraft item texture for "Club" — a Simple Melee bludgeoning weapon. Style: vanilla-Minecraft aesthetic, side view with slight isometric tilt, transparent background. Materials: wooden handle, knotted club head. Target path: `D:/minecraft/mods/dnd-weapons/src/main/resources/assets/dndweapons/textures/item/club.png`.

Repeat per weapon with appropriate description (consult master design section 4 Notes column for material/shape hints):
- `dagger`: small pointed iron blade, short wooden grip, piercing weapon
- `greatclub`: large 2-handed wooden club, knotted head, two-handed
- `handaxe`: small iron axe head, short wooden handle, throwable
- `javelin`: long thin spear shaft, iron point, throwable (longer than dart)
- `light_hammer`: small iron hammer head, short handle, throwable

- [ ] **Step 2: Verify each PNG exists and is non-zero**

```bash
for f in club dagger greatclub handaxe javelin light_hammer; do
  ls -la "D:/minecraft/mods/dnd-weapons/src/main/resources/assets/dndweapons/textures/item/$f.png"
done
```

If any file is missing or 0 bytes, re-invoke the agent for that weapon. If the agent fails repeatedly for a weapon, fall back to copying `longsword.png` (the existing Phase 1 placeholder) into that weapon's path:

```bash
cp "D:/minecraft/mods/dnd-weapons/src/main/resources/assets/dndweapons/textures/item/longsword.png" "D:/minecraft/mods/dnd-weapons/src/main/resources/assets/dndweapons/textures/item/<weapon>.png"
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/assets/dndweapons/textures/item/{club,dagger,greatclub,handaxe,javelin,light_hammer}.png
git commit -m "feat(phase-2b): textures batch 1 (6 simple-melee weapons)"
```

### Task 13: Batch 2 — 6 weapons

**Files (create):**
- `mace.png`, `quarterstaff.png`, `sickle.png`, `spear.png`, `dart.png`, `sling.png`

- [ ] **Step 1: Generate textures**

Invoke the agent per weapon with prompts:
- `mace`: iron flanged mace head, wooden handle, bludgeoning
- `quarterstaff`: long thin wooden pole, two-handed
- `sickle`: curved iron blade, short wooden handle, slashing/farming aesthetic
- `spear`: long wooden shaft, iron point, thrown/versatile
- `dart`: very short throwing dart, iron tip, small piercing
- `sling`: leather pouch + 2 strings, ranged

- [ ] **Step 2: Verify + fall back to longsword.png if needed**

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/assets/dndweapons/textures/item/{mace,quarterstaff,sickle,spear,dart,sling}.png
git commit -m "feat(phase-2b): textures batch 2 (mace + remaining simple melee + simple ranged)"
```

### Task 14: Batch 3 — 6 weapons

**Files (create):**
- `battleaxe.png`, `flail.png`, `glaive.png`, `greataxe.png`, `greatsword.png`, `halberd.png`

- [ ] **Step 1: Generate textures**

- `battleaxe`: 2-handed iron axe head, wooden handle, versatile
- `flail`: iron ball-on-chain, wooden handle, bludgeoning
- `glaive`: polearm with iron blade on long wooden shaft, slashing, reach
- `greataxe`: large 2-handed iron axe, heavy
- `greatsword`: large 2-handed iron sword, heavy, slashing
- `halberd`: polearm with iron axe-head + spike, reach

- [ ] **Step 2: Verify + fall back**

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/assets/dndweapons/textures/item/{battleaxe,flail,glaive,greataxe,greatsword,halberd}.png
git commit -m "feat(phase-2b): textures batch 3 (martial melee 1/3)"
```

### Task 15: Batch 4 — 6 weapons

**Files (create):**
- `lance.png`, `maul.png`, `morningstar.png`, `pike.png`, `rapier.png`, `scimitar.png`

- [ ] **Step 1: Generate textures**

- `lance`: long wooden pole + iron tip, mounted-combat reach
- `maul`: huge 2-handed iron hammer, heavy bludgeoning
- `morningstar`: spiked iron ball + handle, piercing
- `pike`: very long wooden pole + small iron tip, reach + heavy
- `rapier`: thin iron sword, slim guard, piercing/finesse
- `scimitar`: curved iron blade, light + finesse, slashing

- [ ] **Step 2: Verify + fall back**

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/assets/dndweapons/textures/item/{lance,maul,morningstar,pike,rapier,scimitar}.png
git commit -m "feat(phase-2b): textures batch 4 (martial melee 2/3)"
```

### Task 16: Batch 5 — 6 weapons

**Files (create):**
- `war_pick.png`, `warhammer.png`, `whip.png`, `blowgun.png`, `hand_crossbow.png`, `heavy_crossbow.png`

- [ ] **Step 1: Generate textures**

- `war_pick`: curved iron pick head, wooden handle, piercing
- `warhammer`: iron hammer head, longer than mace, versatile
- `whip`: long coiled leather strap, finesse + reach
- `blowgun`: long hollow wooden tube, two-handed
- `hand_crossbow`: small wooden crossbow + iron prod + string, one-handed
- `heavy_crossbow`: large wooden crossbow + iron prod + string, two-handed, heavy

- [ ] **Step 2: Verify + fall back**

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/assets/dndweapons/textures/item/{war_pick,warhammer,whip,blowgun,hand_crossbow,heavy_crossbow}.png
git commit -m "feat(phase-2b): textures batch 5 (martial melee 3/3 + martial ranged 1/2)"
```

### Task 17: Batch 6 — 3 weapons

**Files (create):**
- `longbow.png`, `musket.png`, `pistol.png`

- [ ] **Step 1: Generate textures**

- `longbow`: large wooden bow + string, two-handed, heavy
- `musket`: long iron barrel + wooden stock, flintlock firearm, two-handed
- `pistol`: short iron barrel + wooden grip, flintlock firearm

- [ ] **Step 2: Verify + fall back**

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/assets/dndweapons/textures/item/{longbow,musket,pistol}.png
git commit -m "feat(phase-2b): textures batch 6 (martial ranged 2/2)"
```

---

## Task 18: Final 5-version verification

**Files:**
- Create: `docs/plan-runner-out/phase-2b-verification.md`

- [ ] **Step 1: chiseledBuild**

```bash
./gradlew chiseledBuild
```

Expected: 5 jars produced. Sizes will be larger than pre-Phase-2b (more recipes, models, textures bundled).

- [ ] **Step 2: Per-version test + gametest sweep**

```bash
for V in 1.20.1 1.21.1 1.21.4 1.21.11 26.1.2; do
  ./gradlew "Set active project to $V"
  ./gradlew ":$V:test" ":$V:runGametest" --rerun-tasks
done
```

- [ ] **Step 3: Write verification report**

Create `docs/plan-runner-out/phase-2b-verification.md`:

```markdown
# Phase 2b Final Verification — Catalog Expansion to 38 Weapons

**Date:** <YYYY-MM-DD HH:MM>

## Build / Test / Gametest matrix

| Subproject | :build | :test | :runGametest |
|---|---|---|---|
| 1.20.1  | PASS or FAIL | PASS or FAIL (N/13) | PASS or FAIL |
| 1.21.1  | PASS or FAIL | PASS or FAIL (N/13) | PASS or FAIL |
| 1.21.4  | PASS or FAIL | PASS or FAIL (N/13) | PASS or FAIL |
| 1.21.11 | PASS or FAIL | PASS or FAIL (N/13) | PASS or FAIL |
| 26.1.2  | PASS or FAIL | PASS or FAIL (N/13) | PASS or FAIL |

## chiseledBuild jars

| Subproject | Jar | Size |
|---|---|---|
| 1.20.1  | versions/1.20.1/build/libs/dndweapons-0.1.0+mc1.20.1.jar | N bytes |
| 1.21.1  | ... | ... |
| 1.21.4  | ... | ... |
| 1.21.11 | ... | ... |
| 26.1.2  | ... | ... |

## Catalog summary
- 38 specs total (34 registered + 4 vanilla-mapped)
- 33 new shared recipes + 33 1.20.1 overlays
- 4 role tag JSONs
- 37 new lang entries (+1 existing longsword = 38 item names)
- 33 new model JSONs (+1 existing longsword model)
- 33 new texture PNGs (+1 existing longsword texture)

## Notes
<paragraph: anything notable — failed texture generations that fell back to longsword.png, recipe-schema warnings, etc.>

## Phase 2b status
Catalog expansion: <COMPLETE or PARTIAL>. All 5 MC versions ship the 38-weapon catalog from one source tree.
```

Fill in actual results.

- [ ] **Step 4: Tag the phase**

```bash
git tag -a phase-2b-catalog-expansion -m "Phase 2b: 38-weapon catalog expansion (34 registered + 4 vanilla-mapped) across 5 MC versions"
```

- [ ] **Step 5: Reset Stonecutter active to 1.21.4 (vcsVersion)**

```bash
./gradlew "Set active project to 1.21.4"
```

- [ ] **Step 6: Commit + push**

```bash
git add docs/plan-runner-out/phase-2b-verification.md
git commit -m "test(phase-2b): final verification across 5 MC versions"
git push origin main
git push origin phase-2b-catalog-expansion
```

---

## Done

Phase 2b complete. The mod now ships the full 38-weapon PHB catalog across all 5 MC versions from one source tree. Phase 3 (combat hooks + tooltip injection + `SpecRegistry` runtime lookup for vanilla mappings) is next.
