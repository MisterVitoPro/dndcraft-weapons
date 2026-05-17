package com.dndweapons.test

import com.dndweapons.DndWeaponsMod
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.TooltipFlag

//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation
//?}
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier as ResourceLocation
*///?}

//? if <1.21.5 {
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper

class TooltipInjectionGametest : FabricGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty")
    fun registeredItemTooltipContainsStatBlock(ctx: GameTestHelper) =
        runRegisteredTooltipCase(ctx)

    @GameTest(template = "fabric-gametest-api-v1:empty")
    fun vanillaIronSwordTooltipContainsStatBlock(ctx: GameTestHelper) =
        runVanillaTooltipCase(ctx)
}
//?}

//? if >=1.21.5 {

/*import net.fabricmc.fabric.api.gametest.v1.GameTest
import net.minecraft.gametest.framework.GameTestHelper

class TooltipInjectionGametest {

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun registeredItemTooltipContainsStatBlock(ctx: GameTestHelper) =
        runRegisteredTooltipCase(ctx)

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun vanillaIronSwordTooltipContainsStatBlock(ctx: GameTestHelper) =
        runVanillaTooltipCase(ctx)
}

*///?}

private fun runRegisteredTooltipCase(ctx: GameTestHelper) {
    val resId = makeRlTooltip(DndWeaponsMod.MOD_ID, "longsword")
    //? if >=1.21.2 {
    val item = BuiltInRegistries.ITEM.get(resId)
        .orElseThrow { AssertionError("longsword not registered") }.value()
    //?} else {
    /*val item = BuiltInRegistries.ITEM.get(resId)
        ?: throw AssertionError("longsword not registered")
    *///?}
    val stack = ItemStack(item)
    val lines = collectTooltip(stack)
    val text = lines.joinToString("|") { it.string }
    if (!text.contains("slashing")) {
        throw AssertionError("Longsword tooltip missing 'slashing'. Got: $text")
    }
    if (!text.contains("Versatile")) {
        throw AssertionError("Longsword tooltip missing 'Versatile'. Got: $text")
    }
    ctx.succeed()
}

private fun runVanillaTooltipCase(ctx: GameTestHelper) {
    val stack = ItemStack(Items.IRON_SWORD)
    val lines = collectTooltip(stack)
    val text = lines.joinToString("|") { it.string }
    if (!text.contains("piercing")) {
        throw AssertionError("Iron sword tooltip missing 'piercing'. Got: $text")
    }
    if (!text.contains("Finesse")) {
        throw AssertionError("Iron sword tooltip missing 'Finesse'. Got: $text")
    }
    ctx.succeed()
}

private fun collectTooltip(stack: ItemStack): List<Component> {
    // ItemTooltipCallback fires server-side too when getTooltipLines is invoked
    // with a null player; Fabric API guarantees the callback runs.
    //? if >=1.20.5 {
    return stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL)
    //?} else {
    /*return stack.getTooltipLines(null, TooltipFlag.NORMAL)
    *///?}
}

private fun makeRlTooltip(ns: String, path: String): ResourceLocation {
    //? if >=1.21 {
    return ResourceLocation.fromNamespaceAndPath(ns, path)
    //?} else {
    /*return ResourceLocation(ns, path)
    *///?}
}
