package com.dndweapons.catalog;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WeaponSpecTest {

    @Test
    void canConstructMinimalMeleeSpec() {
        WeaponSpec longsword = new WeaponSpec(
            "longsword", "Longsword",
            Category.MARTIAL_MELEE, DamageType.SLASHING,
            "1d8", "1d10",
            6, 1, 1.5f, 0.0f, 0,
            Set.of(Property.VERSATILE),
            RangeKind.NONE, 250, null
        );
        assertEquals("longsword", longsword.id());
        assertEquals(6, longsword.attackDamage());
        assertTrue(longsword.properties().contains(Property.VERSATILE));
    }

    @Test
    void blankIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new WeaponSpec(
            "", "Longsword",
            Category.MARTIAL_MELEE, DamageType.SLASHING,
            "1d8", null,
            6, 0, 1.5f, 0.0f, 0,
            Set.of(),
            RangeKind.NONE, 250, null
        ));
    }

    @Test
    void vanillaRoleTagWhenSetMakesItVanillaMapped() {
        WeaponSpec shortsword = new WeaponSpec(
            "shortsword", "Shortsword",
            Category.MARTIAL_MELEE, DamageType.PIERCING,
            "1d6", null,
            5, 0, 1.8f, 0.0f, 0,
            Set.of(Property.LIGHT, Property.FINESSE),
            RangeKind.NONE, 250,
            "dndweapons:role/shortsword"
        );
        assertNotNull(shortsword.vanillaRoleTag());
        assertTrue(shortsword.isVanillaMapped());
    }

    @Test
    void nullVanillaRoleTagMeansRegisteredItem() {
        WeaponSpec longsword = new WeaponSpec(
            "longsword", "Longsword",
            Category.MARTIAL_MELEE, DamageType.SLASHING,
            "1d8", "1d10",
            6, 1, 1.5f, 0.0f, 0,
            Set.of(Property.VERSATILE),
            RangeKind.NONE, 250, null
        );
        assertNull(longsword.vanillaRoleTag());
        assertFalse(longsword.isVanillaMapped());
    }
}
