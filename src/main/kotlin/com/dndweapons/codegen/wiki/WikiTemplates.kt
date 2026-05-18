// src/main/kotlin/com/dndweapons/codegen/wiki/WikiTemplates.kt
package com.dndweapons.codegen.wiki

import com.dndweapons.catalog.Category
import com.dndweapons.catalog.DamageType
import com.dndweapons.catalog.Property
import com.dndweapons.catalog.RangeKind
import com.dndweapons.catalog.WeaponSpec

/**
 * Hand-rolled buildString-based Markdown templates. No external templating
 * library. Each function returns a complete page body (no trailing newline
 * normalization beyond a single trailing newline).
 */
object WikiTemplates {

    fun renderWeaponPage(spec: WeaponSpec, lookup: AcquisitionLookup): String = buildString {
        appendLine("# ${spec.displayName}")
        appendLine()
        appendLine(
            "> **Category:** ${labelFor(spec.category)}  |  " +
                "**Damage type:** ${labelFor(spec.damageType)}  |  " +
                "**DnD dice:** ${spec.diceText}" +
                (spec.versatileDice?.let { " (versatile $it)" } ?: "")
        )
        appendLine()

        if (spec.isVanillaMapped) {
            appendLine(renderVanillaCallout(spec))
            appendLine()
        }

        appendLine("## Stats")
        appendLine()
        appendLine("| Property | Value |")
        appendLine("|---|---|")
        val dmgCell =
            if (spec.versatileBonus > 0) "${spec.attackDamage} (+${spec.versatileBonus} versatile)"
            else "${spec.attackDamage}"
        appendLine("| Attack damage | $dmgCell |")
        appendLine("| Attack speed | ${spec.attackSpeed} |")
        appendLine("| Reach bonus | ${spec.reachBonus} |")
        appendLine("| Knockback bonus | +${spec.knockbackBonus} |")
        val propsCell = if (spec.properties.isEmpty()) "(none)"
                        else spec.properties.sortedBy { it.name }.joinToString(", ") { labelFor(it) }
        appendLine("| Properties | $propsCell |")
        appendLine("| Range | ${labelFor(spec.ranged)} |")
        appendLine("| Base durability | ${spec.baseDurability} |")
        appendLine()

        appendLine("## Tiers")
        appendLine()
        if (spec.isVanillaMapped) {
            appendLine("Vanilla material progression applies. No smithing template required.")
        } else {
            appendLine(
                "This weapon supports the full smithing-upgrade ladder " +
                    "(iron -> diamond -> netherite), as described in " +
                    "[Smithing-Upgrade-System](Smithing-Upgrade-System)."
            )
        }
        appendLine()

        appendLine("## Acquisition")
        appendLine()
        val facts = lookup.factsFor(spec.id)
        if (spec.isVanillaMapped) {
            appendLine(
                "Acquired via the vanilla Minecraft item (see callout above). " +
                    "Not present in any structure loot, villager trade, or mob drop in v1.0."
            )
        } else if (facts.isEmpty()) {
            appendLine(
                "Crafting only. Not present in any structure loot, villager trade, " +
                    "or mob drop in v1.0."
            )
        } else {
            for (fact in facts) {
                appendLine("- ${renderFactLine(fact)}")
            }
        }
        appendLine()

        appendLine("## Combat behaviour")
        appendLine()
        appendLine(
            "Properties translated to vanilla-feeling hooks per the " +
                "[Combat-Mechanics](Combat-Mechanics) page."
        )
        if (spec.properties.isNotEmpty()) {
            appendLine()
            for (prop in spec.properties.sortedBy { it.name }) {
                appendLine("- **${labelFor(prop)}**: ${propertyHookSummary(prop)}")
            }
        }
    }

    fun renderCategoryIndex(category: Category, specs: List<WeaponSpec>): String = buildString {
        appendLine("# ${WikiPaths.categoryLabel(category)}")
        appendLine()
        appendLine("| Weapon | Damage | Properties | Notes |")
        appendLine("|---|---|---|---|")
        for (spec in specs.filter { it.category == category }.sortedBy { it.displayName }) {
            val link = WikiPaths.pageSlug(spec.displayName)
            val propsCell = if (spec.properties.isEmpty()) "-"
                            else spec.properties.sortedBy { it.name }.joinToString(", ") { labelFor(it) }
            val notes = if (spec.isVanillaMapped) "Vanilla-mapped" else ""
            appendLine("| [${spec.displayName}]($link) | ${spec.attackDamage} | $propsCell | $notes |")
        }
    }

    fun renderHomeHeader(modVersion: String, buildSha: String): String = buildString {
        appendLine("<!-- AUTO-GENERATED HEADER -->")
        appendLine(
            "**DnD Weapons** for Minecraft -- v$modVersion, build $buildSha. " +
                "Supported MC versions: 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2."
        )
        appendLine()
        appendLine("----")
        appendLine()
        appendLine("<!-- HANDWRITTEN BODY -->")
    }

    // ----- private helpers -----

    private fun renderVanillaCallout(spec: WeaponSpec): String {
        val vanillaName = when (spec.id) {
            "shortsword"     -> "Iron Sword (and any vanilla sword tier)"
            "shortbow"       -> "Bow"
            "light_crossbow" -> "Crossbow"
            "trident"        -> "Trident"
            else             -> "vanilla item"
        }
        return ":information_source:  **This weapon is represented by the vanilla Minecraft " +
            "$vanillaName.** When you craft or pick up the vanilla item, it carries " +
            "the ${spec.displayName} identity: tooltip stat block, combat hooks, and " +
            "role tag. No separate item is registered."
    }

    private fun renderFactLine(fact: AcquisitionFact): String = when (fact) {
        is AcquisitionFact.StructureChest -> {
            val versionNote = fact.minVersion?.let { " _(${it}+ only)_" } ?: ""
            "Structure chest: **${fact.tableLabel}** -- ${fact.chancePct}% (${labelFor(fact.tier)} tier)$versionNote"
        }
        is AcquisitionFact.MobDrop ->
            "Mob drop: **${fact.mobLabel}** -- ${fact.chancePct}% (${labelFor(fact.tier)} tier)"
        is AcquisitionFact.VillagerTrade ->
            "Villager trade: **${fact.profession}** level ${fact.level} -- ${fact.emeralds} emeralds"
    }

    private fun propertyHookSummary(prop: Property): String = when (prop) {
        Property.LIGHT         -> "+1 damage when offhand also holds a Light weapon (dual-wield)"
        Property.HEAVY         -> "+1 knockback level on hit"
        Property.FINESSE       -> "+20% damage when attacker is sprinting"
        Property.VERSATILE     -> "+versatile damage bonus when wielded two-handed"
        Property.TWO_HANDED    -> "Requires both hands; offhand items prevent attack"
        Property.REACH         -> "+1 block attack range"
        Property.THROWN        -> "Right-click to throw as a ranged projectile"
        Property.AMMUNITION    -> "Requires the appropriate ammo item"
        Property.LOADING       -> "Reload animation between shots"
        Property.SPECIAL_LANCE -> "Lance special: see weapon-specific notes (mounted bonus, off-hand restriction)"
    }

    private fun labelFor(category: Category): String = WikiPaths.categoryLabel(category)
        .removeSuffix(" Weapons")

    private fun labelFor(dt: DamageType): String = when (dt) {
        DamageType.SLASHING    -> "Slashing"
        DamageType.PIERCING    -> "Piercing"
        DamageType.BLUDGEONING -> "Bludgeoning"
    }

    private fun labelFor(p: Property): String =
        p.name.lowercase().replaceFirstChar(Char::titlecase).replace('_', ' ')

    private fun labelFor(r: RangeKind): String = when (r) {
        RangeKind.NONE     -> "Melee"
        RangeKind.THROWN   -> "Thrown"
        RangeKind.BOW      -> "Bow"
        RangeKind.CROSSBOW -> "Crossbow"
        RangeKind.FIREARM  -> "Firearm"
        RangeKind.SLING    -> "Sling"
        RangeKind.BLOWGUN  -> "Blowgun"
    }

    private fun labelFor(t: com.dndweapons.catalog.Tier): String = when (t) {
        com.dndweapons.catalog.Tier.IRON      -> "Iron"
        com.dndweapons.catalog.Tier.DIAMOND   -> "Diamond"
        com.dndweapons.catalog.Tier.NETHERITE -> "Netherite"
    }
}
