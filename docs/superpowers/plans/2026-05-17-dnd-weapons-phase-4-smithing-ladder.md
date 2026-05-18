# DnD Weapons Phase 4 — Smithing Ladder Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a smithing upgrade ladder (iron → diamond → netherite) to 27 registered melee + thrown DnD weapons, with two custom smithing-templates obtained via loot, wandering-trader trades, or a multi-step end-game craft.

**Architecture:** Catalog-level `Tier` enum + `WeaponSpec.atTier()` extension produces 81 specs from 27 base specs; `WeaponRegistrarImpl` is parameterized on Tier and applies `.fireResistant()` for netherite. New `SmithingTemplateItems` registry covers the 2 templates + 2 cores + 4 components. Recipes/lang/models are emitted by a one-shot Kotlin codegen entry point; textures come from the `minecraft-asset-generator` agent. Phase 3 `SpecRegistry` and `PlayerAttackMixin` are unmodified — every tiered item is just another registered spec sharing the parent's properties.

**Tech Stack:** Kotlin 2.3.21, Fabric Loom 1.16.2, Stonecutter 0.6, JUnit 5 for unit tests, Fabric Gametest API for runtime tests. MC target matrix: 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2.

**Spec:** `docs/superpowers/specs/2026-05-17-dnd-weapons-phase-4-design.md`

---

## File Map

**New Kotlin files:**
- `src/main/kotlin/com/dndweapons/catalog/Tier.kt` — `Tier` enum
- `src/main/kotlin/com/dndweapons/item/SmithingTemplateItems.kt` — template + core item classes
- `src/main/kotlin/com/dndweapons/item/SmithingComponentItems.kt` — fragment + binding item classes
- `src/main/kotlin/com/dndweapons/registry/SmithingItemRegistrar.kt` — registers the 8 smithing items
- `src/main/kotlin/com/dndweapons/loot/SmithingTemplateLootInjector.kt` — stronghold/bastion loot injection
- `src/main/kotlin/com/dndweapons/trade/SmithingTemplateTrades.kt` — wandering-trader trades
- `src/main/kotlin/com/dndweapons/codegen/Phase4Codegen.kt` — one-shot main that emits recipes, lang, models
- `src/main/kotlin/com/dndweapons/test/SmithingGametest.kt` — 3 new gametests

**New unit tests:**
- `src/test/kotlin/com/dndweapons/catalog/TierTest.kt`
- `src/test/kotlin/com/dndweapons/catalog/WeaponSpecAtTierTest.kt`
- `src/test/kotlin/com/dndweapons/catalog/WeaponsAllTieredTest.kt`

**Modified Kotlin files:**
- `src/main/kotlin/com/dndweapons/catalog/Weapons.kt` — add `WeaponSpec.atTier()`, `Weapons.ALL_TIERED`
- `src/main/kotlin/com/dndweapons/registry/WeaponRegistrar.kt` — add `register(spec, tier)` to interface
- `src/main/kotlin/com/dndweapons/registry/WeaponRegistrarImpl.kt` — implement Tier param, call `.fireResistant()` for netherite
- `src/main/kotlin/com/dndweapons/DndWeaponsMod.kt` — iterate `ALL_TIERED`, register smithing items + loot + trades
- `src/main/resources/fabric.mod.json` — (no change expected; mixins/entrypoints unchanged)
- `src/main/resources/assets/dndweapons/lang/en_us.json` — 66 new entries (emitted by codegen)

**New data files (emitted by codegen, committed to repo):**
- `src/main/resources/data/dndweapons/recipe/<base>_diamond.json` × 27 — `smithing_transform` iron→diamond
- `src/main/resources/data/dndweapons/recipe/<base>_netherite.json` × 27 — `smithing_transform` diamond→netherite
- `src/main/resources/data/dndweapons/recipe/diamond_template_fragment.json` — 4×output crafting
- `src/main/resources/data/dndweapons/recipe/weapon_smithing_binding.json` — 1×output crafting
- `src/main/resources/data/dndweapons/recipe/netherite_template_fragment.json` — 4×output crafting
- `src/main/resources/data/dndweapons/recipe/infernal_binding.json` — 1×output crafting
- `src/main/resources/data/dndweapons/recipe/diamond_template_core.json` — shapeless crafting
- `src/main/resources/data/dndweapons/recipe/netherite_template_core.json` — shapeless crafting
- `src/main/resources/data/dndweapons/recipe/diamond_weapon_upgrade_template_assemble.json` — final smithing
- `src/main/resources/data/dndweapons/recipe/netherite_weapon_upgrade_template_assemble.json` — final smithing

**New asset files:**
- `src/main/resources/assets/dndweapons/models/item/<id>.json` × 62 — auto-emitted models
- `src/main/resources/assets/dndweapons/textures/item/<id>.png` × 62 — Gemini-generated

---

## Task 1: Tier enum

**Files:**
- Create: `src/main/kotlin/com/dndweapons/catalog/Tier.kt`
- Test: `src/test/kotlin/com/dndweapons/catalog/TierTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// src/test/kotlin/com/dndweapons/catalog/TierTest.kt
package com.dndweapons.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TierTest {

    @Test
    fun ironTierIsBaseline() {
        assertEquals("", Tier.IRON.suffix)
        assertEquals("", Tier.IRON.displayPrefix)
        assertEquals(0, Tier.IRON.damageBonus)
        assertEquals(250, Tier.IRON.durability)
        assertFalse(Tier.IRON.fireImmune)
    }

    @Test
    fun diamondTierAddsOneDamageAndVanillaDurability() {
        assertEquals("_diamond", Tier.DIAMOND.suffix)
        assertEquals("Diamond ", Tier.DIAMOND.displayPrefix)
        assertEquals(1, Tier.DIAMOND.damageBonus)
        assertEquals(1561, Tier.DIAMOND.durability)
        assertFalse(Tier.DIAMOND.fireImmune)
    }

    @Test
    fun netheriteTierAddsTwoDamageAndIsFireImmune() {
        assertEquals("_netherite", Tier.NETHERITE.suffix)
        assertEquals("Netherite ", Tier.NETHERITE.displayPrefix)
        assertEquals(2, Tier.NETHERITE.damageBonus)
        assertEquals(2031, Tier.NETHERITE.durability)
        assertTrue(Tier.NETHERITE.fireImmune)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :1.20.1:test --tests com.dndweapons.catalog.TierTest`
Expected: FAIL with "Unresolved reference 'Tier'".

- [ ] **Step 3: Write the Tier enum**

```kotlin
// src/main/kotlin/com/dndweapons/catalog/Tier.kt
package com.dndweapons.catalog

/**
 * Material tier of a registered DnD weapon. Iron is the baseline produced by the
 * existing Phase 2b WeaponSpec catalog; Diamond and Netherite are produced by
 * applying [WeaponSpec.atTier] and registered as separate items in Phase 4.
 *
 * Durability values mirror vanilla MC's `Tiers.IRON/DIAMOND/NETHERITE.getUses()`
 * (pinned here to avoid a per-version `Tiers` class lookup). `damageBonus` is
 * additive on top of the base spec's `attackDamage` and feeds directly into
 * `AttributeCompat` -> `Attributes.ATTACK_DAMAGE` modifier value.
 */
enum class Tier(
    val suffix: String,
    val displayPrefix: String,
    val damageBonus: Int,
    val durability: Int,
    val fireImmune: Boolean,
) {
    IRON(suffix = "", displayPrefix = "", damageBonus = 0, durability = 250, fireImmune = false),
    DIAMOND(suffix = "_diamond", displayPrefix = "Diamond ", damageBonus = 1, durability = 1561, fireImmune = false),
    NETHERITE(suffix = "_netherite", displayPrefix = "Netherite ", damageBonus = 2, durability = 2031, fireImmune = true),
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :1.20.1:test --tests com.dndweapons.catalog.TierTest`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/dndweapons/catalog/Tier.kt src/test/kotlin/com/dndweapons/catalog/TierTest.kt
git commit -m "phase 4: add Tier enum (iron/diamond/netherite)"
```

---

## Task 2: WeaponSpec.atTier() extension

**Files:**
- Modify: `src/main/kotlin/com/dndweapons/catalog/Weapons.kt` (append at bottom of file)
- Test: `src/test/kotlin/com/dndweapons/catalog/WeaponSpecAtTierTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// src/test/kotlin/com/dndweapons/catalog/WeaponSpecAtTierTest.kt
package com.dndweapons.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WeaponSpecAtTierTest {

    @Test
    fun atTierIronReturnsIdenticalSpec() {
        val base = Weapons.LONGSWORD
        val iron = base.atTier(Tier.IRON)
        assertEquals(base.id, iron.id)
        assertEquals(base.displayName, iron.displayName)
        assertEquals(base.attackDamage, iron.attackDamage)
        assertEquals(base.baseDurability, iron.baseDurability)
    }

    @Test
    fun atTierDiamondSuffixesIdAndAddsOneDamage() {
        val diamond = Weapons.LONGSWORD.atTier(Tier.DIAMOND)
        assertEquals("longsword_diamond", diamond.id)
        assertEquals("Diamond Longsword", diamond.displayName)
        assertEquals(Weapons.LONGSWORD.attackDamage + 1, diamond.attackDamage)
        assertEquals(1561, diamond.baseDurability)
    }

    @Test
    fun atTierNetheriteSuffixesIdAndAddsTwoDamage() {
        val netherite = Weapons.GREATAXE.atTier(Tier.NETHERITE)
        assertEquals("greataxe_netherite", netherite.id)
        assertEquals("Netherite Greataxe", netherite.displayName)
        assertEquals(Weapons.GREATAXE.attackDamage + 2, netherite.attackDamage)
        assertEquals(2031, netherite.baseDurability)
    }

    @Test
    fun atTierPreservesPropertiesAndAttackSpeed() {
        val rapier = Weapons.RAPIER
        val diamondRapier = rapier.atTier(Tier.DIAMOND)
        assertEquals(rapier.properties, diamondRapier.properties)
        assertEquals(rapier.attackSpeed, diamondRapier.attackSpeed)
        assertEquals(rapier.reachBonus, diamondRapier.reachBonus)
        assertEquals(rapier.knockbackBonus, diamondRapier.knockbackBonus)
        assertEquals(rapier.damageType, diamondRapier.damageType)
        assertEquals(rapier.category, diamondRapier.category)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :1.20.1:test --tests com.dndweapons.catalog.WeaponSpecAtTierTest`
Expected: FAIL with "Unresolved reference 'atTier'".

- [ ] **Step 3: Add the extension function to Weapons.kt**

Open `src/main/kotlin/com/dndweapons/catalog/Weapons.kt`. After the existing `object Weapons { ... }` block, append at the bottom of the file:

```kotlin
/**
 * Returns a copy of this WeaponSpec with the given tier applied. Tier affects:
 *   - id: suffixed with "_diamond" / "_netherite" (iron leaves it untouched).
 *   - displayName: prefixed with "Diamond " / "Netherite " (iron untouched).
 *   - attackDamage: added to baseline (+1 diamond, +2 netherite).
 *   - baseDurability: replaced with the tier's vanilla durability (250 / 1561 / 2031).
 *
 * Properties, attack speed, damage type, reach, and knockback carry over unchanged.
 * The `vanillaRoleTag` field is preserved but in practice atTier() is only called for
 * non-vanilla-mapped specs (see [Weapons.ALL_TIERED] filter).
 */
fun WeaponSpec.atTier(t: Tier): WeaponSpec = copy(
    id = if (t == Tier.IRON) id else "$id${t.suffix}",
    displayName = if (t == Tier.IRON) displayName else "${t.displayPrefix}$displayName",
    attackDamage = attackDamage + t.damageBonus,
    baseDurability = t.durability,
)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :1.20.1:test --tests com.dndweapons.catalog.WeaponSpecAtTierTest`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/dndweapons/catalog/Weapons.kt src/test/kotlin/com/dndweapons/catalog/WeaponSpecAtTierTest.kt
git commit -m "phase 4: WeaponSpec.atTier() extension"
```

---

## Task 3: Weapons.ALL_TIERED list

**Files:**
- Modify: `src/main/kotlin/com/dndweapons/catalog/Weapons.kt` (add inside the `object Weapons` block)
- Test: `src/test/kotlin/com/dndweapons/catalog/WeaponsAllTieredTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// src/test/kotlin/com/dndweapons/catalog/WeaponsAllTieredTest.kt
package com.dndweapons.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WeaponsAllTieredTest {

    @Test
    fun allTieredExcludesVanillaMappedAndRangedNonThrown() {
        for ((spec, _) in Weapons.ALL_TIERED) {
            assertTrue(spec.vanillaRoleTag == null) { "${spec.id} is vanilla-mapped" }
            assertTrue(spec.ranged == RangeKind.NONE || spec.ranged == RangeKind.THROWN) {
                "${spec.id} has ranged=${spec.ranged}, not NONE/THROWN"
            }
        }
    }

    @Test
    fun allTieredHasThreeEntriesPerBaseWeapon() {
        val ids = Weapons.ALL_TIERED.map { (spec, _) -> spec.id }.toSet()
        val baseIds = Weapons.ALL.filter { it.vanillaRoleTag == null && it.ranged in setOf(RangeKind.NONE, RangeKind.THROWN) }.map { it.id }
        for (baseId in baseIds) {
            assertTrue(baseId in ids) { "$baseId missing from ALL_TIERED" }
            assertTrue("${baseId}_diamond" in ids) { "${baseId}_diamond missing" }
            assertTrue("${baseId}_netherite" in ids) { "${baseId}_netherite missing" }
        }
    }

    @Test
    fun allTieredHasExpectedSize() {
        val baseCount = Weapons.ALL.count { it.vanillaRoleTag == null && it.ranged in setOf(RangeKind.NONE, RangeKind.THROWN) }
        assertEquals(baseCount * 3, Weapons.ALL_TIERED.size)
    }

    @Test
    fun allTieredIdsAreUnique() {
        val ids = Weapons.ALL_TIERED.map { (spec, _) -> spec.id }
        assertEquals(ids.size, ids.toSet().size) { "Duplicate ids in ALL_TIERED" }
    }

    @Test
    fun longswordDiamondHasCorrectDamageAndDurability() {
        val (spec, tier) = Weapons.ALL_TIERED.first { (s, t) -> s.id == "longsword_diamond" && t == Tier.DIAMOND }
        assertEquals(Weapons.LONGSWORD.attackDamage + 1, spec.attackDamage)
        assertEquals(1561, spec.baseDurability)
        assertNotNull(tier)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :1.20.1:test --tests com.dndweapons.catalog.WeaponsAllTieredTest`
Expected: FAIL with "Unresolved reference 'ALL_TIERED'".

- [ ] **Step 3: Add ALL_TIERED to Weapons object**

Inside the `object Weapons { ... }` block in `src/main/kotlin/com/dndweapons/catalog/Weapons.kt`, after the existing `val ALL: List<WeaponSpec> = listOf(...)` declaration, add:

```kotlin
    /**
     * Cartesian product of the 27 non-vanilla-mapped melee + thrown base specs with the
     * 3 tiers. Each entry is `(spec, tier)` where the spec has the tier baked into its
     * id/displayName/attackDamage/baseDurability via [atTier]. IRON entries are
     * functionally identical to the matching entries in [ALL] — registration code uses
     * ALL_TIERED to register all 81 items (the IRON 27 supersede the ALL list when
     * Phase 4 is shipped).
     *
     * Ranged registered weapons (bow/crossbow/firearm/sling/blowgun) are excluded:
     * Phase 4 only tiers melee + thrown DnD weapons per the design spec.
     */
    val ALL_TIERED: List<Pair<WeaponSpec, Tier>> = ALL
        .filter { it.vanillaRoleTag == null && it.ranged in setOf(RangeKind.NONE, RangeKind.THROWN) }
        .flatMap { base -> Tier.values().map { tier -> base.atTier(tier) to tier } }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :1.20.1:test --tests com.dndweapons.catalog.WeaponsAllTieredTest`
Expected: PASS (5 tests).

- [ ] **Step 5: Sanity-check the count locally**

Run: `./gradlew :1.20.1:test --tests com.dndweapons.catalog.WeaponsAllTieredTest --info | grep "allTieredHasExpectedSize" -A 2`
The expected base count is 27 (21 melee NONE + 6 thrown), so ALL_TIERED should have 81 entries.

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/dndweapons/catalog/Weapons.kt src/test/kotlin/com/dndweapons/catalog/WeaponsAllTieredTest.kt
git commit -m "phase 4: Weapons.ALL_TIERED (27 base x 3 tiers)"
```

---

## Task 4: WeaponRegistrar interface accepts Tier

**Files:**
- Modify: `src/main/kotlin/com/dndweapons/registry/WeaponRegistrar.kt`
- Modify: `src/main/kotlin/com/dndweapons/registry/WeaponRegistrarImpl.kt`

- [ ] **Step 1: Read the existing WeaponRegistrar to confirm shape**

Run: `cat src/main/kotlin/com/dndweapons/registry/WeaponRegistrar.kt`
Expected: interface with `fun register(spec: WeaponSpec)` and a default `registerAll(specs: List<WeaponSpec>)`.

- [ ] **Step 2: Modify the interface**

Replace the contents of `src/main/kotlin/com/dndweapons/registry/WeaponRegistrar.kt`:

```kotlin
package com.dndweapons.registry

import com.dndweapons.catalog.Tier
import com.dndweapons.catalog.WeaponSpec

interface WeaponRegistrar {
    fun register(spec: WeaponSpec, tier: Tier)

    fun registerAll(entries: List<Pair<WeaponSpec, Tier>>) {
        for ((spec, tier) in entries) register(spec, tier)
    }
}
```

- [ ] **Step 3: Update WeaponRegistrarImpl**

Replace the contents of `src/main/kotlin/com/dndweapons/registry/WeaponRegistrarImpl.kt`:

```kotlin
package com.dndweapons.registry

import com.dndweapons.DndWeaponsMod
import com.dndweapons.catalog.Tier
import com.dndweapons.catalog.WeaponSpec
import com.dndweapons.compat.AttributeCompat
import com.dndweapons.item.DndWeaponItem
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation
//?}
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier as ResourceLocation
*///?}
import net.minecraft.world.item.Item

class WeaponRegistrarImpl : WeaponRegistrar {

    override fun register(spec: WeaponSpec, tier: Tier) {
        if (spec.isVanillaMapped) {
            // Vanilla-mapped specs are only registered at IRON tier; the spec routes
            // through SpecRegistry.bindRoleTag and no Item is created.
            if (tier == Tier.IRON) SpecRegistry.bindRoleTag(spec)
            return
        }

        //? if >=1.21 {
        val itemId = ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, spec.id)
        //?} else {
        /*val itemId = ResourceLocation(DndWeaponsMod.MOD_ID, spec.id)
        *///?}
        val itemKey = ResourceKey.create(Registries.ITEM, itemId)

        //? if >=1.21.2 {
        var props = Item.Properties().setId(itemKey)
        //?} else {
        /*var props = Item.Properties()
        *///?}
        props = AttributeCompat.applyTo(props, spec)
        if (tier.fireImmune) props = props.fireResistant()

        val item = DndWeaponItem(spec, props)
        Registry.register(BuiltInRegistries.ITEM, itemKey, item)
        SpecRegistry.bindRegistered(item, spec)
        DndWeaponsMod.LOGGER.info("Registered weapon: {} (tier={})", itemId, tier)
    }
}
```

- [ ] **Step 4: Update SpecRegistryTest if it references the old signature**

Run: `grep -n "registrar.register\|registerAll" src/test/kotlin/com/dndweapons/registry/SpecRegistryTest.kt`

If any matches use the OLD signature (`register(spec)` without tier), update them to `register(spec, Tier.IRON)`. If no matches, skip this step.

- [ ] **Step 5: Build the project to verify the signature change compiles**

Run: `./gradlew :1.20.1:build`
Expected: BUILD SUCCESSFUL. (Compile will fail at `DndWeaponsMod.kt` callsites; that's fixed in Task 5.)

If `DndWeaponsMod.kt` fails to compile with `Type mismatch: inferred type is List<WeaponSpec> but List<Pair<WeaponSpec, Tier>> was expected`, that is the expected failure for Task 5 to fix. Skip the build verification here and proceed to Task 5.

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/dndweapons/registry/WeaponRegistrar.kt src/main/kotlin/com/dndweapons/registry/WeaponRegistrarImpl.kt
git commit -m "phase 4: WeaponRegistrar accepts Tier param + fireResistant for netherite"
```

---

## Task 5: Wire ALL_TIERED into mod init

**Files:**
- Modify: `src/main/kotlin/com/dndweapons/DndWeaponsMod.kt`

- [ ] **Step 1: Inspect the current registration call**

Run: `grep -n "registerAll\|Weapons.ALL" src/main/kotlin/com/dndweapons/DndWeaponsMod.kt`
Expected: line 51 calls `registrar.registerAll(Weapons.ALL)`, lines 69 and 84 iterate `Weapons.ALL` for the creative-tab entry list.

- [ ] **Step 2: Replace the registration call**

In `src/main/kotlin/com/dndweapons/DndWeaponsMod.kt`, change line 51:

```kotlin
// OLD:
registrar.registerAll(Weapons.ALL)

// NEW:
registrar.registerAll(Weapons.ALL_TIERED)
```

- [ ] **Step 3: Replace the creative-tab iteration to include all tiers**

For each of the two creative-tab blocks (one inside `//? if <26 {` and one inside `//? if >=26 {`), replace `for (spec in Weapons.ALL)` with:

```kotlin
for ((spec, _) in Weapons.ALL_TIERED) {
    if (spec.vanillaRoleTag != null) continue   // vanilla-mapped have no item to add
```

(Close the loop body the same way the existing code does — typically `entries.accept(ItemStack(BuiltInRegistries.ITEM.get(...)))` or similar. Leave the body unchanged; only swap the loop header.)

- [ ] **Step 4: Update the final log line**

Replace line 91 (or whichever line logs the count):

```kotlin
// OLD:
LOGGER.info("DnD Weapons initialized with {} weapons.", Weapons.ALL.size)

// NEW:
LOGGER.info("DnD Weapons initialized with {} weapon specs ({} tiered items).", Weapons.ALL.size, Weapons.ALL_TIERED.count { (s, _) -> s.vanillaRoleTag == null })
```

- [ ] **Step 5: Build all 5 versions**

Run: `./gradlew chiseledBuild`
Expected: BUILD SUCCESSFUL across all 5 versions.

- [ ] **Step 6: Run gametests to confirm Phase 3 still passes**

Run: `./gradlew chiseledRunGametest`
Expected: All 55/55 mod gametests pass (existing Phase 3 coverage). The new tiered items are registered but no gametest exercises them yet.

If a Phase 3 gametest now fails, investigate which one (likely the iron sword sweep test) — it could be a regression from the registrar signature change, NOT from the tiered items themselves.

- [ ] **Step 7: Commit**

```bash
git add src/main/kotlin/com/dndweapons/DndWeaponsMod.kt
git commit -m "phase 4: register ALL_TIERED instead of ALL on mod init"
```

---

## Task 6: SmithingComponentItems — fragments, bindings, cores

**Files:**
- Create: `src/main/kotlin/com/dndweapons/item/SmithingComponentItems.kt`

These 6 items are pure crafting ingredients. They extend `Item` with default Properties. No combat behavior.

- [ ] **Step 1: Create the component-items file**

```kotlin
// src/main/kotlin/com/dndweapons/item/SmithingComponentItems.kt
package com.dndweapons.item

import com.dndweapons.DndWeaponsMod
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation
//?}
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier as ResourceLocation
*///?}
import net.minecraft.world.item.Item

/**
 * The 6 raw crafting-ingredient items for the Phase 4 smithing system:
 *
 *  - 2 fragments (diamond_template_fragment, netherite_template_fragment): produced
 *    by their respective crafting recipes (4× output per craft), consumed by the
 *    core-assembly crafting recipe (4× input).
 *  - 2 bindings (weapon_smithing_binding, infernal_binding): each consumed once per
 *    core-assembly crafting recipe.
 *  - 2 cores (diamond_template_core, netherite_template_core): output of the
 *    fragment+binding crafting recipe; consumed by the smithing-table template
 *    assembly recipe.
 *
 * All 6 are plain Items with no combat or special behavior. The final templates are
 * registered separately in [SmithingTemplateItems] because they need
 * SmithingTemplateItem for the smithing-table UI.
 */
object SmithingComponentItems {

    val DIAMOND_TEMPLATE_FRAGMENT: Item by lazy { register("diamond_template_fragment") }
    val WEAPON_SMITHING_BINDING: Item by lazy { register("weapon_smithing_binding") }
    val DIAMOND_TEMPLATE_CORE: Item by lazy { register("diamond_template_core") }
    val NETHERITE_TEMPLATE_FRAGMENT: Item by lazy { register("netherite_template_fragment") }
    val INFERNAL_BINDING: Item by lazy { register("infernal_binding") }
    val NETHERITE_TEMPLATE_CORE: Item by lazy { register("netherite_template_core") }

    /** All 6 items in registration order. Call this once during mod init. */
    fun registerAll() {
        // Touch each lazy delegate to force registration.
        listOf(
            DIAMOND_TEMPLATE_FRAGMENT,
            WEAPON_SMITHING_BINDING,
            DIAMOND_TEMPLATE_CORE,
            NETHERITE_TEMPLATE_FRAGMENT,
            INFERNAL_BINDING,
            NETHERITE_TEMPLATE_CORE,
        )
        DndWeaponsMod.LOGGER.info("Registered 6 smithing-component items.")
    }

    private fun register(id: String): Item {
        //? if >=1.21 {
        val itemId = ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, id)
        //?} else {
        /*val itemId = ResourceLocation(DndWeaponsMod.MOD_ID, id)
        *///?}
        val key = ResourceKey.create(Registries.ITEM, itemId)
        //? if >=1.21.2 {
        val props = Item.Properties().setId(key)
        //?} else {
        /*val props = Item.Properties()
        *///?}
        val item = Item(props)
        return Registry.register(BuiltInRegistries.ITEM, key, item)
    }
}
```

- [ ] **Step 2: Build to verify the file compiles on all versions**

Run: `./gradlew chiseledBuild`
Expected: BUILD SUCCESSFUL. The items are defined but not yet wired into the mod-init call; they will not appear in-game yet.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/dndweapons/item/SmithingComponentItems.kt
git commit -m "phase 4: SmithingComponentItems (4 fragments/bindings + 2 cores)"
```

---

## Task 7: SmithingTemplateItems — the 2 upgrade templates

**Files:**
- Create: `src/main/kotlin/com/dndweapons/item/SmithingTemplateItems.kt`

The 2 templates extend vanilla `SmithingTemplateItem` so they appear in the smithing-table UI with proper slot-icon hints. The constructor parameters differ between MC 1.20.1 and 1.21+, so this file is the most stonecutter-heavy in Phase 4.

- [ ] **Step 1: Inspect the SmithingTemplateItem constructors per version**

Decompile vanilla `SmithingTemplateItem` for each MC version to see its constructor:

Run: `find ~/.gradle/caches/fabric-loom -name "minecraft-merged-*1.20.1*.jar" | head -1 | xargs -I {} sh -c 'cd $(dirname {}); javap -p -cp $(basename {}) net.minecraft.world.item.SmithingTemplateItem' | head -20`

The 1.20.1 ctor signature is approximately:
```
public SmithingTemplateItem(Component appliesTo, Component ingredients, Component upgradeDescription, Component baseSlotDescription, Component additionsSlotDescription, List<ResourceLocation> baseSlotEmptyIcons, List<ResourceLocation> additionsSlotEmptyIcons, Item.Properties props)
```

Confirm against 1.21.x and 26.1.2 by running the same `javap` against those merged jars. Document any signature differences in code comments.

- [ ] **Step 2: Create the templates file**

```kotlin
// src/main/kotlin/com/dndweapons/item/SmithingTemplateItems.kt
package com.dndweapons.item

import com.dndweapons.DndWeaponsMod
import net.minecraft.ChatFormatting
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation
//?}
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier as ResourceLocation
*///?}
import net.minecraft.world.item.Item
import net.minecraft.world.item.SmithingTemplateItem

/**
 * The 2 smithing-upgrade templates for Phase 4. Each is a [SmithingTemplateItem]
 * (vanilla class) so the smithing-table UI renders the slot ghosts and tooltip lines
 * Mojang-style.
 *
 * Constructor signature is stable across all 5 MC versions: appliesTo, ingredients,
 * upgradeDescription, baseSlotDescription, additionsSlotDescription, baseSlotEmptyIcons,
 * additionsSlotEmptyIcons, Item.Properties.
 */
object SmithingTemplateItems {

    val DIAMOND: SmithingTemplateItem by lazy {
        register("diamond_weapon_upgrade_template",
            appliesTo = "tooltip.dndweapons.smithing.diamond.applies_to",
            ingredients = "tooltip.dndweapons.smithing.diamond.ingredients",
            upgradeDescription = "tooltip.dndweapons.smithing.diamond.upgrade",
            baseEmptyIconPaths = listOf("item/empty_slot_sword"),
            additionsEmptyIconPaths = listOf("item/empty_slot_diamond"),
        )
    }

    val NETHERITE: SmithingTemplateItem by lazy {
        register("netherite_weapon_upgrade_template",
            appliesTo = "tooltip.dndweapons.smithing.netherite.applies_to",
            ingredients = "tooltip.dndweapons.smithing.netherite.ingredients",
            upgradeDescription = "tooltip.dndweapons.smithing.netherite.upgrade",
            baseEmptyIconPaths = listOf("item/empty_slot_sword"),
            additionsEmptyIconPaths = listOf("item/empty_slot_netherite_ingot"),
        )
    }

    fun registerAll() {
        DIAMOND
        NETHERITE
        DndWeaponsMod.LOGGER.info("Registered 2 smithing-upgrade-template items.")
    }

    private fun register(
        id: String,
        appliesTo: String,
        ingredients: String,
        upgradeDescription: String,
        baseEmptyIconPaths: List<String>,
        additionsEmptyIconPaths: List<String>,
    ): SmithingTemplateItem {
        //? if >=1.21 {
        val itemId = ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, id)
        //?} else {
        /*val itemId = ResourceLocation(DndWeaponsMod.MOD_ID, id)
        *///?}
        val key = ResourceKey.create(Registries.ITEM, itemId)
        //? if >=1.21.2 {
        val props = Item.Properties().setId(key)
        //?} else {
        /*val props = Item.Properties()
        *///?}

        //? if >=1.21 {
        val baseIcons = baseEmptyIconPaths.map { ResourceLocation.fromNamespaceAndPath("minecraft", it) }
        val addIcons = additionsEmptyIconPaths.map { ResourceLocation.fromNamespaceAndPath("minecraft", it) }
        //?} else {
        /*val baseIcons = baseEmptyIconPaths.map { ResourceLocation("minecraft", it) }
        val addIcons = additionsEmptyIconPaths.map { ResourceLocation("minecraft", it) }
        *///?}

        val item = SmithingTemplateItem(
            Component.translatable(appliesTo).withStyle(ChatFormatting.BLUE),
            Component.translatable(ingredients).withStyle(ChatFormatting.BLUE),
            Component.translatable(upgradeDescription),
            Component.translatable("$upgradeDescription.base"),
            Component.translatable("$upgradeDescription.additions"),
            baseIcons,
            addIcons,
            props,
        )
        return Registry.register(BuiltInRegistries.ITEM, key, item)
    }
}
```

- [ ] **Step 3: Build all 5 versions**

Run: `./gradlew chiseledBuild`
Expected: BUILD SUCCESSFUL. If any version fails with `SmithingTemplateItem: constructor signature mismatch`, the per-version ctor differs from the 1.20.1 baseline. Add a `//? if X { ... }` fork around the SmithingTemplateItem(...) call site with the corrected argument list for that version.

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/com/dndweapons/item/SmithingTemplateItems.kt
git commit -m "phase 4: SmithingTemplateItems (diamond + netherite upgrade templates)"
```

---

## Task 8: SmithingItemRegistrar — wire components + templates into mod init

**Files:**
- Create: `src/main/kotlin/com/dndweapons/registry/SmithingItemRegistrar.kt`
- Modify: `src/main/kotlin/com/dndweapons/DndWeaponsMod.kt`

- [ ] **Step 1: Create the registrar**

```kotlin
// src/main/kotlin/com/dndweapons/registry/SmithingItemRegistrar.kt
package com.dndweapons.registry

import com.dndweapons.item.SmithingComponentItems
import com.dndweapons.item.SmithingTemplateItems

/** Phase 4 facade: register the 8 smithing-system items as a single call. */
object SmithingItemRegistrar {
    fun registerAll() {
        SmithingComponentItems.registerAll()
        SmithingTemplateItems.registerAll()
    }
}
```

- [ ] **Step 2: Call it from mod init**

In `src/main/kotlin/com/dndweapons/DndWeaponsMod.kt`, immediately after the `registrar.registerAll(Weapons.ALL_TIERED)` call (added in Task 5), add:

```kotlin
SmithingItemRegistrar.registerAll()
```

Also add the import:

```kotlin
import com.dndweapons.registry.SmithingItemRegistrar
```

- [ ] **Step 3: Build all 5 versions and confirm registration logs**

Run: `./gradlew chiseledBuild`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew :1.20.1:runGametest --console=plain 2>&1 | grep -E "(Registered.*smithing|Registered.*template)"`
Expected: log lines `Registered 6 smithing-component items.` and `Registered 2 smithing-upgrade-template items.`

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/com/dndweapons/registry/SmithingItemRegistrar.kt src/main/kotlin/com/dndweapons/DndWeaponsMod.kt
git commit -m "phase 4: wire SmithingItemRegistrar into mod init"
```

---

## Task 9: Phase4Codegen — one-shot emitter for recipes, lang, models

**Files:**
- Create: `src/main/kotlin/com/dndweapons/codegen/Phase4Codegen.kt`

This codegen is run ONCE by the implementing engineer (not on every build). It writes:
- 54 smithing-transform recipes
- 6 component crafting recipes (placeholder bodies — the real bodies are tasks 11–13)
- 2 final smithing-assembly recipes (placeholder — task 14)
- 62 item-model JSONs (`{ parent: item/generated, textures: { layer0: ... } }`)
- 66 lang entries appended to `en_us.json`

- [ ] **Step 1: Create the codegen entry point**

```kotlin
// src/main/kotlin/com/dndweapons/codegen/Phase4Codegen.kt
package com.dndweapons.codegen

import com.dndweapons.catalog.Tier
import com.dndweapons.catalog.Weapons
import java.io.File

/**
 * One-shot codegen for Phase 4. Run from an IDE or `./gradlew :1.20.1:runMain
 * -PmainClass=com.dndweapons.codegen.Phase4CodegenKt`. Writes recipes, model
 * JSONs, and lang entries directly into src/main/resources/. Re-running is
 * idempotent except for the lang file which is rewritten from scratch.
 *
 * This is NOT a gradle data-generation task; it's a manual one-time run during
 * Phase 4 implementation. The resulting files are committed to the repo and
 * become the source of truth thereafter.
 */
fun main() {
    val root = File("src/main/resources")
    require(root.exists()) { "Run from project root (src/main/resources must exist)" }

    emitSmithingTransformRecipes(root)
    emitModelJsons(root)
    emitLangEntries(root)
    println("Phase 4 codegen complete. 54 transform recipes, 62 models, 66 lang entries.")
    println("NOTE: component/core/template-assembly recipes are NOT generated here; ")
    println("      hand-author them per tasks 11-14 of the implementation plan.")
}

private fun emitSmithingTransformRecipes(root: File) {
    val recipeDir = File(root, "data/dndweapons/recipe").also { it.mkdirs() }
    for ((spec, tier) in Weapons.ALL_TIERED) {
        if (tier == Tier.IRON) continue   // base iron already exists from Phase 2b crafting
        val baseId = if (tier == Tier.DIAMOND) spec.id.removeSuffix("_diamond")
                     else spec.id.removeSuffix("_netherite") + "_diamond"
        val (templateId, additionTag) = when (tier) {
            Tier.DIAMOND -> "dndweapons:diamond_weapon_upgrade_template" to "c:gems/diamond"
            Tier.NETHERITE -> "dndweapons:netherite_weapon_upgrade_template" to "c:ingots/netherite"
            Tier.IRON -> error("unreachable")
        }
        val json = """{
            |  "type": "minecraft:smithing_transform",
            |  "template": { "id": "$templateId" },
            |  "base":     { "id": "dndweapons:$baseId" },
            |  "addition": { "tag": "$additionTag" },
            |  "result":   { "id": "dndweapons:${spec.id}" }
            |}""".trimMargin()
        File(recipeDir, "${spec.id}.json").writeText(json + "\n")
    }
}

private fun emitModelJsons(root: File) {
    val modelDir = File(root, "assets/dndweapons/models/item").also { it.mkdirs() }
    val itemIds = buildList {
        for ((spec, _) in Weapons.ALL_TIERED) if (spec.vanillaRoleTag == null) add(spec.id)
        addAll(listOf(
            "diamond_template_fragment", "weapon_smithing_binding", "diamond_template_core",
            "netherite_template_fragment", "infernal_binding", "netherite_template_core",
            "diamond_weapon_upgrade_template", "netherite_weapon_upgrade_template",
        ))
    }
    for (id in itemIds.distinct()) {
        val json = """{
            |  "parent": "minecraft:item/generated",
            |  "textures": { "layer0": "dndweapons:item/$id" }
            |}""".trimMargin()
        File(modelDir, "$id.json").writeText(json + "\n")
    }
}

private fun emitLangEntries(root: File) {
    val langFile = File(root, "assets/dndweapons/lang/en_us.json")
    require(langFile.exists()) { "en_us.json missing — Phase 2b should have created it" }
    // Read existing JSON as an ordered map by re-tokenising. We don't pull in
    // a JSON library to keep codegen runnable without runtime deps.
    val existing = langFile.readText()
        .lines()
        .filter { it.contains("\":") && !it.trim().startsWith("//") }
        .associate {
            val k = it.substringAfter("\"").substringBefore("\"")
            val v = it.substringAfter("\": \"").substringBefore("\"")
            k to v
        }
        .toMutableMap()

    for ((spec, tier) in Weapons.ALL_TIERED) {
        if (tier == Tier.IRON || spec.vanillaRoleTag != null) continue
        existing["item.dndweapons.${spec.id}"] = spec.displayName
    }
    existing["item.dndweapons.diamond_template_fragment"] = "Diamond Template Fragment"
    existing["item.dndweapons.weapon_smithing_binding"]   = "Weapon Smithing Binding"
    existing["item.dndweapons.diamond_template_core"]     = "Diamond Template Core"
    existing["item.dndweapons.netherite_template_fragment"] = "Netherite Template Fragment"
    existing["item.dndweapons.infernal_binding"]          = "Infernal Binding"
    existing["item.dndweapons.netherite_template_core"]   = "Netherite Template Core"
    existing["item.dndweapons.diamond_weapon_upgrade_template"]   = "Diamond Weapon Upgrade Template"
    existing["item.dndweapons.netherite_weapon_upgrade_template"] = "Netherite Weapon Upgrade Template"
    existing["tooltip.dndweapons.smithing.diamond.applies_to"]    = "DnD Weapons"
    existing["tooltip.dndweapons.smithing.diamond.ingredients"]   = "Diamond"
    existing["tooltip.dndweapons.smithing.diamond.upgrade"]       = "Diamond Weapon Upgrade"
    existing["tooltip.dndweapons.smithing.diamond.upgrade.base"]  = "DnD Weapon"
    existing["tooltip.dndweapons.smithing.diamond.upgrade.additions"] = "Diamond"
    existing["tooltip.dndweapons.smithing.netherite.applies_to"]  = "DnD Diamond Weapons"
    existing["tooltip.dndweapons.smithing.netherite.ingredients"] = "Netherite Ingot"
    existing["tooltip.dndweapons.smithing.netherite.upgrade"]     = "Netherite Weapon Upgrade"
    existing["tooltip.dndweapons.smithing.netherite.upgrade.base"]  = "Diamond DnD Weapon"
    existing["tooltip.dndweapons.smithing.netherite.upgrade.additions"] = "Netherite Ingot"

    val sorted = existing.toSortedMap()
    val sb = StringBuilder("{\n")
    for ((i, e) in sorted.entries.withIndex()) {
        sb.append("  \"${e.key}\": \"${e.value}\"")
        if (i < sorted.size - 1) sb.append(",")
        sb.append("\n")
    }
    sb.append("}\n")
    langFile.writeText(sb.toString())
}
```

- [ ] **Step 2: Run the codegen**

Run: `./gradlew :1.20.1:compileKotlin && java -cp "$(./gradlew :1.20.1:printClasspath -q 2>/dev/null || echo versions/1.20.1/build/classes/kotlin/main):$(find ~/.gradle/caches -name 'kotlin-stdlib-2.3.21.jar' | head -1)" com.dndweapons.codegen.Phase4CodegenKt`

If the classpath dance is painful in your shell, alternative: open `Phase4Codegen.kt` in IntelliJ and click the green ▶ next to `fun main()`.

Expected output:
```
Phase 4 codegen complete. 54 transform recipes, 62 models, 66 lang entries.
NOTE: component/core/template-assembly recipes are NOT generated here; ...
```

After running, verify on disk:
- `ls src/main/resources/data/dndweapons/recipe/ | grep -E "(diamond|netherite)" | wc -l` → 54
- `ls src/main/resources/assets/dndweapons/models/item/ | wc -l` → at least 62 (will be more if Phase 2b already shipped iron model JSONs)
- `cat src/main/resources/assets/dndweapons/lang/en_us.json | grep -c "diamond\|netherite"` → 50+ (rough sanity)

- [ ] **Step 3: Build all 5 versions to confirm the data files parse**

Run: `./gradlew chiseledBuild`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit codegen + generated files**

```bash
git add src/main/kotlin/com/dndweapons/codegen/Phase4Codegen.kt \
        src/main/resources/data/dndweapons/recipe/*_diamond.json \
        src/main/resources/data/dndweapons/recipe/*_netherite.json \
        src/main/resources/assets/dndweapons/models/item/ \
        src/main/resources/assets/dndweapons/lang/en_us.json
git commit -m "phase 4: codegen + 54 smithing_transform recipes + 62 model JSONs + lang"
```

---

## Task 10: Hand-author the 6 component crafting recipes

The codegen does not emit these because their ingredient lists are too specific to template. Hand-author them.

**Files:** all under `src/main/resources/data/dndweapons/recipe/`
- Create: `diamond_template_fragment.json`
- Create: `weapon_smithing_binding.json`
- Create: `diamond_template_core.json`
- Create: `netherite_template_fragment.json`
- Create: `infernal_binding.json`
- Create: `netherite_template_core.json`

- [ ] **Step 1: diamond_template_fragment recipe**

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["PDP", "FDF", "PDP"],
  "key": {
    "P": { "item": "minecraft:paper" },
    "D": { "tag": "c:gems/diamond" },
    "F": { "item": "minecraft:flint" }
  },
  "result": { "id": "dndweapons:diamond_template_fragment", "count": 4 }
}
```

Save to `src/main/resources/data/dndweapons/recipe/diamond_template_fragment.json`.

- [ ] **Step 2: weapon_smithing_binding recipe**

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["SLS", "LBL", "SLS"],
  "key": {
    "S": { "item": "minecraft:string" },
    "L": { "item": "minecraft:leather" },
    "B": { "item": "minecraft:blaze_rod" }
  },
  "result": { "id": "dndweapons:weapon_smithing_binding", "count": 1 }
}
```

Save to `weapon_smithing_binding.json`.

- [ ] **Step 3: diamond_template_core recipe (shapeless)**

```json
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    { "item": "dndweapons:diamond_template_fragment" },
    { "item": "dndweapons:diamond_template_fragment" },
    { "item": "dndweapons:diamond_template_fragment" },
    { "item": "dndweapons:diamond_template_fragment" },
    { "item": "dndweapons:weapon_smithing_binding" }
  ],
  "result": { "id": "dndweapons:diamond_template_core", "count": 1 }
}
```

Save to `diamond_template_core.json`.

- [ ] **Step 4: netherite_template_fragment recipe**

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["OSO", "GNG", "OSO"],
  "key": {
    "O": { "item": "minecraft:obsidian" },
    "S": { "tag": "c:gems/quartz" },
    "G": { "item": "minecraft:ghast_tear" },
    "N": { "item": "minecraft:netherite_scrap" }
  },
  "result": { "id": "dndweapons:netherite_template_fragment", "count": 4 }
}
```

Save to `netherite_template_fragment.json`.

- [ ] **Step 5: infernal_binding recipe**

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["BNB", "SWS", "BNB"],
  "key": {
    "B": { "item": "minecraft:blaze_powder" },
    "N": { "item": "minecraft:nether_star" },
    "S": { "item": "minecraft:soul_sand" },
    "W": { "item": "minecraft:wither_rose" }
  },
  "result": { "id": "dndweapons:infernal_binding", "count": 1 }
}
```

Save to `infernal_binding.json`.

- [ ] **Step 6: netherite_template_core recipe**

```json
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    { "item": "dndweapons:netherite_template_fragment" },
    { "item": "dndweapons:netherite_template_fragment" },
    { "item": "dndweapons:netherite_template_fragment" },
    { "item": "dndweapons:netherite_template_fragment" },
    { "item": "dndweapons:infernal_binding" }
  ],
  "result": { "id": "dndweapons:netherite_template_core", "count": 1 }
}
```

Save to `netherite_template_core.json`.

- [ ] **Step 7: Build to verify all 6 recipes parse**

Run: `./gradlew chiseledBuild`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew :1.20.1:runGametest --console=plain 2>&1 | grep -iE "(recipe|crafting).*(error|fail|malformed)"`
Expected: no output (no recipe parse errors logged).

- [ ] **Step 8: Commit**

```bash
git add src/main/resources/data/dndweapons/recipe/diamond_template_fragment.json \
        src/main/resources/data/dndweapons/recipe/weapon_smithing_binding.json \
        src/main/resources/data/dndweapons/recipe/diamond_template_core.json \
        src/main/resources/data/dndweapons/recipe/netherite_template_fragment.json \
        src/main/resources/data/dndweapons/recipe/infernal_binding.json \
        src/main/resources/data/dndweapons/recipe/netherite_template_core.json
git commit -m "phase 4: 6 component + core crafting recipes (epic multi-step)"
```

---

## Task 11: Hand-author the 2 template-assembly smithing recipes

**Files:**
- Create: `src/main/resources/data/dndweapons/recipe/diamond_weapon_upgrade_template_assemble.json`
- Create: `src/main/resources/data/dndweapons/recipe/netherite_weapon_upgrade_template_assemble.json`

- [ ] **Step 1: Diamond template assembly**

```json
{
  "type": "minecraft:smithing_transform",
  "template": { "id": "minecraft:netherite_upgrade_smithing_template" },
  "base":     { "id": "dndweapons:diamond_template_core" },
  "addition": { "tag": "c:gems/diamond" },
  "result":   { "id": "dndweapons:diamond_weapon_upgrade_template" }
}
```

Save to `diamond_weapon_upgrade_template_assemble.json`.

- [ ] **Step 2: Netherite template assembly**

```json
{
  "type": "minecraft:smithing_transform",
  "template": { "id": "minecraft:netherite_upgrade_smithing_template" },
  "base":     { "id": "dndweapons:netherite_template_core" },
  "addition": { "tag": "c:ingots/netherite" },
  "result":   { "id": "dndweapons:netherite_weapon_upgrade_template" }
}
```

Save to `netherite_weapon_upgrade_template_assemble.json`.

- [ ] **Step 3: Build all 5 versions**

Run: `./gradlew chiseledBuild`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/data/dndweapons/recipe/diamond_weapon_upgrade_template_assemble.json \
        src/main/resources/data/dndweapons/recipe/netherite_weapon_upgrade_template_assemble.json
git commit -m "phase 4: 2 final smithing-table template-assembly recipes"
```

---

## Task 12: Generate textures via the minecraft-asset-generator agent

**Files:** all under `src/main/resources/assets/dndweapons/textures/item/`
- Create: 54 weapon variant PNGs (`<base>_diamond.png` × 27 and `<base>_netherite.png` × 27)
- Create: 8 smithing-system PNGs (4 components + 2 cores + 2 templates)

- [ ] **Step 1: List all 62 texture targets**

Run: `./gradlew :1.20.1:test --tests com.dndweapons.catalog.WeaponsAllTieredTest 2>&1 || true`

Build the target list by hand from `Weapons.ALL_TIERED`. The 54 weapon textures correspond to the 27 base ids each appearing with `_diamond` and `_netherite` suffixes:

```
battleaxe_diamond.png, battleaxe_netherite.png,
club_diamond.png, club_netherite.png,
dagger_diamond.png, dagger_netherite.png,
... (continue for all 27 base ids)
```

Plus the 8 smithing-system textures:

```
diamond_template_fragment.png
weapon_smithing_binding.png
diamond_template_core.png
netherite_template_fragment.png
infernal_binding.png
netherite_template_core.png
diamond_weapon_upgrade_template.png
netherite_weapon_upgrade_template.png
```

- [ ] **Step 2: Dispatch the minecraft-asset-generator agent for weapon-tier batches**

Dispatch the agent at `D:/minecraft/.claude/agents/minecraft-asset-generator.md` with a prompt template like:

```
Generate Minecraft item textures (16x16 PNG, transparent background, vanilla pixel-art style).

I need a tiered variant of an existing iron-tier weapon texture. The shape, hilt, guard,
and silhouette MUST match the iron variant exactly — only the blade-material color
shifts. Attached: the iron-tier reference texture.

Tier: diamond (or netherite)
Color palette:
  - diamond blade: cyan (#5DECF5) with light gray (#A9A9A9) shading
  - netherite blade: dark grey-brown (#443A3B) with faint purple (#5B2A5C) highlights

Lighting: top-left light source, single-pixel shading on the blade edge.
Output: 16x16 PNG, no antialiasing.

Reference iron texture: <attach src/main/resources/assets/dndweapons/textures/item/<base>.png>
```

Run the agent in batches of 5 (10 textures per batch since each batch covers 5 weapons × 2 tiers). Save each output to `src/main/resources/assets/dndweapons/textures/item/<base>_<tier>.png`.

- [ ] **Step 3: Dispatch the agent for the 8 smithing-system textures**

Use per-asset prompts (the iron-reference attachment trick doesn't apply since these have no iron variant):

```
- diamond_template_fragment.png: "shard of a glowing cyan diamond rune, broken edge,
                                  ornate smithing-template fragment, MC pixel art, 16x16"
- netherite_template_fragment.png: "shard of a dark netherite rune with faint purple
                                    glow, broken edge, ornate smithing-template fragment"
- weapon_smithing_binding.png:    "spool of waxed cord wrapped around a brass rivet,
                                   weapon-grip binding, top-down view"
- infernal_binding.png:           "spool of nether-charred cord around a blackstone rivet"
- diamond_template_core.png:      "circular medallion engraved with a complete diamond
                                   rune, cyan glow center, dark-bronze ring"
- netherite_template_core.png:    "circular medallion engraved with a complete netherite
                                   rune, dark purple glow, blackstone ring"
- diamond_weapon_upgrade_template.png:  "ornate cyan smithing template, vanilla template
                                         silhouette, diamond-rune center"
- netherite_weapon_upgrade_template.png:"ornate dark smithing template, vanilla template
                                         silhouette, netherite-rune center"
```

- [ ] **Step 4: Verify all 62 textures landed**

Run:
```bash
expected=62
actual=$(ls src/main/resources/assets/dndweapons/textures/item/ | grep -cE "(_diamond|_netherite|_template|_fragment|_binding|_core)\.png$")
echo "Expected: $expected, actual: $actual"
[ "$actual" -ge "$expected" ] && echo "OK" || echo "MISSING — list:" && \
  for f in $(jq -r '...' /dev/null 2>/dev/null); do : ; done
```

Spot-check 3 textures by opening them in an image viewer and confirming they look like Minecraft items (16x16, transparent bg, recognizable shape).

- [ ] **Step 5: Commit textures**

```bash
git add src/main/resources/assets/dndweapons/textures/item/
git commit -m "phase 4: 54 tier-variant weapon textures + 8 smithing-system textures (Gemini)"
```

---

## Task 13: SmithingGametest — `smithingDiamondUpgradePreservesSpec`

**Files:**
- Create: `src/main/kotlin/com/dndweapons/test/SmithingGametest.kt`

- [ ] **Step 1: Inspect existing gametest for style**

Run: `head -60 src/main/kotlin/com/dndweapons/test/CombatHooksGametest.kt`

Note the version-gated `@GameTest(template = ...)` vs `@GameTest(structure = ...)` and the `FabricGameTest` interface (1.20.1) vs no-base-class (1.21.5+) pattern. Mirror this in SmithingGametest.

- [ ] **Step 2: Create the gametest file**

```kotlin
// src/main/kotlin/com/dndweapons/test/SmithingGametest.kt
package com.dndweapons.test

import com.dndweapons.DndWeaponsMod
import com.dndweapons.catalog.Property
import com.dndweapons.catalog.Tier
import com.dndweapons.catalog.Weapons
import com.dndweapons.registry.SpecRegistry
import net.minecraft.core.registries.BuiltInRegistries

//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation
//?}
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier as ResourceLocation
*///?}

//? if <1.21.5 {
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper

class SmithingGametest : FabricGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty")
    fun smithingDiamondUpgradePreservesSpec(ctx: GameTestHelper) = runDiamondPreservesSpec(ctx)
}
//?}

//? if >=1.21.5 {

/*import net.fabricmc.fabric.api.gametest.v1.GameTest
import net.minecraft.gametest.framework.GameTestHelper

class SmithingGametest {

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun smithingDiamondUpgradePreservesSpec(ctx: GameTestHelper) = runDiamondPreservesSpec(ctx)
}

*///?}

private fun runDiamondPreservesSpec(ctx: GameTestHelper) {
    val baseSpec = Weapons.LONGSWORD
    //? if >=1.21 {
    val itemId = ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, "longsword_diamond")
    //?} else {
    /*val itemId = ResourceLocation(DndWeaponsMod.MOD_ID, "longsword_diamond")
    *///?}
    //? if >=1.21.2 {
    val item = BuiltInRegistries.ITEM.get(itemId).orElseThrow {
        AssertionError("longsword_diamond not registered")
    }.value()
    //?} else {
    /*val item = BuiltInRegistries.ITEM.get(itemId)
        ?: throw AssertionError("longsword_diamond not registered")
    *///?}

    val spec = SpecRegistry.lookup(item)
        ?: throw AssertionError("longsword_diamond has no WeaponSpec")

    if (spec.id != "longsword_diamond")
        throw AssertionError("Expected id 'longsword_diamond', got '${spec.id}'")
    if (spec.attackDamage != baseSpec.attackDamage + Tier.DIAMOND.damageBonus)
        throw AssertionError("Expected attackDamage ${baseSpec.attackDamage + 1}, got ${spec.attackDamage}")
    if (spec.baseDurability != Tier.DIAMOND.durability)
        throw AssertionError("Expected durability ${Tier.DIAMOND.durability}, got ${spec.baseDurability}")
    if (spec.properties != baseSpec.properties)
        throw AssertionError("Properties mismatch: ${spec.properties} vs ${baseSpec.properties}")
    if (Property.VERSATILE !in spec.properties)
        throw AssertionError("Diamond longsword should still be VERSATILE")
    ctx.succeed()
}
```

- [ ] **Step 3: Register the new gametest class in fabric.mod.json**

Open `src/main/resources/fabric.mod.json` and add `"com.dndweapons.test.SmithingGametest"` to the `fabric-gametest` entrypoint list (alongside the existing 3 entries).

- [ ] **Step 4: Run gametests, focus on 1.20.1 first**

Run: `./gradlew :1.20.1:runGametest --console=plain 2>&1 | grep -E "smithingdiamondupgradepreservesspec|GAME TESTS COMPLETE"`
Expected: a `smithingdiamondupgradepreservesspec` test entry with PASS, plus the existing 9 combat + 2 tooltip = 11 tests now 12 with the new one.

- [ ] **Step 5: Run all 5 versions**

Run: `./gradlew chiseledRunGametest`
Expected: all versions green (each adds 1 to its pre-Phase-4 total).

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/dndweapons/test/SmithingGametest.kt src/main/resources/fabric.mod.json
git commit -m "phase 4: gametest smithingDiamondUpgradePreservesSpec"
```

---

## Task 14: SmithingGametest — `netheriteFireImmunityFires`

**Files:**
- Modify: `src/main/kotlin/com/dndweapons/test/SmithingGametest.kt`

- [ ] **Step 1: Add the new @GameTest method to both class branches**

In `SmithingGametest.kt`, inside the `<1.21.5` class body add:

```kotlin
    @GameTest(template = "fabric-gametest-api-v1:empty")
    fun netheriteFireImmunityFires(ctx: GameTestHelper) = runNetheriteFireImmunity(ctx)
```

And inside the `>=1.21.5` commented-out class body, add (within the `/*...*/`):

```
    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun netheriteFireImmunityFires(ctx: GameTestHelper) = runNetheriteFireImmunity(ctx)
```

- [ ] **Step 2: Add the helper at the bottom of the file**

```kotlin
private fun runNetheriteFireImmunity(ctx: GameTestHelper) {
    // Spawn a netherite-tier item entity in mid-air over a lava block. Wait 60 ticks.
    // Assert the entity is still alive. Then repeat with iron-tier and assert it
    // burns (sanity check that the fire-immune flag is what's keeping netherite alive).
    val world = ctx.level

    // Place a lava block at structure-relative (2, 1, 2).
    val lavaPos = net.minecraft.core.BlockPos(2, 1, 2)
    ctx.setBlock(lavaPos, net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState())

    val abs = ctx.absolutePos(lavaPos)
    val nx = abs.x + 0.5; val ny = abs.y + 0.5; val nz = abs.z + 0.5

    //? if >=1.21 {
    val netheriteId = ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, "longsword_netherite")
    val ironId      = ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, "longsword")
    //?} else {
    /*val netheriteId = ResourceLocation(DndWeaponsMod.MOD_ID, "longsword_netherite")
    val ironId      = ResourceLocation(DndWeaponsMod.MOD_ID, "longsword")
    *///?}
    //? if >=1.21.2 {
    val netheriteItem = BuiltInRegistries.ITEM.get(netheriteId).orElseThrow().value()
    val ironItem      = BuiltInRegistries.ITEM.get(ironId).orElseThrow().value()
    //?} else {
    /*val netheriteItem = BuiltInRegistries.ITEM.get(netheriteId) ?: throw AssertionError("longsword_netherite missing")
    val ironItem      = BuiltInRegistries.ITEM.get(ironId)      ?: throw AssertionError("longsword missing")
    *///?}

    val netheriteEntity = net.minecraft.world.entity.item.ItemEntity(
        world, nx, ny, nz, net.minecraft.world.item.ItemStack(netheriteItem),
    )
    world.addFreshEntity(netheriteEntity)

    ctx.runAfterDelay(60L) {
        if (netheriteEntity.isRemoved)
            throw AssertionError("Netherite longsword burned up in lava — fireResistant flag not applied")

        val ironEntity = net.minecraft.world.entity.item.ItemEntity(
            world, nx, ny, nz, net.minecraft.world.item.ItemStack(ironItem),
        )
        world.addFreshEntity(ironEntity)

        ctx.runAfterDelay(30L) {
            if (!ironEntity.isRemoved)
                throw AssertionError("Iron longsword did NOT burn in lava — sanity check failed; the fire-immune test is meaningless")
            ctx.succeed()
        }
    }
}
```

- [ ] **Step 3: Run the test on 1.20.1**

Run: `./gradlew :1.20.1:runGametest --console=plain 2>&1 | grep -E "netheritefireimmunityfires"`
Expected: PASS.

- [ ] **Step 4: Run all 5 versions**

Run: `./gradlew chiseledRunGametest`
Expected: 12-tests (was 11, now +1 fire immunity) per version, all pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/dndweapons/test/SmithingGametest.kt
git commit -m "phase 4: gametest netheriteFireImmunityFires"
```

---

## Task 15: SmithingGametest — `tieredItemTriggersDnDMixin`

**Files:**
- Modify: `src/main/kotlin/com/dndweapons/test/SmithingGametest.kt`

This test proves that a diamond-tier item passes through both the vanilla attribute pipeline (tier bonus) and the Phase 3 DnD mixin (VERSATILE bonus).

- [ ] **Step 1: Add the @GameTest method to both class branches**

In `SmithingGametest.kt`, inside the `<1.21.5` class body:

```kotlin
    @GameTest(template = "fabric-gametest-api-v1:empty")
    fun tieredItemTriggersDnDMixin(ctx: GameTestHelper) = runTieredMixinCase(ctx)
```

And inside the `>=1.21.5` commented-out class:

```
    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun tieredItemTriggersDnDMixin(ctx: GameTestHelper) = runTieredMixinCase(ctx)
```

- [ ] **Step 2: Add the helper at the bottom of the file**

```kotlin
private fun runTieredMixinCase(ctx: GameTestHelper) {
    // Mock player + diamond longsword (VERSATILE) + empty offhand. Expected damage:
    //   base (longsword 5) + diamond tier (+1) + VERSATILE empty bonus (+1) = 7.
    //? if >=1.21.1 {
    val player = ctx.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL)
    //?} else {
    /*val player = ctx.makeMockPlayer()
    *///?}

    //? if >=1.21 {
    val itemId = ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, "longsword_diamond")
    //?} else {
    /*val itemId = ResourceLocation(DndWeaponsMod.MOD_ID, "longsword_diamond")
    *///?}
    //? if >=1.21.2 {
    val item = BuiltInRegistries.ITEM.get(itemId).orElseThrow().value()
    //?} else {
    /*val item = BuiltInRegistries.ITEM.get(itemId)!!
    *///?}

    val weapon = net.minecraft.world.item.ItemStack(item)
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, weapon)
    player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, net.minecraft.world.item.ItemStack.EMPTY)
    player.isSprinting = false

    val pigPos = net.minecraft.core.BlockPos(2, 1, 2)
    //? if <1.21.11 {
    val pig = ctx.spawn(net.minecraft.world.entity.EntityType.PIG, pigPos)
        as net.minecraft.world.entity.animal.Pig
    //?} else {
    /*val pig = ctx.spawn(net.minecraft.world.entity.EntityType.PIG, pigPos)
        as net.minecraft.world.entity.animal.pig.Pig
    *///?}

    val before = pig.health
    primeAttackStrength(player)   // helper from CombatHooksGametest.kt
    player.attack(pig)
    val after = pig.health
    val dealt = before - after

    val expected = (Weapons.LONGSWORD.attackDamage + Tier.DIAMOND.damageBonus + 1).toFloat()  // +1 versatile
    val tol = 0.5f
    if (Math.abs(dealt - expected) > tol) {
        throw AssertionError(
            "Tiered diamond longsword + VERSATILE empty: dealt=$dealt expected~$expected " +
                "(base=${Weapons.LONGSWORD.attackDamage}, tier=+${Tier.DIAMOND.damageBonus}, versatile=+1)"
        )
    }
    ctx.succeed()
}
```

- [ ] **Step 3: Note on shared helpers**

`primeAttackStrength` is defined as a private top-level function in `CombatHooksGametest.kt`. Kotlin's `private top-level` is file-private, so SmithingGametest cannot call it. Either:
- (a) Promote `primeAttackStrength` and `applyMainHandModifiers` to `internal` visibility (or move them to a shared `TestHelpers.kt`); OR
- (b) Copy the helper into SmithingGametest.kt.

Choose (a). Move both helpers to a new file `src/main/kotlin/com/dndweapons/test/TestHelpers.kt`:

```kotlin
package com.dndweapons.test

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

internal fun primeAttackStrength(entity: LivingEntity) {
    entity.attackStrengthTicker = 100
    applyMainHandModifiers(entity)
}

internal fun applyMainHandModifiers(entity: LivingEntity) {
    val stack = entity.mainHandItem
    if (stack.isEmpty) return
    try {
        applyMainHandModifiersImpl(entity, stack)
    } catch (t: Throwable) {
        println("[applyMainHandModifiers] threw on ${entity.javaClass.name} with ${stack.item}: ${t.javaClass.name}: ${t.message}")
        t.printStackTrace()
        throw t
    }
}

private fun applyMainHandModifiersImpl(entity: LivingEntity, stack: ItemStack) {
    //? if >=1.20.5 {
    val mods = stack.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS) ?: return
    val map = com.google.common.collect.HashMultimap.create<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier>()
    for (entry in mods.modifiers()) {
        if (entry.slot() == net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND ||
            entry.slot() == net.minecraft.world.entity.EquipmentSlotGroup.HAND ||
            entry.slot() == net.minecraft.world.entity.EquipmentSlotGroup.ANY) {
            map.put(entry.attribute(), entry.modifier())
        }
    }
    entity.attributes.addTransientAttributeModifiers(map)
    //?} else {
    /*val item = stack.item
    val mm = item.getDefaultAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
    entity.attributes.addTransientAttributeModifiers(mm)
    *///?}
}
```

Then DELETE the `private fun primeAttackStrength` and `private fun applyMainHandModifiers` / `applyMainHandModifiersImpl` declarations from `CombatHooksGametest.kt` (their definitions; the call sites stay).

- [ ] **Step 4: Run gametests on 1.20.1**

Run: `./gradlew :1.20.1:runGametest --console=plain 2>&1 | grep -E "tiereditemtriggers|GAME TESTS COMPLETE"`
Expected: PASS for `tiereditemtriggersdndmixin`. The full count should be 13 tests on 1.20.1 (was 11 pre-Phase-4, +2 smithing tests).

- [ ] **Step 5: Run all 5 versions**

Run: `./gradlew chiseledRunGametest`
Expected: all green.

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/dndweapons/test/SmithingGametest.kt src/main/kotlin/com/dndweapons/test/TestHelpers.kt src/main/kotlin/com/dndweapons/test/CombatHooksGametest.kt
git commit -m "phase 4: gametest tieredItemTriggersDnDMixin + extract TestHelpers"
```

---

## Task 16: SmithingTemplateLootInjector — stronghold and bastion loot

**Files:**
- Create: `src/main/kotlin/com/dndweapons/loot/SmithingTemplateLootInjector.kt`
- Modify: `src/main/kotlin/com/dndweapons/DndWeaponsMod.kt` (call `SmithingTemplateLootInjector.register()` on mod init)

- [ ] **Step 1: Inspect any existing loot-injection patterns in the repo**

Run: `grep -rn "LootTableEvents" src/main/kotlin/ 2>&1 | head -5`

Likely no prior Phase-2b loot-injection code exists (Phase 2b only ships recipes/textures/lang). If so, this task introduces the first loot-injection class.

- [ ] **Step 2: Create the loot injector**

```kotlin
// src/main/kotlin/com/dndweapons/loot/SmithingTemplateLootInjector.kt
package com.dndweapons.loot

import com.dndweapons.DndWeaponsMod
import com.dndweapons.item.SmithingTemplateItems
import net.fabricmc.fabric.api.loot.v2.LootTableEvents
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue

//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation
//?}
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier as ResourceLocation
*///?}

/**
 * Injects the diamond and netherite smithing-upgrade templates into vanilla loot
 * tables so players can discover them outside of the crafting path.
 *
 *  - Stronghold library (low chance, weight 5) -> diamond template
 *  - Bastion treasure (lower chance, weight 3) -> netherite template
 *
 * Uses the Fabric Loot API v2 LootTableEvents.MODIFY hook, available since 1.20.x
 * on all our target versions. Resource locations differ per MC version for table
 * identifiers; the table strings below are stable in all 5 targets.
 */
object SmithingTemplateLootInjector {

    private const val STRONGHOLD_LIBRARY = "minecraft:chests/stronghold_library"
    private const val BASTION_TREASURE = "minecraft:chests/bastion_treasure"

    fun register() {
        LootTableEvents.MODIFY.register { key, builder, source, _ ->
            //? if >=1.21.1 {
            val tableId = key.location().toString()
            //?} else {
            /*val tableId = key.toString()
            *///?}

            when (tableId) {
                STRONGHOLD_LIBRARY -> builder.pool(
                    net.minecraft.world.level.storage.loot.LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(SmithingTemplateItems.DIAMOND)
                            .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0f)))
                            .setWeight(5))
                        .add(net.minecraft.world.level.storage.loot.entries.EmptyLootItem.emptyItem().setWeight(95))
                        .build()
                )
                BASTION_TREASURE -> builder.pool(
                    net.minecraft.world.level.storage.loot.LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(SmithingTemplateItems.NETHERITE)
                            .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0f)))
                            .setWeight(3))
                        .add(net.minecraft.world.level.storage.loot.entries.EmptyLootItem.emptyItem().setWeight(97))
                        .build()
                )
            }
        }
        DndWeaponsMod.LOGGER.info("Registered smithing-template loot injectors (stronghold + bastion).")
    }
}
```

- [ ] **Step 3: Wire it into mod init**

In `src/main/kotlin/com/dndweapons/DndWeaponsMod.kt`, after `SmithingItemRegistrar.registerAll()`, add:

```kotlin
SmithingTemplateLootInjector.register()
```

And the import:

```kotlin
import com.dndweapons.loot.SmithingTemplateLootInjector
```

- [ ] **Step 4: Build all 5 versions**

Run: `./gradlew chiseledBuild`
Expected: BUILD SUCCESSFUL. If `LootTableEvents.MODIFY.register { ... }` fails to type-check on a specific version (the lambda signature changed between v2 and v3), fork the lambda body via `//? if <1.21.1 { ... } //?}` and `//? if >=1.21.1 { /*...*/ //?}`.

- [ ] **Step 5: Run gametests**

Run: `./gradlew chiseledRunGametest`
Expected: all 13 tests pass per version (loot injection runs at server startup; no test directly probes it but a crash in the injector lambda would fail mod init).

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/dndweapons/loot/SmithingTemplateLootInjector.kt src/main/kotlin/com/dndweapons/DndWeaponsMod.kt
git commit -m "phase 4: loot inject smithing templates into stronghold + bastion"
```

---

## Task 17: SmithingTemplateTrades — wandering-trader trades

**Files:**
- Create: `src/main/kotlin/com/dndweapons/trade/SmithingTemplateTrades.kt`
- Modify: `src/main/kotlin/com/dndweapons/DndWeaponsMod.kt`

- [ ] **Step 1: Create the trades class**

```kotlin
// src/main/kotlin/com/dndweapons/trade/SmithingTemplateTrades.kt
package com.dndweapons.trade

import com.dndweapons.DndWeaponsMod
import com.dndweapons.item.SmithingTemplateItems
import net.fabricmc.fabric.api.`object`.builder.v1.trade.TradeOfferHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.trading.MerchantOffer

/**
 * Adds rare wandering-trader trades for the 2 smithing-upgrade templates. Each trade
 * is uses-capped at 1 per trader spawn so it doesn't dominate the trader's listings.
 *
 *  - 32 emeralds + 1 emerald block -> 1 diamond_weapon_upgrade_template
 *  - 48 emeralds + 1 diamond block -> 1 netherite_weapon_upgrade_template
 *
 * Uses Fabric's TradeOfferHelper.registerWanderingTraderOffers at level 2 (rare).
 */
object SmithingTemplateTrades {

    fun register() {
        TradeOfferHelper.registerWanderingTraderOffers(2) { factories ->
            factories.add { _, _ ->
                MerchantOffer(
                    net.minecraft.world.item.trading.ItemCost(Items.EMERALD, 32),
                    java.util.Optional.of(net.minecraft.world.item.trading.ItemCost(Items.EMERALD_BLOCK, 1)),
                    ItemStack(SmithingTemplateItems.DIAMOND, 1),
                    /* uses        = */ 0,
                    /* maxUses     = */ 1,
                    /* xp          = */ 30,
                    /* priceMult   = */ 0.05f,
                )
            }
            factories.add { _, _ ->
                MerchantOffer(
                    net.minecraft.world.item.trading.ItemCost(Items.EMERALD, 48),
                    java.util.Optional.of(net.minecraft.world.item.trading.ItemCost(Items.DIAMOND_BLOCK, 1)),
                    ItemStack(SmithingTemplateItems.NETHERITE, 1),
                    0, 1, 40, 0.05f,
                )
            }
        }
        DndWeaponsMod.LOGGER.info("Registered wandering-trader trades for 2 smithing templates.")
    }
}
```

The `ItemCost` and `MerchantOffer(ItemCost, Optional<ItemCost>, ItemStack, ...)` constructor shape is stable on 1.21.1+. For 1.20.1 the API differs (uses `ItemStack` directly for cost). Add a stonecutter fork:

```kotlin
//? if >=1.21.1 {
TradeOfferHelper.registerWanderingTraderOffers(2) { factories ->
    /* (use the ItemCost-based offers above) */
}
//?} else {
/*TradeOfferHelper.registerWanderingTraderOffers(2) { factories ->
    factories.add(net.minecraft.world.entity.npc.VillagerTrades.TradeListing { _, _ ->
        MerchantOffer(
            ItemStack(Items.EMERALD, 32),
            ItemStack(Items.EMERALD_BLOCK, 1),
            ItemStack(SmithingTemplateItems.DIAMOND, 1),
            0, 1, 30, 0.05f
        )
    })
    factories.add(net.minecraft.world.entity.npc.VillagerTrades.TradeListing { _, _ ->
        MerchantOffer(
            ItemStack(Items.EMERALD, 48),
            ItemStack(Items.DIAMOND_BLOCK, 1),
            ItemStack(SmithingTemplateItems.NETHERITE, 1),
            0, 1, 40, 0.05f
        )
    })
}
*///?}
```

Verify the exact 1.20.1 trade-listing factory signature with `javap -p versions/1.20.1/.../VillagerTrades$TradeListing` if the build fails — Fabric's helper API has shifted slightly across versions.

- [ ] **Step 2: Wire into mod init**

In `DndWeaponsMod.kt`, after `SmithingTemplateLootInjector.register()`:

```kotlin
SmithingTemplateTrades.register()
```

```kotlin
import com.dndweapons.trade.SmithingTemplateTrades
```

- [ ] **Step 3: Build all 5 versions**

Run: `./gradlew chiseledBuild`
Expected: BUILD SUCCESSFUL. Most likely failure point is the per-version trade API; fix by forking.

- [ ] **Step 4: Run gametests**

Run: `./gradlew chiseledRunGametest`
Expected: all 13 tests still pass per version.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/dndweapons/trade/SmithingTemplateTrades.kt src/main/kotlin/com/dndweapons/DndWeaponsMod.kt
git commit -m "phase 4: wandering-trader trades for 2 smithing templates"
```

---

## Task 18: Full matrix verification + Phase 4 verification doc

**Files:**
- Create: `docs/superpowers/plans/phase-4-verification-final.md`

- [ ] **Step 1: Run the full matrix**

```bash
./gradlew chiseledBuild chiseledTest chiseledRunGametest
```

Expected:
- `chiseledBuild` GREEN on all 5 versions
- `chiseledTest` GREEN on all 5 versions (the new TierTest, WeaponSpecAtTierTest, WeaponsAllTieredTest run on each)
- `chiseledRunGametest` GREEN on all 5 versions: 13 mod tests per version (was 11 in Phase 3, +3 smithing tests), plus the 1-2 vanilla `minecraft:always_pass` on 1.21.11+ for 14-15 per version.

- [ ] **Step 2: Tally per version**

```bash
for v in 1.20.1 1.21.1 1.21.4 1.21.11 26.1.2; do
  total=$(cat "versions/$v/build/gametest-report.xml" 2>/dev/null | grep -o "<testcase" | wc -l)
  fails=$(cat "versions/$v/build/gametest-report.xml" 2>/dev/null | grep -o "<failure" | wc -l)
  echo "$v: $((total - fails))/$total pass"
done
```

Expected: every line shows `13/13` or `14/14` per version with 0 failures.

- [ ] **Step 3: Write the verification doc**

Save to `docs/superpowers/plans/phase-4-verification-final.md` with this template (fill in actual numbers):

```markdown
# Phase 4 Verification — Final

**Date:** YYYY-MM-DD
**Branch / commit:** main / <sha>
**Verified locally via:** ./gradlew chiseledBuild chiseledTest chiseledRunGametest

## Top-line result
| Check | Versions | Status |
|---|---|---|
| chiseledBuild | all 5 | GREEN |
| chiseledTest  | all 5 | GREEN |
| chiseledRunGametest | all 5 | GREEN (13 mod tests + 1-2 vanilla = N/N per version) |

## Per-version gametest tally
- 1.20.1: 13/13
- 1.21.1: 13/13
- 1.21.4: 13/13
- 1.21.11: 14/14 (includes minecraft:always_pass)
- 26.1.2: 14/14 (includes minecraft:always_pass)

## New artifacts
- 62 registered items (54 tiered weapons + 6 components + 2 templates)
- 62 recipes (54 smithing-transform + 6 component-crafting + 2 final-smithing-assembly)
- 66 lang entries
- 62 textures (Gemini-generated)
- 62 model JSONs (codegen-emitted)
- 3 new gametests (smithingDiamondUpgradePreservesSpec, netheriteFireImmunityFires, tieredItemTriggersDnDMixin)

## Tag command
git tag phase-4-smithing-ladder
git push origin phase-4-smithing-ladder
```

- [ ] **Step 4: Commit the verification doc**

```bash
git add docs/superpowers/plans/phase-4-verification-final.md
git commit -m "phase 4: final verification doc (all green, 5-version matrix)"
```

- [ ] **Step 5: Create the tag**

```bash
git tag phase-4-smithing-ladder
```

(Push deferred until the user explicitly authorizes — same flow as Phase 3.)

---

## Self-Review (already done by plan author)

**Spec coverage:**
- §1 Scope → Tasks 1–17 cover all enumerated deliverables. ✓
- §2 Decisions → All 14 decisions traceable to tasks (Tier in T1, atTier in T2, ALL_TIERED in T3, registrar in T4, mod-init in T5, fireResistant in T4, suffix convention in T1/T2, smithing items in T6/T7, recipes in T9–T11, loot in T16, trader in T17, art via agent in T12, Phase 3 carryover validated in T13/T15). ✓
- §3–§4 Tier spec + catalog wiring → T1–T3. ✓
- §5 Smithing system → T6 (components/cores), T7 (templates), T9–T11 (recipes). ✓
- §6 Template acquisition → T9–T11 (craft), T16 (loot), T17 (trader). ✓
- §7 Asset strategy → T9 (model JSONs), T12 (textures). ✓
- §8 Lang strings → T9 (emitted by codegen). ✓
- §9 Phase 3 compatibility → T13/T15 verify via gametest. ✓
- §10 Testing → T13/T14/T15 ship the 3 gametests. ✓
- §11 File-by-file change list → matches the File Map at the top of this plan. ✓
- §12 Open questions → mitigations baked into the relevant tasks (smithing-transform-1-per-slot → T6+T10 introduce cores; SmithingTemplateItem ctor skew → T7 step 1 dumps signature first; loot v2/v3 → T16 step 4 forks; trader API skew → T17 step 1 forks; Gemini style → T12 attaches iron reference; tiered + DnD bonus stacking regression → T15).

**Placeholder scan:** No TBD/TODO/"implement later" in task bodies (the only TBD is the implementation-plan back-link in the spec front-matter, which is intentional).

**Type consistency:**
- `Tier.IRON/DIAMOND/NETHERITE` properties used consistently in T1, T2, T3, T4, T13, T15.
- `WeaponSpec.atTier(t: Tier)` signature consistent.
- `WeaponRegistrar.register(spec, tier)` signature consistent across T4 and T5.
- `SmithingTemplateItems.DIAMOND/NETHERITE` referenced consistently in T7, T8, T16, T17.
- `SmithingComponentItems.*` constants referenced consistently in T6, T8, T10.
- `primeAttackStrength(LivingEntity)` consistent in T15 (extraction from CombatHooksGametest).

---

## Execution handoff

Plan complete and saved to `docs/superpowers/plans/2026-05-17-dnd-weapons-phase-4-smithing-ladder.md`. Two execution options:

1. **Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration.
2. **Inline Execution** — Execute tasks in this session using `superpowers:executing-plans`, batch execution with checkpoints.

Which approach?
