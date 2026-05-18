# DnD Weapons Phase 6 -- Wiki Generator Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Auto-generate a player-facing GitHub Wiki from the existing `Weapons.ALL` and `AcquisitionCatalog` source-of-truth catalogs; add `generateWiki` and `publishWiki` Gradle tasks that emit to `build/wiki/` and sync to the wiki sidecar repo respectively; ship one CI workflow that publishes on `v*` / `phase-*` tag pushes.

**Architecture:** A single Kotlin runnable `main` (`WikiGen.kt`) reads `Weapons.ALL` plus `AcquisitionCatalog.{STRUCTURE_LOOT, MOB_DROPS, VILLAGER_TRADES}`, builds a weapon -> acquisition-facts inverse index (`AcquisitionLookup`), renders each weapon spec through hand-rolled `buildString` templates in `WikiTemplates`, copies 11 hand-authored Markdown pages from `wiki/handwritten/` verbatim, and writes everything under `build/wiki/`. No MC API calls; no version drift -- catalog data is identical across all 5 forks. Gradle wraps the main in `generateWiki` (`JavaExec` against `:1.21.4:` runtime classpath) and `publishWiki` (shallow-clone of `<repo>.wiki.git`, copy, commit, push, auth via env var `WIKI_PUBLISH_TOKEN`).

**Tech Stack:** Kotlin 2.3.21, Fabric Loom 1.16.2, Stonecutter 0.6, JUnit 5 for unit tests. No new runtime dependencies. MC target matrix unchanged: 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2. The wiki generator runs against any single fork (CI pins `:1.21.4:`).

**Spec:** `docs/superpowers/specs/2026-05-18-dnd-weapons-phase-6-design.md`

---

## File Map

**New Kotlin files:**
- `src/main/kotlin/com/dndweapons/codegen/wiki/WikiPaths.kt` -- filename + URL-slug helpers
- `src/main/kotlin/com/dndweapons/codegen/wiki/AcquisitionLookup.kt` -- weapon-id -> acquisition facts inverse index
- `src/main/kotlin/com/dndweapons/codegen/wiki/WikiTemplates.kt` -- buildString page renderers
- `src/main/kotlin/com/dndweapons/codegen/WikiGen.kt` -- runnable `main`

**New unit test:**
- `src/test/kotlin/com/dndweapons/codegen/WikiGenTest.kt` -- 5 smoke tests

**New hand-authored Markdown (committed verbatim, ships placeholder content in v1.0):**
- `wiki/handwritten/Home.md`
- `wiki/handwritten/Getting-Started.md`
- `wiki/handwritten/Combat-Mechanics.md`
- `wiki/handwritten/Smithing-Upgrade-System.md`
- `wiki/handwritten/Vanilla-Mapped-Weapons.md`
- `wiki/handwritten/Acquisition-Guide.md`
- `wiki/handwritten/Material-Gating.md`
- `wiki/handwritten/Version-Notes.md`
- `wiki/handwritten/DnD-Translation-Philosophy.md`
- `wiki/handwritten/Future-Work.md`
- `wiki/handwritten/_Sidebar.md`
- `wiki/handwritten/_Footer.md`

**New CI workflow:**
- `.github/workflows/wiki-publish.yml`

**Modified files:**
- `build.gradle.kts` -- add `generateWiki` (`JavaExec`) and `publishWiki` (Exec) tasks
- `.gitignore` -- add `build/wiki/` and `build/wiki-repo/`

---

## Task 1: WikiPaths.kt -- filename and slug helpers

**Files:**
- Create: `src/main/kotlin/com/dndweapons/codegen/wiki/WikiPaths.kt`

- [ ] **Step 1: Write the helpers**

```kotlin
// src/main/kotlin/com/dndweapons/codegen/wiki/WikiPaths.kt
package com.dndweapons.codegen.wiki

import com.dndweapons.catalog.Category

/**
 * Filename/URL helpers for the GitHub Wiki output. GitHub Wiki uses
 * hyphen-separated PascalCase filenames; spaces in display names map to hyphens.
 *
 * Examples:
 *   "Longsword"       -> "Longsword.md"        / link target "Longsword"
 *   "Hand Crossbow"   -> "Hand-Crossbow.md"    / link target "Hand-Crossbow"
 *   "Light Crossbow"  -> "Light-Crossbow.md"   / link target "Light-Crossbow"
 */
object WikiPaths {

    /** Convert a WeaponSpec.displayName to a wiki page filename (with `.md`). */
    fun weaponFilename(displayName: String): String =
        "${pageSlug(displayName)}.md"

    /** Convert a WeaponSpec.displayName to a GitHub Wiki link target (no `.md`, hyphen-separated). */
    fun pageSlug(displayName: String): String =
        displayName.trim().replace(Regex("\\s+"), "-")

    /** Convert a Category to its category-index filename. */
    fun categoryIndexFilename(category: Category): String =
        "${categoryIndexSlug(category)}.md"

    /** Convert a Category to its category-index slug (link target). */
    fun categoryIndexSlug(category: Category): String = when (category) {
        Category.SIMPLE_MELEE   -> "Simple-Melee"
        Category.SIMPLE_RANGED  -> "Simple-Ranged"
        Category.MARTIAL_MELEE  -> "Martial-Melee"
        Category.MARTIAL_RANGED -> "Martial-Ranged"
    }

    /** Human-readable label for a Category (used in headings). */
    fun categoryLabel(category: Category): String = when (category) {
        Category.SIMPLE_MELEE   -> "Simple Melee Weapons"
        Category.SIMPLE_RANGED  -> "Simple Ranged Weapons"
        Category.MARTIAL_MELEE  -> "Martial Melee Weapons"
        Category.MARTIAL_RANGED -> "Martial Ranged Weapons"
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :1.21.4:compileKotlin`
Expected: BUILD SUCCESSFUL.

---

## Task 2: AcquisitionLookup.kt -- inverse index over AcquisitionCatalog

**Files:**
- Create: `src/main/kotlin/com/dndweapons/codegen/wiki/AcquisitionLookup.kt`

- [ ] **Step 1: Write the lookup**

```kotlin
// src/main/kotlin/com/dndweapons/codegen/wiki/AcquisitionLookup.kt
package com.dndweapons.codegen.wiki

import com.dndweapons.acquisition.AcquisitionCatalog
import com.dndweapons.catalog.Tier

/**
 * Inverse index: for each weapon id (e.g. "longsword"), where can the player
 * acquire it? Built once at WikiGen time from the three AcquisitionCatalog maps.
 *
 * Used by WikiTemplates to render the "Acquisition" section of each weapon page.
 */
class AcquisitionLookup private constructor(
    private val byWeaponId: Map<String, List<AcquisitionFact>>,
) {

    fun factsFor(weaponId: String): List<AcquisitionFact> =
        byWeaponId[weaponId].orEmpty()

    companion object {
        fun build(): AcquisitionLookup {
            val out = linkedMapOf<String, MutableList<AcquisitionFact>>()

            // Structure loot
            for ((tableId, entry) in AcquisitionCatalog.STRUCTURE_LOOT) {
                val tableLabel = friendlyStructure(tableId)
                for (weaponId in entry.weapons) {
                    out.getOrPut(weaponId) { mutableListOf() }
                        .add(
                            AcquisitionFact.StructureChest(
                                tableLabel = tableLabel,
                                chancePct = entry.chancePct,
                                tier = entry.tier,
                                minVersion = entry.minVersion,
                            )
                        )
                }
            }

            // Mob drops
            for ((entityId, drop) in AcquisitionCatalog.MOB_DROPS) {
                val mobLabel = friendlyMob(entityId)
                val ironWeapon = drop.ironWeapon
                if (ironWeapon != null && drop.ironPct > 0) {
                    out.getOrPut(ironWeapon) { mutableListOf() }
                        .add(AcquisitionFact.MobDrop(mobLabel, drop.ironPct, Tier.IRON))
                }
                // Netherite mob drops with a specific weapon are stacked alongside the
                // iron drop; netherite-with-null-weapon (Warden) is a random pick and
                // is documented on every netherite weapon page via NetheriteRandomDrop.
                if (ironWeapon != null && drop.netheritePct > 0) {
                    out.getOrPut(ironWeapon) { mutableListOf() }
                        .add(AcquisitionFact.MobDrop(mobLabel, drop.netheritePct, Tier.NETHERITE))
                }
            }

            // Villager trades
            for ((profession, levels) in AcquisitionCatalog.VILLAGER_TRADES) {
                val profLabel = friendlyProfession(profession)
                for ((level, trades) in levels) {
                    for (trade in trades) {
                        out.getOrPut(trade.weapon) { mutableListOf() }
                            .add(
                                AcquisitionFact.VillagerTrade(
                                    profession = profLabel,
                                    level = level,
                                    emeralds = trade.emeralds,
                                )
                            )
                    }
                }
            }

            return AcquisitionLookup(out)
        }

        private fun friendlyStructure(tableId: String): String = tableId
            .removePrefix("minecraft:chests/")
            .replace('_', ' ')
            .split(' ', '/')
            .joinToString(" ") { it.replaceFirstChar(Char::titlecase) }

        private fun friendlyMob(entityId: String): String = entityId
            .removePrefix("minecraft:entities/")
            .replace('_', ' ')
            .split(' ')
            .joinToString(" ") { it.replaceFirstChar(Char::titlecase) }

        private fun friendlyProfession(profession: String): String = profession
            .removePrefix("minecraft:")
            .replace('_', ' ')
            .split(' ')
            .joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
    }
}

/** Tagged union describing one acquisition source for one weapon. */
sealed interface AcquisitionFact {
    data class StructureChest(
        val tableLabel: String,
        val chancePct: Int,
        val tier: Tier,
        val minVersion: String?,
    ) : AcquisitionFact

    data class MobDrop(
        val mobLabel: String,
        val chancePct: Int,
        val tier: Tier,
    ) : AcquisitionFact

    data class VillagerTrade(
        val profession: String,
        val level: Int,
        val emeralds: Int,
    ) : AcquisitionFact
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :1.21.4:compileKotlin`
Expected: BUILD SUCCESSFUL.

---

## Task 3: WikiTemplates.kt -- page renderers

**Files:**
- Create: `src/main/kotlin/com/dndweapons/codegen/wiki/WikiTemplates.kt`

- [ ] **Step 1: Write the templates**

```kotlin
// src/main/kotlin/com/dndweapons/codegen/wiki/WikiTemplates.kt
package com.dndweapons.codegen.wiki

import com.dndweapons.catalog.Category
import com.dndweapons.catalog.DamageType
import com.dndweapons.catalog.Property
import com.dndweapons.catalog.RangeKind
import com.dndweapons.catalog.WeaponSpec

/**
 * Hand-rolled buildString-based Markdown templates. No external templating
 * library. Each function returns a complete page body (no trailing newline
 * normalization beyond a single trailing newline).
 */
object WikiTemplates {

    fun renderWeaponPage(spec: WeaponSpec, lookup: AcquisitionLookup): String = buildString {
        appendLine("# ${spec.displayName}")
        appendLine()
        appendLine(
            "> **Category:** ${labelFor(spec.category)}  |  " +
                "**Damage type:** ${labelFor(spec.damageType)}  |  " +
                "**DnD dice:** ${spec.diceText}" +
                (spec.versatileDice?.let { " (versatile $it)" } ?: "")
        )
        appendLine()

        if (spec.isVanillaMapped) {
            appendLine(renderVanillaCallout(spec))
            appendLine()
        }

        appendLine("## Stats")
        appendLine()
        appendLine("| Property | Value |")
        appendLine("|---|---|")
        val dmgCell =
            if (spec.versatileBonus > 0) "${spec.attackDamage} (+${spec.versatileBonus} versatile)"
            else "${spec.attackDamage}"
        appendLine("| Attack damage | $dmgCell |")
        appendLine("| Attack speed | ${spec.attackSpeed} |")
        appendLine("| Reach bonus | ${spec.reachBonus} |")
        appendLine("| Knockback bonus | +${spec.knockbackBonus} |")
        val propsCell = if (spec.properties.isEmpty()) "(none)"
                        else spec.properties.sortedBy { it.name }.joinToString(", ") { labelFor(it) }
        appendLine("| Properties | $propsCell |")
        appendLine("| Range | ${labelFor(spec.ranged)} |")
        appendLine("| Base durability | ${spec.baseDurability} |")
        appendLine()

        appendLine("## Tiers")
        appendLine()
        if (spec.isVanillaMapped) {
            appendLine("Vanilla material progression applies. No smithing template required.")
        } else {
            appendLine(
                "This weapon supports the full smithing-upgrade ladder " +
                    "(iron -> diamond -> netherite), as described in " +
                    "[Smithing-Upgrade-System](Smithing-Upgrade-System)."
            )
        }
        appendLine()

        appendLine("## Acquisition")
        appendLine()
        val facts = lookup.factsFor(spec.id)
        if (spec.isVanillaMapped) {
            appendLine(
                "Acquired via the vanilla Minecraft item (see callout above). " +
                    "Not present in any structure loot, villager trade, or mob drop in v1.0."
            )
        } else if (facts.isEmpty()) {
            appendLine(
                "Crafting only. Not present in any structure loot, villager trade, " +
                    "or mob drop in v1.0."
            )
        } else {
            for (fact in facts) {
                appendLine("- ${renderFactLine(fact)}")
            }
        }
        appendLine()

        appendLine("## Combat behaviour")
        appendLine()
        appendLine(
            "Properties translated to vanilla-feeling hooks per the " +
                "[Combat-Mechanics](Combat-Mechanics) page."
        )
        if (spec.properties.isNotEmpty()) {
            appendLine()
            for (prop in spec.properties.sortedBy { it.name }) {
                appendLine("- **${labelFor(prop)}**: ${propertyHookSummary(prop)}")
            }
        }
    }

    fun renderCategoryIndex(category: Category, specs: List<WeaponSpec>): String = buildString {
        appendLine("# ${WikiPaths.categoryLabel(category)}")
        appendLine()
        appendLine("| Weapon | Damage | Properties | Notes |")
        appendLine("|---|---|---|---|")
        for (spec in specs.filter { it.category == category }.sortedBy { it.displayName }) {
            val link = WikiPaths.pageSlug(spec.displayName)
            val propsCell = if (spec.properties.isEmpty()) "-"
                            else spec.properties.sortedBy { it.name }.joinToString(", ") { labelFor(it) }
            val notes = if (spec.isVanillaMapped) "Vanilla-mapped" else ""
            appendLine("| [${spec.displayName}]($link) | ${spec.attackDamage} | $propsCell | $notes |")
        }
    }

    fun renderHomeHeader(modVersion: String, buildSha: String): String = buildString {
        appendLine("<!-- AUTO-GENERATED HEADER -->")
        appendLine(
            "**DnD Weapons** for Minecraft -- v$modVersion, build $buildSha. " +
                "Supported MC versions: 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2."
        )
        appendLine()
        appendLine("----")
        appendLine()
        appendLine("<!-- HANDWRITTEN BODY -->")
    }

    // ----- private helpers -----

    private fun renderVanillaCallout(spec: WeaponSpec): String {
        val vanillaName = when (spec.id) {
            "shortsword"     -> "Iron Sword (and any vanilla sword tier)"
            "shortbow"       -> "Bow"
            "light_crossbow" -> "Crossbow"
            "trident"        -> "Trident"
            else             -> "vanilla item"
        }
        return ":information_source:  **This weapon is represented by the vanilla Minecraft " +
            "$vanillaName.** When you craft or pick up the vanilla item, it carries " +
            "the ${spec.displayName} identity: tooltip stat block, combat hooks, and " +
            "role tag. No separate item is registered."
    }

    private fun renderFactLine(fact: AcquisitionFact): String = when (fact) {
        is AcquisitionFact.StructureChest -> {
            val versionNote = fact.minVersion?.let { " _(${it}+ only)_" } ?: ""
            "Structure chest: **${fact.tableLabel}** -- ${fact.chancePct}% (${labelFor(fact.tier)} tier)$versionNote"
        }
        is AcquisitionFact.MobDrop ->
            "Mob drop: **${fact.mobLabel}** -- ${fact.chancePct}% (${labelFor(fact.tier)} tier)"
        is AcquisitionFact.VillagerTrade ->
            "Villager trade: **${fact.profession}** level ${fact.level} -- ${fact.emeralds} emeralds"
    }

    private fun propertyHookSummary(prop: Property): String = when (prop) {
        Property.LIGHT       -> "+1 damage when offhand also holds a Light weapon (dual-wield)"
        Property.HEAVY       -> "+1 knockback level on hit"
        Property.FINESSE     -> "+20% damage when attacker is sprinting"
        Property.VERSATILE   -> "+versatile damage bonus when wielded two-handed"
        Property.TWO_HANDED  -> "Requires both hands; offhand items prevent attack"
        Property.REACH       -> "+1 block attack range"
        Property.THROWN      -> "Right-click to throw as a ranged projectile"
        Property.AMMUNITION  -> "Requires the appropriate ammo item"
        Property.LOADING     -> "Reload animation between shots"
        Property.SPECIAL     -> "See weapon-specific notes"
    }

    private fun labelFor(category: Category): String = WikiPaths.categoryLabel(category)
        .removeSuffix(" Weapons")

    private fun labelFor(dt: DamageType): String = when (dt) {
        DamageType.SLASHING    -> "Slashing"
        DamageType.PIERCING    -> "Piercing"
        DamageType.BLUDGEONING -> "Bludgeoning"
    }

    private fun labelFor(p: Property): String =
        p.name.lowercase().replaceFirstChar(Char::titlecase).replace('_', ' ')

    private fun labelFor(r: RangeKind): String = when (r) {
        RangeKind.NONE   -> "Melee"
        RangeKind.THROWN -> "Thrown"
        RangeKind.BOW    -> "Bow"
        RangeKind.CROSSBOW -> "Crossbow"
        RangeKind.FIREARM  -> "Firearm"
    }

    private fun labelFor(t: com.dndweapons.catalog.Tier): String = when (t) {
        com.dndweapons.catalog.Tier.IRON      -> "Iron"
        com.dndweapons.catalog.Tier.DIAMOND   -> "Diamond"
        com.dndweapons.catalog.Tier.NETHERITE -> "Netherite"
    }
}
```

> NOTE: The `Property`, `RangeKind`, `Tier`, `Category`, `DamageType` enums already exist in `src/main/kotlin/com/dndweapons/catalog/`. If any enum variant referenced above does not exist (e.g. `Property.SPECIAL` may be named differently), the agent MUST inspect the actual enum file and map the cases exactly. Do not invent variants. The `else` branch at the bottom of `propertyHookSummary` may be necessary if there are properties not enumerated above; if so, return `"(see weapon notes)"` for any unhandled case.

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :1.21.4:compileKotlin`
Expected: BUILD SUCCESSFUL.

---

## Task 4: WikiGen.kt -- runnable main

**Files:**
- Create: `src/main/kotlin/com/dndweapons/codegen/WikiGen.kt`

- [ ] **Step 1: Write the runnable**

```kotlin
// src/main/kotlin/com/dndweapons/codegen/WikiGen.kt
package com.dndweapons.codegen

import com.dndweapons.catalog.Category
import com.dndweapons.catalog.Weapons
import com.dndweapons.codegen.wiki.AcquisitionLookup
import com.dndweapons.codegen.wiki.WikiPaths
import com.dndweapons.codegen.wiki.WikiTemplates
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.streams.toList

/**
 * Phase 6 wiki generator. Reads Weapons.ALL and AcquisitionCatalog from the
 * compiled source set and emits Markdown under args[0] (default: build/wiki/).
 *
 * Run via Gradle:
 *   ./gradlew :1.21.4:generateWiki
 *
 * Direct invocation:
 *   java -cp <main runtime classpath> com.dndweapons.codegen.WikiGenKt <out-dir>
 */
object WikiGen {

    fun run(outDir: Path, modVersion: String, buildSha: String, handwrittenDir: Path) {
        val weaponsOut = outDir.resolve("Weapons")
        weaponsOut.createDirectories()

        val lookup = AcquisitionLookup.build()
        val allSpecs = Weapons.ALL

        // 1. Per-weapon pages
        for (spec in allSpecs) {
            val md = WikiTemplates.renderWeaponPage(spec, lookup)
            weaponsOut.resolve(WikiPaths.weaponFilename(spec.displayName)).writeText(md)
        }

        // 2. Category indexes
        for (cat in Category.values()) {
            val md = WikiTemplates.renderCategoryIndex(cat, allSpecs)
            weaponsOut.resolve(WikiPaths.categoryIndexFilename(cat)).writeText(md)
        }

        // 3. Handwritten pages (verbatim) + Home.md (mixed)
        if (!handwrittenDir.exists()) {
            error("Handwritten dir does not exist: $handwrittenDir")
        }
        Files.list(handwrittenDir).use { stream ->
            for (path in stream.toList()) {
                val name = path.fileName.toString()
                if (!name.endsWith(".md")) continue
                val target = outDir.resolve(name)
                if (name == "Home.md") {
                    val header = WikiTemplates.renderHomeHeader(modVersion, buildSha)
                    target.writeText(header + "\n" + path.readText())
                } else {
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }

        // 4. Summary
        val pageCount = allSpecs.size + Category.values().size
        val handwrittenCount = Files.list(handwrittenDir).use { it.count() }
        println("WikiGen: wrote $pageCount auto-generated pages and $handwrittenCount handwritten pages into $outDir")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val outDir = Path.of(args.getOrNull(0) ?: "build/wiki")
        val modVersion = args.getOrNull(1) ?: System.getProperty("modVersion") ?: "dev"
        val buildSha = args.getOrNull(2) ?: System.getProperty("buildSha") ?: "local"
        val handwrittenDir = Path.of(args.getOrNull(3) ?: "wiki/handwritten")
        run(outDir, modVersion, buildSha, handwrittenDir)
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :1.21.4:compileKotlin`
Expected: BUILD SUCCESSFUL.

---

## Task 5: WikiGenTest.kt -- 5 smoke tests

**Files:**
- Create: `src/test/kotlin/com/dndweapons/codegen/WikiGenTest.kt`

- [ ] **Step 1: Write the test**

```kotlin
// src/test/kotlin/com/dndweapons/codegen/WikiGenTest.kt
package com.dndweapons.codegen

import com.dndweapons.acquisition.AcquisitionCatalog
import com.dndweapons.catalog.Category
import com.dndweapons.catalog.Weapons
import com.dndweapons.codegen.wiki.WikiPaths
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WikiGenTest {

    @TempDir lateinit var tmp: Path
    private lateinit var outDir: Path

    @BeforeAll
    fun setup() {
        // Build a minimal handwritten dir with placeholders so WikiGen.run can succeed.
        val hand = tmp.resolve("handwritten")
        hand.createDirectories()
        for (name in listOf(
            "Home.md", "Getting-Started.md", "Combat-Mechanics.md",
            "Smithing-Upgrade-System.md", "Vanilla-Mapped-Weapons.md",
            "Acquisition-Guide.md", "Material-Gating.md", "Version-Notes.md",
            "DnD-Translation-Philosophy.md", "Future-Work.md",
            "_Sidebar.md", "_Footer.md",
        )) {
            hand.resolve(name).writeText("# $name placeholder\n")
        }

        outDir = tmp.resolve("out")
        WikiGen.run(outDir, modVersion = "test-1.0", buildSha = "deadbeef", handwrittenDir = hand)
    }

    @Test
    fun everyWeaponInCatalogRendersToAFile() {
        for (spec in Weapons.ALL) {
            val expected = outDir.resolve("Weapons").resolve(WikiPaths.weaponFilename(spec.displayName))
            assertTrue(expected.exists(), "Expected weapon page at $expected")
        }
    }

    @Test
    fun everyCategoryIndexExists() {
        for (cat in Category.values()) {
            val expected = outDir.resolve("Weapons").resolve(WikiPaths.categoryIndexFilename(cat))
            assertTrue(expected.exists(), "Expected category index at $expected")
            val text = expected.readText()
            for (spec in Weapons.ALL.filter { it.category == cat }) {
                assertTrue(
                    text.contains(spec.displayName),
                    "Category index ${expected.fileName} missing ${spec.displayName}",
                )
            }
        }
    }

    @Test
    fun vanillaMappedPagesIncludeTheCallout() {
        val vanillaIds = setOf("shortsword", "shortbow", "light_crossbow", "trident")
        val vanillaSpecs = Weapons.ALL.filter { it.id in vanillaIds }
        assertEquals(4, vanillaSpecs.size, "Expected 4 vanilla-mapped specs in catalog")
        for (spec in vanillaSpecs) {
            val page = outDir.resolve("Weapons").resolve(WikiPaths.weaponFilename(spec.displayName))
            val text = page.readText()
            assertTrue(
                text.contains("represented by the vanilla"),
                "Vanilla callout missing on ${page.fileName}",
            )
        }
    }

    @Test
    fun acquisitionSectionListsAtLeastOneCatalogEntryForKnownWeapon() {
        // longsword appears in stronghold_corridor (Phase 5 catalog) -- verify the rendered page reflects it.
        val hasLongsword = AcquisitionCatalog.STRUCTURE_LOOT.values.any { "longsword" in it.weapons }
        assertTrue(hasLongsword, "Test precondition: longsword must appear in STRUCTURE_LOOT")

        val longswordSpec = Weapons.ALL.first { it.id == "longsword" }
        val page = outDir.resolve("Weapons").resolve(WikiPaths.weaponFilename(longswordSpec.displayName))
        val text = page.readText()
        assertTrue(
            text.contains("Structure chest:"),
            "Longsword page should list at least one structure-chest acquisition fact",
        )
    }

    @Test
    fun handwrittenPagesAreCopied() {
        val handFiles = listOf(
            "Getting-Started.md", "Combat-Mechanics.md", "_Sidebar.md", "_Footer.md",
        )
        for (name in handFiles) {
            val out = outDir.resolve(name)
            assertTrue(out.exists(), "Handwritten file $name was not copied to $outDir")
        }
        // Home.md is the mixed page; ensure it exists and has BOTH the auto header and the handwritten body.
        val home = outDir.resolve("Home.md")
        assertTrue(home.exists())
        val homeText = home.readText()
        assertTrue(homeText.contains("AUTO-GENERATED HEADER"), "Home.md missing auto-generated header")
        assertTrue(homeText.contains("Home.md placeholder"), "Home.md missing handwritten body")
    }
}
```

- [ ] **Step 2: Verify tests compile**

Run: `./gradlew :1.21.4:compileTestKotlin`
Expected: BUILD SUCCESSFUL.

---

## Task 6: Hand-authored Markdown placeholders (12 files)

**Files:**
- Create: `wiki/handwritten/Home.md`
- Create: `wiki/handwritten/Getting-Started.md`
- Create: `wiki/handwritten/Combat-Mechanics.md`
- Create: `wiki/handwritten/Smithing-Upgrade-System.md`
- Create: `wiki/handwritten/Vanilla-Mapped-Weapons.md`
- Create: `wiki/handwritten/Acquisition-Guide.md`
- Create: `wiki/handwritten/Material-Gating.md`
- Create: `wiki/handwritten/Version-Notes.md`
- Create: `wiki/handwritten/DnD-Translation-Philosophy.md`
- Create: `wiki/handwritten/Future-Work.md`
- Create: `wiki/handwritten/_Sidebar.md`
- Create: `wiki/handwritten/_Footer.md`

Each file is a one-section Markdown placeholder. Real content gets filled in post-Phase-6. The agent should write each file with the structure below, varying only the title and the one-sentence body.

- [ ] **Step 1: Write Home.md**

```markdown
Welcome to the **DnD Weapons** wiki -- a Fabric mod that brings the simple
and martial weapons from the *Dungeons & Dragons 2024 Player's Handbook* into
Minecraft, translated into vanilla-feeling mechanics.

See the [Getting-Started](Getting-Started) page for an introduction, or jump
straight to a weapon category in the sidebar.
```

- [ ] **Step 2: Write Getting-Started.md**

```markdown
# Getting Started

(Placeholder.) This page will cover: installation, finding your first DnD
weapon, the smithing upgrade ladder, and where to look for acquisition
opportunities. For now, see the master design spec under `docs/superpowers/`
in the repo.
```

- [ ] **Step 3: Write Combat-Mechanics.md**

```markdown
# Combat Mechanics

(Placeholder.) The DnD weapon properties (Light, Heavy, Finesse, Versatile,
Reach, Thrown, Two-Handed, Special) translate to vanilla-feeling combat
hooks. See each weapon page for the property list and behaviour summary.
```

- [ ] **Step 4: Write Smithing-Upgrade-System.md**

```markdown
# Smithing Upgrade System

(Placeholder.) Iron-tier base weapons can be upgraded to diamond, then to
netherite, via the vanilla smithing table using dedicated smithing templates.
See the Phase 4 design for details.
```

- [ ] **Step 5: Write Vanilla-Mapped-Weapons.md**

```markdown
# Vanilla-Mapped Weapons

(Placeholder.) Four DnD weapons are *not* registered as separate items --
they ride on top of their vanilla equivalents: Shortsword on Iron Sword,
Shortbow on Bow, Light Crossbow on Crossbow, Trident on Trident. They get
tooltip + combat hooks via role tags.
```

- [ ] **Step 6: Write Acquisition-Guide.md**

```markdown
# Acquisition Guide

(Placeholder.) DnD weapons are obtained through four surfaces: crafting,
structure loot, villager trades, and mob drops. See the per-weapon pages
for exact rates and locations.
```

- [ ] **Step 7: Write Material-Gating.md**

```markdown
# Material Gating

(Placeholder.) All target MC versions (1.20.1 through 26.1.2) ship the same
core materials (iron, diamond, netherite, copper, etc.), so no recipes are
gated in v1.0. Future material additions (Mithril, Adamantine) are deferred.
```

- [ ] **Step 8: Write Version-Notes.md**

```markdown
# Version Notes

(Placeholder.) Per-MC-version notes go here. Currently the catalog is
identical across 1.20.1, 1.21.1, 1.21.4, 1.21.11, and 26.1.2; differences
are limited to runtime API drift (loot tables v2/v3, trade signatures,
identifier accessors) and don't affect catalog content.
```

- [ ] **Step 9: Write DnD-Translation-Philosophy.md**

```markdown
# DnD Translation Philosophy

(Placeholder.) We translate DnD properties to mechanics that *feel* vanilla
rather than reproducing the dice math literally. Damage values are
calibrated so 1d8 = vanilla iron-sword damage (6).
```

- [ ] **Step 10: Write Future-Work.md**

```markdown
# Future Work

(Placeholder.) DnD 5.5e Mastery properties, per-mob damage-type
vulnerabilities, datapack-extensible catalog, and Patchouli in-game guide
book are all parking-lot items for v1.1+.
```

- [ ] **Step 11: Write _Sidebar.md**

```markdown
**DnD Weapons**

- [Home](Home)
- [Getting Started](Getting-Started)
- [Combat Mechanics](Combat-Mechanics)
- [Smithing Upgrade System](Smithing-Upgrade-System)
- [Vanilla-Mapped Weapons](Vanilla-Mapped-Weapons)
- [Acquisition Guide](Acquisition-Guide)

**Weapons**

- [Simple Melee](Simple-Melee)
- [Simple Ranged](Simple-Ranged)
- [Martial Melee](Martial-Melee)
- [Martial Ranged](Martial-Ranged)

**Reference**

- [Material Gating](Material-Gating)
- [Version Notes](Version-Notes)
- [DnD Translation Philosophy](DnD-Translation-Philosophy)
- [Future Work](Future-Work)
```

- [ ] **Step 12: Write _Footer.md**

```markdown
---
DnD Weapons mod by [MisterVitoPro](https://github.com/MisterVitoPro/dndcraft-weapons). Wiki auto-generated from the source catalog; do not edit pages directly -- edits will be overwritten by the next `publishWiki` run.
```

---

## Task 7: Gradle task wiring -- generateWiki + publishWiki

**Files:**
- Modify: `build.gradle.kts`
- Modify: `.gitignore`

- [ ] **Step 1: Add the tasks to build.gradle.kts**

Append the following block to `build.gradle.kts`, after the existing `tasks.withType<KotlinCompile>` blocks (or at the bottom of the file if those are not present). The task block is identical across all Stonecutter forks -- no version drift, no stonecutter directives needed.

```kotlin
// ===== Phase 6: Wiki generator =====

tasks.register<JavaExec>("generateWiki") {
    group = "wiki"
    description = "Generate the player-facing Markdown wiki under build/wiki/."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.dndweapons.codegen.WikiGen")
    val out = layout.buildDirectory.dir("wiki").get().asFile
    val hand = rootProject.layout.projectDirectory.dir("wiki/handwritten").asFile
    val sha = try {
        providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
        }.standardOutput.asText.get().trim()
    } catch (_: Exception) {
        "local"
    }
    args(out.absolutePath, modVersion, sha, hand.absolutePath)
    dependsOn("classes")
    doFirst {
        out.mkdirs()
        if (!hand.exists()) {
            throw GradleException("Missing handwritten wiki dir: $hand")
        }
    }
}

tasks.register("publishWiki") {
    group = "wiki"
    description = "Sync build/wiki/ to <repo>.wiki.git (push). Auth via env WIKI_PUBLISH_TOKEN."
    dependsOn("generateWiki")
    doLast {
        val wikiDir = layout.buildDirectory.dir("wiki").get().asFile
        val cloneDir = layout.buildDirectory.dir("wiki-repo").get().asFile
        val token: String? = System.getenv("WIKI_PUBLISH_TOKEN")
        val url = if (!token.isNullOrBlank()) {
            "https://x-access-token:$token@github.com/MisterVitoPro/dndcraft-weapons.wiki.git"
        } else {
            "git@github.com:MisterVitoPro/dndcraft-weapons.wiki.git"
        }
        val dryRun = project.findProperty("dryRun")?.toString().toBoolean()

        logger.lifecycle("publishWiki: source = $wikiDir")
        logger.lifecycle("publishWiki: target = $url")
        logger.lifecycle("publishWiki: dryRun = $dryRun")

        if (cloneDir.exists()) cloneDir.deleteRecursively()
        cloneDir.parentFile.mkdirs()

        providers.exec {
            commandLine("git", "clone", "--depth", "1", url, cloneDir.absolutePath)
        }.result.get()

        // Wipe everything except .git
        cloneDir.listFiles()?.forEach { f ->
            if (f.name != ".git") {
                if (f.isDirectory) f.deleteRecursively() else f.delete()
            }
        }

        // Copy generated + handwritten wiki output into the clone
        wikiDir.listFiles()?.forEach { f ->
            val dest = java.io.File(cloneDir, f.name)
            if (f.isDirectory) f.copyRecursively(dest, overwrite = true)
            else f.copyTo(dest, overwrite = true)
        }

        providers.exec {
            workingDir = cloneDir
            commandLine("git", "add", ".")
        }.result.get()

        // Check whether there is anything to commit
        val status = providers.exec {
            workingDir = cloneDir
            commandLine("git", "status", "--porcelain")
        }.standardOutput.asText.get().trim()
        if (status.isEmpty()) {
            logger.lifecycle("publishWiki: no changes to publish, exiting clean.")
            return@doLast
        }

        val msg = "Wiki: sync from ${project.version}"
        providers.exec {
            workingDir = cloneDir
            commandLine("git", "-c", "user.email=wiki-bot@dndcraft-weapons", "-c", "user.name=Wiki Bot", "commit", "-m", msg)
        }.result.get()

        if (dryRun) {
            logger.lifecycle("publishWiki: dry-run mode, NOT pushing.")
            return@doLast
        }
        providers.exec {
            workingDir = cloneDir
            commandLine("git", "push", "origin", "HEAD")
        }.result.get()
        logger.lifecycle("publishWiki: pushed to $url")
    }
}
```

- [ ] **Step 2: Add wiki paths to .gitignore**

Append the following lines to `.gitignore` (create if it doesn't exist; preserve all existing rules):

```
# Phase 6 wiki output -- regenerated each build
build/wiki/
build/wiki-repo/
```

- [ ] **Step 3: Verify the task list resolves**

Run: `./gradlew :1.21.4:tasks --group wiki`
Expected output contains:
```
Wiki tasks
----------
generateWiki - Generate the player-facing Markdown wiki under build/wiki/.
publishWiki - Sync build/wiki/ to <repo>.wiki.git (push). Auth via env WIKI_PUBLISH_TOKEN.
```

---

## Task 8: GitHub Actions workflow

**Files:**
- Create: `.github/workflows/wiki-publish.yml`

- [ ] **Step 1: Write the workflow**

```yaml
# .github/workflows/wiki-publish.yml
name: Publish Wiki

on:
  push:
    tags:
      - 'v*'
      - 'phase-*'

jobs:
  publish:
    runs-on: ubuntu-latest
    permissions:
      contents: read

    steps:
      - name: Check out repo
        uses: actions/checkout@v4

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21

      - name: Cache Gradle
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
          cache: gradle

      - name: Generate + publish wiki
        env:
          WIKI_PUBLISH_TOKEN: ${{ secrets.WIKI_PUBLISH_TOKEN }}
        run: ./gradlew :1.21.4:generateWiki publishWiki

      - name: Upload generated wiki as workflow artifact
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: generated-wiki
          path: 1.21.4/build/wiki/
          if-no-files-found: warn
          retention-days: 14
```

- [ ] **Step 2: Verify workflow YAML parses**

The file is YAML; verification is "loads without parse error". The CI provider's first run on the next tag push will surface any issues. No local verification step is required for this plan.

---

## Task 9: Run gradle matrix + write Phase 6 verification doc

**Files:**
- Create: `docs/superpowers/plans/phase-6-verification-final.md`

This is the final verification wave. The agent runs gradle, captures actual results, and documents them.

- [ ] **Step 1: Run the full matrix verification**

```
./gradlew chiseledBuild
./gradlew chiseledTest
./gradlew :1.21.4:generateWiki
```

Capture per-version PASS/FAIL for the first two commands. The third runs against a single fork (1.21.4) and produces the wiki output.

- [ ] **Step 2: Count and spot-check generated files**

After `:1.21.4:generateWiki`:
```
ls 1.21.4/build/wiki/                       # expect 13 handwritten + Home.md + Weapons/ dir
ls 1.21.4/build/wiki/Weapons/ | wc -l       # expect 38 weapon pages + 4 category indexes = 42
grep -l "represented by the vanilla" 1.21.4/build/wiki/Weapons/{Shortsword,Shortbow,Light-Crossbow,Trident}.md
                                            # expect 4 hits
```

- [ ] **Step 3: Write the verification doc**

Create `docs/superpowers/plans/phase-6-verification-final.md` with this template, filled in with the actual results from steps 1-2:

```markdown
# Phase 6 Verification -- Wiki Generator

**Date:** <today>
**Status:** GREEN / HOLD (pick one based on results)

## Top-line results

| Version | chiseledBuild | chiseledTest | generateWiki (output file counts) |
|---|---|---|---|
| 1.20.1  | <PASS/FAIL>   | <PASS/FAIL>  | n/a (gen runs against 1.21.4 only)   |
| 1.21.1  | <PASS/FAIL>   | <PASS/FAIL>  | n/a |
| 1.21.4  | <PASS/FAIL>   | <PASS/FAIL>  | <weapon-page-count>, <category-page-count>, <handwritten-page-count> |
| 1.21.11 | <PASS/FAIL>   | <PASS/FAIL>  | n/a |
| 26.1.2  | <PASS/FAIL>   | <PASS/FAIL>  | n/a |

## Commands run

(paste exact command list run by the agent and BUILD SUCCESSFUL / FAILED line)

## File counts after generateWiki

- Auto-generated weapon pages: <N>  (expected 38)
- Category indexes: <N>             (expected 4)
- Handwritten pages copied: <N>     (expected 12, including Home.md mixed)
- Vanilla-mapped callouts: <N>      (expected 4 -- Shortsword, Shortbow, Light-Crossbow, Trident)

## WikiGenTest results

(per-version pass/fail of the 5 tests; should be identical across all 5 forks)

## Tag command (NOT executed by plan-runner)

```bash
git tag phase-6-wiki
git push origin phase-6-wiki
```

Only apply manually after eyeballing the generated wiki output in `1.21.4/build/wiki/`.
```

- [ ] **Step 4: Commit the verification doc**

(Plan-runner orchestrator commits per wave; the agent only writes the file.)
