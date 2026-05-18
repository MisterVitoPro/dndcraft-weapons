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
