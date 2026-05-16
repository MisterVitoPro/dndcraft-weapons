package com.dndweapons.test

import com.dndweapons.DndWeaponsMod
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.registry.Registries
import net.minecraft.test.GameTest
import net.minecraft.test.TestContext
import net.minecraft.util.Identifier

class RegistrationGametest : FabricGameTest {

    @GameTest(templateName = "fabric-gametest-api-v1:empty")
    fun longswordIsRegistered(ctx: TestContext) {
        val id = Identifier.of(DndWeaponsMod.MOD_ID, "longsword")
        val item = Registries.ITEM.get(id)
        if (item == Registries.ITEM.get(Identifier.ofVanilla("air"))) {
            throw AssertionError("Longsword not registered (resolved to AIR)")
        }
        ctx.complete()
    }
}
