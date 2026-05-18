package com.dndweapons.acquisition

import com.dndweapons.DndWeaponsMod
import com.dndweapons.catalog.Tier
import com.dndweapons.catalog.Weapons
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation
//?} else {
/*import net.minecraft.resources.Identifier as ResourceLocation
*///?}

/**
 * Resolves (base weapon id, tier) -> registered Item by consulting the Phase 4
 * `Weapons.ALL_TIERED` list and the vanilla item registry. Used by both the
 * loot registrar (to convert catalog entries into LootItem stacks) and the
 * trade registrar (to build MerchantOffer result stacks).
 *
 * Returns null when:
 *  - The base id is not in `Weapons.ALL`, OR
 *  - The (id, tier) pair is not in `Weapons.ALL_TIERED` (e.g. requesting diamond
 *    for a vanilla-mapped weapon like shortsword, or a ranged weapon excluded
 *    from the tier ladder in Phase 4).
 */
object WeaponLookup {

    fun byId(weaponId: String, tier: Tier): Item? {
        val targetId = weaponId + tier.suffix
        val location = rl(DndWeaponsMod.MOD_ID, targetId)
        //? if >=1.21.2 {
        return BuiltInRegistries.ITEM.get(location)
            .map { it.value() }
            .orElse(null)
        //?} else {
        /*val item = BuiltInRegistries.ITEM.get(location)
        val airItem = BuiltInRegistries.ITEM.get(rl("minecraft", "air"))
        return if (item == null || item === airItem) null else item
        *///?}
    }

    /**
     * All registered netherite-tier weapons. Used by the Warden mob-drop pool
     * (random selection) and the Wither trophy event handler.
     */
    fun allNetherite(): List<Item> = Weapons.ALL_TIERED
        .filter { (_, tier) -> tier == Tier.NETHERITE }
        .mapNotNull { (spec, _) -> byId(spec.id.removeSuffix("_netherite"), Tier.NETHERITE) }

    private fun rl(namespace: String, path: String): ResourceLocation {
        //? if >=1.21 {
        return ResourceLocation.fromNamespaceAndPath(namespace, path)
        //?} else {
        /*return ResourceLocation(namespace, path)
        *///?}
    }
}
