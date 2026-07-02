package com.dndweapons.codegen.wiki

import com.dndweapons.catalog.DamageType
import com.dndweapons.catalog.Property
import com.dndweapons.catalog.RangeKind
import com.dndweapons.catalog.WeaponSpec
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Test Markdown sanitization in wiki generation (SEC-004).
 * Verifies that weapon display names with special characters are escaped properly.
 */
class WikiSanitizationTest {

    @Test
    fun sanitizationHandlesBackticks() {
        val spec = specWithName("Test`Weapon")
        val page = WikiTemplates.renderWeaponPage(spec, EmptyAcquisitionLookup)

        assertTrue(page.contains("Test\\`Weapon"), "Backticks should be escaped")
    }

    @Test
    fun sanitizationHandlesBrackets() {
        val spec = specWithName("Test[Weapon]")
        val page = WikiTemplates.renderWeaponPage(spec, EmptyAcquisitionLookup)

        assertTrue(page.contains("Test\\[Weapon\\]"), "Brackets should be escaped")
    }

    @Test
    fun sanitizationHandlesPipes() {
        val spec = specWithName("Test|Weapon")
        val page = WikiTemplates.renderWeaponPage(spec, EmptyAcquisitionLookup)

        assertTrue(page.contains("Test\\|Weapon"), "Pipes should be escaped")
    }

    @Test
    fun sanitizationHandlesMultipleSpecialChars() {
        val spec = specWithName("Test[Weapon]|Special`Char")
        val page = WikiTemplates.renderWeaponPage(spec, EmptyAcquisitionLookup)

        assertTrue(page.contains("Test\\[Weapon\\]\\|Special\\`Char"), "Multiple special chars should all be escaped")
    }

    @Test
    fun normalNamesPassThrough() {
        val spec = specWithName("Longsword")
        val page = WikiTemplates.renderWeaponPage(spec, EmptyAcquisitionLookup)

        assertTrue(page.contains("Longsword"), "Normal names should pass through unchanged")
    }

    /**
     * Helper to create a WeaponSpec with a custom display name.
     */
    private fun specWithName(name: String): WeaponSpec = WeaponSpec(
        id = "test_weapon",
        displayName = name,
        damageBase = 5.0,
        properties = emptySet(),
        category = "test",
        tier = null,
        vanillaRoleTag = null,
        ranged = RangeKind.NONE
    )

    /**
     * Stub acquisition lookup that returns no facts.
     */
    private object EmptyAcquisitionLookup : AcquisitionLookup {
        override fun factsFor(weaponId: String): List<AcquisitionFact> = emptyList()
    }
}
