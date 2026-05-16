# DnD Weapons — Phase 2a Design Specification

**Date:** 2026-05-16
**Status:** Approved (brainstorm complete, ready for implementation planning)
**Author:** brainstorm session with the user
**Parent design:** [2026-05-16-dnd-weapons-design.md](2026-05-16-dnd-weapons-design.md)
**Phase 1 plan (predecessor):** [2026-05-16-dnd-weapons-phase-1-foundation.md](../plans/2026-05-16-dnd-weapons-phase-1-foundation.md)
**Implementation plan:** TBD (next step — invoke `superpowers:writing-plans`)

---

## 1. Goal & Scope

**Goal:** Convert the Phase 1 Java codebase to Kotlin, then stand up 4 additional Stonecutter version subprojects (`1.20.1`, `1.21.1`, `26.1.2`, `1.21.11`) so the Longsword registers, has a working recipe, and passes a gametest on each MC version. Five total versions building from one shared `src/main/kotlin/` tree.

### In scope

- Convert all Phase 1 `.java` files to idiomatic Kotlin:
  - `DndWeaponsMod`, `WeaponSpec`, `Weapons`, `DndWeaponItem`, `WeaponRegistrar`, `WeaponRegistrarImpl`, the 4 catalog enums, both unit tests, the gametest.
- Rewrite Java code examples in `docs/superpowers/specs/2026-05-16-dnd-weapons-design.md` to Kotlin so the master design doc stays the source of truth.
- Add the Kotlin Gradle plugin (`org.jetbrains.kotlin.jvm`) to `build.gradle.kts`.
- Add the `fabric-language-kotlin` (FLK) runtime dependency per-version (FLK ships separate per-MC release lines).
- Extract per-epoch attribute-modifier logic into `compat/AttributeCompat.kt` with Stonecutter `//?` directives selecting between Epoch A (1.20.1 — UUID-keyed, returned via `Item.getAttributeModifiers` override) and Epoch C (1.21+ — Identifier-keyed, in `DataComponentTypes.ATTRIBUTE_MODIFIERS`).
- Add 4 Stonecutter subprojects with their own `gradle.properties` pinning MC version, mappings (yarn or Mojang for 26.1+), Fabric loader, Fabric API, Loom, and FLK versions.
- Handle the 1.20.1 recipe folder/key migration (`recipes/` + `"item"` key) via per-version `src/main/resources/` overlays under `versions/<mc>/`.
- One JUnit catalog test + one gametest per version, all green.
- `./gradlew chiseledBuild` produces 5 jars; `chiseledTest` and `chiseledRunGametest` pass.

### Out of scope (deferred)

| Concern | Deferred to |
|---|---|
| Other weapons (catalog stays at LONGSWORD only) | Phase 2b |
| Vanilla mappings (Shortsword/Shortbow/Light Crossbow/Trident role tags) | Phase 2b |
| Combat hooks (Versatile, Finesse, Light, Heavy, Lance Special) | Phase 3 |
| Tooltip stat-block injection | Phase 3 |
| Smithing upgrade ladder (iron → diamond → netherite) | Phase 4 |
| Loot/trade/mob drops | Phase 5 |
| Wiki generator | Phase 6 |

---

## 2. Decision Log

| # | Decision | Choice | Rationale |
|---|---|---|---|
| 1 | Phase 2 split | Phase 2a (versions) + Phase 2b (catalog) | Each plan-runner-cycle-sized; per-epoch bugs surface before catalog work |
| 2 | Version sequencing within 2a | All 4 in parallel waves | plan-runner parallelizes file-disjoint work; faster than version-by-version cycles |
| 3 | Language for mod source | Kotlin (was Java in Phase 1) | User preference; pivot recorded 2026-05-16 |
| 4 | Phase 1 Java disposition | Convert as part of Phase 2a | Avoid permanent mixed-language codebase |
| 5 | Per-epoch code organization | One `AttributeCompat.kt` with `//?` directives, not 3 sibling files | Stonecutter is line-based; one-file forks are its natural pattern. The design doc's "v1201/v1205/v121" naming is conceptual |
| 6 | Recipe-folder migration | Per-version overlays under `versions/<mc>/src/main/resources/` | Stonecutter directives don't work reliably inside JSON; resource overlays do |
| 7 | Fabric entrypoint adapter | `"adapter": "kotlin"` in `fabric.mod.json` | Required so FLK instantiates the `DndWeaponsMod` Kotlin `object` |
| 8 | Java release target | 17 for 1.20.1 subproject; 21 for the rest | Matches each MC version's runtime requirement. Build JDK is always 21 |
| 9 | Mappings on 26.1.2 | `loom.officialMojangMappings()` (no yarn) | Yarn doesn't ship 26.1 mappings; design doc already noted this |
| 10 | Stonecutter fallback | Git branches per MC version | If Stonecutter blocks Phase 2a at any structural level, fall back to one branch per version (no shared `src/`). Plan includes early smoke tests to discover this before significant investment |

---

## 3. Architecture

### Source layout after Phase 2a

```
dnd-weapons/
  stonecutter.gradle.kts              # active version + chiseledBuild
  settings.gradle.kts                 # 5 versions list
  build.gradle.kts                    # shared Loom + Kotlin config
  gradle.properties                   # modId, group, version, kotlin_version

  versions/
    1.20.1/gradle.properties          # mc/yarn/loader/fabric-api/loom/flk + java_release=17
    1.21.1/gradle.properties          # ...                                   java_release=21
    1.21.4/gradle.properties          # already exists from Phase 1
    26.1.2/gradle.properties          # mojang_mappings flag; java_release=21
    1.21.11/gradle.properties

  src/main/kotlin/com/dndweapons/
    DndWeaponsMod.kt                  # converted from Phase 1; Kotlin object
    catalog/
      Category.kt, DamageType.kt, Property.kt, RangeKind.kt
      WeaponSpec.kt                   # data class (was Java record)
      Weapons.kt                      # object (was final class)
    item/
      DndWeaponItem.kt
    registry/
      WeaponRegistrar.kt              # interface
      WeaponRegistrarImpl.kt          # thin; delegates to AttributeCompat
    compat/
      AttributeCompat.kt              # //? if MC >= 1.21 / else
    test/
      RegistrationGametest.kt
  src/test/kotlin/com/dndweapons/
    catalog/WeaponSpecTest.kt
    catalog/WeaponsTest.kt

  # Per-version recipe overlays (folder + result-key differ by epoch):
  versions/1.20.1/src/main/resources/data/dndweapons/recipes/longsword.json
  versions/1.21.1/src/main/resources/data/dndweapons/recipe/longsword.json
  versions/1.21.4/src/main/resources/data/dndweapons/recipe/longsword.json   # already exists
  versions/26.1.2/src/main/resources/data/dndweapons/recipe/longsword.json
  versions/1.21.11/src/main/resources/data/dndweapons/recipe/longsword.json

  src/main/resources/
    fabric.mod.json                   # shared; ${mc_version_range} token expanded per version
    assets/dndweapons/                # shared; lang + model + texture
  # Removed: src/main/java/ entire tree (after conversion)
```

### Key design choices

**One `AttributeCompat.kt` with `//?` directives.** Stonecutter is line-based; one file with version-conditional bodies is the canonical pattern. Three sibling files would require Stonecutter source-set switching, which adds friction with no benefit. The design doc's "AttributeCompat_v1201 / v1205 / v121" naming is conceptual — physical layout collapses to one file with two branches (Epoch A and Epoch C; Epoch B is unused by our targets).

**Recipe JSON per-version overlays.** Stonecutter does not handle JSON files reliably (no `//?` directive support inside JSON). Per-version `src/main/resources/` overlays let each subproject ship its own `data/dndweapons/recipe(s)/longsword.json` with the correct folder name and result-id key.

**`WeaponRegistrarImpl.kt` stays thin.** All per-epoch knowledge lives in `AttributeCompat`. The registrar calls `AttributeCompat.applyTo(settings, spec)` which returns a fully-configured `Item.Settings` on 1.21+ or stores state for `DndWeaponItem.getAttributeModifiers` to consume on 1.20.1.

**Token expansion for `fabric.mod.json`.** `processResources` filters `fabric.mod.json` and substitutes `${version}` and `${mc_version_range}`. Each subproject's `gradle.properties` defines `mc_version_range` (e.g., `~1.20.1`, `~1.21.1`, etc.).

**FLK entrypoint adapter.** `"entrypoints": { "main": [{"adapter": "kotlin", "value": "com.dndweapons.DndWeaponsMod"}] }` tells Fabric to use FLK's reflection path for instantiating the `DndWeaponsMod` Kotlin singleton.

---

## 4. Per-Epoch Compat Detail

### Epochs in play

| Epoch | MC versions | Attribute API | Storage | Affects our targets |
|---|---|---|---|---|
| A | 1.20.1 (–1.20.4) | UUID-keyed `EntityAttributeModifier(UUID, String, double, Operation)` | `Item.getAttributeModifiers(EquipmentSlot)` override returning a `Multimap` | 1.20.1 |
| B | 1.20.5–1.20.6 | UUID-keyed | `DataComponentTypes.ATTRIBUTE_MODIFIERS` via `Item.Settings.attributeModifiers` | none |
| C | 1.21+ | Identifier-keyed `EntityAttributeModifier(Identifier, double, Operation)` | Same data-component plumbing as B | 1.21.1, 1.21.4, 26.1.2, 1.21.11 |

Phase 2a writes **two branches** (A and C). Epoch B is omitted because we have no target on it.

### `AttributeCompat.kt`

```kotlin
package com.dndweapons.compat

import com.dndweapons.catalog.WeaponSpec
import net.minecraft.item.Item

//? if MC >= 1.21 {
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.AttributeModifierSlot
import net.minecraft.component.type.AttributeModifiersComponent
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.util.Identifier
import com.dndweapons.DndWeaponsMod

object AttributeCompat {
    fun applyTo(settings: Item.Settings, spec: WeaponSpec): Item.Settings {
        val mods = AttributeModifiersComponent.builder()
            .add(
                EntityAttributes.GENERIC_ATTACK_DAMAGE,
                EntityAttributeModifier(
                    Identifier.of(DndWeaponsMod.MOD_ID, "base_attack_damage_${spec.id}"),
                    spec.attackDamage - 1.0,
                    EntityAttributeModifier.Operation.ADD_VALUE,
                ),
                AttributeModifierSlot.MAINHAND,
            )
            .add(
                EntityAttributes.GENERIC_ATTACK_SPEED,
                EntityAttributeModifier(
                    Identifier.of(DndWeaponsMod.MOD_ID, "base_attack_speed_${spec.id}"),
                    spec.attackSpeed - 4.0,
                    EntityAttributeModifier.Operation.ADD_VALUE,
                ),
                AttributeModifierSlot.MAINHAND,
            )
            .build()

        return settings
            .maxDamage(spec.baseDurability)
            .component(DataComponentTypes.ATTRIBUTE_MODIFIERS, mods)
    }

    fun storeFor(spec: WeaponSpec) { /* no-op on Epoch C */ }

    // Returned only when DndWeaponItem.getAttributeModifiers is called on Epoch A.
    // On Epoch C, the data component handles attribute resolution; this is never called.
    fun modifiersFor(
        spec: WeaponSpec,
        slot: net.minecraft.entity.EquipmentSlot,
    ): com.google.common.collect.Multimap<net.minecraft.entity.attribute.EntityAttribute, EntityAttributeModifier> =
        com.google.common.collect.ImmutableMultimap.of()
}
//?} else {
/*
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import java.util.UUID

object AttributeCompat {
    private data class CachedMods(
        val damage: EntityAttributeModifier,
        val speed: EntityAttributeModifier,
    )
    private val store = mutableMapOf<String, CachedMods>()

    fun applyTo(settings: Item.Settings, spec: WeaponSpec): Item.Settings {
        storeFor(spec)
        return settings.maxDamage(spec.baseDurability)
    }

    fun storeFor(spec: WeaponSpec) {
        store.getOrPut(spec.id) {
            CachedMods(
                damage = EntityAttributeModifier(
                    UUID.nameUUIDFromBytes("dndweapons:dmg:${spec.id}".toByteArray()),
                    "Weapon base attack damage",
                    spec.attackDamage - 1.0,
                    EntityAttributeModifier.Operation.ADDITION,
                ),
                speed = EntityAttributeModifier(
                    UUID.nameUUIDFromBytes("dndweapons:spd:${spec.id}".toByteArray()),
                    "Weapon base attack speed",
                    spec.attackSpeed - 4.0,
                    EntityAttributeModifier.Operation.ADDITION,
                ),
            )
        }
    }

    fun modifiersFor(
        spec: WeaponSpec,
        slot: EquipmentSlot,
    ): Multimap<EntityAttribute, EntityAttributeModifier> {
        if (slot != EquipmentSlot.MAINHAND) return ImmutableMultimap.of()
        val cached = store[spec.id] ?: return ImmutableMultimap.of()
        return ImmutableMultimap.builder<EntityAttribute, EntityAttributeModifier>()
            .put(EntityAttributes.GENERIC_ATTACK_DAMAGE, cached.damage)
            .put(EntityAttributes.GENERIC_ATTACK_SPEED, cached.speed)
            .build()
    }
}
*/
//?}
```

### `DndWeaponItem.kt`

```kotlin
package com.dndweapons.item

import com.dndweapons.catalog.WeaponSpec
import com.dndweapons.compat.AttributeCompat
import net.minecraft.item.Item

open class DndWeaponItem(val spec: WeaponSpec, settings: Settings) : Item(settings) {
    init { AttributeCompat.storeFor(spec) }

    //? if MC < 1.20.5 {
    /*
    override fun getAttributeModifiers(slot: net.minecraft.entity.EquipmentSlot)
        : com.google.common.collect.Multimap<net.minecraft.entity.attribute.EntityAttribute, net.minecraft.entity.attribute.EntityAttributeModifier> =
        AttributeCompat.modifiersFor(spec, slot)
    */
    //?}
}
```

### `WeaponRegistrarImpl.kt`

```kotlin
package com.dndweapons.registry

import com.dndweapons.DndWeaponsMod
import com.dndweapons.catalog.WeaponSpec
import com.dndweapons.compat.AttributeCompat
import com.dndweapons.item.DndWeaponItem
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

class WeaponRegistrarImpl : WeaponRegistrar {
    override fun register(spec: WeaponSpec) {
        if (spec.isVanillaMapped) return

        val itemId = Identifier.of(DndWeaponsMod.MOD_ID, spec.id)
        val settings = AttributeCompat.applyTo(Item.Settings(), spec)
        val item = DndWeaponItem(spec, settings)

        Registry.register(Registries.ITEM, itemId, item)
        DndWeaponsMod.LOGGER.info("Registered weapon: {}", itemId)
    }
}
```

No `//?` directives in the registrar. All per-epoch knowledge is in `AttributeCompat`.

### Per-version `gradle.properties` matrix

| Version | minecraft_version | mappings | loader_version | fabric_version | loom_version | flk_version | java_release |
|---|---|---|---|---|---|---|---|
| 1.20.1 | 1.20.1 | `yarn 1.20.1+build.10` | 0.15.11 | 0.92.6+1.20.1 | 1.7-SNAPSHOT | 1.10.x+kotlin... | 17 |
| 1.21.1 | 1.21.1 | `yarn 1.21.1+build.3` | 0.16.10 | 0.115.0+1.21.1 | 1.10-SNAPSHOT | 1.13.x+kotlin... | 21 |
| 1.21.4 | 1.21.4 | `yarn 1.21.4+build.8` | 0.16.10 | 0.115.0+1.21.4 | 1.10-SNAPSHOT | 1.13.x+kotlin... | 21 |
| 26.1.2 | 26.1.2 | **Mojang official** (no yarn) | 0.17+ | 0.130+ | 1.11+ | (per MC 26.1 release line) | 21 |
| 1.21.11 | 1.21.11 | `yarn 1.21.11+build.X` | 0.17+ | 0.130+ | 1.11+ | (per MC 1.21.11 release line) | 21 |

Exact `loom_version`, `fabric_version`, `flk_version` for 26.1.2 and 1.21.11 are looked up at plan time via Context7 (or the Fabric Maven directly) — the plan includes a "research current versions" task ahead of the per-version subproject creation.

---

## 5. Data Flow

### Build-time

1. `./gradlew :<mc>:build` triggers Stonecutter to materialize `versions/<mc>/` (no-op if already materialized).
2. Stonecutter copies `src/main/kotlin/` into the subproject's build context, applying `//?` directives — branches matching the current MC version are kept, others are commented out as `/* ... */` blocks.
3. Kotlin compiler runs against the version-specific MC + Fabric API + mappings (yarn or Mojang) classpath; targets the configured `jvmTarget` (17 or 21).
4. `processResources` copies `src/main/resources/` then layers `versions/<mc>/src/main/resources/` on top — that's how 1.20.1 gets `data/dndweapons/recipes/longsword.json` while every other subproject gets `data/dndweapons/recipe/longsword.json`.
5. `processResources` token-expands `fabric.mod.json`, filling in `${version}` and `${mc_version_range}`.
6. Loom remaps the jar (named → intermediary → official) and writes `versions/<mc>/build/libs/dndweapons-0.1.0+mc<mc>.jar`.

### Runtime (mod load on each version)

1. Fabric loader reads `fabric.mod.json`, sees `"adapter": "kotlin"`, delegates instantiation to `fabric-language-kotlin`.
2. FLK resolves `DndWeaponsMod` as a Kotlin `object` (singleton); calls `onInitialize()`.
3. `onInitialize` calls `WeaponRegistrarImpl().registerAll(Weapons.ALL)`. For each spec, `register()`:
   - skips if `isVanillaMapped` (Phase 2a has no vanilla mappings, so this branch is never taken; it stays present for Phase 2b compatibility)
   - builds `Identifier.of("dndweapons", spec.id)`
   - calls `AttributeCompat.applyTo(Item.Settings(), spec)`:
     - On Epoch C: returns settings with `DataComponentTypes.ATTRIBUTE_MODIFIERS` baked in.
     - On Epoch A: stores UUID-keyed modifiers in `AttributeCompat.store`, returns settings with only `maxDamage`.
   - constructs `DndWeaponItem(spec, settings)`. The `init` block calls `AttributeCompat.storeFor(spec)` — defensive on both epochs.
   - `Registry.register(Registries.ITEM, itemId, item)`.
4. Creative tab registers; `ItemGroupEvents.modifyEntriesEvent` adds Longsword.
5. (Epoch A only) When vanilla calls `item.getAttributeModifiers(MAINHAND)`, the `DndWeaponItem` override delegates to `AttributeCompat.modifiersFor(spec, slot)` which returns the cached UUID-keyed pair from `store`.

### Phase 1 → Phase 2a transition for the existing 1.21.4 subproject

The existing Phase 1 commits land 7 `.java` files + 2 tests + 1 gametest. Phase 2a's first wave:
1. Deletes the entire `src/main/java/com/dndweapons/` tree.
2. Writes equivalent `.kt` files under `src/main/kotlin/com/dndweapons/`.
3. Moves tests from `src/test/java/com/dndweapons/` to `src/test/kotlin/com/dndweapons/`.
4. Updates `fabric.mod.json` with the Kotlin entrypoint adapter and FLK depend.
5. Updates `build.gradle.kts` with the Kotlin plugin and FLK runtime dep.

The verifier confirms the Kotlin code parses; the 1.21.4 subproject still builds and tests pass.

### Risk surfaces (early-warning tasks the plan will include)

| When | Task | What it catches |
|---|---|---|
| First | Add a 2nd Stonecutter subproject (1.21.1) wired up with unmodified Phase 1 Java | Stonecutter materializing a sibling subproject is the structural smoke test. If it can't, fall back to git-branches-per-version |
| After Kotlin conversion | Verify 1.21.4 still loads in `runClient` | FLK adapter wiring; if `"adapter": "kotlin"` resolves wrong, mod silently fails to init |
| First contact with 26.1.2 | `:26.1.2:dependencies` resolves | Validates `loom.officialMojangMappings()` works before the rest of the 26.1.2 work is done |

---

## 6. Testing & Acceptance Criteria

### Per-version testing matrix

| Layer | Tooling | Per-version | What it proves |
|---|---|---|---|
| Unit | JUnit 5 (Kotlin) | `WeaponSpecTest`, `WeaponsTest` | Catalog data integrity; pure JVM; runs identically on all 5 |
| Integration | Fabric Gametest | `RegistrationGametest.longswordIsRegistered` | Longsword resolves in `Registries.ITEM` when the mod loads in a real MC server |
| Build | Loom `:build` task | n/a | Compiles + remaps + produces installable jar |

### Test conversion specifics

- JUnit annotations and assertion calls map one-to-one between Java and Kotlin. The only mechanical changes:
  - `@Test void blankIdIsRejected()` → `@Test fun blankIdIsRejected()`
  - `assertThrows(IllegalArgumentException.class, () -> ...)` → `assertThrows<IllegalArgumentException> { ... }`
  - `Set.of(...)` → `setOf(...)` (and `WeaponSpec.properties` now takes `Set<Property>` in Kotlin idiom)
- `FabricGameTest` is a Java interface and is implementable from Kotlin with no friction. `@GameTest(templateName = "fabric-gametest-api-v1:empty")` annotation syntax is identical.

### Acceptance criteria for Phase 2a complete

| # | Criterion | Verification |
|---|---|---|
| 1 | Phase 1 Java tree removed | `git status` shows no `.java` files under `src/main/java/com/dndweapons/` |
| 2 | Equivalent `.kt` files exist with same identifiers | Each Phase 1 type has a Kotlin counterpart; package/name unchanged |
| 3 | `WeaponSpec` is a Kotlin data class with init-block validation | `init { require(id.isNotBlank()) { ... }; require(displayName.isNotBlank()) { ... } }`. `properties: Set<Property>` constructor parameter; immutability comes from callers passing `setOf(...)` (Kotlin stdlib returns immutable sets) — no explicit defensive copy needed |
| 4 | `Weapons` is a Kotlin `object` (singleton) | `object Weapons { val LONGSWORD = ...; val ALL = listOf(LONGSWORD) }` |
| 5 | `fabric.mod.json` uses Kotlin entrypoint adapter | `{"adapter": "kotlin", "value": "com.dndweapons.DndWeaponsMod"}` |
| 6 | `build.gradle.kts` applies the Kotlin Gradle plugin + adds FLK runtime dep | `id("org.jetbrains.kotlin.jvm")`; `modImplementation("net.fabricmc:fabric-language-kotlin:${flk_version}")` |
| 7 | All 5 `versions/<mc>/gradle.properties` files exist with mc/mappings/loader/fabric-api/flk values | File present; values match the Section 4 matrix |
| 8 | `settings.gradle.kts` `stonecutter.create()` lists all 5 versions | Grep the file |
| 9 | `AttributeCompat.kt` with `//?` directives covers Epoch A and C | Static inspection; one file, two branches |
| 10 | Per-version recipe overlays exist | `versions/1.20.1/src/main/resources/data/dndweapons/recipes/longsword.json` plus 4× `recipe/` for the others |
| 11 | All 5 subprojects build cleanly | `./gradlew chiseledBuild` |
| 12 | Unit tests pass on all 5 versions | `./gradlew chiseledTest` |
| 13 | Gametest passes on all 5 versions | `./gradlew chiseledRunGametest` |
| 14 | Master design doc code examples are Kotlin | `docs/superpowers/specs/2026-05-16-dnd-weapons-design.md` greps clean for `record `, `public final class`, `List.of`, `Set.of` in code blocks |

---

## 7. Resolved at Plan Time

The following are intentionally not pinned here; they get resolved by the plan-writing step:

- Exact FLK / Loom / Fabric API versions for 26.1.2 and 1.21.11 are resolved at plan time (Context7 or Fabric Maven).
- If Stonecutter cannot materialize a 2nd subproject (Task 1 smoke test fails), Phase 2a aborts to the git-branch-per-version fallback. That branch is a separate design — not pre-specified here because it should never trigger.
