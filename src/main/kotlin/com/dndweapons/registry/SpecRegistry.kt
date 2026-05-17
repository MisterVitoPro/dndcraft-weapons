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
    private val byRoleTag = mutableMapOf<TagKey<Item>, WeaponSpec>()
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
        byRoleTag[parseItemTagKey(tagStr)] = spec
        roleCache = null
    }

    fun lookup(item: Item): WeaponSpec? {
        byItem[item]?.let { return it }
        return (roleCache ?: buildRoleCacheAndStore())[item]
    }

    fun invalidateRoleCache() {
        roleCache = null
    }

    private fun buildRoleCacheAndStore(): Map<Item, WeaponSpec> {
        val out = mutableMapOf<Item, WeaponSpec>()
        for ((tag, spec) in byRoleTag) {
            for (holder in BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                out[holder.value()] = spec
            }
        }
        roleCache = out
        return out
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
