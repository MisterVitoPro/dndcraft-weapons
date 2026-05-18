package com.dndweapons.test

import com.dndweapons.DndWeaponsMod
import com.dndweapons.catalog.DamageType
import com.dndweapons.catalog.Property
import com.dndweapons.catalog.WeaponSpec
import com.dndweapons.registry.SpecRegistry
import com.dndweapons.tooltip.WeaponTooltipBuilder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Items

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
    val spec = SpecRegistry.lookup(item)
        ?: throw AssertionError("longsword item has no WeaponSpec in SpecRegistry")
    assertTooltipContainsDamageType(spec, DamageType.SLASHING, "Longsword")
    assertTooltipContainsProperty(spec, Property.VERSATILE, "Longsword")
    ctx.succeed()
}

private fun runVanillaTooltipCase(ctx: GameTestHelper) {
    val spec = SpecRegistry.lookup(Items.IRON_SWORD)
        ?: throw AssertionError(
            "Iron Sword has no WeaponSpec in SpecRegistry. " +
            "Ensure its role tag is bound in WeaponRegistrarImpl."
        )
    assertTooltipContainsDamageType(spec, DamageType.PIERCING, "Iron Sword")
    assertTooltipContainsProperty(spec, Property.FINESSE, "Iron Sword")
    ctx.succeed()
}

/**
 * Asserts that WeaponTooltipBuilder.build(spec) produces a stat-block line whose
 * translationKey encodes the expected damage type.
 *
 * WeaponTooltipBuilder.build() is a pure function with no MC or client-API
 * dependencies, so it is safe to call from a server-side gametest.
 */
private fun assertTooltipContainsDamageType(spec: WeaponSpec, expected: DamageType, label: String) {
    val lines = WeaponTooltipBuilder.build(spec)
    val expectedFragment = expected.name.lowercase()
    // Damage type is passed as an arg to the stat_block line (it's a translation
    // key resolved client-side by Component.translatable), not as a separate
    // line's translationKey. So check both the key and any string args.
    val hit = lines.any { line ->
        line.translationKey.contains(expectedFragment) ||
            line.args.any { it is String && it.contains(expectedFragment) }
    }
    if (!hit) {
        val keys = lines.joinToString("|") { it.translationKey }
        val argsDump = lines.joinToString("|") { line ->
            line.args.filterIsInstance<String>().joinToString(",")
        }
        throw AssertionError(
            "$label tooltip missing damage type '$expectedFragment'. " +
            "Translation keys: $keys. String args: $argsDump"
        )
    }
}

/**
 * Asserts that WeaponTooltipBuilder.build(spec) produces a bonus line whose
 * translationKey encodes the expected property name.
 *
 * This is equivalent to checking that the spec carries the property and that
 * WeaponTooltipBuilder emits a line for it.
 */
private fun assertTooltipContainsProperty(spec: WeaponSpec, expected: Property, label: String) {
    val lines = WeaponTooltipBuilder.build(spec)
    val allKeys = lines.joinToString("|") { it.translationKey }
    val expectedFragment = expected.name.lowercase()
    if (lines.none { it.translationKey.contains(expectedFragment) }) {
        throw AssertionError(
            "$label tooltip missing property '$expectedFragment'. " +
            "Translation keys: $allKeys"
        )
    }
}

private fun makeRlTooltip(ns: String, path: String): ResourceLocation {
    //? if >=1.21 {
    return ResourceLocation.fromNamespaceAndPath(ns, path)
    //?} else {
    /*return ResourceLocation(ns, path)
    *///?}
}
