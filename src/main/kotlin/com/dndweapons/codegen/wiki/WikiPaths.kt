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
