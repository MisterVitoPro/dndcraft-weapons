package com.dndweapons.codegen

import com.dndweapons.catalog.Tier
import com.dndweapons.catalog.Weapons
import java.io.File

/**
 * One-shot codegen for Phase 4. Run from an IDE or `./gradlew :1.20.1:runMain
 * -PmainClass=com.dndweapons.codegen.Phase4CodegenKt`. Writes recipes, model
 * JSONs, and lang entries directly into src/main/resources/. Re-running is
 * idempotent except for the lang file which is rewritten from scratch.
 *
 * This is NOT a gradle data-generation task; it's a manual one-time run during
 * Phase 4 implementation. The resulting files are committed to the repo and
 * become the source of truth thereafter.
 */
fun main() {
    val root = File("src/main/resources")
    require(root.exists()) { "Run from project root (src/main/resources must exist)" }

    emitSmithingTransformRecipes(root)
    emitModelJsons(root)
    emitLangEntries(root)
    println("Phase 4 codegen complete. 54 transform recipes, 62 models, 66 lang entries.")
    println("NOTE: component/core/template-assembly recipes are NOT generated here; ")
    println("      hand-author them per tasks 11-14 of the implementation plan.")
}

private fun emitSmithingTransformRecipes(root: File) {
    val recipeDir = File(root, "data/dndweapons/recipe").also { it.mkdirs() }
    for ((spec, tier) in Weapons.ALL_TIERED) {
        if (tier == Tier.IRON) continue   // base iron already exists from Phase 2b crafting
        val baseId = if (tier == Tier.DIAMOND) spec.id.removeSuffix("_diamond")
                     else spec.id.removeSuffix("_netherite") + "_diamond"
        val (templateId, additionTag) = when (tier) {
            Tier.DIAMOND -> "dndweapons:diamond_weapon_upgrade_template" to "c:gems/diamond"
            Tier.NETHERITE -> "dndweapons:netherite_weapon_upgrade_template" to "c:ingots/netherite"
            Tier.IRON -> error("unreachable")
        }
        val json = """{
  "type": "minecraft:smithing_transform",
  "template": { "item": "$templateId" },
  "base":     { "item": "dndweapons:$baseId" },
  "addition": { "tag": "$additionTag" },
  "result":   { "id": "dndweapons:${spec.id}" }
}
"""
        File(recipeDir, "${spec.id}.json").writeText(json)
    }
}

private fun emitModelJsons(root: File) {
    val modelDir = File(root, "assets/dndweapons/models/item").also { it.mkdirs() }
    val itemIds = buildList {
        for ((spec, _) in Weapons.ALL_TIERED) if (spec.vanillaRoleTag == null) add(spec.id)
        addAll(listOf(
            "diamond_template_fragment", "weapon_smithing_binding", "diamond_template_core",
            "netherite_template_fragment", "infernal_binding", "netherite_template_core",
            "diamond_weapon_upgrade_template", "netherite_weapon_upgrade_template",
        ))
    }
    for (id in itemIds.distinct()) {
        val json = """{
  "parent": "minecraft:item/generated",
  "textures": { "layer0": "dndweapons:item/$id" }
}
"""
        File(modelDir, "$id.json").writeText(json)
    }
}

private fun emitLangEntries(root: File) {
    val langFile = File(root, "assets/dndweapons/lang/en_us.json")
    require(langFile.exists()) { "en_us.json missing — Phase 2b should have created it" }
    // Read existing JSON as an ordered map by re-tokenising. We don't pull in
    // a JSON library to keep codegen runnable without runtime deps.
    val existing = langFile.readText()
        .lines()
        .filter { it.contains("\":") && !it.trim().startsWith("//") }
        .associate {
            val k = it.substringAfter("\"").substringBefore("\"")
            val v = it.substringAfter("\": \"").substringBefore("\"")
            k to v
        }
        .toMutableMap()

    for ((spec, tier) in Weapons.ALL_TIERED) {
        if (tier == Tier.IRON || spec.vanillaRoleTag != null) continue
        existing["item.dndweapons.${spec.id}"] = spec.displayName
    }
    existing["item.dndweapons.diamond_template_fragment"] = "Diamond Template Fragment"
    existing["item.dndweapons.weapon_smithing_binding"]   = "Weapon Smithing Binding"
    existing["item.dndweapons.diamond_template_core"]     = "Diamond Template Core"
    existing["item.dndweapons.netherite_template_fragment"] = "Netherite Template Fragment"
    existing["item.dndweapons.infernal_binding"]          = "Infernal Binding"
    existing["item.dndweapons.netherite_template_core"]   = "Netherite Template Core"
    existing["item.dndweapons.diamond_weapon_upgrade_template"]   = "Diamond Weapon Upgrade Template"
    existing["item.dndweapons.netherite_weapon_upgrade_template"] = "Netherite Weapon Upgrade Template"
    existing["tooltip.dndweapons.smithing.diamond.applies_to"]    = "DnD Weapons"
    existing["tooltip.dndweapons.smithing.diamond.ingredients"]   = "Diamond"
    existing["tooltip.dndweapons.smithing.diamond.upgrade"]       = "Diamond Weapon Upgrade"
    existing["tooltip.dndweapons.smithing.diamond.upgrade.base"]  = "DnD Weapon"
    existing["tooltip.dndweapons.smithing.diamond.upgrade.additions"] = "Diamond"
    existing["tooltip.dndweapons.smithing.netherite.applies_to"]  = "DnD Diamond Weapons"
    existing["tooltip.dndweapons.smithing.netherite.ingredients"] = "Netherite Ingot"
    existing["tooltip.dndweapons.smithing.netherite.upgrade"]     = "Netherite Weapon Upgrade"
    existing["tooltip.dndweapons.smithing.netherite.upgrade.base"]  = "Diamond DnD Weapon"
    existing["tooltip.dndweapons.smithing.netherite.upgrade.additions"] = "Netherite Ingot"

    val sorted = existing.toSortedMap()
    val sb = StringBuilder("{\n")
    for ((i, e) in sorted.entries.withIndex()) {
        sb.append("  \"${e.key}\": \"${e.value}\"")
        if (i < sorted.size - 1) sb.append(",")
        sb.append("\n")
    }
    sb.append("}\n")
    langFile.writeText(sb.toString())
}
