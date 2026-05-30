// src/test/kotlin/com/dndweapons/registry/WeaponRegistrarShimTest.kt
package com.dndweapons.registry

import com.dndweapons.catalog.Tier
import com.dndweapons.catalog.WeaponSpec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * P2-004: Phase 4 §4 says `register(spec)` single-arg shim should stay on the
 * WeaponRegistrar interface for backward compat with callers outside the new
 * tier-aware loop. After fix, the interface exposes both:
 *   - register(spec, tier)
 *   - register(spec)   (delegates to register(spec, Tier.IRON))
 */
class WeaponRegistrarShimTest {

    @Test
    fun singleArgRegisterDelegatesToIronTier() {
        // Use a fake impl that records every (spec, tier) the interface calls into.
        val recorded = mutableListOf<Pair<String, Tier>>()
        val r = object : WeaponRegistrar {
            override fun register(spec: WeaponSpec, tier: Tier) {
                recorded += spec.id to tier
            }
        }
        val fakeSpec = com.dndweapons.catalog.Weapons.LONGSWORD
        r.register(fakeSpec)
        assertEquals(listOf("longsword" to Tier.IRON), recorded,
            "register(spec) shim must delegate to register(spec, Tier.IRON)")
    }
}
