// src/main/kotlin/com/dndweapons/item/SmithingComponentItems.kt
package com.dndweapons.item

import com.dndweapons.DndWeaponsMod
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation
//?}
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier as ResourceLocation
*///?}
import net.minecraft.world.item.Item

/**
 * The 6 raw crafting-ingredient items for the Phase 4 smithing system:
 *
 *  - 2 fragments (diamond_template_fragment, netherite_template_fragment): produced
 *    by their respective crafting recipes (4x output per craft), consumed by the
 *    core-assembly crafting recipe (4x input).
 *  - 2 bindings (weapon_smithing_binding, infernal_binding): each consumed once per
 *    core-assembly crafting recipe.
 *  - 2 cores (diamond_template_core, netherite_template_core): output of the
 *    fragment+binding crafting recipe; consumed by the smithing-table template
 *    assembly recipe.
 *
 * All 6 are plain Items with no combat or special behavior. The final templates are
 * registered separately in [SmithingTemplateItems] because they need
 * SmithingTemplateItem for the smithing-table UI.
 */
object SmithingComponentItems {

    val DIAMOND_TEMPLATE_FRAGMENT: Item by lazy { register("diamond_template_fragment") }
    val WEAPON_SMITHING_BINDING: Item by lazy { register("weapon_smithing_binding") }
    val DIAMOND_TEMPLATE_CORE: Item by lazy { register("diamond_template_core") }
    val NETHERITE_TEMPLATE_FRAGMENT: Item by lazy { register("netherite_template_fragment") }
    val INFERNAL_BINDING: Item by lazy { register("infernal_binding") }
    val NETHERITE_TEMPLATE_CORE: Item by lazy { register("netherite_template_core") }

    /** All 6 items in registration order. Call this once during mod init. */
    fun registerAll() {
        // Touch each lazy delegate to force registration.
        listOf(
            DIAMOND_TEMPLATE_FRAGMENT,
            WEAPON_SMITHING_BINDING,
            DIAMOND_TEMPLATE_CORE,
            NETHERITE_TEMPLATE_FRAGMENT,
            INFERNAL_BINDING,
            NETHERITE_TEMPLATE_CORE,
        )
        DndWeaponsMod.LOGGER.info("Registered 6 smithing-component items.")
    }

    private fun register(id: String): Item {
        //? if >=1.21 {
        val itemId = ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, id)
        //?} else {
        /*val itemId = ResourceLocation(DndWeaponsMod.MOD_ID, id)
        *///?}
        val key = ResourceKey.create(Registries.ITEM, itemId)
        //? if >=1.21.2 {
        val props = Item.Properties().setId(key)
        //?} else {
        /*val props = Item.Properties()
        *///?}
        val item = Item(props)
        return Registry.register(BuiltInRegistries.ITEM, key, item)
    }
}
