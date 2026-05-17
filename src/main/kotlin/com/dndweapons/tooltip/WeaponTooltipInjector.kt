package com.dndweapons.tooltip

import com.dndweapons.registry.SpecRegistry
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

/**
 * Registers ItemTooltipCallback to inject DnD tooltip lines for any item that
 * resolves to a WeaponSpec (registered or vanilla-mapped via role tag).
 *
 * Lines are inserted at index 1 (right after the item display name) so they
 * appear above the vanilla attribute block.
 *
 * Styling: italic gray, matching vanilla flavor-text convention.
 */
object WeaponTooltipInjector {

    fun register() {
        //? if >=1.20.5 {
        ItemTooltipCallback.EVENT.register(ItemTooltipCallback { stack, _, _, lines ->
            val spec = SpecRegistry.lookup(stack.item) ?: return@ItemTooltipCallback
            val newLines = WeaponTooltipBuilder.build(spec)
            if (newLines.isEmpty()) return@ItemTooltipCallback
            val components = newLines.map { it.toComponent() }
            lines.addAll(1, components)
        })
        //?} else {
        /*ItemTooltipCallback.EVENT.register(ItemTooltipCallback { stack, _, lines ->
            val spec = SpecRegistry.lookup(stack.item) ?: return@ItemTooltipCallback
            val newLines = WeaponTooltipBuilder.build(spec)
            if (newLines.isEmpty()) return@ItemTooltipCallback
            val components = newLines.map { it.toComponent() }
            lines.addAll(1, components)
        })
        *///?}
    }

    private fun TooltipLine.toComponent(): Component {
        // Args that themselves look like translation keys (start with "tooltip.dndweapons.")
        // get wrapped in Component.translatable so they resolve through the translation pipeline.
        val resolvedArgs: Array<Any> = args.map { arg ->
            if (arg is String && arg.startsWith("tooltip.dndweapons.")) {
                Component.translatable(arg) as Any
            } else arg
        }.toTypedArray()
        val component: MutableComponent = if (resolvedArgs.isEmpty()) {
            Component.translatable(translationKey)
        } else {
            Component.translatable(translationKey, *resolvedArgs)
        }
        return component.withStyle { it.withColor(ChatFormatting.GRAY).withItalic(true) }
    }
}
