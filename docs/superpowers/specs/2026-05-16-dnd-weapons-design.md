# DnD Weapons — Design Specification

**Date**: 2026-05-16
**Status**: Approved (brainstorm complete, ready for implementation planning)
**Author**: brainstorm session with the user
**Implementation plan**: TBD (see `writing-plans` skill)

---

## 1. Overview

A Fabric Minecraft mod that adds the simple and martial weapons from the 2024 *Dungeons & Dragons Player's Handbook* (5.5e) to Minecraft, translated into vanilla-feeling mechanics. Supports five MC versions from a single codebase: **1.20.1, 1.21.1, 1.21.4, 26.1.2, and 1.21.11**.

### Goals
- Add all 38 simple/martial weapons from the 2024 PHB.
- Preserve vanilla Minecraft's look, feel, and balance language.
- Translate DnD weapon properties (Reach, Versatile, Heavy, Finesse, Light, Two-Handed, Thrown, Ammunition, Loading, Special) into mechanics that fit Minecraft.
- Single codebase across five MC versions; gracefully degrade material recipes when materials are missing from a version, and light up automatically if a mod adds them.
- Player-facing wiki on GitHub Wiki, auto-generated from the source-of-truth weapon catalog.

### Non-goals (v1.0)
- Magic weapons (only base PHB simple/martial weapons).
- DnD 5.5e Mastery properties (Cleave, Graze, Nick, Push, Sap, Slow, Topple, Vex).
- Per-mob damage-type vulnerabilities (Slashing/Piercing/Bludgeoning).
- Mob spawn-with-weapon behavior changes (mobs still wield vanilla weapons; only drop tables modified).
- Datapack-extensible weapon catalog.
- Localization beyond `en_us`.

---

## 2. Decision Log

| # | Decision | Choice | Rationale |
|---|---|---|---|
| 1 | Translation depth | Hybrid (core properties only; no Mastery) | Vanilla feel + DnD authenticity balance |
| 2 | Materials & tiering | One canonical weapon per DnD entry, plus optional material upgrade system | DnD-faithful base, MC progression layered on top |
| 3 | Upgrade mechanic | Smithing table + custom templates | Mirrors vanilla netherite upgrade; works on all target versions |
| 4 | Upgrade ladder | Iron base -> Diamond -> Netherite | All materials exist in every target version; no gating needed |
| 5 | Acquisition methods | All four: crafting, structure loot, villager trades, mob drops | Maximum discovery surface |
| 6 | Tooltip style | Italic DnD stat-block line above vanilla attribute lines | Combines DnD info + vanilla layout |
| 7 | Multi-version build tool | Stonecutter (Kikugie) | De-facto standard; only Fabric-loader-only solution that fits |
| 8 | Heavy property mechanic | +1 knockback level; slow swing already in base stats | Real trade-off without movement penalty |
| 9 | Finesse property mechanic | +20% damage when attacker is sprinting | Vanilla-adjacent; rewards mobile play |
| 10 | Light property mechanic | +1 mainhand damage when offhand also holds a Light weapon | Dual-wield bonus without breaking rclick semantics |
| 11 | Lance Special mechanic | Damage halved when on foot | DnD-faithful penalty |
| 12 | Damage Type mechanic | Tooltip flavor only; no combat effect | Preserves vanilla feel |
| 13 | Vanilla-mapped weapons | Shortsword, Shortbow, Light Crossbow, Trident | Avoid duplicate items; vanilla fills the role |
| 14 | Vanilla mapping treatment | Tooltip injection + combat hooks; smithing upgrade NOT applied | Vanilla material tier system handles their progression |
| 15 | Wiki platform | GitHub Wiki | Free, integrated, easy to clone/sync |
| 16 | Wiki content | Auto-generated weapon pages + hand-authored overview pages | Single source of truth from `Weapons.kt` |

---

## 3. Foundation

### Mod identity
- **Mod ID**: `dndweapons`
- **Display name**: "DnD Weapons"
- **Namespace** (items, recipes, tags): `dndweapons`
- **Java package**: `com.dndweapons`
- **License**: MIT (placeholder; review with user)
- **Target loader**: Fabric (only)
- **Target MC versions**: `1.20.1`, `1.21.1`, `1.21.4`, `26.1.2`, `1.21.11`

### Tech stack
| Concern | Choice | Notes |
|---|---|---|
| Mod source language | Kotlin | Pivoted from Java on 2026-05-16; FLK (`fabric-language-kotlin`) handles entrypoint adapter |
| Multi-version build | Stonecutter | Single `src/`, comment directives, one Gradle subproject per MC version |
| Fabric build plugin | Fabric Loom 1.15+ | Loom version pinned per subproject |
| Mappings | Yarn for legacy branches, Mojang mappings for 26.1+ (forced) | Stonecutter abstracts |
| Java release target | 17 for 1.20.1; 21 for everything else | JDK to build with is always 21 |
| Recipe gating | Fabric Recipe Conditions (FRC) | `fabric:tags_populated` keeps recipes inert when tags empty |
| Material references | `c:` Common item tags | `c:ingots/iron`, `c:gems/diamond`, etc. |
| Reach attribute | Vanilla `entity_interaction_range` on 1.20.5+; Reach Entity Attributes lib on 1.20.1 only | Only runtime dep beyond Fabric API |
| Loot injection | `LootTableEvents` v2 on <=1.21; v3 on 1.21.1+ | Stonecutter forks one shim file |
| Villager trades | `TradeOfferHelper` legacy on 1.20.1-1.21.4; new `RegistryKey` signature on 1.21.5+; data-driven JSON on 26.1+ | Three forks |
| DnD source | 2024 PHB (5.5e) weapon stat blocks | Mastery NOT implemented |

### Project structure (Stonecutter layout)

```
dnd-weapons/
  stonecutter.gradle.kts            # active version + versions list
  settings.gradle.kts                # Stonecutter plugin
  build.gradle.kts                   # shared Loom config
  versions/                          # auto-generated, one per MC target
    1.20.1/
    1.21.1/
    1.21.4/
    26.1.2/
    1.21.11/
  src/main/kotlin/com/dndweapons/
    DndWeaponsMod.kt                 # mod init
    catalog/
      Weapons.kt                     # ALL 34 weapon specs + 4 vanilla-mapped role specs (Kotlin object)
      WeaponSpec.kt                  # data class
      Property.kt                    # enum: REACH, TWO_HANDED, THROWN, VERSATILE, ...
      DamageType.kt                  # SLASHING, PIERCING, BLUDGEONING
      Category.kt                    # SIMPLE_MELEE, SIMPLE_RANGED, MARTIAL_MELEE, MARTIAL_RANGED
      RangeKind.kt                   # NONE, THROWN, BOW, CROSSBOW, FIREARM, SLING, BLOWGUN
    registry/
      WeaponRegistrar.kt             # interface
      WeaponRegistrarImpl.kt         # thin; delegates to AttributeCompat
    compat/
      AttributeCompat.kt             # //? UUID-vs-Identifier modifier shim
      ReachCompat.kt                 # vanilla vs Reach Entity Attributes
      TradeCompat.kt                 # three trade flavors via //?
      LootCompat.kt                  # loot v2/v3 shim via //?
      TooltipCompat.kt               # 1.20.x appendTooltip vs 1.21+ data-component approach
    item/
      DndWeaponItem.kt               # base class
      DndThrownWeaponItem.kt         # extends/wraps trident
      DndBowItem.kt
      DndCrossbowItem.kt
      DndSlingItem.kt
      DndBlowgunItem.kt
      DndFirearmItem.kt
    combat/
      WeaponAttackHandler.kt         # single AttackEntityCallback subscriber; modifyDamage()
    tooltip/
      WeaponTooltipInjector.kt       # ItemTooltipCallback; works for registered + vanilla-mapped
    ModCompat.kt                     # isModLoaded helpers
  src/main/resources/
    fabric.mod.json                  # //? per-version mod metadata
    assets/dndweapons/               # textures + en_us.json + models
    data/dndweapons/
      recipe/                        # crafting + smithing JSONs (with FRC conditions)
      loot_table/                    # injected entries
      tags/items/                    # role tags + property tags
      trade/                         # 26.1+ data-driven trades
  src/test/java/                     # JVM unit tests
  src/gametest/java/                 # Fabric gametest suites
  buildSrc/                          # wiki generator Gradle task
  wiki/handwritten/                  # hand-authored wiki pages
  docs/superpowers/specs/            # this document + future plans
```

---

## 4. Weapon Catalog & Translation Rules

### Translation rules

| DnD stat | MC translation |
|---|---|
| Average damage of dice | `attack_damage = average + 1.5`, rounded (calibrated to vanilla iron sword) |
| Damage dice (e.g., 1d8) | Single static value in MC; no dice rolling per hit |
| Damage type (S/P/B) | Tooltip-only; no combat effect |
| Light | +1 mainhand damage when offhand also holds a Light weapon |
| Heavy | +1 knockback level baked in; slow swing already in attack speed |
| Finesse | +20% damage when attacker is sprinting at moment of hit |
| Reach | `entity_interaction_range` +1.0 block (3.0 -> 4.0) |
| Two-Handed | Blocks offhand item while held |
| Versatile (1dX) | Damage uses larger die when offhand is empty (checked at hit) |
| Thrown | Right-click throws like trident; retrievable |
| Ammunition | Bow/crossbow behavior; uses arrows or bolts |
| Loading | Cooldown after firing (use cooldown manager) |
| Special (Lance) | Damage halved when on foot; normal when mounted |

### Attack speed buckets

| Bucket | Speed | Members |
|---|---|---|
| Very fast | 1.8 | Dagger, Sickle, Scimitar, Shortsword (vanilla), Dart, Whip |
| Fast | 1.6 | Rapier, Light Hammer, Handaxe, Club |
| Normal | 1.5 | Longsword, Mace, Warhammer, Morningstar, War Pick, Battleaxe, Quarterstaff, Spear, Javelin, Flail, Trident (vanilla) |
| Slow polearm | 1.0 | Glaive, Halberd, Pike, Lance |
| Slow heavy | 0.9 | Greatsword, Greataxe, Maul, Greatclub |
| Ranged | (vanilla draw mechanics) | All bows, crossbows, sling, blowgun, firearms |

### Full catalog

**Simple Melee (10 weapons)** — all registered

| Weapon | DnD | MC dmg | Speed | Reach | Notes |
|---|---|---|---|---|---|
| Club | 1d4 B, Light | 4 | 1.6 | - | offhand ok |
| Dagger | 1d4 P, Light, Thrown 20/60 | 4 | 1.8 | - | throwable |
| Greatclub | 1d8 B, Two-Handed | 6 | 0.9 | - | offhand blocked |
| Handaxe | 1d6 S, Light, Thrown 20/60 | 5 | 1.6 | - | throwable |
| Javelin | 1d6 P, Thrown 30/120 | 5 | 1.5 | - | throwable, longer |
| Light Hammer | 1d4 B, Light, Thrown 20/60 | 4 | 1.6 | - | throwable |
| Mace | 1d6 B | 5 | 1.5 | - | - |
| Quarterstaff | 1d6 B, Versatile (1d8) | 5 / 6 versatile | 1.5 | - | +1 dmg if offhand empty |
| Sickle | 1d4 S, Light | 4 | 1.8 | - | offhand ok |
| Spear | 1d6 P, Thrown 20/60, Versatile (1d8) | 5 / 6 versatile | 1.5 | - | throwable + versatile |

**Simple Ranged (4 weapons)** — 1 vanilla-mapped (Shortbow), 3 registered

| Weapon | DnD | Mapping / Behavior |
|---|---|---|
| Dart | 1d4 P, Finesse, Thrown | Registered. Throwable like snowball, stacks |
| Light Crossbow | 1d8 P, Loading, Two-Handed | Vanilla-mapped (vanilla crossbow fills role) |
| Shortbow | 1d6 P, Two-Handed | Vanilla-mapped (vanilla bow fills role) |
| Sling | 1d4 B, Ammunition | Registered. Fires pebbles/balls, low damage |

**Martial Melee (18 weapons)** — 1 vanilla-mapped (Shortsword), 17 registered

| Weapon | DnD | MC dmg | Speed | Reach | Notes |
|---|---|---|---|---|---|
| Battleaxe | 1d8 S, Versatile (1d10) | 6 / 7 versatile | 1.5 | - | - |
| Flail | 1d8 B | 6 | 1.5 | - | - |
| Glaive | 1d10 S, Heavy, Reach, Two-Handed | 7 | 1.0 | +1 | polearm |
| Greataxe | 1d12 S, Heavy, Two-Handed | 8 | 0.9 | - | offhand blocked |
| Greatsword | 2d6 S, Heavy, Two-Handed | 9 | 0.9 | - | offhand blocked |
| Halberd | 1d10 S, Heavy, Reach, Two-Handed | 7 | 1.0 | +1 | polearm |
| Lance | 1d10 P, Heavy, Reach, Two-Handed, Special | 7 | 1.0 | +1 | half dmg on foot |
| Longsword | 1d8 S, Versatile (1d10) | 6 / 7 versatile | 1.5 | - | flagship one-hander |
| Maul | 2d6 B, Heavy, Two-Handed | 9 | 0.9 | - | offhand blocked |
| Morningstar | 1d8 P | 6 | 1.5 | - | - |
| Pike | 1d10 P, Heavy, Reach, Two-Handed | 7 | 1.0 | +1 | polearm |
| Rapier | 1d8 P, Finesse | 6 | 1.6 | - | one-handed precision |
| Scimitar | 1d6 S, Finesse, Light | 5 | 1.8 | - | offhand ok |
| Shortsword | 1d6 P, Finesse, Light | 5 | 1.8 | - | **vanilla-mapped (vanilla sword)** |
| Trident | 1d8 P, Thrown 20/60, Versatile (1d10) | 6 / 7 versatile | 1.5 | - | **vanilla-mapped (vanilla trident)** |
| War Pick | 1d8 P | 6 | 1.5 | - | - |
| Warhammer | 1d8 B, Versatile (1d10) | 6 / 7 versatile | 1.5 | - | - |
| Whip | 1d4 S, Finesse, Reach | 4 | 1.8 | +1 | - |

**Martial Ranged (6 weapons)** — all registered

| Weapon | DnD | Behavior |
|---|---|---|
| Blowgun | 1 P, Loading | Single-dart, ~2 dmg, long cooldown |
| Hand Crossbow | 1d6 P, Light, Loading | One-handed crossbow |
| Heavy Crossbow | 1d10 P, Heavy, Loading, Two-Handed | Strongest crossbow, ~2s reload |
| Longbow | 1d8 P, Heavy, Two-Handed | Strongest bow |
| Musket | 1d12 P, Loading, Two-Handed | Firearm; ammo = lead ball (iron-nugget recipe) |
| Pistol | 1d10 P, Loading | Firearm; ammo = lead ball |

**Totals**: 38 PHB weapons. 4 vanilla-mapped (Shortsword, Shortbow, Light Crossbow, Trident). **34 registered items**.

### Damage formula

`attack_damage = round(average_dice + 1.5)`. Calibrated so a 1d8 DnD Longsword equals vanilla iron sword (6 damage).

| Dice | Average | MC damage |
|---|---|---|
| 1 | 1.0 | 2 |
| 1d4 | 2.5 | 4 |
| 1d6 | 3.5 | 5 |
| 1d8 | 4.5 | 6 |
| 1d10 | 5.5 | 7 |
| 1d12 | 6.5 | 8 |
| 2d6 | 7.0 | 9 |

---

## 5. Data Model & Per-Epoch Registration Layer

### `WeaponSpec` data class — single source of truth

```kotlin
data class WeaponSpec(
    val id: String,                    // "longsword"
    val displayName: String,           // "Longsword"
    val category: Category,
    val damageType: DamageType,
    val diceText: String,              // "1d8"
    val versatileDice: String?,        // "1d10" or null
    val attackDamage: Int,
    val versatileBonus: Int,           // +1 when offhand empty; 0 if not versatile
    val attackSpeed: Float,
    val reachBonus: Float,             // 0.0 or 1.0
    val knockbackBonus: Int,           // 0 or 1 (for Heavy)
    val properties: Set<Property>,
    val ranged: RangeKind,             // NONE, THROWN, BOW, CROSSBOW, FIREARM, SLING, BLOWGUN
    val baseDurability: Int,           // 250 iron-tier baseline
    val vanillaRoleTag: String?        // nullable; e.g., "dndweapons:role/shortsword"
                                       // when non-null, NO item is registered;
                                       // the named tag determines which vanilla items
                                       // get DnD treatment (lookup happens at runtime)
) {
    init {
        require(id.isNotBlank()) { "WeaponSpec id must be non-blank" }
        require(displayName.isNotBlank()) { "WeaponSpec displayName must be non-blank" }
    }

    val isVanillaMapped: Boolean get() = vanillaRoleTag != null
}
```

Note: `vanillaRoleTag` is a `String`, not an `Item` or `TagKey<Item>`. On 26.1+, item/registry references made during static initialization would crash (registries are only available after world load). String lookup is deferred to first use.

### `Weapons.kt` — example entries

```kotlin
object Weapons {
    val LONGSWORD = WeaponSpec(
        id = "longsword", displayName = "Longsword",
        category = MARTIAL_MELEE, damageType = SLASHING,
        diceText = "1d8", versatileDice = "1d10",
        attackDamage = 6, versatileBonus = 1,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(VERSATILE),
        ranged = RangeKind.NONE, baseDurability = 250,
        vanillaRoleTag = null,
    )

    val SHORTSWORD = WeaponSpec(
        id = "shortsword", displayName = "Shortsword",
        category = MARTIAL_MELEE, damageType = PIERCING,
        diceText = "1d6", versatileDice = null,
        attackDamage = 5, versatileBonus = 0,
        attackSpeed = 1.8f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(LIGHT, FINESSE),
        ranged = RangeKind.NONE, baseDurability = 250,
        vanillaRoleTag = "dndweapons:role/shortsword",   // vanilla-mapped; tag lists vanilla swords
    )

    val GREATSWORD = WeaponSpec(
        id = "greatsword", displayName = "Greatsword",
        category = MARTIAL_MELEE, damageType = SLASHING,
        diceText = "2d6", versatileDice = null,
        attackDamage = 9, versatileBonus = 0,
        attackSpeed = 0.9f, reachBonus = 0.0f, knockbackBonus = 1,
        properties = setOf(HEAVY, TWO_HANDED),
        ranged = RangeKind.NONE, baseDurability = 250,
        vanillaRoleTag = null,
    )

    val LANCE = WeaponSpec(
        id = "lance", displayName = "Lance",
        category = MARTIAL_MELEE, damageType = PIERCING,
        diceText = "1d10", versatileDice = null,
        attackDamage = 7, versatileBonus = 0,
        attackSpeed = 1.0f, reachBonus = 1.0f, knockbackBonus = 1,
        properties = setOf(HEAVY, REACH, TWO_HANDED, SPECIAL_LANCE),
        ranged = RangeKind.NONE, baseDurability = 250,
        vanillaRoleTag = null,
    )

    val ALL: List<WeaponSpec> = listOf(/* all 38 */)
}
```

Note: `vanillaRoleTag` is stored as a String. The actual tag and its member items are resolved lazily at first lookup, which keeps the catalog safe to initialize before world load on 26.1+.

### Per-epoch registration

`WeaponRegistrar.kt` (interface, version-independent):
```kotlin
interface WeaponRegistrar {
    fun register(spec: WeaponSpec)
    fun registerAll(specs: List<WeaponSpec>) {
        specs.forEach(::register)
    }
}
```

`WeaponRegistrarImpl.kt` (thin; per-epoch logic delegated to `AttributeCompat`):
```kotlin
class WeaponRegistrarImpl : WeaponRegistrar {
    override fun register(spec: WeaponSpec) {
        if (spec.isVanillaMapped) {
            // Vanilla-mapped: no item registered.
            // SpecRegistry will later resolve the named tag to its items
            // and route lookups (combat hooks, tooltip injector) through it.
            SpecRegistry.bindRoleTag(spec)
            return
        }

        val itemId = Identifier.of(DndWeaponsMod.MOD_ID, spec.id)
        val settings = AttributeCompat.applyTo(Item.Settings(), spec)
        val item = createItemForSpec(spec, settings)
        Registry.register(Registries.ITEM, itemId, item)
    }
}
```

Per-epoch logic lives in a single `compat/AttributeCompat.kt` with Stonecutter `//?` directives selecting between epochs:
- Epoch A (`MC < 1.20.5`): UUID-keyed modifiers; `DndWeaponItem.getAttributeModifiers` override returns a `Multimap`
- Epoch B (`MC >= 1.20.5 && MC < 1.21`): UUID-keyed in `DataComponentTypes.ATTRIBUTE_MODIFIERS` (unused — no current targets)
- Epoch C (`MC >= 1.21`): Identifier-keyed in `DataComponentTypes.ATTRIBUTE_MODIFIERS`

Naming like "AttributeCompat_v1201/v1205/v121" is conceptual; the physical layout collapses to one file with `//?` directives — that's the Stonecutter-native pattern.

### Item subclass tree

| Class | Used by | Behavior |
|---|---|---|
| `DndWeaponItem : Item` | All melee | Attack damage/speed/reach via settings; Heavy knockback; Versatile/Finesse/Light hooks |
| `DndThrownWeaponItem : TridentItem` | Dagger, Handaxe, Javelin, Light Hammer, Spear | Right-click throws; retrievable. Stonecutter-forked (TridentItem changed) |
| `DndBowItem : BowItem` | Longbow | Vanilla bow behavior; max damage capped by spec |
| `DndCrossbowItem : CrossbowItem` | Heavy Crossbow, Hand Crossbow | Vanilla crossbow + per-spec reload time |
| `DndSlingItem : Item` | Sling | Custom — fires pebbles, no draw |
| `DndBlowgunItem : Item` | Blowgun | Custom — fires darts |
| `DndFirearmItem : Item` | Musket, Pistol | Custom — fires lead balls, slow reload |

### Combat-hook handler

One file, ~80 lines, handles every runtime damage modifier:

```kotlin
object WeaponAttackHandler {
    fun modifyDamage(attacker: PlayerEntity, target: Entity, base: Float, mainhand: WeaponSpec): Float {
        var dmg = base
        if (VERSATILE in mainhand.properties && attacker.offHandStack.isEmpty) {
            dmg += mainhand.versatileBonus
        }
        if (FINESSE in mainhand.properties && attacker.isSprinting) {
            dmg *= 1.20f
        }
        if (LIGHT in mainhand.properties) {
            val off = attacker.offHandStack
            val offSpec = SpecRegistry.lookup(off.item)
            if (offSpec != null && LIGHT in offSpec.properties) {
                dmg += 1.0f
            }
        }
        if (SPECIAL_LANCE in mainhand.properties && !attacker.hasVehicle()) {
            dmg *= 0.50f
        }
        return dmg
    }
}
```

Wired into `AttackEntityCallback.EVENT.register(...)` in `DndWeaponsMod.onInitialize()`. `SpecRegistry.lookup(Item)` checks the item registry AND the role tags — so the same handler fires for registered DnD weapons and for vanilla-mapped items.

### Tooltip injection

`WeaponTooltipInjector` registers an `ItemTooltipCallback` (Fabric API, all 5 versions). For any item that has a spec (registered or via role tag), it prepends the DnD stat-block line: `1d8 slashing · Versatile (1d10)`. For weapons with conditional bonuses (Finesse, Lance Special), appends an italic line like `+20% damage while sprinting`.

---

## 6. Vanilla-Mapped Weapons

### The four mappings
| DnD weapon | Vanilla item | Match |
|---|---|---|
| Shortsword | All vanilla swords (6 material tiers) | 1d6 P, Finesse, Light |
| Shortbow | Vanilla bow | 1d6 P |
| Light Crossbow | Vanilla crossbow | 1d8 P, Loading, Two-Handed |
| Trident | Vanilla trident | 1d8 P, Versatile, Thrown |

### How they work
- **No new item registered.** Vanilla item carries the DnD identity via role tag.
- **Role tags** (`dndweapons:role/shortsword` etc.) contain all vanilla item variants that fill the role.
- **`SpecRegistry.lookup(Item)`** checks the item registry first; if not found, scans role tags and returns the matching `WeaponSpec`.
- **Combat hooks** (sprint bonus, dual-Light bonus, Versatile) fire on vanilla items via the same `WeaponAttackHandler`.
- **Tooltip injection** prepends the DnD stat block via `ItemTooltipCallback`.
- **Smithing upgrade ladder does NOT apply** — vanilla items keep their vanilla material progression (wood -> stone -> iron -> diamond -> netherite).

### Role tag definitions
| Tag | Contents |
|---|---|
| `dndweapons:role/shortsword` | All 6 vanilla swords |
| `dndweapons:role/shortbow` | `minecraft:bow` |
| `dndweapons:role/light_crossbow` | `minecraft:crossbow` |
| `dndweapons:role/trident` | `minecraft:trident` |

---

## 7. Recipes, Smithing Upgrades, Material Gating

### Crafting recipes

Every registered weapon has a base crafting recipe in `data/dndweapons/recipe/<id>.json`. Vanilla-style shaped recipes using `c:` tags + FRC `fabric:tags_populated` conditions.

Example (Longsword):
```json
{
  "type": "minecraft:crafting_shaped",
  "fabric:conditions": [
    { "condition": "fabric:tags_populated", "values": ["c:ingots/iron"] }
  ],
  "pattern": ["I", "I", "S"],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": { "id": "dndweapons:longsword", "count": 1 }
}
```

Polearm pattern (Glaive, Halberd, Pike, Lance):
```
I I .
. S .
. S .
```

Throwable spear-style pattern (Javelin, Spear):
```
. I .
. S .
. S .
```

### Smithing upgrade system

Three tiers per registered weapon: **iron base -> diamond -> netherite**.

Two custom smithing templates:
- `dndweapons:diamond_weapon_upgrade_template`
- `dndweapons:netherite_weapon_upgrade_template`

Templates obtained via stronghold library loot, bastion remnant loot, wandering trader trades. Players already understand the smithing-template loop from vanilla netherite upgrades.

Upgrade recipe example (`data/dndweapons/recipe/longsword_diamond.json`):
```json
{
  "type": "minecraft:smithing_transform",
  "template": { "id": "dndweapons:diamond_weapon_upgrade_template" },
  "base": { "id": "dndweapons:longsword" },
  "addition": { "tag": "c:gems/diamond" },
  "result": { "id": "dndweapons:longsword_diamond" }
}
```

Per-tier stats:
| Tier | Damage | Durability | Knockback (if Heavy) | Other |
|---|---|---|---|---|
| Iron (base) | `spec.attackDamage` | 250 | `spec.knockbackBonus` | - |
| Diamond | base + 2 | 1561 | base | - |
| Netherite | base + 3 | 2031 | base + 1 | Fire-resistant |

`Weapons.kt` generates all three tier variants from one `WeaponSpec` programmatically. **Vanilla-mapped weapons do NOT have these upgrade variants.**

### Material gating

Every material reference uses a `c:` tag + an FRC `fabric:tags_populated` condition. If a tag is empty, the recipe silently no-ops. If a mod populates the tag at runtime, the recipe lights up.

For our 3-tier base ladder, gating is a no-op (iron/diamond/netherite exist in all targets). The pattern is in place defensively and enables **mod-conditional optional tiers**: ship a `longsword_mithril.json` gated on `c:ingots/mithril`; if any mod populates that tag, the recipe activates without code changes.

### Tag inventory

| Tag | Members | Purpose |
|---|---|---|
| `dndweapons:role/shortsword` | All 6 vanilla swords | Vanilla mapping |
| `dndweapons:role/shortbow` | `minecraft:bow` | Vanilla mapping |
| `dndweapons:role/light_crossbow` | `minecraft:crossbow` | Vanilla mapping |
| `dndweapons:role/trident` | `minecraft:trident` | Vanilla mapping |
| `dndweapons:weapons` | All registered weapons + role-tagged items | Identity check |
| `dndweapons:two_handed` | Weapons with TWO_HANDED | Offhand-block check |
| `dndweapons:light` | Weapons with LIGHT | Dual-Light check |
| `dndweapons:finesse` | Weapons with FINESSE | Sprint bonus check |
| `dndweapons:heavy` | Weapons with HEAVY | Informational |

Plus referenced `c:` material tags (not defined by us): `c:ingots/iron`, `c:gems/diamond`, `c:ingots/netherite`, `c:rods/wooden`, `c:leather`, `c:string`.

### Recipe per-epoch differences (Stonecutter-handled)

| Concern | 1.20.1 | 1.20.5-1.21.4 | 1.21.5+ / 26.1 |
|---|---|---|---|
| Recipe folder | `recipes/` | `recipe/` (renamed) | `recipe/` |
| `smithing_transform` schema | `template_item`/`base_item`/`addition` | same | same |
| Result item ID key | `"item"` | `"id"` (renamed) | `"id"` |
| FRC plugin compatibility | FRC v1 | FRC v2 | FRC v2 |

---

## 8. Acquisition (Loot, Trades, Mob Drops)

**Default acquisition is crafting** — every registered weapon is craftable from the start. Loot/trade/drop placements are for flavor.

### Structure loot

| Structure | Weapons (drop chance per chest opening) | Theme |
|---|---|---|
| Stronghold (library) | Quarterstaff, Whip, Sickle, Rapier (~10%) | Scholarly/agile |
| Stronghold (corridor) | Longsword, Mace, Battleaxe, Warhammer (~12%) | Adventurer fare |
| Woodland Mansion | Greatsword, Halberd, Rapier, Longsword (~8%) | Illager fancy |
| Nether Fortress | Pike, Maul, Greataxe, Flail (~10%) | Hellish heavy |
| Bastion Remnant (other) | Glaive, Heavy Crossbow; Diamond/Netherite templates (~7%) | Piglin armory |
| End City | Greatsword (diamond), Longsword (diamond) (~5%) | Endgame |
| Ancient City | Morningstar, War Pick, Whip (~6%) | Sculk weight |
| Desert Pyramid | Scimitar, Spear, Dart (~12%) | Ancient warrior |
| Jungle Temple | Blowgun, Sling, Dart (~10%) | Hunter/trapper |
| Shipwreck (treasure) | Pistol, Musket, Hand Crossbow, Sickle (~8%) | Pirate flavor |
| Ocean Monument | Spear (~6%) | Aquatic |
| Pillager Outpost | Heavy Crossbow, Hand Crossbow (~5%) | Pillager arms |
| Trial Chambers (1.21+) | Rapier, Longsword, Pike; smithing templates in vaults (~varies) | Combat trial |

Trial Chambers row omitted from 1.20.1 build (structure doesn't exist).

### Villager trades

**Weaponsmith**:
| Level | Trades |
|---|---|
| Novice | Mace, Sickle, Quarterstaff (3-5 emeralds) |
| Apprentice | Spear, Club, Greatclub, Light Hammer (4-7 emeralds) |
| Journeyman | Longsword, Battleaxe, Warhammer, Morningstar (8-12 emeralds + iron) |
| Expert | Glaive, Halberd, Pike, Maul (15-20 emeralds + iron) |
| Master | Greatsword, Greataxe, Lance, Rapier, Diamond Upgrade Template (22-30 emeralds) |

**Fletcher**:
| Level | Trades |
|---|---|
| Apprentice | Dart stack (5 emeralds) |
| Journeyman | Hand Crossbow (10 emeralds) |
| Expert | Heavy Crossbow, Longbow (15-22 emeralds) |
| Master | Blowgun, Musket/Pistol (30+ emeralds) |

### Mob drops (conservative)

| Mob | Weapon drop | Rate |
|---|---|---|
| Vindicator | Battleaxe | 8% |
| Pillager | Heavy Crossbow | 4% |
| Pillager (Captain) | Hand Crossbow | 15% |
| Wither Skeleton | Glaive | 6% |
| Husk | Club | 5% |
| Drowned (with trident) | Spear | 25% chance to substitute |
| Piglin Brute | Maul | 10% |
| Skeleton (stronghold) | Dagger | 2% |

Mobs do NOT spawn pre-wielding these weapons. Drops only.

### Implementation: per-epoch shims

**Loot injection** (`compat/LootCompat.kt`):
```kotlin
//? if MC >= 1.21.1 {
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
//?} else {
/*import net.fabricmc.fabric.api.loot.v2.LootTableEvents*/
//?}

object LootCompat {
    fun registerInjections() {
        LootTableEvents.MODIFY.register { key, builder, source ->
            LootInjections.forKey(key)?.let(builder::pool)
        }
    }
}
```

`LootInjections` is a single declarative table mapping vanilla loot table IDs to injected `LootPool.Builder`s. Built from the catalog.

**Villager trade injection** — three forks in one `compat/TradeCompat.kt`, selected by Stonecutter `//?` directives:
- `MC < 1.21.5`: `TradeOfferHelper.registerVillagerOffers(VillagerProfession, level, factories)`
- `1.21.5 <= MC < 26.1`: `TradeOfferHelper.registerVillagerOffers(RegistryKey<VillagerProfession>, level, factories)`
- `MC >= 26.1`: no-op (trades shipped as JSON under `data/dndweapons/trade/`)

For 26.1+, trade JSONs are generated at build time by a Gradle task that reads the same catalog.

---

## 9. Testing Strategy, Build Matrix, Release

### Three test layers

**Layer 1: JVM unit tests** (`src/test/java/`)
- `Weapons.ALL` integrity (unique IDs, valid ranges, sensible property combos)
- Damage formula correctness per dice notation
- Combat modifier outputs for representative inputs
- Tooltip string builder produces expected format
- Tooling: JUnit 5; ~50 tests; runs on every subproject build

**Layer 2: Fabric Gametest** (`src/gametest/java/`)
- Mod inits without throwing on each version
- All expected item IDs present in `Registries.ITEM`
- Sprint with Rapier -> 1.20x damage
- Two daggers -> +1 damage
- Lance on foot vs mounted -> 0.5x vs 1.0x damage
- Smithing recipe: iron longsword + diamond + template -> diamond longsword
- Recipe JSON loads without errors
- Loot injection observable (open stronghold chest in N rolls)
- Vanilla mapping: vanilla iron sword carries DnD tooltip + sprint bonus fires
- Run via `./gradlew runGametest`; ~15-20 tests; ~1 min per version

**Layer 3: Manual playtest checklist** (`docs/superpowers/specs/playtest-checklist.md`)
- Per-version walkthrough before release
- Visual confirmations (textures, models, tooltip layout, sounds)

### Build matrix

```yaml
jobs:
  build:
    strategy:
      matrix:
        mc: [1.20.1, 1.21.1, 1.21.4, 26.1.2, 1.21.11]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 21 }
      - run: ./gradlew "Set active project" -PccVersion=${{ matrix.mc }}
      - run: ./gradlew :versions:${{ matrix.mc }}:build
      - run: ./gradlew :versions:${{ matrix.mc }}:runGametest
      - uses: actions/upload-artifact@v4
        with:
          name: jar-${{ matrix.mc }}
          path: versions/${{ matrix.mc }}/build/libs/*.jar
```

Build JDK is 21 across the board. 1.20.1's `javaCompile.options.release` and Kotlin `jvmTarget` are pinned to 17 per its `gradle.properties`; all other subprojects target 21. Local one-shot: `./gradlew chiseledBuild`.

### Release & distribution

- **Targets**: Modrinth (primary), CurseForge (broader audience)
- **Versioning**: `1.0.0+mc1.20.1`, `1.0.0+mc1.21.4`, etc.
- **Automation**: `modrinth/minotaur` for Modrinth; `CurseForgeGradle` for CurseForge; wired into CI on `v*` tags
- **Per-release**: JAR + SHA256 + changelog + dependency declarations

### Update strategy

When a new MC version drops:
1. Add to `stonecutter.gradle.kts` versions list
2. Run `./gradlew chiseledBuild`
3. Add `//? if MC >= NEW_VERSION` directives at any new breakage points
4. Run gametests + manual checklist
5. Tag a release

The catalog (`Weapons.kt`) only changes if WotC publishes new PHB content.

---

## 10. Player Wiki (GitHub Wiki)

### Hosting
GitHub Wiki — sidecar git repo (`<repo>.wiki.git`). Markdown pages, sidebar/footer customization. Free and integrated with the main repo. Portable to GitLab/Gitea/Codeberg later if needed.

### Content strategy
- **Per-weapon pages: auto-generated** from `Weapons.ALL` via Gradle task
- **Narrative/overview pages: hand-authored** in `wiki/handwritten/`
- Both flow into the published wiki via one publish script
- Single source of truth: `Weapons.kt`

### Page layout
```
Home.md
Getting-Started.md
Combat-Mechanics.md
Smithing-Upgrade-System.md
Vanilla-Mapped-Weapons.md
Acquisition-Guide.md
Material-Gating.md
Version-Notes.md
DnD-Translation-Philosophy.md
Future-Work.md
_Sidebar.md
_Footer.md
Weapons/
  Simple-Melee.md      (category index)
  Simple-Ranged.md
  Martial-Melee.md
  Martial-Ranged.md
  Longsword.md         (auto-generated; one per weapon)
  Greatsword.md
  ... (38 pages, including vanilla-mapped)
```

### Vanilla-mapped weapon pages

Each vanilla-mapped weapon (Shortsword, Shortbow, Light Crossbow, Trident) gets a page that opens with:

> **This weapon is represented by the vanilla Minecraft [Sword]**. When you craft or pick up any vanilla sword (wood, stone, iron, gold, diamond, netherite), it carries the DnD Shortsword identity: tooltip stat block, Finesse and Light combat hooks all apply.

Avoids the "where's the shortsword?" support question.

### Generation pipeline

**Gradle task `generateWiki`** (in `buildSrc/`):
- Reads `Weapons.ALL` (via reflection on compiled output, or via a build-time JSON dump)
- Renders each spec through a template into `build/wiki/Weapons/<Name>.md`
- Generates the four category index pages

**Gradle task `publishWiki`**:
1. Shallow-clones `<repo>.wiki.git` into `build/wiki-repo`
2. Wipes target dir; copies `wiki/handwritten/*` and `build/wiki/*` into it
3. `git add . && git commit -m "Wiki: sync from <commit-sha>" && git push`
4. Auth via env var `WIKI_PUBLISH_TOKEN` (PAT)

**CI**: publishes on `v*` tags so the wiki always matches the latest released catalog.

### Testing the generator

`buildSrc/src/test/`:
- Every spec renders without template errors
- Required sections present (Stats, Tiers, Crafting, Acquisition)
- Vanilla-mapped pages contain the vanilla-X callout
- Category indexes include every spec in their category

---

## 11. Future Work (parking lot)

Explicitly NOT in v1.0:
- Datapack-extensible weapon catalog (add weapons via JSON without recompile)
- DnD Mastery properties (Cleave, Graze, Nick, Push, Sap, Slow, Topple, Vex)
- Per-mob damage-type vulnerabilities
- Mob spawn-with-weapon behavior changes
- Per-mod compat hooks beyond `c:` tag pickup
- Localization beyond `en_us`
- In-game guide book (Patchouli or similar)
- Mod-conditional optional material tiers (Mithril, Adamantine) — framework is in place; concrete recipes deferred

---

## 12. Acceptance Criteria for v1.0

| Item | Acceptance |
|---|---|
| 34 weapons registered | All present in `Registries.ITEM` on every target version |
| 4 vanilla mappings active | Tooltip + combat hooks fire on vanilla iron sword |
| Smithing upgrade ladder | Iron -> diamond -> netherite works for every registered weapon |
| Loot injection | Stronghold corridor + Nether Fortress contain >=1 DnD weapon in test seeds |
| Trade injection | Weaponsmith + Fletcher offer documented trades |
| Mob drops | Vindicator drops Battleaxe at ~8% in N=100 kill test |
| Combat mechanics | Heavy knockback, Finesse sprint, Light dual-wield, Versatile, Lance dismount verified via gametest |
| Recipes load on all 5 versions | No recipe errors on datapack reload |
| Builds + tests pass | CI green on all 5 matrix entries |
| Wiki publishes | `publishWiki` task succeeds; every weapon has a page; vanilla-mapped callouts render |

---

## 13. Open questions

None at time of writing. All design decisions resolved during brainstorm.
