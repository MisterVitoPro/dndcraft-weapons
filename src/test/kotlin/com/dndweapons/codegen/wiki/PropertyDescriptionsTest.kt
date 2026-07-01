package com.dndweapons.codegen.wiki

import com.dndweapons.catalog.Property
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PropertyDescriptionsTest {

    @Test
    fun allPropertiesHaveDescriptions() {
        // P1-008: Every property must have a description
        for (prop in Property.values()) {
            val description = PropertyDescriptions.summaryFor(prop)
            assertNotNull(description, "Property $prop should have a description")
            assertTrue(description.isNotEmpty(), "Description for property $prop should not be empty")
        }
    }

    @Test
    fun descriptionContent() {
        // P1-008: Verify some sample descriptions to ensure they're reasonable
        val lightDesc = PropertyDescriptions.summaryFor(Property.LIGHT)
        assertTrue(lightDesc.contains("damage") || lightDesc.contains("wield"), "Light description should mention damage or wielding")

        val finesseDesc = PropertyDescriptions.summaryFor(Property.FINESSE)
        assertTrue(finesseDesc.contains("damage") || finesseDesc.contains("sprint"), "Finesse description should mention damage or sprinting")
    }
}
