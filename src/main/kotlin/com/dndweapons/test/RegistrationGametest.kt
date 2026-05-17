package com.dndweapons.test

import com.dndweapons.DndWeaponsMod
import net.minecraft.core.registries.BuiltInRegistries

//? if <1.21.11 {
/*import net.minecraft.resources.ResourceLocation
*///?}
//? if >=1.21.11 {
import net.minecraft.resources.Identifier as ResourceLocation
//?}

//? if <1.21.5 {
/*import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper

class RegistrationGametest : FabricGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty")
    fun longswordIsRegistered(ctx: GameTestHelper) {
        val id = makeRl(DndWeaponsMod.MOD_ID, "longsword")
        val airId = makeRl("minecraft", "air")
        val item = BuiltInRegistries.ITEM.get(id)
        val airItem = BuiltInRegistries.ITEM.get(airId)
        if (item == airItem) {
            throw AssertionError("Longsword not registered (resolved to AIR)")
        }
        ctx.succeed()
    }
}
*///?}

//? if >=1.21.5 {

import net.fabricmc.fabric.api.gametest.v1.GameTest
import net.minecraft.gametest.framework.GameTestHelper

class RegistrationGametest {

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun longswordIsRegistered(ctx: GameTestHelper) {
        val id = makeRl(DndWeaponsMod.MOD_ID, "longsword")
        val airId = makeRl("minecraft", "air")
        val item = BuiltInRegistries.ITEM.get(id)
        val airItem = BuiltInRegistries.ITEM.get(airId)
        if (item == airItem) {
            throw AssertionError("Longsword not registered (resolved to AIR)")
        }
        ctx.succeed()
    }
}

//?}

private fun makeRl(ns: String, path: String): ResourceLocation {
    //? if >=1.21 {
    return ResourceLocation.fromNamespaceAndPath(ns, path)
    //?} else {
    /*return ResourceLocation(ns, path)
    *///?}
}
