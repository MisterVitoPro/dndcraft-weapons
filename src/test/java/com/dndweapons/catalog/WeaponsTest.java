package com.dndweapons.catalog;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WeaponsTest {

    @Test
    void allIsNonEmpty() {
        assertFalse(Weapons.ALL.isEmpty());
    }

    @Test
    void allIdsAreUnique() {
        Set<String> seen = new HashSet<>();
        for (WeaponSpec spec : Weapons.ALL) {
            assertTrue(seen.add(spec.id()), "duplicate spec id: " + spec.id());
        }
    }

    @Test
    void longswordIsPresentAndCorrect() {
        WeaponSpec longsword = Weapons.ALL.stream()
            .filter(s -> s.id().equals("longsword"))
            .findFirst()
            .orElseThrow();
        assertEquals("Longsword", longsword.displayName());
        assertEquals(Category.MARTIAL_MELEE, longsword.category());
        assertEquals(DamageType.SLASHING, longsword.damageType());
        assertEquals(6, longsword.attackDamage());
        assertEquals(1, longsword.versatileBonus());
        assertEquals(1.5f, longsword.attackSpeed());
        assertTrue(longsword.properties().contains(Property.VERSATILE));
        assertFalse(longsword.isVanillaMapped());
    }
}
