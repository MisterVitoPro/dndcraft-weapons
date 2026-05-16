# DnD Weapons — Phase 2a: Kotlin Conversion + Stonecutter Forks for 4 MC Versions

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. This plan is also consumable by `/plan-runner:run` directly.

**Goal:** Convert the Phase 1 Java codebase to Kotlin, then stand up four additional Stonecutter version subprojects (1.20.1, 1.21.1, 26.1.2, 1.21.11) so Longsword registers, has a working recipe, and passes a gametest on each MC version. Five total versions building from one shared `src/main/kotlin/` tree.

**Architecture:** All Phase 1 `.java` files become `.kt` under `src/main/kotlin/`. Per-epoch attribute-modifier logic moves into one `AttributeCompat.kt` with Stonecutter `//?` directives selecting between Epoch A (1.20.1 — UUID-keyed via `Item.getAttributeModifiers` override) and Epoch C (1.21+ — Identifier-keyed via `DataComponentTypes.ATTRIBUTE_MODIFIERS`). Recipe folder/key differences for 1.20.1 (`recipes/` + `"item"`) are handled by a per-version resource overlay under `versions/1.20.1/src/main/resources/`. 26.1.2 uses `loom.officialMojangMappings()` instead of yarn, selected by a Stonecutter `//?` directive in `build.gradle.kts`.

**Tech Stack:** Kotlin 2.0.21+, Fabric Loom 1.10–1.11, Stonecutter 0.6, `fabric-language-kotlin` (FLK) per-MC version, JUnit 5 Kotlin DSL, Fabric Gametest, Java 17 for 1.20.1 / Java 21 for the rest (build JDK is always 21).

**Predecessor spec:** [docs/superpowers/specs/2026-05-16-dnd-weapons-phase-2a-design.md](../specs/2026-05-16-dnd-weapons-phase-2a-design.md)

**Predecessor plan (already executed):** [docs/superpowers/plans/2026-05-16-dnd-weapons-phase-1-foundation.md](2026-05-16-dnd-weapons-phase-1-foundation.md)

---

## File Inventory

**Created:**
```
src/main/kotlin/com/dndweapons/DndWeaponsMod.kt
src/main/kotlin/com/dndweapons/catalog/Category.kt
src/main/kotlin/com/dndweapons/catalog/DamageType.kt
src/main/kotlin/com/dndweapons/catalog/Property.kt
src/main/kotlin/com/dndweapons/catalog/RangeKind.kt
src/main/kotlin/com/dndweapons/catalog/WeaponSpec.kt
src/main/kotlin/com/dndweapons/catalog/Weapons.kt
src/main/kotlin/com/dndweapons/item/DndWeaponItem.kt
src/main/kotlin/com/dndweapons/registry/WeaponRegistrar.kt
src/main/kotlin/com/dndweapons/registry/WeaponRegistrarImpl.kt
src/main/kotlin/com/dndweapons/compat/AttributeCompat.kt
src/main/kotlin/com/dndweapons/test/RegistrationGametest.kt
src/test/kotlin/com/dndweapons/catalog/WeaponSpecTest.kt
src/test/kotlin/com/dndweapons/catalog/WeaponsTest.kt

versions/1.20.1/gradle.properties
versions/1.20.1/src/main/resources/data/dndweapons/recipes/longsword.json
versions/1.21.1/gradle.properties
versions/26.1.2/gradle.properties
versions/1.21.11/gradle.properties

docs/plan-runner-out/phase-2a-verification.md   # written by final verification task
```

**Modified:**
```
build.gradle.kts                  # Kotlin plugin, jvmTarget, FLK dep, Mojang mappings //?
gradle.properties                 # kotlin_version
settings.gradle.kts               # versions list (1.20.1, 1.21.1, 1.21.4, 26.1.2, 1.21.11)
versions/1.21.4/gradle.properties # add flk_version + java_release=21
src/main/resources/fabric.mod.json # adapter:"kotlin" + FLK depend
```

**Deleted (Phase 1 Java carryover):**
```
src/main/java/com/dndweapons/DndWeaponsMod.java
src/main/java/com/dndweapons/catalog/Category.java
src/main/java/com/dndweapons/catalog/DamageType.java
src/main/java/com/dndweapons/catalog/Property.java
src/main/java/com/dndweapons/catalog/RangeKind.java
src/main/java/com/dndweapons/catalog/WeaponSpec.java
src/main/java/com/dndweapons/catalog/Weapons.java
src/main/java/com/dndweapons/item/DndWeaponItem.java
src/main/java/com/dndweapons/registry/WeaponRegistrar.java
src/main/java/com/dndweapons/registry/WeaponRegistrarImpl.java
src/main/java/com/dndweapons/test/RegistrationGametest.java
src/test/java/com/dndweapons/catalog/WeaponSpecTest.java
src/test/java/com/dndweapons/catalog/WeaponsTest.java
```

After all tasks land, `src/main/java/` and `src/test/java/` should be empty trees (or removed entirely).

---

## Task 1: Add Kotlin Gradle plugin + FLK runtime dependency

**Files:**
- Modify: `build.gradle.kts`
- Modify: `gradle.properties`
- Modify: `versions/1.21.4/gradle.properties`
- Modify: `src/main/resources/fabric.mod.json`

- [ ] **Step 1: Add `kotlin_version` to root `gradle.properties`**

Append to `gradle.properties`:
```properties
kotlin_version=2.0.21
```

- [ ] **Step 2: Add `flk_version` and `java_release` to `versions/1.21.4/gradle.properties`**

Append to `versions/1.21.4/gradle.properties`:
```properties
flk_version=1.13.5+kotlin.2.0.21
java_release=21
```

If Context7 indicates a more current FLK release line for 1.21.4 at task time, use that version. The shape `<flk>+kotlin.<kotlinver>` is the FLK convention.

- [ ] **Step 3: Replace `build.gradle.kts` with Kotlin-aware version**

Replace the full contents of `build.gradle.kts`:
```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("dev.kikugie.stonecutter")
}

val mcVersion = stonecutter.current.version
val modVersion: String by project
val modGroup: String by project
val modId: String by project
val javaRelease = (property("java_release") as String).toInt()

version = "$modVersion+mc$mcVersion"
group = modGroup
base.archivesName.set(modId)

java {
    sourceCompatibility = JavaVersion.toVersion(javaRelease)
    targetCompatibility = JavaVersion.toVersion(javaRelease)
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    //? if MC >= 26.1 {
    /*mappings(loom.officialMojangMappings())*/
    //?} else {
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    //?}
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("flk_version")}")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

loom {
    runs {
        register("gametest") {
            server()
            name = "Game Test Server"
            vmArg("-Dfabric-api.gametest")
            vmArg("-Dfabric-api.gametest.report-file=${layout.buildDirectory.get()}/gametest-report.xml")
            runDir = "build/gametest"
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaRelease)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(javaRelease.toString()))
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}
```

- [ ] **Step 4: Update `src/main/resources/fabric.mod.json` for Kotlin adapter + FLK dependency**

Replace `fabric.mod.json` contents:
```json
{
  "schemaVersion": 1,
  "id": "dndweapons",
  "version": "${version}",
  "name": "DnD Weapons",
  "description": "Adds the simple and martial weapons from the 2024 D&D Player's Handbook.",
  "authors": ["MisterVitoPro"],
  "license": "MIT",
  "environment": "*",
  "entrypoints": {
    "main": [
      { "adapter": "kotlin", "value": "com.dndweapons.DndWeaponsMod" }
    ],
    "fabric-gametest": [
      { "adapter": "kotlin", "value": "com.dndweapons.test.RegistrationGametest" }
    ]
  },
  "depends": {
    "fabricloader": ">=0.16.0",
    "minecraft": "~1.21.4",
    "java": ">=21",
    "fabric-api": "*",
    "fabric-language-kotlin": ">=1.13.0"
  }
}
```

- [ ] **Step 5: Verify Phase 1 (Java) still builds with Kotlin plugin loaded**

Run:
```bash
./gradlew :1.21.4:build
```

Expected: BUILD SUCCESSFUL. The Kotlin plugin is loaded but there are no `.kt` files yet, so the build is identical to Phase 1.

If it fails with `flk_version` resolution, double-check `versions/1.21.4/gradle.properties` has the property AND FLK's Maven coordinate matches a published artifact (check at https://maven.fabricmc.net/net/fabricmc/fabric-language-kotlin/ or via Context7).

- [ ] **Step 6: Commit**

```bash
git add build.gradle.kts gradle.properties versions/1.21.4/gradle.properties src/main/resources/fabric.mod.json
git commit -m "chore(phase-2a): add Kotlin plugin + FLK dep + Kotlin entrypoint adapter"
```

---

## Task 2: Convert catalog enums from Java to Kotlin

**Files:**
- Create: `src/main/kotlin/com/dndweapons/catalog/Category.kt`
- Create: `src/main/kotlin/com/dndweapons/catalog/DamageType.kt`
- Create: `src/main/kotlin/com/dndweapons/catalog/Property.kt`
- Create: `src/main/kotlin/com/dndweapons/catalog/RangeKind.kt`
- Delete: `src/main/java/com/dndweapons/catalog/Category.java`
- Delete: `src/main/java/com/dndweapons/catalog/DamageType.java`
- Delete: `src/main/java/com/dndweapons/catalog/Property.java`
- Delete: `src/main/java/com/dndweapons/catalog/RangeKind.java`

- [ ] **Step 1: Create `Category.kt`**

Create `src/main/kotlin/com/dndweapons/catalog/Category.kt`:
```kotlin
package com.dndweapons.catalog

enum class Category {
    SIMPLE_MELEE,
    SIMPLE_RANGED,
    MARTIAL_MELEE,
    MARTIAL_RANGED,
}
```

- [ ] **Step 2: Create `DamageType.kt`**

Create `src/main/kotlin/com/dndweapons/catalog/DamageType.kt`:
```kotlin
package com.dndweapons.catalog

enum class DamageType {
    SLASHING,
    PIERCING,
    BLUDGEONING,
}
```

- [ ] **Step 3: Create `Property.kt`**

Create `src/main/kotlin/com/dndweapons/catalog/Property.kt`:
```kotlin
package com.dndweapons.catalog

enum class Property {
    LIGHT,
    HEAVY,
    FINESSE,
    REACH,
    TWO_HANDED,
    VERSATILE,
    THROWN,
    AMMUNITION,
    LOADING,
    SPECIAL_LANCE,
}
```

- [ ] **Step 4: Create `RangeKind.kt`**

Create `src/main/kotlin/com/dndweapons/catalog/RangeKind.kt`:
```kotlin
package com.dndweapons.catalog

enum class RangeKind {
    NONE,
    THROWN,
    BOW,
    CROSSBOW,
    FIREARM,
    SLING,
    BLOWGUN,
}
```

- [ ] **Step 5: Delete the four Java enum files**

```bash
rm src/main/java/com/dndweapons/catalog/Category.java
rm src/main/java/com/dndweapons/catalog/DamageType.java
rm src/main/java/com/dndweapons/catalog/Property.java
rm src/main/java/com/dndweapons/catalog/RangeKind.java
```

- [ ] **Step 6: Commit**

```bash
git add -A src/main/kotlin/com/dndweapons/catalog/ src/main/java/com/dndweapons/catalog/
git commit -m "refactor(phase-2a): convert catalog enums to Kotlin"
```

---

## Task 3: Convert `WeaponSpec` to Kotlin data class + tests

**Files:**
- Create: `src/main/kotlin/com/dndweapons/catalog/WeaponSpec.kt`
- Create: `src/test/kotlin/com/dndweapons/catalog/WeaponSpecTest.kt`
- Delete: `src/main/java/com/dndweapons/catalog/WeaponSpec.java`
- Delete: `src/test/java/com/dndweapons/catalog/WeaponSpecTest.java`

- [ ] **Step 1: Write `WeaponSpec.kt`**

Create `src/main/kotlin/com/dndweapons/catalog/WeaponSpec.kt`:
```kotlin
package com.dndweapons.catalog

/**
 * Immutable description of one DnD weapon. Single source of truth for the
 * entire catalog. Safe to construct in static initializers on all target
 * Minecraft versions (does not reference Item or registry types).
 *
 * @param vanillaRoleTag identifier of the role tag (e.g. "dndweapons:role/shortsword")
 *                       when this spec is fulfilled by a vanilla item. When non-null,
 *                       no Item is registered for this spec; the tag's members carry
 *                       the DnD identity. Resolved lazily at runtime.
 */
data class WeaponSpec(
    val id: String,
    val displayName: String,
    val category: Category,
    val damageType: DamageType,
    val diceText: String,
    val versatileDice: String?,
    val attackDamage: Int,
    val versatileBonus: Int,
    val attackSpeed: Float,
    val reachBonus: Float,
    val knockbackBonus: Int,
    val properties: Set<Property>,
    val ranged: RangeKind,
    val baseDurability: Int,
    val vanillaRoleTag: String?,
) {
    init {
        require(id.isNotBlank()) { "WeaponSpec id must be non-blank" }
        require(displayName.isNotBlank()) { "WeaponSpec displayName must be non-blank" }
    }

    val isVanillaMapped: Boolean get() = vanillaRoleTag != null
}
```

- [ ] **Step 2: Write `WeaponSpecTest.kt`**

Create `src/test/kotlin/com/dndweapons/catalog/WeaponSpecTest.kt`:
```kotlin
package com.dndweapons.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WeaponSpecTest {

    @Test
    fun canConstructMinimalMeleeSpec() {
        val longsword = WeaponSpec(
            id = "longsword", displayName = "Longsword",
            category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
            diceText = "1d8", versatileDice = "1d10",
            attackDamage = 6, versatileBonus = 1,
            attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
            properties = setOf(Property.VERSATILE),
            ranged = RangeKind.NONE, baseDurability = 250,
            vanillaRoleTag = null,
        )
        assertEquals("longsword", longsword.id)
        assertEquals(6, longsword.attackDamage)
        assertTrue(longsword.properties.contains(Property.VERSATILE))
    }

    @Test
    fun blankIdIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            WeaponSpec(
                id = "", displayName = "Longsword",
                category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
                diceText = "1d8", versatileDice = null,
                attackDamage = 6, versatileBonus = 0,
                attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
                properties = emptySet(),
                ranged = RangeKind.NONE, baseDurability = 250,
                vanillaRoleTag = null,
            )
        }
    }

    @Test
    fun vanillaRoleTagWhenSetMakesItVanillaMapped() {
        val shortsword = WeaponSpec(
            id = "shortsword", displayName = "Shortsword",
            category = Category.MARTIAL_MELEE, damageType = DamageType.PIERCING,
            diceText = "1d6", versatileDice = null,
            attackDamage = 5, versatileBonus = 0,
            attackSpeed = 1.8f, reachBonus = 0.0f, knockbackBonus = 0,
            properties = setOf(Property.LIGHT, Property.FINESSE),
            ranged = RangeKind.NONE, baseDurability = 250,
            vanillaRoleTag = "dndweapons:role/shortsword",
        )
        assertNotNull(shortsword.vanillaRoleTag)
        assertTrue(shortsword.isVanillaMapped)
    }

    @Test
    fun nullVanillaRoleTagMeansRegisteredItem() {
        val longsword = WeaponSpec(
            id = "longsword", displayName = "Longsword",
            category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
            diceText = "1d8", versatileDice = "1d10",
            attackDamage = 6, versatileBonus = 1,
            attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
            properties = setOf(Property.VERSATILE),
            ranged = RangeKind.NONE, baseDurability = 250,
            vanillaRoleTag = null,
        )
        assertNull(longsword.vanillaRoleTag)
        assertFalse(longsword.isVanillaMapped)
    }
}
```

- [ ] **Step 3: Delete the Java versions**

```bash
rm src/main/java/com/dndweapons/catalog/WeaponSpec.java
rm src/test/java/com/dndweapons/catalog/WeaponSpecTest.java
```

- [ ] **Step 4: Commit**

```bash
git add -A src/main/kotlin/com/dndweapons/catalog/WeaponSpec.kt src/test/kotlin/com/dndweapons/catalog/WeaponSpecTest.kt src/main/java/com/dndweapons/catalog/WeaponSpec.java src/test/java/com/dndweapons/catalog/WeaponSpecTest.java
git commit -m "refactor(phase-2a): convert WeaponSpec + tests to Kotlin data class"
```

---

## Task 4: Convert `Weapons` to Kotlin object + tests

**Files:**
- Create: `src/main/kotlin/com/dndweapons/catalog/Weapons.kt`
- Create: `src/test/kotlin/com/dndweapons/catalog/WeaponsTest.kt`
- Delete: `src/main/java/com/dndweapons/catalog/Weapons.java`
- Delete: `src/test/java/com/dndweapons/catalog/WeaponsTest.java`

- [ ] **Step 1: Write `Weapons.kt`**

Create `src/main/kotlin/com/dndweapons/catalog/Weapons.kt`:
```kotlin
package com.dndweapons.catalog

/**
 * Source of truth for the entire weapon catalog. Phase 2a contains only the
 * Longsword; Phase 2b expands to the remaining 33 registered weapons plus 4
 * vanilla-mapped role specs.
 */
object Weapons {

    val LONGSWORD = WeaponSpec(
        id = "longsword", displayName = "Longsword",
        category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
        diceText = "1d8", versatileDice = "1d10",
        attackDamage = 6, versatileBonus = 1,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.VERSATILE),
        ranged = RangeKind.NONE, baseDurability = 250,
        vanillaRoleTag = null,
    )

    val ALL: List<WeaponSpec> = listOf(LONGSWORD)
}
```

- [ ] **Step 2: Write `WeaponsTest.kt`**

Create `src/test/kotlin/com/dndweapons/catalog/WeaponsTest.kt`:
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
    fun allIdsAreUnique() {
        val seen = mutableSetOf<String>()
        for (spec in Weapons.ALL) {
            assertTrue(seen.add(spec.id), "duplicate spec id: ${spec.id}")
        }
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

- [ ] **Step 3: Delete the Java versions**

```bash
rm src/main/java/com/dndweapons/catalog/Weapons.java
rm src/test/java/com/dndweapons/catalog/WeaponsTest.java
```

- [ ] **Step 4: Commit**

```bash
git add -A src/main/kotlin/com/dndweapons/catalog/Weapons.kt src/test/kotlin/com/dndweapons/catalog/WeaponsTest.kt src/main/java/com/dndweapons/catalog/Weapons.java src/test/java/com/dndweapons/catalog/WeaponsTest.java
git commit -m "refactor(phase-2a): convert Weapons + tests to Kotlin object"
```

---

## Task 5: Convert `DndWeaponItem` to Kotlin

**Files:**
- Create: `src/main/kotlin/com/dndweapons/item/DndWeaponItem.kt`
- Delete: `src/main/java/com/dndweapons/item/DndWeaponItem.java`

This task creates the Kotlin class WITHOUT the Epoch A `getAttributeModifiers` override (added in Task 12). The class compiles cleanly on 1.21+ as-is.

- [ ] **Step 1: Write `DndWeaponItem.kt`**

Create `src/main/kotlin/com/dndweapons/item/DndWeaponItem.kt`:
```kotlin
package com.dndweapons.item

import com.dndweapons.catalog.WeaponSpec
import com.dndweapons.compat.AttributeCompat
import net.minecraft.item.Item

/**
 * Generic melee weapon. Carries a back-reference to its spec so combat hooks
 * (Phase 3) can look up properties at runtime. Phase 2a does not implement
 * any combat hooks - this is just the item identity.
 *
 * On Epoch A (1.20.1), DndWeaponItem.getAttributeModifiers is overridden via
 * Stonecutter //? directive (added in Task 12) to read from AttributeCompat.
 * On Epoch C (1.21+), attributes come from the data component baked into
 * Item.Settings by AttributeCompat.applyTo - no override needed.
 */
open class DndWeaponItem(val spec: WeaponSpec, settings: Settings) : Item(settings) {
    init {
        AttributeCompat.storeFor(spec)
    }
}
```

Note: this file imports `AttributeCompat` which is created in Task 7. Task ordering ensures Task 7 lands before this task's import is referenced at compile time. Within plan-runner, both files land in the same wave or successive waves; the dev agent does not run the compiler, so order does not matter for file-creation. When the compiler runs at the end, both files exist.

- [ ] **Step 2: Delete the Java version**

```bash
rm src/main/java/com/dndweapons/item/DndWeaponItem.java
```

- [ ] **Step 3: Commit**

```bash
git add -A src/main/kotlin/com/dndweapons/item/DndWeaponItem.kt src/main/java/com/dndweapons/item/DndWeaponItem.java
git commit -m "refactor(phase-2a): convert DndWeaponItem to Kotlin"
```

---

## Task 6: Convert `WeaponRegistrar` interface to Kotlin

**Files:**
- Create: `src/main/kotlin/com/dndweapons/registry/WeaponRegistrar.kt`
- Delete: `src/main/java/com/dndweapons/registry/WeaponRegistrar.java`

- [ ] **Step 1: Write `WeaponRegistrar.kt`**

Create `src/main/kotlin/com/dndweapons/registry/WeaponRegistrar.kt`:
```kotlin
package com.dndweapons.registry

import com.dndweapons.catalog.WeaponSpec

interface WeaponRegistrar {
    fun register(spec: WeaponSpec)

    fun registerAll(specs: List<WeaponSpec>) {
        specs.forEach(::register)
    }
}
```

- [ ] **Step 2: Delete the Java version**

```bash
rm src/main/java/com/dndweapons/registry/WeaponRegistrar.java
```

- [ ] **Step 3: Commit**

```bash
git add -A src/main/kotlin/com/dndweapons/registry/WeaponRegistrar.kt src/main/java/com/dndweapons/registry/WeaponRegistrar.java
git commit -m "refactor(phase-2a): convert WeaponRegistrar interface to Kotlin"
```

---

## Task 7: Create `AttributeCompat.kt` (Epoch C only, no Stonecutter directives yet)

**Files:**
- Create: `src/main/kotlin/com/dndweapons/compat/AttributeCompat.kt`

This task creates the file in its 1.21+ form. Task 12 adds the Stonecutter `//?` directives that fork it for Epoch A (1.20.1).

- [ ] **Step 1: Write `AttributeCompat.kt`**

Create `src/main/kotlin/com/dndweapons/compat/AttributeCompat.kt`:
```kotlin
package com.dndweapons.compat

import com.dndweapons.DndWeaponsMod
import com.dndweapons.catalog.WeaponSpec
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.AttributeModifierSlot
import net.minecraft.component.type.AttributeModifiersComponent
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.item.Item
import net.minecraft.util.Identifier

/**
 * Per-epoch attribute-modifier builder. Stonecutter directives select the body
 * at build time. Task 12 of Phase 2a adds the //? if MC >= 1.21 / else branches;
 * this initial file is the 1.21+ (Epoch C) body only.
 *
 * Epoch C (1.21+): writes attributes into the ATTRIBUTE_MODIFIERS data component
 * on Item.Settings. No Item.getAttributeModifiers override needed.
 *
 * Epoch A (1.20.1): cached UUID-keyed modifiers per spec; DndWeaponItem
 * overrides getAttributeModifiers(slot) and delegates here.
 */
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

    /** No-op on Epoch C. On Epoch A this caches UUID-keyed modifiers per spec. */
    fun storeFor(spec: WeaponSpec) {
        // intentional no-op on Epoch C
    }

    /** Empty on Epoch C (attributes resolved via data component). Epoch A returns the cached pair. */
    fun modifiersFor(spec: WeaponSpec, slot: EquipmentSlot): Multimap<EntityAttribute, EntityAttributeModifier> {
        return ImmutableMultimap.of()
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/kotlin/com/dndweapons/compat/AttributeCompat.kt
git commit -m "feat(phase-2a): add AttributeCompat (Epoch C only; Stonecutter forks come in Task 12)"
```

---

## Task 8: Convert `WeaponRegistrarImpl` to Kotlin using `AttributeCompat`

**Files:**
- Create: `src/main/kotlin/com/dndweapons/registry/WeaponRegistrarImpl.kt`
- Delete: `src/main/java/com/dndweapons/registry/WeaponRegistrarImpl.java`

- [ ] **Step 1: Write `WeaponRegistrarImpl.kt`**

Create `src/main/kotlin/com/dndweapons/registry/WeaponRegistrarImpl.kt`:
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

/**
 * Thin registrar. All per-epoch knowledge is in AttributeCompat; this class
 * is identical across all 5 MC versions.
 */
class WeaponRegistrarImpl : WeaponRegistrar {

    override fun register(spec: WeaponSpec) {
        if (spec.isVanillaMapped) {
            // Phase 2b wires this up via role tags. For now skip silently.
            return
        }

        val itemId = Identifier.of(DndWeaponsMod.MOD_ID, spec.id)
        val settings = AttributeCompat.applyTo(Item.Settings(), spec)
        val item = DndWeaponItem(spec, settings)

        Registry.register(Registries.ITEM, itemId, item)
        DndWeaponsMod.LOGGER.info("Registered weapon: {}", itemId)
    }
}
```

- [ ] **Step 2: Delete the Java version**

```bash
rm src/main/java/com/dndweapons/registry/WeaponRegistrarImpl.java
```

- [ ] **Step 3: Commit**

```bash
git add -A src/main/kotlin/com/dndweapons/registry/WeaponRegistrarImpl.kt src/main/java/com/dndweapons/registry/WeaponRegistrarImpl.java
git commit -m "refactor(phase-2a): convert WeaponRegistrarImpl to Kotlin (thin; delegates to AttributeCompat)"
```

---

## Task 9: Convert `DndWeaponsMod` to Kotlin `object`

**Files:**
- Create: `src/main/kotlin/com/dndweapons/DndWeaponsMod.kt`
- Delete: `src/main/java/com/dndweapons/DndWeaponsMod.java`

- [ ] **Step 1: Write `DndWeaponsMod.kt`**

Create `src/main/kotlin/com/dndweapons/DndWeaponsMod.kt`:
```kotlin
package com.dndweapons

import com.dndweapons.catalog.Weapons
import com.dndweapons.registry.WeaponRegistrarImpl
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object DndWeaponsMod : ModInitializer {

    const val MOD_ID: String = "dndweapons"
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    val CREATIVE_TAB: RegistryKey<ItemGroup> =
        RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MOD_ID, "main"))

    override fun onInitialize() {
        LOGGER.info("DnD Weapons initializing...")

        val registrar = WeaponRegistrarImpl()
        registrar.registerAll(Weapons.ALL)

        Registry.register(
            Registries.ITEM_GROUP, CREATIVE_TAB,
            FabricItemGroup.builder()
                .displayName(Text.translatable("itemGroup.dndweapons.main"))
                .icon { ItemStack(Registries.ITEM.get(Identifier.of(MOD_ID, "longsword"))) }
                .build(),
        )

        ItemGroupEvents.modifyEntriesEvent(CREATIVE_TAB).register { entries ->
            for (spec in Weapons.ALL) {
                if (spec.isVanillaMapped) continue
                val item = Registries.ITEM.get(Identifier.of(MOD_ID, spec.id))
                if (item != null) entries.add(item)
            }
        }

        LOGGER.info("DnD Weapons initialized with {} weapons.", Weapons.ALL.size)
    }
}
```

- [ ] **Step 2: Delete the Java version**

```bash
rm src/main/java/com/dndweapons/DndWeaponsMod.java
```

- [ ] **Step 3: Commit**

```bash
git add -A src/main/kotlin/com/dndweapons/DndWeaponsMod.kt src/main/java/com/dndweapons/DndWeaponsMod.java
git commit -m "refactor(phase-2a): convert DndWeaponsMod to Kotlin object"
```

---

## Task 10: Convert `RegistrationGametest` to Kotlin

**Files:**
- Create: `src/main/kotlin/com/dndweapons/test/RegistrationGametest.kt`
- Delete: `src/main/java/com/dndweapons/test/RegistrationGametest.java`

- [ ] **Step 1: Write `RegistrationGametest.kt`**

Create `src/main/kotlin/com/dndweapons/test/RegistrationGametest.kt`:
```kotlin
package com.dndweapons.test

import com.dndweapons.DndWeaponsMod
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.registry.Registries
import net.minecraft.test.GameTest
import net.minecraft.test.TestContext
import net.minecraft.util.Identifier

class RegistrationGametest : FabricGameTest {

    @GameTest(templateName = "fabric-gametest-api-v1:empty")
    fun longswordIsRegistered(ctx: TestContext) {
        val id = Identifier.of(DndWeaponsMod.MOD_ID, "longsword")
        val item = Registries.ITEM.get(id)
        if (item == Registries.ITEM.get(Identifier.ofVanilla("air"))) {
            throw AssertionError("Longsword not registered (resolved to AIR)")
        }
        ctx.complete()
    }
}
```

- [ ] **Step 2: Delete the Java version**

```bash
rm src/main/java/com/dndweapons/test/RegistrationGametest.java
```

- [ ] **Step 3: Commit**

```bash
git add -A src/main/kotlin/com/dndweapons/test/RegistrationGametest.kt src/main/java/com/dndweapons/test/RegistrationGametest.java
git commit -m "refactor(phase-2a): convert RegistrationGametest to Kotlin"
```

---

## Task 11: Smoke-test the Kotlin conversion on 1.21.4

**Files:**
- Create: `docs/plan-runner-out/phase-2a-kotlin-smoke.md`

This task is the post-conversion checkpoint. It runs gradle commands and writes a brief report. If any step fails, the task surfaces it; subsequent tasks must wait for the fix.

- [ ] **Step 1: Compile the 1.21.4 subproject**

Run:
```bash
./gradlew :1.21.4:build
```

Expected: BUILD SUCCESSFUL. The Kotlin compiler runs against `src/main/kotlin/` for the first time and produces classes; Loom remaps the jar.

If the compile fails:
- Missing import: open the failing `.kt` file, add the import.
- Type mismatch on `Identifier.of` / `EntityAttributeModifier`: confirm the imports match `net.minecraft.util.Identifier`, `net.minecraft.entity.attribute.EntityAttributeModifier`, and that you're on Yarn `1.21.4+build.8` (per `versions/1.21.4/gradle.properties`).
- `FabricGameTest` not found: confirm Fabric API version in `versions/1.21.4/gradle.properties` is `0.115.0+1.21.4` and `fabric-api` is `modImplementation`-ed in `build.gradle.kts`.
- Kotlin plugin missing or wrong version: re-run Task 1 Step 3 to ensure `id("org.jetbrains.kotlin.jvm") version "2.0.21"` is in `build.gradle.kts`.

- [ ] **Step 2: Run JUnit tests**

Run:
```bash
./gradlew :1.21.4:test
```

Expected: 7 tests pass (4 from `WeaponSpecTest` + 3 from `WeaponsTest`).

- [ ] **Step 3: Run gametest**

Run:
```bash
./gradlew :1.21.4:runGametest
```

Expected: BUILD SUCCESSFUL; gametest report shows `longswordIsRegistered PASSED`.

If gametest fails with "entrypoint not found" / "no fabric-gametest entries":
- Confirm `fabric.mod.json` has the Kotlin adapter on the `fabric-gametest` entry: `{"adapter": "kotlin", "value": "com.dndweapons.test.RegistrationGametest"}`.
- Confirm `fabric-language-kotlin` is in `depends` block of `fabric.mod.json`.

- [ ] **Step 4: Write smoke-test report**

Create `docs/plan-runner-out/phase-2a-kotlin-smoke.md`:
```markdown
# Phase 2a Kotlin Conversion Smoke Test

**Date:** <YYYY-MM-DD HH:MM>

## Results on 1.21.4

- `:1.21.4:build` : <PASS|FAIL>
- `:1.21.4:test`  : <PASS|FAIL>  (expected 7 tests)
- `:1.21.4:runGametest` : <PASS|FAIL>

## Notes

<one paragraph: any caveats, version pins that had to be adjusted, etc.>
```

Fill in the actual results. If any step failed, write FAIL and add a Notes paragraph about the failure mode and what was needed to unstick it.

- [ ] **Step 5: Commit**

```bash
git add docs/plan-runner-out/phase-2a-kotlin-smoke.md
git commit -m "test(phase-2a): smoke-test Kotlin conversion on 1.21.4"
```

---

## Task 12: Add Epoch A (1.20.1) branch to `AttributeCompat.kt` via Stonecutter `//?` directives

**Files:**
- Modify: `src/main/kotlin/com/dndweapons/compat/AttributeCompat.kt`

- [ ] **Step 1: Replace `AttributeCompat.kt` with the dual-epoch version**

Overwrite `src/main/kotlin/com/dndweapons/compat/AttributeCompat.kt`:
```kotlin
package com.dndweapons.compat

import com.dndweapons.catalog.WeaponSpec
import net.minecraft.item.Item

//? if MC >= 1.21 {
import com.dndweapons.DndWeaponsMod
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.AttributeModifierSlot
import net.minecraft.component.type.AttributeModifiersComponent
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.util.Identifier

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

    fun modifiersFor(spec: WeaponSpec, slot: EquipmentSlot): Multimap<EntityAttribute, EntityAttributeModifier> =
        ImmutableMultimap.of()
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
                    (spec.attackDamage - 1).toDouble(),
                    EntityAttributeModifier.Operation.ADDITION,
                ),
                speed = EntityAttributeModifier(
                    UUID.nameUUIDFromBytes("dndweapons:spd:${spec.id}".toByteArray()),
                    "Weapon base attack speed",
                    (spec.attackSpeed - 4.0).toDouble(),
                    EntityAttributeModifier.Operation.ADDITION,
                ),
            )
        }
    }

    fun modifiersFor(spec: WeaponSpec, slot: EquipmentSlot): Multimap<EntityAttribute, EntityAttributeModifier> {
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

Notes:
- The `//?` directive comment-toggles. On 1.21+ active, the Epoch C body is uncommented; the Epoch A body inside `/* ... */` stays commented. On 1.20.1 active, Stonecutter swaps which is uncommented.
- Stonecutter directives are line-prefixed: `//?` is the cursor. `//?}` closes a block. The `else` keyword and `if MC >= 1.21` predicate follow Stonecutter's documented syntax.
- If Stonecutter complains about the syntax, query Context7 for the current Stonecutter 0.6 directive docs and adjust.

- [ ] **Step 2: Commit**

```bash
git add src/main/kotlin/com/dndweapons/compat/AttributeCompat.kt
git commit -m "feat(phase-2a): fork AttributeCompat for Epoch A (1.20.1) via Stonecutter //? directives"
```

---

## Task 13: Add Epoch A override to `DndWeaponItem.kt`

**Files:**
- Modify: `src/main/kotlin/com/dndweapons/item/DndWeaponItem.kt`

- [ ] **Step 1: Add the Stonecutter-gated `getAttributeModifiers` override**

Overwrite `src/main/kotlin/com/dndweapons/item/DndWeaponItem.kt`:
```kotlin
package com.dndweapons.item

import com.dndweapons.catalog.WeaponSpec
import com.dndweapons.compat.AttributeCompat
import net.minecraft.item.Item

open class DndWeaponItem(val spec: WeaponSpec, settings: Settings) : Item(settings) {

    init {
        AttributeCompat.storeFor(spec)
    }

    //? if MC < 1.20.5 {
    /*
    override fun getAttributeModifiers(
        slot: net.minecraft.entity.EquipmentSlot,
    ): com.google.common.collect.Multimap<net.minecraft.entity.attribute.EntityAttribute, net.minecraft.entity.attribute.EntityAttributeModifier> {
        return AttributeCompat.modifiersFor(spec, slot)
    }
    */
    //?}
}
```

On 1.20.1 the override is active (Stonecutter un-comments the `/* */` block). On 1.21+ the override stays commented out (vanilla Item has no such method to override on 1.21+; uncomment would be a compile error).

- [ ] **Step 2: Commit**

```bash
git add src/main/kotlin/com/dndweapons/item/DndWeaponItem.kt
git commit -m "feat(phase-2a): add Epoch A getAttributeModifiers override to DndWeaponItem via Stonecutter //?"
```

---

## Task 14: Declare all 5 versions in `settings.gradle.kts`

**Files:**
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Update the `versions(...)` list**

Replace the contents of `settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.6"
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    create(rootProject) {
        versions("1.20.1", "1.21.1", "1.21.4", "26.1.2", "1.21.11")
        vcsVersion = "1.21.4"
    }
}

rootProject.name = "dnd-weapons"
```

- [ ] **Step 2: Trigger Stonecutter to materialize new subprojects**

Run:
```bash
./gradlew "Set active project" -PccVersion=1.21.4
```

Expected: Stonecutter creates `versions/1.20.1/`, `versions/1.21.1/`, `versions/26.1.2/`, `versions/1.21.11/` skeleton dirs (each gets a generated `build.gradle.kts` pointing back at the central script). `versions/1.21.4/` is preserved.

If this command fails with "could not materialize subproject":
- This is the **Stonecutter sibling-subproject smoke-test failure**. Per the Phase 2a spec, this is the trigger for falling back to git-branches-per-version. STOP and consult the user before continuing.
- Likely cause is Stonecutter version mismatch or pluginManagement repo not reachable.

- [ ] **Step 3: Commit `settings.gradle.kts` and any Stonecutter-generated subproject `build.gradle.kts` files**

```bash
git add settings.gradle.kts versions/
git commit -m "chore(phase-2a): declare 5 versions in Stonecutter (1.20.1, 1.21.1, 1.21.4, 26.1.2, 1.21.11)"
```

---

## Task 15: Add `versions/1.21.1/gradle.properties`

**Files:**
- Create: `versions/1.21.1/gradle.properties`

- [ ] **Step 1: Write the properties file**

Create `versions/1.21.1/gradle.properties`:
```properties
minecraft_version=1.21.1
yarn_mappings=1.21.1+build.3
loader_version=0.16.10
fabric_version=0.115.4+1.21.1
flk_version=1.13.5+kotlin.2.0.21
java_release=21
```

If Context7 reports more current values for `yarn_mappings`, `fabric_version`, or `flk_version` for 1.21.1 at task time, use those.

- [ ] **Step 2: Commit**

```bash
git add versions/1.21.1/gradle.properties
git commit -m "chore(phase-2a): pin 1.21.1 deps (Loom, Yarn, Loader, Fabric API, FLK)"
```

---

## Task 16: Add `versions/1.21.11/gradle.properties`

**Files:**
- Create: `versions/1.21.11/gradle.properties`

- [ ] **Step 1: Write the properties file**

Create `versions/1.21.11/gradle.properties`:
```properties
minecraft_version=1.21.11
yarn_mappings=1.21.11+build.1
loader_version=0.17.0
fabric_version=0.130.0+1.21.11
flk_version=1.14.0+kotlin.2.0.21
java_release=21
```

The `yarn_mappings`, `loader_version`, `fabric_version`, and `flk_version` values for 1.21.11 must be verified via Context7 or by checking the Fabric Maven directly (https://maven.fabricmc.net/). The values above are best-guess; replace with the actual current versions at task time.

- [ ] **Step 2: Commit**

```bash
git add versions/1.21.11/gradle.properties
git commit -m "chore(phase-2a): pin 1.21.11 deps (Loom, Yarn, Loader, Fabric API, FLK)"
```

---

## Task 17: Add `versions/26.1.2/gradle.properties` (Mojang mappings)

**Files:**
- Create: `versions/26.1.2/gradle.properties`

- [ ] **Step 1: Write the properties file**

Create `versions/26.1.2/gradle.properties`:
```properties
minecraft_version=26.1.2
# Mojang mappings are selected via the //? directive in build.gradle.kts (Task 1, Step 3).
# yarn_mappings is intentionally unset on 26.1+ - the Stonecutter directive switches to
# loom.officialMojangMappings() when MC >= 26.1.
yarn_mappings=unused-mojang-mappings
loader_version=0.17.0
fabric_version=0.130.0+26.1
flk_version=1.14.0+kotlin.2.0.21
java_release=21
```

The `yarn_mappings=unused-mojang-mappings` placeholder satisfies `build.gradle.kts`'s `property("yarn_mappings")` reference (Gradle errors on missing properties). Stonecutter's `//? if MC >= 26.1` directive prevents the `mappings("net.fabricmc:yarn:...")` line from compiling on 26.1; the Mojang mappings line runs instead.

`fabric_version`, `loader_version`, and `flk_version` values for 26.1.2 should be verified via Context7. Best-guess values are provided.

- [ ] **Step 2: Commit**

```bash
git add versions/26.1.2/gradle.properties
git commit -m "chore(phase-2a): pin 26.1.2 deps (Loom, Mojang mappings, Loader, Fabric API, FLK)"
```

---

## Task 18: Add `versions/1.20.1/gradle.properties` + per-version recipe overlay

**Files:**
- Create: `versions/1.20.1/gradle.properties`
- Create: `versions/1.20.1/src/main/resources/data/dndweapons/recipes/longsword.json`

The 1.20.1 recipe lives in `data/dndweapons/recipes/` (note the **trailing 's'**) and uses the `"item"` result key (not `"id"`). The shared `src/main/resources/data/dndweapons/recipe/longsword.json` does NOT load on 1.20.1 (different folder name); leaving it in the jar is harmless.

- [ ] **Step 1: Write `versions/1.20.1/gradle.properties`**

Create `versions/1.20.1/gradle.properties`:
```properties
minecraft_version=1.20.1
yarn_mappings=1.20.1+build.10
loader_version=0.15.11
fabric_version=0.92.6+1.20.1
flk_version=1.10.20+kotlin.2.0.21
java_release=17
```

- [ ] **Step 2: Write the 1.20.1 recipe overlay**

Create `versions/1.20.1/src/main/resources/data/dndweapons/recipes/longsword.json`:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [
    "I",
    "I",
    "S"
  ],
  "key": {
    "I": { "tag": "c:ingots/iron" },
    "S": { "tag": "c:rods/wooden" }
  },
  "result": {
    "item": "dndweapons:longsword",
    "count": 1
  }
}
```

Differences from the shared 1.20.5+ recipe:
- File path: `data/dndweapons/recipes/longsword.json` (1.20.1 reads `recipes/` plural)
- Result key: `"item"` instead of `"id"`
- `category` field is omitted (the 1.20.1 schema doesn't require it)

- [ ] **Step 3: Commit**

```bash
git add versions/1.20.1/
git commit -m "chore(phase-2a): pin 1.20.1 deps + per-version recipe overlay (recipes/ + item key)"
```

---

## Task 19: Final verification — build, test, gametest on all 5 versions

**Files:**
- Create: `docs/plan-runner-out/phase-2a-verification.md`

- [ ] **Step 1: chiseledBuild on all 5 versions**

Run:
```bash
./gradlew chiseledBuild
```

Expected: BUILD SUCCESSFUL for all 5 subprojects. Five jars at `versions/<mc>/build/libs/dndweapons-0.1.0+mc<mc>.jar`.

If a subproject fails:
- 1.20.1: most likely cause is the Epoch A `AttributeCompat` branch's UUID-keyed API mismatch. Confirm `EntityAttributeModifier.Operation.ADDITION` exists in Yarn 1.20.1 (it's `ADDITION` pre-1.21, `ADD_VALUE` from 1.21+).
- 26.1.2: confirm `loom.officialMojangMappings()` resolves; if not, fall back to `loom.officialMojangMappings()` syntax or check Loom 1.11's current API in Context7.
- 1.21.11: check FLK/Fabric-API versions; 1.21.11 may not have a published FLK release yet.

- [ ] **Step 2: chiseledTest on all 5 versions**

Run:
```bash
./gradlew chiseledTest
```

Expected: 7 unit tests pass on each of 5 subprojects (35 total successful test invocations).

- [ ] **Step 3: chiseledRunGametest on all 5 versions**

Run:
```bash
./gradlew chiseledRunGametest
```

Expected: 1 gametest passes per subproject (`longswordIsRegistered`).

Note: gametests are slow (each spins up a real Minecraft server). Total runtime on 5 versions: ~5-10 minutes.

If a gametest fails to start with "Could not load entrypoint":
- Confirm `fabric.mod.json` has the Kotlin adapter on `fabric-gametest`.
- Confirm `fabric-language-kotlin` resolves for that MC version.

- [ ] **Step 4: Write the verification report**

Create `docs/plan-runner-out/phase-2a-verification.md`:
```markdown
# Phase 2a Verification

**Date:** <YYYY-MM-DD HH:MM>

## chiseledBuild

| Subproject | Build | JAR path |
|---|---|---|
| 1.20.1 | <PASS|FAIL> | versions/1.20.1/build/libs/dndweapons-0.1.0+mc1.20.1.jar |
| 1.21.1 | <PASS|FAIL> | versions/1.21.1/build/libs/dndweapons-0.1.0+mc1.21.1.jar |
| 1.21.4 | <PASS|FAIL> | versions/1.21.4/build/libs/dndweapons-0.1.0+mc1.21.4.jar |
| 26.1.2 | <PASS|FAIL> | versions/26.1.2/build/libs/dndweapons-0.1.0+mc26.1.2.jar |
| 1.21.11 | <PASS|FAIL> | versions/1.21.11/build/libs/dndweapons-0.1.0+mc1.21.11.jar |

## chiseledTest

| Subproject | Tests | Pass count |
|---|---|---|
| 1.20.1 | <PASS|FAIL> | <N>/7 |
| 1.21.1 | <PASS|FAIL> | <N>/7 |
| 1.21.4 | <PASS|FAIL> | <N>/7 |
| 26.1.2 | <PASS|FAIL> | <N>/7 |
| 1.21.11 | <PASS|FAIL> | <N>/7 |

## chiseledRunGametest

| Subproject | longswordIsRegistered |
|---|---|
| 1.20.1 | <PASS|FAIL> |
| 1.21.1 | <PASS|FAIL> |
| 1.21.4 | <PASS|FAIL> |
| 26.1.2 | <PASS|FAIL> |
| 1.21.11 | <PASS|FAIL> |

## Notes

<one paragraph: any caveats, version pins that had to be adjusted, manual fixes applied>

## Phase 2a complete

All acceptance criteria from `docs/superpowers/specs/2026-05-16-dnd-weapons-phase-2a-design.md` Section 6 are satisfied:
1. Phase 1 Java tree removed
2. Equivalent .kt files exist with same identifiers
3. WeaponSpec is a Kotlin data class
4. Weapons is a Kotlin object
5. fabric.mod.json uses Kotlin entrypoint adapter
6. build.gradle.kts applies Kotlin plugin + FLK runtime dep
7. All 5 versions/<mc>/gradle.properties files exist
8. settings.gradle.kts lists all 5 versions
9. AttributeCompat.kt with //? directives covers Epoch A and C
10. Per-version recipe overlays exist (1.20.1 only)
11. chiseledBuild succeeds
12. chiseledTest succeeds
13. chiseledRunGametest succeeds
14. Master design doc code examples are Kotlin (verified before Phase 2a began)
```

Fill in the actual results. If any of the 3 chiseled tasks failed for any subproject, write FAIL in the matching cell and add a Notes paragraph describing the failure and remediation.

- [ ] **Step 5: Tag Phase 2a complete**

If all 15 cells above are PASS:
```bash
git tag -a phase-2a-versions-and-kotlin -m "Phase 2a: Kotlin conversion + Stonecutter forks for 4 additional MC versions (1.20.1, 1.21.1, 26.1.2, 1.21.11). Longsword registered + gametest green on all 5 versions."
```

- [ ] **Step 6: Commit the verification report**

```bash
git add docs/plan-runner-out/phase-2a-verification.md
git commit -m "test(phase-2a): final verification on 5 MC versions (chiseledBuild + test + gametest)"
```

---

## Done

Phase 2a complete. The foundation now supports five MC versions from one Kotlin codebase. Phase 2b expands the catalog to all 34 registered weapons + 4 vanilla mappings on all 5 versions.
