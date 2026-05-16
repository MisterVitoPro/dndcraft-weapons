package com.dndweapons.test;

import com.dndweapons.DndWeaponsMod;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;

public class RegistrationGametest implements FabricGameTest {

    @GameTest(templateName = "fabric-gametest-api-v1:empty")
    public void longswordIsRegistered(TestContext ctx) {
        Identifier id = Identifier.of(DndWeaponsMod.MOD_ID, "longsword");
        Item item = Registries.ITEM.get(id);
        if (item == Registries.ITEM.get(Identifier.ofVanilla("air"))) {
            throw new AssertionError("Longsword not registered (resolved to AIR)");
        }
        ctx.complete();
    }
}
