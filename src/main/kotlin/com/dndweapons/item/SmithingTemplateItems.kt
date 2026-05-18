package com.dndweapons.item

import com.dndweapons.DndWeaponsMod
import net.minecraft.ChatFormatting
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation
//?}
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier as ResourceLocation
*///?}
import net.minecraft.world.item.Item
import net.minecraft.world.item.SmithingTemplateItem

/**
 * The 2 smithing-upgrade templates for Phase 4. Each is a [SmithingTemplateItem]
 * (vanilla class) so the smithing-table UI renders the slot ghosts and tooltip lines
 * Mojang-style.
 *
 * Constructor signature is stable across all 5 MC versions: appliesTo, ingredients,
 * upgradeDescription, baseSlotDescription, additionsSlotDescription, baseSlotEmptyIcons,
 * additionsSlotEmptyIcons, Item.Properties.
 */
object SmithingTemplateItems {

    val DIAMOND: SmithingTemplateItem by lazy {
        register(
            "diamond_weapon_upgrade_template",
            appliesTo = "tooltip.dndweapons.smithing.diamond.applies_to",
            ingredients = "tooltip.dndweapons.smithing.diamond.ingredients",
            upgradeDescription = "tooltip.dndweapons.smithing.diamond.upgrade",
            baseEmptyIconPaths = listOf("item/empty_slot_sword"),
            additionsEmptyIconPaths = listOf("item/empty_slot_diamond"),
        )
    }

    val NETHERITE: SmithingTemplateItem by lazy {
        register(
            "netherite_weapon_upgrade_template",
            appliesTo = "tooltip.dndweapons.smithing.netherite.applies_to",
            ingredients = "tooltip.dndweapons.smithing.netherite.ingredients",
            upgradeDescription = "tooltip.dndweapons.smithing.netherite.upgrade",
            baseEmptyIconPaths = listOf("item/empty_slot_sword"),
            additionsEmptyIconPaths = listOf("item/empty_slot_netherite_ingot"),
        )
    }

    fun registerAll() {
        DIAMOND
        NETHERITE
        DndWeaponsMod.LOGGER.info("Registered 2 smithing-upgrade-template items.")
    }

    private fun register(
        id: String,
        appliesTo: String,
        ingredients: String,
        upgradeDescription: String,
        baseEmptyIconPaths: List<String>,
        additionsEmptyIconPaths: List<String>,
    ): SmithingTemplateItem {
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

        //? if >=1.21 {
        val baseIcons = baseEmptyIconPaths.map { ResourceLocation.fromNamespaceAndPath("minecraft", it) }
        val addIcons = additionsEmptyIconPaths.map { ResourceLocation.fromNamespaceAndPath("minecraft", it) }
        //?} else {
        /*val baseIcons = baseEmptyIconPaths.map { ResourceLocation("minecraft", it) }
        val addIcons = additionsEmptyIconPaths.map { ResourceLocation("minecraft", it) }
        *///?}

        val item = SmithingTemplateItem(
            Component.translatable(appliesTo).withStyle(ChatFormatting.BLUE),
            Component.translatable(ingredients).withStyle(ChatFormatting.BLUE),
            Component.translatable(upgradeDescription),
            Component.translatable("$upgradeDescription.base"),
            Component.translatable("$upgradeDescription.additions"),
            baseIcons,
            addIcons,
            props,
        )
        return Registry.register(BuiltInRegistries.ITEM, key, item)
    }
}
