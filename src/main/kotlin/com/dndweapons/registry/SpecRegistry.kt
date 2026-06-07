package com.dndweapons.registry

import com.dndweapons.catalog.WeaponSpec
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation
//?}
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier as ResourceLocation
*///?}

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

    fun init() {
        CommonLifecycleEvents.TAGS_LOADED.register { _, _ -> invalidateRoleCache() }
    }

    fun bindRegistered(item: Item, spec: WeaponSpec) {
        byItem[item] = spec
    }

    fun bindRoleTag(spec: WeaponSpec) {
        val tagStr = spec.vanillaRoleTag
            ?: error("bindRoleTag: spec '${spec.id}' has no vanillaRoleTag")
        // Validate the "ns:path" shape eagerly so a malformed spec fails at bind
        // time rather than at first lookup. Do NOT touch Registries / TagKey here.
        validateTagString(tagStr)
        byRoleTag[tagStr] = spec
        roleCache = null
    }

    fun lookup(item: Item): WeaponSpec? {
        byItem[item]?.let { return it }
        return (roleCache ?: buildRoleCacheAndStore())[item]
    }

    fun invalidateRoleCache() {
        roleCache = null
    }

    /**
     * P2-017: synchronized to close the invalidate-during-build race. The benign-race
     * note in the class doc covered concurrent build-then-overwrite (both threads
     * compute the same map content). It did NOT cover the case where
     * invalidateRoleCache() fires (TAGS_LOADED on server thread) mid-way through a
     * concurrent client-thread tooltip build, which would write stale tag data
     * back to roleCache AFTER invalidate set it null. @Synchronized linearizes the
     * build with both invalidate and any concurrent build, eliminating the race.
     */
    @Synchronized
    private fun buildRoleCacheAndStore(): Map<Item, WeaponSpec> {
        // Re-check inside the monitor: another thread may have completed the
        // build between our lookup() probe (roleCache?: ...) and our acquisition
        // of the lock. Return the existing cache to avoid duplicate work.
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
}
