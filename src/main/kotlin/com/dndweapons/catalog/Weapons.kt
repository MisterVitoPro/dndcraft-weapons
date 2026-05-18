package com.dndweapons.catalog

/**
 * Source of truth for the entire weapon catalog. 38 PHB weapons total:
 * 34 registered + 4 vanilla-mapped (Shortsword, Shortbow, Light Crossbow, Trident).
 *
 * Values translated from master design section 4
 * (docs/superpowers/specs/2026-05-16-dnd-weapons-design.md).
 *
 * Conventions:
 * - attackDamage = round(dice_average + 1.5); calibrated so 1d8 = 6 (vanilla iron sword).
 * - versatileBonus = attackDamage delta when wielded two-handed (versatile property).
 * - attackSpeed: 1.8 very fast / 1.6 fast / 1.5 normal / 1.0 polearm / 0.9 heavy.
 *   Ranged weapons use 1.5 placeholder; vanilla draw mechanics override at runtime.
 * - reachBonus: 1.0 for Reach property weapons (polearms + whip), 0.0 otherwise.
 * - knockbackBonus: 1 for Heavy property weapons, 0 otherwise.
 * - baseDurability: 250 (iron-tier baseline) for all.
 */
object Weapons {

    // ===== Simple Melee (10) =====

    val CLUB = WeaponSpec(
        id = "club", displayName = "Club",
        category = Category.SIMPLE_MELEE, damageType = DamageType.BLUDGEONING,
        diceText = "1d4", versatileDice = null,
        attackDamage = 4, versatileBonus = 0,
        attackSpeed = 1.6f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.LIGHT),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val DAGGER = WeaponSpec(
        id = "dagger", displayName = "Dagger",
        category = Category.SIMPLE_MELEE, damageType = DamageType.PIERCING,
        diceText = "1d4", versatileDice = null,
        attackDamage = 4, versatileBonus = 0,
        attackSpeed = 1.8f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.LIGHT, Property.THROWN),
        ranged = RangeKind.THROWN, baseDurability = 250, vanillaRoleTag = null,
    )

    val GREATCLUB = WeaponSpec(
        id = "greatclub", displayName = "Greatclub",
        category = Category.SIMPLE_MELEE, damageType = DamageType.BLUDGEONING,
        diceText = "1d8", versatileDice = null,
        attackDamage = 6, versatileBonus = 0,
        attackSpeed = 0.9f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.TWO_HANDED),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val HANDAXE = WeaponSpec(
        id = "handaxe", displayName = "Handaxe",
        category = Category.SIMPLE_MELEE, damageType = DamageType.SLASHING,
        diceText = "1d6", versatileDice = null,
        attackDamage = 5, versatileBonus = 0,
        attackSpeed = 1.6f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.LIGHT, Property.THROWN),
        ranged = RangeKind.THROWN, baseDurability = 250, vanillaRoleTag = null,
    )

    val JAVELIN = WeaponSpec(
        id = "javelin", displayName = "Javelin",
        category = Category.SIMPLE_MELEE, damageType = DamageType.PIERCING,
        diceText = "1d6", versatileDice = null,
        attackDamage = 5, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.THROWN),
        ranged = RangeKind.THROWN, baseDurability = 250, vanillaRoleTag = null,
    )

    val LIGHT_HAMMER = WeaponSpec(
        id = "light_hammer", displayName = "Light Hammer",
        category = Category.SIMPLE_MELEE, damageType = DamageType.BLUDGEONING,
        diceText = "1d4", versatileDice = null,
        attackDamage = 4, versatileBonus = 0,
        attackSpeed = 1.6f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.LIGHT, Property.THROWN),
        ranged = RangeKind.THROWN, baseDurability = 250, vanillaRoleTag = null,
    )

    val MACE = WeaponSpec(
        id = "mace", displayName = "Mace",
        category = Category.SIMPLE_MELEE, damageType = DamageType.BLUDGEONING,
        diceText = "1d6", versatileDice = null,
        attackDamage = 5, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = emptySet(),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val QUARTERSTAFF = WeaponSpec(
        id = "quarterstaff", displayName = "Quarterstaff",
        category = Category.SIMPLE_MELEE, damageType = DamageType.BLUDGEONING,
        diceText = "1d6", versatileDice = "1d8",
        attackDamage = 5, versatileBonus = 1,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.VERSATILE),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val SICKLE = WeaponSpec(
        id = "sickle", displayName = "Sickle",
        category = Category.SIMPLE_MELEE, damageType = DamageType.SLASHING,
        diceText = "1d4", versatileDice = null,
        attackDamage = 4, versatileBonus = 0,
        attackSpeed = 1.8f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.LIGHT),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val SPEAR = WeaponSpec(
        id = "spear", displayName = "Spear",
        category = Category.SIMPLE_MELEE, damageType = DamageType.PIERCING,
        diceText = "1d6", versatileDice = "1d8",
        attackDamage = 5, versatileBonus = 1,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.THROWN, Property.VERSATILE),
        ranged = RangeKind.THROWN, baseDurability = 250, vanillaRoleTag = null,
    )

    // ===== Simple Ranged (4: 2 registered, 2 vanilla-mapped) =====

    val DART = WeaponSpec(
        id = "dart", displayName = "Dart",
        category = Category.SIMPLE_RANGED, damageType = DamageType.PIERCING,
        diceText = "1d4", versatileDice = null,
        attackDamage = 4, versatileBonus = 0,
        attackSpeed = 1.8f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.FINESSE, Property.THROWN),
        ranged = RangeKind.THROWN, baseDurability = 250, vanillaRoleTag = null,
    )

    val SLING = WeaponSpec(
        id = "sling", displayName = "Sling",
        category = Category.SIMPLE_RANGED, damageType = DamageType.BLUDGEONING,
        diceText = "1d4", versatileDice = null,
        attackDamage = 4, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.AMMUNITION),
        ranged = RangeKind.SLING, baseDurability = 250, vanillaRoleTag = null,
    )

    val SHORTBOW = WeaponSpec(
        id = "shortbow", displayName = "Shortbow",
        category = Category.SIMPLE_RANGED, damageType = DamageType.PIERCING,
        diceText = "1d6", versatileDice = null,
        attackDamage = 5, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.TWO_HANDED),
        ranged = RangeKind.BOW, baseDurability = 250,
        vanillaRoleTag = "dndweapons:role/shortbow",
    )

    val LIGHT_CROSSBOW = WeaponSpec(
        id = "light_crossbow", displayName = "Light Crossbow",
        category = Category.SIMPLE_RANGED, damageType = DamageType.PIERCING,
        diceText = "1d8", versatileDice = null,
        attackDamage = 6, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.LOADING, Property.TWO_HANDED),
        ranged = RangeKind.CROSSBOW, baseDurability = 250,
        vanillaRoleTag = "dndweapons:role/light_crossbow",
    )

    // ===== Martial Melee (18: 16 registered (incl. existing Longsword), 2 vanilla-mapped) =====

    val LONGSWORD = WeaponSpec(
        id = "longsword", displayName = "Longsword",
        category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
        diceText = "1d8", versatileDice = "1d10",
        attackDamage = 6, versatileBonus = 1,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.VERSATILE),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val BATTLEAXE = WeaponSpec(
        id = "battleaxe", displayName = "Battleaxe",
        category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
        diceText = "1d8", versatileDice = "1d10",
        attackDamage = 6, versatileBonus = 1,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.VERSATILE),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val FLAIL = WeaponSpec(
        id = "flail", displayName = "Flail",
        category = Category.MARTIAL_MELEE, damageType = DamageType.BLUDGEONING,
        diceText = "1d8", versatileDice = null,
        attackDamage = 6, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = emptySet(),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val GLAIVE = WeaponSpec(
        id = "glaive", displayName = "Glaive",
        category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
        diceText = "1d10", versatileDice = null,
        attackDamage = 7, versatileBonus = 0,
        attackSpeed = 1.0f, reachBonus = 1.0f, knockbackBonus = 1,
        properties = setOf(Property.HEAVY, Property.REACH, Property.TWO_HANDED),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val GREATAXE = WeaponSpec(
        id = "greataxe", displayName = "Greataxe",
        category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
        diceText = "1d12", versatileDice = null,
        attackDamage = 8, versatileBonus = 0,
        attackSpeed = 0.9f, reachBonus = 0.0f, knockbackBonus = 1,
        properties = setOf(Property.HEAVY, Property.TWO_HANDED),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val GREATSWORD = WeaponSpec(
        id = "greatsword", displayName = "Greatsword",
        category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
        diceText = "2d6", versatileDice = null,
        attackDamage = 9, versatileBonus = 0,
        attackSpeed = 0.9f, reachBonus = 0.0f, knockbackBonus = 1,
        properties = setOf(Property.HEAVY, Property.TWO_HANDED),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val HALBERD = WeaponSpec(
        id = "halberd", displayName = "Halberd",
        category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
        diceText = "1d10", versatileDice = null,
        attackDamage = 7, versatileBonus = 0,
        attackSpeed = 1.0f, reachBonus = 1.0f, knockbackBonus = 1,
        properties = setOf(Property.HEAVY, Property.REACH, Property.TWO_HANDED),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val LANCE = WeaponSpec(
        id = "lance", displayName = "Lance",
        category = Category.MARTIAL_MELEE, damageType = DamageType.PIERCING,
        diceText = "1d10", versatileDice = null,
        attackDamage = 7, versatileBonus = 0,
        attackSpeed = 1.0f, reachBonus = 1.0f, knockbackBonus = 1,
        properties = setOf(Property.HEAVY, Property.REACH, Property.TWO_HANDED, Property.SPECIAL_LANCE),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val MAUL = WeaponSpec(
        id = "maul", displayName = "Maul",
        category = Category.MARTIAL_MELEE, damageType = DamageType.BLUDGEONING,
        diceText = "2d6", versatileDice = null,
        attackDamage = 9, versatileBonus = 0,
        attackSpeed = 0.9f, reachBonus = 0.0f, knockbackBonus = 1,
        properties = setOf(Property.HEAVY, Property.TWO_HANDED),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val MORNINGSTAR = WeaponSpec(
        id = "morningstar", displayName = "Morningstar",
        category = Category.MARTIAL_MELEE, damageType = DamageType.PIERCING,
        diceText = "1d8", versatileDice = null,
        attackDamage = 6, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = emptySet(),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val PIKE = WeaponSpec(
        id = "pike", displayName = "Pike",
        category = Category.MARTIAL_MELEE, damageType = DamageType.PIERCING,
        diceText = "1d10", versatileDice = null,
        attackDamage = 7, versatileBonus = 0,
        attackSpeed = 1.0f, reachBonus = 1.0f, knockbackBonus = 1,
        properties = setOf(Property.HEAVY, Property.REACH, Property.TWO_HANDED),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val RAPIER = WeaponSpec(
        id = "rapier", displayName = "Rapier",
        category = Category.MARTIAL_MELEE, damageType = DamageType.PIERCING,
        diceText = "1d8", versatileDice = null,
        attackDamage = 6, versatileBonus = 0,
        attackSpeed = 1.6f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.FINESSE),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val SCIMITAR = WeaponSpec(
        id = "scimitar", displayName = "Scimitar",
        category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
        diceText = "1d6", versatileDice = null,
        attackDamage = 5, versatileBonus = 0,
        attackSpeed = 1.8f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.FINESSE, Property.LIGHT),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val SHORTSWORD = WeaponSpec(
        id = "shortsword", displayName = "Shortsword",
        category = Category.MARTIAL_MELEE, damageType = DamageType.PIERCING,
        diceText = "1d6", versatileDice = null,
        attackDamage = 5, versatileBonus = 0,
        attackSpeed = 1.8f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.FINESSE, Property.LIGHT),
        ranged = RangeKind.NONE, baseDurability = 250,
        vanillaRoleTag = "dndweapons:role/shortsword",
    )

    val TRIDENT = WeaponSpec(
        id = "trident", displayName = "Trident",
        category = Category.MARTIAL_MELEE, damageType = DamageType.PIERCING,
        diceText = "1d8", versatileDice = "1d10",
        attackDamage = 6, versatileBonus = 1,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.THROWN, Property.VERSATILE),
        ranged = RangeKind.THROWN, baseDurability = 250,
        vanillaRoleTag = "dndweapons:role/trident",
    )

    val WAR_PICK = WeaponSpec(
        id = "war_pick", displayName = "War Pick",
        category = Category.MARTIAL_MELEE, damageType = DamageType.PIERCING,
        diceText = "1d8", versatileDice = null,
        attackDamage = 6, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = emptySet(),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val WARHAMMER = WeaponSpec(
        id = "warhammer", displayName = "Warhammer",
        category = Category.MARTIAL_MELEE, damageType = DamageType.BLUDGEONING,
        diceText = "1d8", versatileDice = "1d10",
        attackDamage = 6, versatileBonus = 1,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.VERSATILE),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    val WHIP = WeaponSpec(
        id = "whip", displayName = "Whip",
        category = Category.MARTIAL_MELEE, damageType = DamageType.SLASHING,
        diceText = "1d4", versatileDice = null,
        attackDamage = 4, versatileBonus = 0,
        attackSpeed = 1.8f, reachBonus = 1.0f, knockbackBonus = 0,
        properties = setOf(Property.FINESSE, Property.REACH),
        ranged = RangeKind.NONE, baseDurability = 250, vanillaRoleTag = null,
    )

    // ===== Martial Ranged (6) =====

    val BLOWGUN = WeaponSpec(
        id = "blowgun", displayName = "Blowgun",
        category = Category.MARTIAL_RANGED, damageType = DamageType.PIERCING,
        diceText = "1", versatileDice = null,
        attackDamage = 2, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.LOADING),
        ranged = RangeKind.BLOWGUN, baseDurability = 250, vanillaRoleTag = null,
    )

    val HAND_CROSSBOW = WeaponSpec(
        id = "hand_crossbow", displayName = "Hand Crossbow",
        category = Category.MARTIAL_RANGED, damageType = DamageType.PIERCING,
        diceText = "1d6", versatileDice = null,
        attackDamage = 5, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.LIGHT, Property.LOADING),
        ranged = RangeKind.CROSSBOW, baseDurability = 250, vanillaRoleTag = null,
    )

    val HEAVY_CROSSBOW = WeaponSpec(
        id = "heavy_crossbow", displayName = "Heavy Crossbow",
        category = Category.MARTIAL_RANGED, damageType = DamageType.PIERCING,
        diceText = "1d10", versatileDice = null,
        attackDamage = 7, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 1,
        properties = setOf(Property.HEAVY, Property.LOADING, Property.TWO_HANDED),
        ranged = RangeKind.CROSSBOW, baseDurability = 250, vanillaRoleTag = null,
    )

    val LONGBOW = WeaponSpec(
        id = "longbow", displayName = "Longbow",
        category = Category.MARTIAL_RANGED, damageType = DamageType.PIERCING,
        diceText = "1d8", versatileDice = null,
        attackDamage = 6, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 1,
        properties = setOf(Property.HEAVY, Property.TWO_HANDED),
        ranged = RangeKind.BOW, baseDurability = 250, vanillaRoleTag = null,
    )

    val MUSKET = WeaponSpec(
        id = "musket", displayName = "Musket",
        category = Category.MARTIAL_RANGED, damageType = DamageType.PIERCING,
        diceText = "1d12", versatileDice = null,
        attackDamage = 8, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.LOADING, Property.TWO_HANDED),
        ranged = RangeKind.FIREARM, baseDurability = 250, vanillaRoleTag = null,
    )

    val PISTOL = WeaponSpec(
        id = "pistol", displayName = "Pistol",
        category = Category.MARTIAL_RANGED, damageType = DamageType.PIERCING,
        diceText = "1d10", versatileDice = null,
        attackDamage = 7, versatileBonus = 0,
        attackSpeed = 1.5f, reachBonus = 0.0f, knockbackBonus = 0,
        properties = setOf(Property.LOADING),
        ranged = RangeKind.FIREARM, baseDurability = 250, vanillaRoleTag = null,
    )

    // ===== Catalog list (38 entries) =====

    val ALL: List<WeaponSpec> = listOf(
        // Simple Melee (10)
        CLUB, DAGGER, GREATCLUB, HANDAXE, JAVELIN,
        LIGHT_HAMMER, MACE, QUARTERSTAFF, SICKLE, SPEAR,
        // Simple Ranged (4)
        DART, SLING, SHORTBOW, LIGHT_CROSSBOW,
        // Martial Melee (18)
        LONGSWORD, BATTLEAXE, FLAIL, GLAIVE, GREATAXE, GREATSWORD,
        HALBERD, LANCE, MAUL, MORNINGSTAR, PIKE, RAPIER, SCIMITAR,
        SHORTSWORD, TRIDENT, WAR_PICK, WARHAMMER, WHIP,
        // Martial Ranged (6)
        BLOWGUN, HAND_CROSSBOW, HEAVY_CROSSBOW, LONGBOW, MUSKET, PISTOL,
    )
}

/**
 * Returns a copy of this WeaponSpec with the given tier applied. Tier affects:
 *   - id: suffixed with "_diamond" / "_netherite" (iron leaves it untouched).
 *   - displayName: prefixed with "Diamond " / "Netherite " (iron untouched).
 *   - attackDamage: added to baseline (+1 diamond, +2 netherite).
 *   - baseDurability: replaced with the tier's vanilla durability (250 / 1561 / 2031).
 *
 * Properties, attack speed, damage type, reach, and knockback carry over unchanged.
 * The `vanillaRoleTag` field is preserved but in practice atTier() is only called for
 * non-vanilla-mapped specs (see [Weapons.ALL_TIERED] filter).
 */
fun WeaponSpec.atTier(t: Tier): WeaponSpec = copy(
    id = if (t == Tier.IRON) id else "$id${t.suffix}",
    displayName = if (t == Tier.IRON) displayName else "${t.displayPrefix}$displayName",
    attackDamage = attackDamage + t.damageBonus,
    baseDurability = t.durability,
)
