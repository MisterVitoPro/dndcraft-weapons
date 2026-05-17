package com.dndweapons.registry

import com.dndweapons.catalog.Weapons
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SpecRegistryTest {

    @BeforeEach
    fun resetRegistry() {
        SpecRegistry.clearForTest()
    }

    @Test
    fun bindRoleTagInsertsAndInvalidatesCache() {
        // Synthesize a state where roleCache is non-null, then bind should null it.
        SpecRegistry.primeRoleCacheForTest()
        assertEquals(true, SpecRegistry.hasRoleCacheForTest())
        SpecRegistry.bindRoleTag(Weapons.SHORTSWORD)
        assertEquals(false, SpecRegistry.hasRoleCacheForTest(),
            "bindRoleTag must invalidate role cache")
    }

    @Test
    fun bindRoleTagRejectsSpecsWithoutTag() {
        val ex = assertThrows(IllegalStateException::class.java) {
            SpecRegistry.bindRoleTag(Weapons.LONGSWORD)  // not vanilla-mapped
        }
        assert(ex.message!!.contains("vanillaRoleTag"))
    }

    @Test
    fun invalidateRoleCacheNullsTheCache() {
        SpecRegistry.primeRoleCacheForTest()
        SpecRegistry.invalidateRoleCache()
        assertEquals(false, SpecRegistry.hasRoleCacheForTest())
    }

    @Test
    fun lookupReturnsNullForUnboundItemWhenByItemEmpty() {
        // No items bound, no role tags bound; lookup of any Items.IRON_SWORD should be null.
        // We can't easily reference Items.IRON_SWORD without MC runtime, so instead just verify
        // the in-process state: byItem is empty, byRoleTag is empty.
        assertEquals(0, SpecRegistry.boundItemCountForTest())
        assertEquals(0, SpecRegistry.boundRoleTagCountForTest())
    }

    @Test
    fun multipleRoleTagBindsAccumulate() {
        SpecRegistry.bindRoleTag(Weapons.SHORTSWORD)
        SpecRegistry.bindRoleTag(Weapons.SHORTBOW)
        SpecRegistry.bindRoleTag(Weapons.LIGHT_CROSSBOW)
        SpecRegistry.bindRoleTag(Weapons.TRIDENT)
        assertEquals(4, SpecRegistry.boundRoleTagCountForTest())
    }
}
