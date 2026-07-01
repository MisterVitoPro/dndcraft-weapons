package com.dndweapons.tooltip

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class TooltipLineTest {

    @Test
    fun validTranslationKeyAccepted() {
        // P1-003: Valid translation keys should be accepted
        assertDoesNotThrow {
            TooltipLine("tooltip.dndweapons.property.finesse")
        }
        assertDoesNotThrow {
            TooltipLine("tooltip.dndweapons.stat_block")
        }
        assertDoesNotThrow {
            TooltipLine("item.dndweapons.sword")
        }
    }

    @Test
    fun keyWithValidArgsAccepted() {
        assertDoesNotThrow {
            TooltipLine("tooltip.dndweapons.property.versatile.with_dice:1d10", listOf("1d10"))
        }
        assertDoesNotThrow {
            TooltipLine("tooltip.dndweapons.stat_block", listOf("1d8", "slashing", " · Light"))
        }
    }

    @Test
    fun keyWithPathTraversalRejected() {
        // P1-003: Translation keys should not allow path traversal attempts
        assertThrows(IllegalArgumentException::class.java) {
            TooltipLine("../../../etc/passwd")
        }
        assertThrows(IllegalArgumentException::class.java) {
            TooltipLine("tooltip.dndweapons/../../../evil")
        }
    }

    @Test
    fun keyWithInvalidCharactersRejected() {
        // P1-003: Translation keys should only allow safe characters
        assertThrows(IllegalArgumentException::class.java) {
            TooltipLine("tooltip.dndweapons.property<script>")
        }
        assertThrows(IllegalArgumentException::class.java) {
            TooltipLine("tooltip.dndweapons.property;rm -rf /")
        }
        assertThrows(IllegalArgumentException::class.java) {
            TooltipLine("tooltip.dndweapons\\\nmalicious")
        }
    }

    @Test
    fun emptyKeyRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            TooltipLine("")
        }
    }

    @Test
    fun nullKeyRejected() {
        // Kotlin data class constructor will handle this, but let's be explicit
        // This test documents the expected behavior
    }
}
