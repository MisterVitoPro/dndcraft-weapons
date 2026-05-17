# DnD Weapons — Phase 2a-3: Mojang Naming + MC 26.x Support

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert the dnd-weapons codebase from Yarn naming to Mojang naming (`loom.officialMojangMappings()`) and add MC 26.1.2 as a 5th supported version. End state: `./gradlew chiseledBuild` produces 5 jars (1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2) and each passes `:build`, `:test`, `:runGametest`.

**Architecture:** Replace all `net.minecraft.util.Identifier` / `net.minecraft.text.Text` / `net.minecraft.registry.Registry` / etc. Yarn imports with their Mojang equivalents (`net.minecraft.resources.ResourceLocation`, `net.minecraft.network.chat.Component`, `net.minecraft.core.Registry`). Within Mojang naming, MC versions still differ on specific names (notably `ResourceLocation` → `Identifier` at 1.21.11, and the entire data-component pipeline being 1.20.5+ only); these differences continue to be handled by Stonecutter `//?` directives.

**Tech Stack:** Kotlin 2.3.21, Gradle 9.5.1, Fabric Loom 1.16.2 for 1.20.1-1.21.11 + Loom 1.15-SNAPSHOT for 26.x, `loom.officialMojangMappings()` for 1.20.1-1.21.11 + `intermediary:0.0.0` stub for 26.x, JDK 25 daemon (compiles all down to release 17 for 1.20.1 and 21 for 1.21.x via toolchain).

---

## Prior context (read before starting)

- `docs/plan-runner-out/phase-2a-multi-version-postmortem.md` — earlier postmortem (corrected by mc26 blocker doc)
- `docs/plan-runner-out/phase-2a-mc26-blocker.md` — explains the Yarn→Mojang naming shift at MC 26.x
- Working reference: https://github.com/MattCzyr/NaturesCompass/tree/fabric-26.1 (Java single-project, no Stonecutter, but the build.gradle + import patterns are correct for 26.x)
- Mojang dropped `client_mappings` for MC 26.x; their `client.jar` ships in source-level names already. `net.fabricmc:intermediary:0.0.0` is the no-op stub.
- Mojang renamed `ResourceLocation` → `Identifier` between 1.21.4 and 1.21.11 (verified via Loom-cached mappings files).
- Mojang renamed `Attributes.GENERIC_ATTACK_DAMAGE` → `Attributes.ATTACK_DAMAGE` between 1.21.1 and 1.21.4.
- `Item.Properties.setId(ResourceKey)` was added in 1.21.2 (required by 1.21.4 runtime to avoid "Item id not set" NPE).
- `BuiltInRegistries.ITEM.get(...)` return-type changed across versions: returns `Item` directly (1.20.1 - 1.21.1), `Optional<Holder<Item>>` (1.21.2 - 1.21.10), then back to `Item`-direct or `Holder<Item>` (1.21.11+; needs verification at task time).
- `AttributeModifier.Operation.ADDITION` (1.20.1 - 1.20.4) vs `ADD_VALUE` (1.20.5+).
- 1.20.5+: data components (`ItemAttributeModifiers`, `DataComponents.ATTRIBUTE_MODIFIERS`). 1.20.1: UUID-keyed modifiers returned via `Item.getAttributeModifiers(EquipmentSlot)` override.
- Fabric gametest API 3.x for 1.21.5+ uses Fabric's own `@GameTest(structure=...)` annotation (different package, no `FabricGameTest` interface). Earlier versions use Minecraft's `@GameTest(templateName=...)` + `FabricGameTest` interface.

---

## File Inventory (modified in this phase)

```
build.gradle.kts                         # mappings switch: yarn → officialMojangMappings + intermediary stub for 26.x; per-MC Loom version if needed
src/main/kotlin/com/dndweapons/DndWeaponsMod.kt
src/main/kotlin/com/dndweapons/compat/AttributeCompat.kt
src/main/kotlin/com/dndweapons/item/DndWeaponItem.kt
src/main/kotlin/com/dndweapons/registry/WeaponRegistrarImpl.kt
src/main/kotlin/com/dndweapons/test/RegistrationGametest.kt
src/main/resources/fabric.mod.json       # entrypoint adapter, java/fabricloader/fabric-language-kotlin depend ranges
settings.gradle.kts                      # re-enable 26.1.2 in versions list
versions/26.1.2/gradle.properties        # actual values: loader_version=0.18.4, fabric_version=0.144.0+26.1, no yarn_mappings, java_release=25
docs/plan-runner-out/phase-2a-3-verification.md   # written by final task
```

No file deletions — Yarn-naming imports are replaced in place.

---

## Task 1: Switch root build.gradle.kts mappings to Mojang for 1.20.1-1.21.11, intermediary stub for 26.x

**Files:**
- Modify: `build.gradle.kts`

- [ ] **Step 1: Replace `mappings(...)` block**

In `build.gradle.kts`, replace:
```kotlin
mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
```
with:
```kotlin
if ((property("minecraft_version") as String).startsWith("26.")) {
    // MC 26.x: Mojang stopped obfuscating client.jar; intermediary 0.0.0 is the
    // no-op stub from Fabric meta API (https://meta.fabricmc.net/v2/versions/intermediary/26.1.2).
    mappings("net.fabricmc:intermediary:0.0.0:v2")
} else {
    mappings(loom.officialMojangMappings())
}
```

- [ ] **Step 2: Verify 1.21.4 still builds with Mojang mappings active**

Run:
```bash
./gradlew "Set active project to 1.21.4"
./gradlew :1.21.4:compileKotlin
```

Expected: FAIL with many `Unresolved reference` errors — those are the Yarn imports the rest of this plan converts.

- [ ] **Step 3: Commit**

```bash
git add build.gradle.kts
git commit -m "phase-2a-3: switch mappings from yarn to Mojang for 1.20.1-1.21.11"
```

---

## Task 2: Convert `DndWeaponsMod.kt` to Mojang naming with per-version forks

**Files:**
- Modify: `src/main/kotlin/com/dndweapons/DndWeaponsMod.kt`

- [ ] **Step 1: Replace imports**

The Yarn → Mojang import map (Stonecutter `//?` brackets noted where needed):

| Yarn | Mojang |
|---|---|
| `net.minecraft.item.ItemGroup` | `net.minecraft.world.item.CreativeModeTab` |
| `net.minecraft.item.ItemStack` | `net.minecraft.world.item.ItemStack` |
| `net.minecraft.registry.Registries` | `net.minecraft.core.registries.BuiltInRegistries` (for the static instance) AND `net.minecraft.core.registries.Registries` (for the registry-key namespace) |
| `net.minecraft.registry.Registry` | `net.minecraft.core.Registry` |
| `net.minecraft.registry.RegistryKey` | `net.minecraft.resources.ResourceKey` |
| `net.minecraft.registry.RegistryKeys` | `net.minecraft.core.registries.Registries` |
| `net.minecraft.text.Text` | `net.minecraft.network.chat.Component` |
| `net.minecraft.util.Identifier` (1.20.1 - 1.21.4) | `net.minecraft.resources.ResourceLocation` |
| `net.minecraft.util.Identifier` (1.21.11+) | `net.minecraft.resources.Identifier` |

Final `DndWeaponsMod.kt`:
```kotlin
package com.dndweapons

import com.dndweapons.catalog.Weapons
import com.dndweapons.registry.WeaponRegistrarImpl
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation
//?}
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier as ResourceLocation*/
//?}
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private fun rl(namespace: String, path: String): ResourceLocation {
    //? if >=1.21 {
    return ResourceLocation.fromNamespaceAndPath(namespace, path)
    //?} else {
    /*return ResourceLocation(namespace, path)*/
    //?}
}

object DndWeaponsMod : ModInitializer {

    const val MOD_ID: String = "dndweapons"
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    val CREATIVE_TAB: ResourceKey<CreativeModeTab> =
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, rl(MOD_ID, "main"))

    override fun onInitialize() {
        LOGGER.info("DnD Weapons initializing...")

        val registrar = WeaponRegistrarImpl()
        registrar.registerAll(Weapons.ALL)

        Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB, CREATIVE_TAB,
            FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.dndweapons.main"))
                .icon { iconStack() }
                .build(),
        )

        ItemGroupEvents.modifyEntriesEvent(CREATIVE_TAB).register { entries ->
            for (spec in Weapons.ALL) {
                if (spec.isVanillaMapped) continue
                addToEntries(entries, spec)
            }
        }

        LOGGER.info("DnD Weapons initialized with {} weapons.", Weapons.ALL.size)
    }

    private fun iconStack(): ItemStack {
        //? if >=1.21.2 && <1.21.11 {
        return BuiltInRegistries.ITEM
            .get(rl(MOD_ID, "longsword"))
            .map { ItemStack(it) }
            .orElse(ItemStack.EMPTY)
        //?} else {
        /*return ItemStack(BuiltInRegistries.ITEM.get(rl(MOD_ID, "longsword")))*/
        //?}
    }

    private fun addToEntries(entries: net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries, spec: com.dndweapons.catalog.WeaponSpec) {
        val itemId = rl(MOD_ID, spec.id)
        //? if >=1.21.2 && <1.21.11 {
        BuiltInRegistries.ITEM.get(itemId).ifPresent { entries.accept(it.value()) }
        //?} else {
        /*
        val item = BuiltInRegistries.ITEM.get(itemId)
        if (item != null) entries.accept(item)
        */
        //?}
    }
}
```

Note: The 1.21.11 `Identifier` import uses `as ResourceLocation` alias so the rest of the file body can reference `ResourceLocation` uniformly. This is cleaner than per-call-site directives.

Verify the `addToEntries` signature type lookup is correct: `FabricItemGroupEntries` is at `net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries` — confirm at task time on each MC version.

- [ ] **Step 2: Verify 1.21.4 compiles**

```bash
./gradlew "Set active project to 1.21.4"
./gradlew :1.21.4:compileKotlin
```

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/dndweapons/DndWeaponsMod.kt
git commit -m "phase-2a-3: convert DndWeaponsMod to Mojang naming with per-version //? forks"
```

---

## Task 3: Convert `WeaponRegistrarImpl.kt` to Mojang naming

**Files:**
- Modify: `src/main/kotlin/com/dndweapons/registry/WeaponRegistrarImpl.kt`

- [ ] **Step 1: Replace imports + body**

Final content:
```kotlin
package com.dndweapons.registry

import com.dndweapons.DndWeaponsMod
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
/*import net.minecraft.resources.Identifier as ResourceLocation*/
//?}
import net.minecraft.world.item.Item

class WeaponRegistrarImpl : WeaponRegistrar {

    override fun register(spec: WeaponSpec) {
        if (spec.isVanillaMapped) return

        //? if >=1.21 {
        val itemId = ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, spec.id)
        //?} else {
        /*val itemId = ResourceLocation(DndWeaponsMod.MOD_ID, spec.id)*/
        //?}
        val itemKey = ResourceKey.create(Registries.ITEM, itemId)

        //? if >=1.21.2 {
        val settings = AttributeCompat.applyTo(Item.Properties().setId(itemKey), spec)
        //?} else {
        /*val settings = AttributeCompat.applyTo(Item.Properties(), spec)*/
        //?}

        val item = DndWeaponItem(spec, settings)
        Registry.register(BuiltInRegistries.ITEM, itemKey, item)
        DndWeaponsMod.LOGGER.info("Registered weapon: {}", itemId)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/kotlin/com/dndweapons/registry/WeaponRegistrarImpl.kt
git commit -m "phase-2a-3: convert WeaponRegistrarImpl to Mojang naming"
```

---

## Task 4: Convert `AttributeCompat.kt` to Mojang naming with full Epoch A/C fork

**Files:**
- Modify: `src/main/kotlin/com/dndweapons/compat/AttributeCompat.kt`

- [ ] **Step 1: Replace with full Mojang-named version**

```kotlin
package com.dndweapons.compat

import com.dndweapons.DndWeaponsMod
import com.dndweapons.catalog.WeaponSpec
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.Item

//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.item.component.ItemAttributeModifiers
//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation
//?}
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier as ResourceLocation*/
//?}
//?}

//? if <1.20.5 {
/*
import java.util.UUID
*/
//?}

/**
 * Per-epoch attribute-modifier builder.
 *
 * Epoch C (1.20.5+): bakes an `ItemAttributeModifiers` data component into Item.Properties.
 *   On 1.21.2+ the attribute fields are `ATTACK_DAMAGE` / `ATTACK_SPEED`;
 *   on 1.20.5-1.21.1 they retain the `GENERIC_` prefix.
 *   Modifier IDs are ResourceLocation-based; Operation.ADD_VALUE.
 *
 * Epoch A (<1.20.5): caches UUID-keyed modifiers per spec via Operation.ADDITION;
 *   DndWeaponItem.getAttributeModifiers(slot) override returns them at runtime.
 */
object AttributeCompat {

    //? if >=1.21.2 {
    private val damageAttr = Attributes.ATTACK_DAMAGE
    private val speedAttr = Attributes.ATTACK_SPEED
    //?} else {
    /*
    private val damageAttr = Attributes.GENERIC_ATTACK_DAMAGE
    private val speedAttr = Attributes.GENERIC_ATTACK_SPEED
    */
    //?}

    //? if >=1.20.5 {
    fun applyTo(settings: Item.Properties, spec: WeaponSpec): Item.Properties {
        val mods = ItemAttributeModifiers.builder()
            .add(
                damageAttr,
                AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, "base_attack_damage_${spec.id}"),
                    spec.attackDamage - 1.0,
                    AttributeModifier.Operation.ADD_VALUE,
                ),
                EquipmentSlotGroup.MAINHAND,
            )
            .add(
                speedAttr,
                AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, "base_attack_speed_${spec.id}"),
                    spec.attackSpeed - 4.0,
                    AttributeModifier.Operation.ADD_VALUE,
                ),
                EquipmentSlotGroup.MAINHAND,
            )
            .build()
        return settings
            .durability(spec.baseDurability)
            .component(DataComponents.ATTRIBUTE_MODIFIERS, mods)
    }

    fun storeFor(spec: WeaponSpec) { /* no-op on Epoch C */ }

    fun modifiersFor(spec: WeaponSpec, slot: EquipmentSlot): Multimap<Attribute, AttributeModifier> =
        ImmutableMultimap.of()
    //?}

    //? if <1.20.5 {
    /*
    private data class CachedMods(
        val damage: AttributeModifier,
        val speed: AttributeModifier,
    )

    private val store = mutableMapOf<String, CachedMods>()

    fun applyTo(settings: Item.Properties, spec: WeaponSpec): Item.Properties {
        storeFor(spec)
        return settings.durability(spec.baseDurability)
    }

    fun storeFor(spec: WeaponSpec) {
        store.getOrPut(spec.id) {
            CachedMods(
                damage = AttributeModifier(
                    UUID.nameUUIDFromBytes("dndweapons:dmg:${spec.id}".toByteArray()),
                    "Weapon base attack damage",
                    (spec.attackDamage - 1).toDouble(),
                    AttributeModifier.Operation.ADDITION,
                ),
                speed = AttributeModifier(
                    UUID.nameUUIDFromBytes("dndweapons:spd:${spec.id}".toByteArray()),
                    "Weapon base attack speed",
                    (spec.attackSpeed - 4.0).toDouble(),
                    AttributeModifier.Operation.ADDITION,
                ),
            )
        }
    }

    fun modifiersFor(spec: WeaponSpec, slot: EquipmentSlot): Multimap<Attribute, AttributeModifier> {
        if (slot != EquipmentSlot.MAINHAND) return ImmutableMultimap.of()
        val cached = store[spec.id] ?: return ImmutableMultimap.of()
        return ImmutableMultimap.builder<Attribute, AttributeModifier>()
            .put(damageAttr, cached.damage)
            .put(speedAttr, cached.speed)
            .build()
    }
    */
    //?}
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/kotlin/com/dndweapons/compat/AttributeCompat.kt
git commit -m "phase-2a-3: convert AttributeCompat to Mojang naming with Epoch A/C //? fork"
```

---

## Task 5: Convert `DndWeaponItem.kt` to Mojang naming

**Files:**
- Modify: `src/main/kotlin/com/dndweapons/item/DndWeaponItem.kt`

- [ ] **Step 1: Replace**

```kotlin
package com.dndweapons.item

import com.dndweapons.catalog.WeaponSpec
import com.dndweapons.compat.AttributeCompat
import net.minecraft.world.item.Item

open class DndWeaponItem(val spec: WeaponSpec, settings: Properties) : Item(settings) {

    init {
        AttributeCompat.storeFor(spec)
    }

    //? if <1.20.5 {
    /*
    override fun getAttributeModifiers(
        slot: net.minecraft.world.entity.EquipmentSlot,
    ): com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> {
        return AttributeCompat.modifiersFor(spec, slot)
    }
    */
    //?}
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/kotlin/com/dndweapons/item/DndWeaponItem.kt
git commit -m "phase-2a-3: convert DndWeaponItem to Mojang naming"
```

---

## Task 6: Convert `RegistrationGametest.kt` to Mojang naming with fabric-gametest API epoch fork

**Files:**
- Modify: `src/main/kotlin/com/dndweapons/test/RegistrationGametest.kt`

The fabric-gametest API changed package + class structure at 1.21.5:
- `<1.21.5`: implement `FabricGameTest` interface, use `@GameTest(templateName)` from Minecraft, take `TestContext` parameter, call `ctx.complete()`.
- `>=1.21.5`: no interface (Fabric registers methods directly), use `@GameTest(structure)` from Fabric, take `GameTestHelper`, call `ctx.succeed()`.

- [ ] **Step 1: Write the dual-epoch gametest**

```kotlin
package com.dndweapons.test

import com.dndweapons.DndWeaponsMod
import net.minecraft.core.registries.BuiltInRegistries

//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation
//?}
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier as ResourceLocation*/
//?}

//? if <1.21.5 {
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper

class RegistrationGametest : FabricGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty")
    fun longswordIsRegistered(ctx: GameTestHelper) {
        val id = makeRl(DndWeaponsMod.MOD_ID, "longsword")
        val airId = makeRl("minecraft", "air")
        val item = BuiltInRegistries.ITEM.get(id)
        val airItem = BuiltInRegistries.ITEM.get(airId)
        if (item == airItem) {
            throw AssertionError("Longsword not registered (resolved to AIR)")
        }
        ctx.succeed()
    }
}
//?}

//? if >=1.21.5 {
/*
import net.fabricmc.fabric.api.gametest.v1.GameTest
import net.minecraft.gametest.framework.GameTestHelper

class RegistrationGametest {

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun longswordIsRegistered(ctx: GameTestHelper) {
        val id = makeRl(DndWeaponsMod.MOD_ID, "longsword")
        val airId = makeRl("minecraft", "air")
        val item = BuiltInRegistries.ITEM.get(id)
        val airItem = BuiltInRegistries.ITEM.get(airId)
        if (item == airItem) {
            throw AssertionError("Longsword not registered (resolved to AIR)")
        }
        ctx.succeed()
    }
}
*/
//?}

private fun makeRl(ns: String, path: String): ResourceLocation {
    //? if >=1.21 {
    return ResourceLocation.fromNamespaceAndPath(ns, path)
    //?} else {
    /*return ResourceLocation(ns, path)*/
    //?}
}
```

Note: at task time, verify the `BuiltInRegistries.ITEM.get` return type on each MC version — comparison may need `.value()` on some versions where the return is `Holder<Item>` or `Optional<Holder<Item>>`. If so, refine the equality check per-epoch.

- [ ] **Step 2: Commit**

```bash
git add src/main/kotlin/com/dndweapons/test/RegistrationGametest.kt
git commit -m "phase-2a-3: convert RegistrationGametest to Mojang naming, fabric-gametest epoch fork"
```

---

## Task 7: Verify 1.21.4 end-to-end (smoke test)

**Files:**
- Create: `docs/plan-runner-out/phase-2a-3-smoke-1.21.4.md`

- [ ] **Step 1: Run all 1.21.4 tasks**

```bash
./gradlew "Set active project to 1.21.4"
./gradlew :1.21.4:build :1.21.4:test :1.21.4:runGametest
```

Expected: BUILD SUCCESSFUL on all three. If anything fails, fix inline and add notes to the smoke-test report. Do NOT proceed to per-version expansion until 1.21.4 (the baseline) is green.

- [ ] **Step 2: Write smoke-test report and commit**

Create `docs/plan-runner-out/phase-2a-3-smoke-1.21.4.md` with build/test/gametest pass-fail and any inline fixes applied. Commit.

---

## Task 8: Verify 1.21.1 (Epoch C, GENERIC_ attribute names)

- [ ] **Step 1: Switch active and build**

```bash
./gradlew "Set active project to 1.21.1"
./gradlew :1.21.1:build :1.21.1:test :1.21.1:runGametest
```

Fix any compile errors that surface — these are the 1.21.1-specific differences from 1.21.4 (`GENERIC_ATTACK_DAMAGE` instead of `ATTACK_DAMAGE`, no `Item.Properties.setId`, possibly different `BuiltInRegistries.get` return type). All differences should already be handled by `//?` directives from Tasks 2-6; if any aren't, refine.

- [ ] **Step 2: Commit refinements**

If any inline fixes were needed:
```bash
git add -A
git commit -m "phase-2a-3: refine //? directives for 1.21.1 specifics"
```

---

## Task 9: Verify 1.21.11 (Identifier rename, fabric-gametest v3)

- [ ] **Step 1: Switch active and build**

```bash
./gradlew "Set active project to 1.21.11"
./gradlew :1.21.11:build :1.21.11:test :1.21.11:runGametest
```

Expected blockers and how to fix each:
- `Unresolved reference 'ResourceLocation'`: confirm the `Identifier as ResourceLocation` alias import is firing. If the alias-import directive doesn't compile-toggle correctly, fall back to changing every body-site reference per directive.
- `BuiltInRegistries.ITEM.get(...)` shape: verify return type for 1.21.11; adjust gametest equality check if needed.
- Fabric gametest API: confirm `>=1.21.5` directive activates the v3 path with `@GameTest(structure=...)` and `GameTestHelper`.

- [ ] **Step 2: Commit refinements**

---

## Task 10: Verify 1.20.1 (Epoch A — old attributes API)

- [ ] **Step 1: Switch active and build**

```bash
./gradlew "Set active project to 1.20.1"
./gradlew :1.20.1:build :1.20.1:test :1.20.1:runGametest
```

Expected: AttributeCompat's `<1.20.5` branch activates, `DndWeaponItem.getAttributeModifiers` override activates. ResourceLocation constructor (not `fromNamespaceAndPath`) activates.

Fix any remaining 1.20.1-specific issues. Particular things to verify at task time:
- `Item.Properties.durability(int)` exists on 1.20.1 (it should — same name back to MC 1.18).
- `AttributeModifier.Operation.ADDITION` exists on 1.20.1 (it should).
- `EquipmentSlot.MAINHAND` exists on 1.20.1 (it does — used in the override).

- [ ] **Step 2: Commit refinements**

---

## Task 11: Add 26.1.2 subproject

**Files:**
- Modify: `settings.gradle.kts` (add `26.1.2` back to versions list)
- Modify: `versions/26.1.2/gradle.properties` (real values per NaturesCompass reference)
- Modify: `build.gradle.kts` (per-version Loom version handling if needed)

- [ ] **Step 1: Update `versions/26.1.2/gradle.properties`**

```properties
minecraft_version=26.1.2
loader_version=0.18.4
fabric_version=0.144.0+26.1
flk_version=1.13.11+kotlin.2.3.21
java_release=25
```

Note: `yarn_mappings` line removed entirely — 26.x doesn't use yarn.

- [ ] **Step 2: Update `settings.gradle.kts`**

Change versions list to:
```kotlin
versions("1.20.1", "1.21.1", "1.21.4", "1.21.11", "26.1.2")
```

- [ ] **Step 3: Materialize the 26.1.2 subproject**

```bash
./gradlew "Set active project to 26.1.2"
```

Expected: Stonecutter materializes versions/26.1.2/. If the switch errors, the Stonecutter directives in our source files have syntax issues — fix them before proceeding.

- [ ] **Step 4: Configure 26.1.2 (Loom version, mappings stub, JDK 25 daemon)**

If `:26.1.2:build` errors at configuration phase because of Loom version, try in `build.gradle.kts` conditional on `mc.startsWith("26.")`:
- Use `loom.noIntermediateMappings()` inside `loom { ... }` block.
- Consider Loom 1.15-SNAPSHOT instead of 1.16.2 for the 26.x subproject (NaturesCompass reference uses 1.15-SNAPSHOT). May need a Stonecutter directive on the loom plugin version line, OR run with a single Loom version that supports both.

JDK 25 daemon required:
```properties
# root gradle.properties
org.gradle.java.home=C:/Program Files/Amazon Corretto/jdk25.0.3_9
```

- [ ] **Step 5: Build, test, gametest 26.1.2**

```bash
./gradlew :26.1.2:build :26.1.2:test :26.1.2:runGametest
```

Iterate on per-version directive refinements until all three pass.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "phase-2a-3: add 26.1.2 subproject — Mojang naming + intermediary stub mappings"
```

---

## Task 12: Final verification across all 5 versions

**Files:**
- Create: `docs/plan-runner-out/phase-2a-3-verification.md`

- [ ] **Step 1: chiseledBuild**

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

- [ ] **Step 3: Write final verification report**

Create `docs/plan-runner-out/phase-2a-3-verification.md` with a 5×3 PASS/FAIL matrix (5 versions × {:build, :test, :runGametest}) and per-cell notes for any inline fixes applied.

- [ ] **Step 4: Tag the phase**

```bash
git tag -a phase-2a-3-mojang-naming -m "Phase 2a-3: 5-version Mojang-naming codebase (1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2)"
git push origin phase-2a-3-mojang-naming
```

- [ ] **Step 5: Commit verification report and push**

```bash
git add -A
git commit -m "phase-2a-3: final verification across 5 MC versions"
git push origin main
```

---

## Done

Phase 2a-3 complete. The mod now ships against all 5 originally-planned MC versions using one source tree with Stonecutter `//?` directives for per-MC API differences. The codebase is on Mojang naming, which is the convention forward and the only path that supports MC 26.x.
