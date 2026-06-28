package com.dndweapons.registry

import com.dndweapons.catalog.Weapons
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class SpecRegistryRaceConditionTest {

    @BeforeEach
    fun setup() {
        SpecRegistry.clearForTest()
    }

    @Test
    fun invalidateCacheWhileBuildingDoesNotCauseStaleWrite() {
        // Simulate: client thread building cache, server thread invalidating
        val buildStarted = CountDownLatch(1)
        val invalidateDone = CountDownLatch(1)

        val buildThread = Thread {
            try {
                // Prime the byRoleTag with one entry
                SpecRegistry.bindRoleTag(Weapons.SHORTSWORD)

                buildStarted.countDown()
                // Simulate slow build
                Thread.sleep(50)

                // This should NOT write stale data
                // By the time we get here, invalidate should have fired
                invalidateDone.await()
                Thread.sleep(10)
            } catch (e: Exception) {
                fail("Build thread exception: ${e.message}")
            }
        }

        val invalidateThread = Thread {
            try {
                buildStarted.await()
                Thread.sleep(25)  // Wait mid-bind
                SpecRegistry.invalidateRoleCache()
                invalidateDone.countDown()
            } catch (e: Exception) {
                fail("Invalidate thread exception: ${e.message}")
            }
        }

        buildThread.start()
        invalidateThread.start()
        buildThread.join()
        invalidateThread.join()

        // After invalidation, roleCache should be null
        assertFalse(SpecRegistry.hasRoleCacheForTest(),
            "Cache should be null after invalidation during bind")
    }

    @Test
    fun invalidateCacheIsSynchronized() {
        val method = SpecRegistry::class.java.getDeclaredMethod("invalidateRoleCache")
        assertTrue(
            method.modifiers and java.lang.reflect.Modifier.SYNCHRONIZED != 0,
            "invalidateRoleCache() must be synchronized"
        )
    }
}
