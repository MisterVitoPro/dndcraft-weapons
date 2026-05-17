package com.dndweapons.test

import com.dndweapons.DndWeaponsMod
import net.minecraft.registry.Registries
import net.minecraft.test.TestContext
import net.minecraft.util.Identifier

//? if <1.21.5 {
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.test.GameTest

class RegistrationGametest : FabricGameTest {

    @GameTest(templateName = "fabric-gametest-api-v1:empty")
    fun longswordIsRegistered(ctx: TestContext) {
        //? if >=1.21 {
        val id = Identifier.of(DndWeaponsMod.MOD_ID, "longsword")
        val airId = Identifier.ofVanilla("air")
        //?} else {
        /*
        val id = Identifier(DndWeaponsMod.MOD_ID, "longsword")
        val airId = Identifier("minecraft", "air")
        */
        //?}
        val item = Registries.ITEM.get(id)
        if (item == Registries.ITEM.get(airId)) {
            throw AssertionError("Longsword not registered (resolved to AIR)")
        }
        ctx.complete()
    }
}
//?}

//? if >=1.21.5 {

/*import net.fabricmc.fabric.api.gametest.v1.GameTest

class RegistrationGametest {

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    fun longswordIsRegistered(ctx: TestContext) {
        val id = Identifier.of(DndWeaponsMod.MOD_ID, "longsword")
        val airId = Identifier.ofVanilla("air")
        val item = Registries.ITEM.get(id)
        if (item == Registries.ITEM.get(airId)) {
            throw AssertionError("Longsword not registered (resolved to AIR)")
        }
        ctx.complete()
    }
}

*///?}
