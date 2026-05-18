// src/main/kotlin/com/dndweapons/test/AcquisitionGametest.kt
package com.dndweapons.test

//? if <1.21.5 {
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
//?}
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper

/**
 * Phase 5 acquisition gametests. Three smoke tests:
 *  1. Stronghold corridor loot table produces a mod weapon over N rolls.
 *  2. Weaponsmith level-1 trades include a mod weapon.
 *  3. Vindicator drops a mod battleaxe in a small kill batch.
 *
 * Class declaration is version-gated like CombatHooksGametest and
 * SmithingGametest: pre-1.21.5 extends FabricGameTest with `template`,
 * post-1.21.5 is a bare class with `structure`.
 */
//? if <1.21.5 {
class AcquisitionGametest : FabricGameTest {
    // Phase 5 gametests will be appended here in Tasks 13-15.
}
//?} else {
/*class AcquisitionGametest {
    // Phase 5 gametests will be appended here in Tasks 13-15.
}
*///?}
