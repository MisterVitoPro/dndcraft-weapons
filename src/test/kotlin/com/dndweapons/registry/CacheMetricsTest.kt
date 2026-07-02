package com.dndweapons.registry

import com.dndweapons.catalog.Property
import com.dndweapons.catalog.RangeKind
import com.dndweapons.catalog.WeaponSpec
import net.minecraft.world.item.Item
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test cache performance metrics tracking for SpecRegistry.
 * Verifies that cache hits/misses/invalidations are tracked and logged.
 */
class CacheMetricsTest {

    @BeforeEach
    fun setup() {
        SpecRegistry.clearForTest()
    }

    @Test
    fun metricsTracksInitialState() {
        val metrics = SpecRegistry.getCacheMetrics()

        assertEquals(0, metrics.totalLookups, "Initial state should have 0 lookups")
        assertEquals(0, metrics.cacheHits, "Initial state should have 0 hits")
        assertEquals(0, metrics.cacheMisses, "Initial state should have 0 misses")
        assertEquals(0, metrics.cacheInvalidations, "Initial state should have 0 invalidations")
    }

    @Test
    fun metricsTracksCacheHits() {
        val spec = WeaponSpec(
            id = "test_sword",
            displayName = "Test Sword",
            damageBase = 6.0,
            properties = emptySet(),
            category = "sword",
            tier = null,
            vanillaRoleTag = null,
            ranged = RangeKind.NONE
        )

        // Register an item directly (not via role tag)
        val mockItem = MockItem()
        SpecRegistry.bindRegistered(mockItem, spec)

        // First lookup should be a hit
        val result1 = SpecRegistry.lookup(mockItem)
        assertEquals(spec, result1)

        // Second lookup should also be a hit
        val result2 = SpecRegistry.lookup(mockItem)
        assertEquals(spec, result2)

        val metrics = SpecRegistry.getCacheMetrics()
        assertEquals(2, metrics.cacheHits, "Both direct lookups should be hits")
        assertEquals(0, metrics.cacheMisses, "No misses expected")
        assertEquals(2, metrics.totalLookups, "Total lookups should be 2")
    }

    @Test
    fun metricsTracksInvalidations() {
        val metrics1 = SpecRegistry.getCacheMetrics()
        assertEquals(0, metrics1.cacheInvalidations)

        SpecRegistry.invalidateRoleCache()

        val metrics2 = SpecRegistry.getCacheMetrics()
        assertEquals(1, metrics2.cacheInvalidations, "Should track one invalidation")

        SpecRegistry.invalidateRoleCache()

        val metrics3 = SpecRegistry.getCacheMetrics()
        assertEquals(2, metrics3.cacheInvalidations, "Should track two invalidations")
    }

    @Test
    fun metricsHitRatioCalculation() {
        val mockItem = MockItem()
        val spec = WeaponSpec(
            id = "test_axe",
            displayName = "Test Axe",
            damageBase = 7.0,
            properties = emptySet(),
            category = "axe",
            tier = null,
            vanillaRoleTag = null,
            ranged = RangeKind.NONE
        )

        SpecRegistry.bindRegistered(mockItem, spec)

        // 10 lookups of the same item
        repeat(10) { SpecRegistry.lookup(mockItem) }

        val metrics = SpecRegistry.getCacheMetrics()
        assertEquals(10, metrics.totalLookups)
        assertEquals(10, metrics.cacheHits)
        assertEquals(0, metrics.cacheMisses)

        val hitRatio = metrics.hitRatio()
        assertEquals(1.0, hitRatio, 0.001, "Hit ratio should be 1.0 (100%)")
    }

    @Test
    fun metricsResetClearsAllCounters() {
        val mockItem = MockItem()
        val spec = WeaponSpec(
            id = "test_hammer",
            displayName = "Test Hammer",
            damageBase = 8.0,
            properties = emptySet(),
            category = "hammer",
            tier = null,
            vanillaRoleTag = null,
            ranged = RangeKind.NONE
        )

        SpecRegistry.bindRegistered(mockItem, spec)
        repeat(5) { SpecRegistry.lookup(mockItem) }
        SpecRegistry.invalidateRoleCache()

        val beforeReset = SpecRegistry.getCacheMetrics()
        assertTrue(beforeReset.totalLookups > 0, "Metrics should have data before reset")

        SpecRegistry.resetCacheMetrics()

        val afterReset = SpecRegistry.getCacheMetrics()
        assertEquals(0, afterReset.totalLookups, "Reset should clear lookups")
        assertEquals(0, afterReset.cacheHits, "Reset should clear hits")
        assertEquals(0, afterReset.cacheInvalidations, "Reset should clear invalidations")
    }

    /**
     * Mock Item for testing (does not require Minecraft registry).
     */
    private class MockItem : Item(Item.Properties())
}
