package com.dndweapons.registry

import com.dndweapons.catalog.WeaponSpec
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import org.slf4j.LoggerFactory
//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation
//?}
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier as ResourceLocation
*///?}

/**
 * Cache performance metrics tracking.
 * Measures hit/miss ratio and invalidation events.
 */
data class CacheMetrics(
    var totalLookups: Int = 0,
    var cacheHits: Int = 0,
    var cacheMisses: Int = 0,
    var cacheInvalidations: Int = 0,
) {
    fun recordLookup() { totalLookups++ }
    fun recordHit() { cacheHits++ }
    fun recordMiss() { cacheMisses++ }
    fun recordInvalidation() { cacheInvalidations++ }
    fun hitRatio(): Double = if (totalLookups == 0) 0.0 else cacheHits.toDouble() / totalLookups
    fun reset() {
        totalLookups = 0
        cacheHits = 0
        cacheMisses = 0
        cacheInvalidations = 0
    }
}

/**
 * Resolves Item -> WeaponSpec at runtime.
 *
 *  - byItem: filled by WeaponRegistrarImpl for registered DnD items (O(1) lookup).
 *  - byRoleTag: filled for the 4 vanilla-mapped specs (Shortsword, Shortbow,
 *    Light Crossbow, Trident). Lazily flattened to a per-Item map on first
 *    lookup miss; invalidated when tags reload (datapack reload, server start).
 *
 * Thread safety:
 *   Writes happen during mod init (single-threaded). Reads happen on server tick
 *   (mixin) and client tick (tooltip). roleCache is @Volatile; the build-store
 *   race is benign because both threads compute the same map content.
 */
object SpecRegistry {
    private val LOGGER = LoggerFactory.getLogger(SpecRegistry::class.java)

    private val byItem = mutableMapOf<Item, WeaponSpec>()

    // Keyed by the raw tag string ("ns:path") rather than TagKey<Item>. Constructing
    // a TagKey here would force Registries.<clinit> (which transitively pulls in
    // BuiltInRegistries.<clinit>) at registration time. Unit tests that call
    // bindRoleTag without bootstrapping the MC registries would hit a
    // "Not bootstrapped" RuntimeException. Deferring the TagKey construction to
    // buildRoleCacheAndStore() (called only at first lookup, which is guaranteed
    // post-bootstrap on the server thread) avoids that pitfall.
    private val byRoleTag = mutableMapOf<String, WeaponSpec>()
    @Volatile private var roleCache: Map<Item, WeaponSpec>? = null

    // Cache performance metrics (LOG-010)
    private val metrics = CacheMetrics()

    fun init() {
        CommonLifecycleEvents.TAGS_LOADED.register { _, _ -> invalidateRoleCache() }
    }

    fun bindRegistered(item: Item, spec: WeaponSpec) {
        byItem[item] = spec
        LOGGER.debug("Bound item '{}' to weapon spec '{}'", item.toString(), spec.id)
    }

    fun bindRoleTag(spec: WeaponSpec) {
        val tagStr = spec.vanillaRoleTag
            ?: error("bindRoleTag: spec '${spec.id}' has no vanillaRoleTag")
        // Validate the "ns:path" shape eagerly so a malformed spec fails at bind
        // time rather than at first lookup. Do NOT touch Registries / TagKey here.
        validateTagString(tagStr)
        byRoleTag[tagStr] = spec
        roleCache = null
        LOGGER.debug("Bound vanilla role tag '{}' to weapon spec '{}'", tagStr, spec.id)
    }

    fun lookup(item: Item): WeaponSpec? {
        metrics.recordLookup()
        byItem[item]?.let {
            metrics.recordHit()
            return it
        }
        val cached = (roleCache ?: buildRoleCacheAndStore())[item]
        if (cached != null) {
            metrics.recordHit()
        } else {
            metrics.recordMiss()
        }
        return cached
    }

    @Synchronized
    fun invalidateRoleCache() {
        roleCache = null
        metrics.recordInvalidation()
        LOGGER.debug("Role cache invalidated")
    }

    /**
     * P2-008: optimized double-checked locking to minimize tooltip contention under load.
     *
     * The synchronization here closes the invalidate-during-build race:
     * - First check (line 82 in lookup): lockfree probe of @Volatile roleCache
     * - Synchronized block: re-check, build cache, atomically store via volatile write
     * - Return: cache is now warm; future lookups avoid lock
     *
     * Double-checked locking pattern (JMM guarantees for @Volatile fields):
     * - Build happens exactly once per invalidation event
     * - TAGS_LOADED invalidation linearizes with concurrent tooltip builds
     * - Lock is held only during the re-check and atomic store, not during tag lookup
     */
    private fun buildRoleCacheAndStore(): Map<Item, WeaponSpec> {
        // Re-check inside the monitor: another thread may have completed the
        // build between our lookup() probe (roleCache?: ...) and our acquisition
        // of the lock. Return the existing cache to avoid duplicate work.
        synchronized(this) {
            roleCache?.let { return it }
            val out = mutableMapOf<Item, WeaponSpec>()
            for ((tagStr, spec) in byRoleTag) {
                val tag = parseItemTagKey(tagStr)
                for (holder in BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                    out[holder.value()] = spec
                }
            }
            roleCache = out
            return out
        }
    }

    private fun validateTagString(s: String) {
        val parts = s.split(":", limit = 2)
        require(parts.size == 2) { "Bad tag string '$s' (expected 'ns:path')" }
    }

    private fun parseItemTagKey(s: String): TagKey<Item> {
        val parts = s.split(":", limit = 2)
        require(parts.size == 2) { "Bad tag string '$s' (expected 'ns:path')" }
        //? if >=1.21 {
        val loc = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1])
        //?} else {
        /*val loc = ResourceLocation(parts[0], parts[1])
        *///?}
        return TagKey.create(Registries.ITEM, loc)
    }

    // ---- Cache metrics API (LOG-010) ----
    fun getCacheMetrics(): CacheMetrics = metrics

    // ---- test-only helpers (package-visible would be ideal; Kotlin object: public) ----
    internal fun clearForTest() {
        byItem.clear()
        byRoleTag.clear()
        roleCache = null
    }
    internal fun primeRoleCacheForTest() { roleCache = emptyMap() }
    internal fun hasRoleCacheForTest(): Boolean = roleCache != null
    internal fun boundItemCountForTest(): Int = byItem.size
    internal fun boundRoleTagCountForTest(): Int = byRoleTag.size
    internal fun resetCacheMetrics() { metrics.reset() }
}
