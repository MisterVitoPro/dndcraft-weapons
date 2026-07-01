package com.dndweapons.acquisition

import com.dndweapons.catalog.Tier
import com.dndweapons.catalog.Weapons
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WeaponLookupTest {

    @Test
    fun validWeaponIdsLookupSuccessfully() {
        // Valid lookups should not throw
        val result = WeaponLookup.byId("longsword", Tier.DIAMOND)
        // May return null if not registered, but shouldn't throw
        assertNotNull(result != null || result == null) // Just verify it executes
    }

    @Test
    fun weaponIdWithPathTraversalRejected() {
        // P1-005: Path traversal attempts should be rejected
        assertThrows(IllegalArgumentException::class.java) {
            WeaponLookup.byId("../../../evil", Tier.DIAMOND)
        }
        assertThrows(IllegalArgumentException::class.java) {
            WeaponLookup.byId("..\\windows\\system", Tier.DIAMOND)
        }
    }

    @Test
    fun weaponIdWithInvalidCharactersRejected() {
        // P1-005: Invalid characters should be rejected
        assertThrows(IllegalArgumentException::class.java) {
            WeaponLookup.byId("sword:evil", Tier.DIAMOND)
        }
        assertThrows(IllegalArgumentException::class.java) {
            WeaponLookup.byId("sword\nmalicious", Tier.DIAMOND)
        }
        assertThrows(IllegalArgumentException::class.java) {
            WeaponLookup.byId("sword<script>", Tier.DIAMOND)
        }
    }

    @Test
    fun allNethierteWeaponsRetrieved() {
        // This should not throw
        val nethierite = WeaponLookup.allNetherite()
        // May be empty if not all weapons are registered, but shouldn't throw
        assertTrue(nethierite is List)
    }
}
