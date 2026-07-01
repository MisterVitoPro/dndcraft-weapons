// src/main/kotlin/com/dndweapons/codegen/wiki/WikiPaths.kt
package com.dndweapons.codegen.wiki

import com.dndweapons.catalog.Category

/**
 * Filename/URL helpers for the GitHub Wiki output. GitHub Wiki uses
 * hyphen-separated PascalCase filenames; spaces in display names map to hyphens.
 *
 * P1-004: Validates filenames to prevent path traversal attacks.
 *
 * Examples:
 *   "Longsword"       -> "Longsword.md"        / link target "Longsword"
 *   "Hand Crossbow"   -> "Hand-Crossbow.md"    / link target "Hand-Crossbow"
 *   "Light Crossbow"  -> "Light-Crossbow.md"   / link target "Light-Crossbow"
 */
object WikiPaths {

    // P1-004: Safe filename pattern - only alphanumerics, spaces, and hyphens
    private val SAFE_FILENAME_PATTERN = Regex("^[a-zA-Z0-9\\s\\-]+$")

    /** Convert a WeaponSpec.displayName to a wiki page filename (with `.md`). */
    fun weaponFilename(displayName: String): String =
        "${pageSlug(displayName)}.md"

    /** Convert a WeaponSpec.displayName to a GitHub Wiki link target (no `.md`, hyphen-separated). */
    fun pageSlug(displayName: String): String {
        val trimmed = displayName.trim()
        // P1-004: Validate against path traversal and malicious patterns
        validateFilename(trimmed)
        return trimmed.replace(Regex("\\s+"), "-")
    }

    /** P1-004: Validate filename to prevent path traversal and injection attacks. */
    private fun validateFilename(name: String) {
        require(name.isNotEmpty()) { "Filename must not be empty" }
        require(!name.contains("..") && !name.contains("//") && !name.contains("\\")) {
            "Filename '$name' contains path traversal patterns (.., //, or backslashes)"
        }
        require(SAFE_FILENAME_PATTERN.matches(name)) {
            "Filename '$name' contains invalid characters. Only alphanumerics, spaces, and hyphens are allowed."
        }
    }

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
