package com.dndweapons.codegen

import com.dndweapons.catalog.Category
import com.dndweapons.codegen.wiki.WikiPaths
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class WikiPathsTest {

    @Test
    fun validWeaponNamesProduceValidFilenames() {
        // Valid cases
        assertEquals("Longsword.md", WikiPaths.weaponFilename("Longsword"))
        assertEquals("Hand-Crossbow.md", WikiPaths.weaponFilename("Hand Crossbow"))
        assertEquals("Light-Crossbow.md", WikiPaths.weaponFilename("Light Crossbow"))
    }

    @Test
    fun weaponNameWithPathTraversalRejected() {
        // P1-004: Path traversal attempts should be rejected
        assertThrows(IllegalArgumentException::class.java) {
            WikiPaths.weaponFilename("../../../etc/passwd")
        }
        assertThrows(IllegalArgumentException::class.java) {
            WikiPaths.weaponFilename("Sword..txt")
        }
        assertThrows(IllegalArgumentException::class.java) {
            WikiPaths.weaponFilename("..\\System")
        }
    }

    @Test
    fun weaponNameWithMaliciousCharactersRejected() {
        // P1-004: Reject potentially dangerous characters
        assertThrows(IllegalArgumentException::class.java) {
            WikiPaths.weaponFilename("Sword<script>alert()</script>")
        }
        assertThrows(IllegalArgumentException::class.java) {
            WikiPaths.weaponFilename("Sword;rm -rf /")
        }
        assertThrows(IllegalArgumentException::class.java) {
            WikiPaths.weaponFilename("Sword`command`")
        }
    }

    @Test
    fun validCategoryIndexFilenames() {
        // These should work fine as they come from enum
        assertEquals("Simple-Melee.md", WikiPaths.categoryIndexFilename(Category.SIMPLE_MELEE))
        assertEquals("Martial-Ranged.md", WikiPaths.categoryIndexFilename(Category.MARTIAL_RANGED))
    }

    @Test
    fun pageSlugValidation() {
        // P1-004: Test slug generation with validation
        assertEquals("Longsword", WikiPaths.pageSlug("Longsword"))
        assertEquals("Hand-Crossbow", WikiPaths.pageSlug("Hand Crossbow"))

        // Rejection of dangerous inputs
        assertThrows(IllegalArgumentException::class.java) {
            WikiPaths.pageSlug("../evil")
        }
    }
}
