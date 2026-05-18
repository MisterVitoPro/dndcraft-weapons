// src/test/kotlin/com/dndweapons/codegen/WikiGenTest.kt
package com.dndweapons.codegen

import com.dndweapons.acquisition.AcquisitionCatalog
import com.dndweapons.catalog.Category
import com.dndweapons.catalog.Weapons
import com.dndweapons.codegen.wiki.WikiPaths
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WikiGenTest {

    private lateinit var outDir: Path

    @BeforeAll
    fun setup(@TempDir tmp: Path) {
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
