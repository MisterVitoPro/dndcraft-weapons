package com.dndweapons.combat

import com.dndweapons.catalog.Category
import com.dndweapons.catalog.DamageType
import com.dndweapons.catalog.Property
import com.dndweapons.catalog.RangeKind
import com.dndweapons.catalog.Weapons
import com.dndweapons.catalog.WeaponSpec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WeaponAttackHandlerTest {

    private fun ctx(
        sprinting: Boolean = false,
        hasVehicle: Boolean = false,
        offhandEmpty: Boolean = true,
        offhandSpec: WeaponSpec? = null,
    ) = WeaponAttackHandler.Context(sprinting, hasVehicle, offhandEmpty, offhandSpec)

    @Test
    fun noPropertiesReturnsBase() {
        val out = WeaponAttackHandler.modifyDamage(5f, Weapons.MACE, ctx())
        assertEquals(5f, out, 0.0001f)
    }

    @Test
    fun versatileWithEmptyOffhandAddsBonus() {
        val out = WeaponAttackHandler.modifyDamage(6f, Weapons.LONGSWORD, ctx(offhandEmpty = true))
        assertEquals(7f, out, 0.0001f)
    }

    @Test
    fun versatileWithFilledOffhandUnchanged() {
        val out = WeaponAttackHandler.modifyDamage(6f, Weapons.LONGSWORD, ctx(offhandEmpty = false))
        assertEquals(6f, out, 0.0001f)
    }

    @Test
    fun finesseWhileSprintingMultiplies() {
        val out = WeaponAttackHandler.modifyDamage(6f, Weapons.RAPIER, ctx(sprinting = true))
        assertEquals(7.2f, out, 0.0001f)
    }

    @Test
    fun finesseWhileWalkingUnchanged() {
        val out = WeaponAttackHandler.modifyDamage(6f, Weapons.RAPIER, ctx(sprinting = false))
        assertEquals(6f, out, 0.0001f)
    }

    @Test
    fun lightWithLightOffhandAddsOne() {
        val out = WeaponAttackHandler.modifyDamage(
            4f, Weapons.DAGGER,
            ctx(offhandEmpty = false, offhandSpec = Weapons.SCIMITAR),
        )
        assertEquals(5f, out, 0.0001f)
    }

    @Test
    fun lightWithNonLightOffhandUnchanged() {
        val out = WeaponAttackHandler.modifyDamage(
            4f, Weapons.DAGGER,
            ctx(offhandEmpty = false, offhandSpec = Weapons.LONGSWORD),
        )
        assertEquals(4f, out, 0.0001f)
    }

    @Test
    fun lightWithEmptyOffhandUnchanged() {
        val out = WeaponAttackHandler.modifyDamage(
            4f, Weapons.DAGGER,
            ctx(offhandEmpty = true, offhandSpec = null),
        )
        assertEquals(4f, out, 0.0001f)
    }

    @Test
    fun lanceOnFootHalves() {
        val out = WeaponAttackHandler.modifyDamage(7f, Weapons.LANCE, ctx(hasVehicle = false))
        assertEquals(3.5f, out, 0.0001f)
    }

    @Test
    fun lanceMountedFullDamage() {
        val out = WeaponAttackHandler.modifyDamage(7f, Weapons.LANCE, ctx(hasVehicle = true))
        assertEquals(7f, out, 0.0001f)
    }

    @Test
    fun finesseAndLightStackOnOneStrike() {
        // Scimitar = Finesse + Light. Dagger offhand (Light). Sprinting.
        // Order pinned by design: Light adds first -> 5 + 1 = 6, then Finesse x 1.20 -> 7.2.
        val out = WeaponAttackHandler.modifyDamage(
            5f, Weapons.SCIMITAR,
            ctx(sprinting = true, offhandEmpty = false, offhandSpec = Weapons.DAGGER),
        )
        assertEquals(7.2f, out, 0.0001f)
    }

    @Test
    fun lanceWithFinesseMultipliersChain() {
        val synthetic = WeaponSpec(
            id = "test_finesse_lance",
            displayName = "Test",
            category = Category.MARTIAL_MELEE,
            damageType = DamageType.PIERCING,
            diceText = "1d10",
            versatileDice = null,
            attackDamage = 7,
            versatileBonus = 0,
            attackSpeed = 1.0f,
            reachBonus = 0.0f,
            knockbackBonus = 1,
            properties = setOf(Property.FINESSE, Property.SPECIAL_LANCE),
            ranged = RangeKind.NONE,
            baseDurability = 250,
            vanillaRoleTag = null,
        )
        // Sprinting + on-foot: x 1.20 x 0.5 = x 0.6 -> 10 * 0.6 = 6
        val out = WeaponAttackHandler.modifyDamage(
            10f, synthetic, ctx(sprinting = true, hasVehicle = false),
        )
        assertEquals(6f, out, 0.0001f)
    }
}
