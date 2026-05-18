// src/main/kotlin/com/dndweapons/test/SmithingGametest.kt
package com.dndweapons.test

import com.dndweapons.DndWeaponsMod
import com.dndweapons.catalog.Property
import com.dndweapons.catalog.Tier
import com.dndweapons.catalog.Weapons
import com.dndweapons.registry.SpecRegistry
import net.minecraft.core.registries.BuiltInRegistries

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

class SmithingGametest : FabricGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty")
    fun smithingDiamondUpgradePreservesSpec(ctx: GameTestHelper) = runDiamondPreservesSpec(ctx)
}
//?}

//? if >=1.21.5 {

/*import net.fabricmc.fabric.api.gametest.v1.GameTest
import net.minecraft.gametest.framework.GameTestHelper

class SmithingGametest {

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun smithingDiamondUpgradePreservesSpec(ctx: GameTestHelper) = runDiamondPreservesSpec(ctx)
}

*///?}

private fun runDiamondPreservesSpec(ctx: GameTestHelper) {
    val baseSpec = Weapons.LONGSWORD
    //? if >=1.21 {
    val itemId = ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, "longsword_diamond")
    //?} else {
    /*val itemId = ResourceLocation(DndWeaponsMod.MOD_ID, "longsword_diamond")
    *///?}
    //? if >=1.21.2 {
    val item = BuiltInRegistries.ITEM.get(itemId).orElseThrow {
        AssertionError("longsword_diamond not registered")
    }.value()
    //?} else {
    /*val item = BuiltInRegistries.ITEM.get(itemId)
        ?: throw AssertionError("longsword_diamond not registered")
    *///?}

    val spec = SpecRegistry.lookup(item)
        ?: throw AssertionError("longsword_diamond has no WeaponSpec")

    if (spec.id != "longsword_diamond")
        throw AssertionError("Expected id 'longsword_diamond', got '${spec.id}'")
    if (spec.attackDamage != baseSpec.attackDamage + Tier.DIAMOND.damageBonus)
        throw AssertionError("Expected attackDamage ${baseSpec.attackDamage + 1}, got ${spec.attackDamage}")
    if (spec.baseDurability != Tier.DIAMOND.durability)
        throw AssertionError("Expected durability ${Tier.DIAMOND.durability}, got ${spec.baseDurability}")
    if (spec.properties != baseSpec.properties)
        throw AssertionError("Properties mismatch: ${spec.properties} vs ${baseSpec.properties}")
    if (Property.VERSATILE !in spec.properties)
        throw AssertionError("Diamond longsword should still be VERSATILE")
    ctx.succeed()
}
