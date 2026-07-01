// src/main/kotlin/com/dndweapons/codegen/WikiGen.kt
package com.dndweapons.codegen

import com.dndweapons.catalog.Category
import com.dndweapons.catalog.Weapons
import com.dndweapons.codegen.wiki.AcquisitionLookup
import com.dndweapons.codegen.wiki.WikiPaths
import com.dndweapons.codegen.wiki.WikiTemplates
import org.slf4j.LoggerFactory
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
    private val LOGGER = LoggerFactory.getLogger(WikiGen::class.java)

    fun run(outDir: Path, modVersion: String, buildSha: String, handwrittenDir: Path) {
        val weaponsOut = outDir.resolve("Weapons")
        weaponsOut.createDirectories()

        val lookup = AcquisitionLookup.build()
        val allSpecs = Weapons.ALL

        // 1. Per-weapon pages
        for (spec in allSpecs) {
            try {
                val md = WikiTemplates.renderWeaponPage(spec, lookup)
                val filename = WikiPaths.weaponFilename(spec.displayName)
                val targetPath = weaponsOut.resolve(filename).normalize()
                // P1-004: Validate that the resolved path is still within weaponsOut
                validatePathWithinDirectory(targetPath, weaponsOut)
                targetPath.writeText(md)
            } catch (e: Exception) {
                LOGGER.error("Failed to write weapon page for '${spec.displayName}': ${e.message}", e)
            }
        }

        // 2. Category indexes
        for (cat in Category.values()) {
            try {
                val md = WikiTemplates.renderCategoryIndex(cat, allSpecs)
                val filename = WikiPaths.categoryIndexFilename(cat)
                val targetPath = weaponsOut.resolve(filename).normalize()
                // P1-004: Validate that the resolved path is still within weaponsOut
                validatePathWithinDirectory(targetPath, weaponsOut)
                targetPath.writeText(md)
            } catch (e: Exception) {
                LOGGER.error("Failed to write category index for '${cat.name}': ${e.message}", e)
            }
        }

        // 3. Handwritten pages (verbatim) + Home.md (mixed)
        // P2-010: Cache file list from single read to avoid double Files.list() call
        if (!handwrittenDir.exists()) {
            error("Handwritten dir does not exist: $handwrittenDir")
        }
        var handwrittenCount = 0
        try {
            Files.list(handwrittenDir).use { stream ->
                val handwrittenFiles = stream.toList()
                handwrittenCount = handwrittenFiles.size
                for (path in handwrittenFiles) {
                    val name = path.fileName.toString()
                    if (!name.endsWith(".md")) continue
                    val target = outDir.resolve(name)
                    try {
                        if (name == "Home.md") {
                            val header = WikiTemplates.renderHomeHeader(modVersion, buildSha)
                            target.writeText(header + "\n" + path.readText())
                        } else {
                            Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING)
                        }
                    } catch (e: Exception) {
                        LOGGER.error("Failed to process handwritten file '$name': ${e.message}", e)
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to list handwritten directory '$handwrittenDir': ${e.message}", e)
        }

        // 4. Summary
        try {
            val pageCount = allSpecs.size + Category.values().size
            LOGGER.info("WikiGen: wrote $pageCount auto-generated pages and $handwrittenCount handwritten pages into $outDir")
        } catch (e: Exception) {
            LOGGER.error("Failed to generate summary statistics: ${e.message}", e)
        }
    }

    /** P1-004: Validate that targetPath is within the allowed directory. */
    private fun validatePathWithinDirectory(targetPath: Path, allowedDir: Path) {
        val normalizedAllowed = allowedDir.normalize()
        val normalizedTarget = targetPath.normalize()
        require(normalizedTarget.startsWith(normalizedAllowed)) {
            "Attempted to write file outside allowed directory: $normalizedTarget is not within $normalizedAllowed"
        }
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
