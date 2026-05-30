@file:JvmName("Phase4LegacyCodegen")
package com.dndweapons.codegen

import com.dndweapons.catalog.Tier
import com.dndweapons.catalog.Weapons
import java.io.File

/**
 * P0-002 codegen: emits the 62 1.20.1-format recipe JSONs missing from
 * `versions/1.20.1/src/main/resources/data/dndweapons/recipes/`.
 *
 * 1.20.1 reads `recipes/` (plural) and uses an older smithing-transform schema:
 *   - `template_item` instead of `template`
 *   - `base_item`     instead of `base`
 *   - `addition`      as `{ "tag": "..." }` (no hash-prefix shorthand)
 *   - `result.item`   instead of `result.id`
 *
 * Component crafting recipes (shaped/shapeless) use the 1.20.1 result shape
 * `{ "item": "...", "count": N }` and bare item keys for ingredients are accepted.
 *
 * The 62 files emitted:
 *   - 54 smithing_transform (27 weapons x 2 tier upgrades)
 *   -  4 component crafting (diamond_template_fragment, weapon_smithing_binding,
 *                            netherite_template_fragment, infernal_binding)
 *   -  2 component shapeless (diamond_template_core, netherite_template_core)
 *   -  2 template-assembly smithing_transform (diamond/netherite_weapon_upgrade_template)
 *
 * Run once via `./gradlew :1.20.1:runMain` style invocation or directly from an IDE.
 * Output files are committed to the repo.
 */
fun main() {
    val root = File("versions/1.20.1/src/main/resources/data/dndweapons/recipes")
    require(root.parentFile.parentFile.parentFile.parentFile.parentFile.parentFile.exists()) {
        "Run from project root (versions/1.20.1/src/main/resources must exist)"
    }
    root.mkdirs()

    var written = 0
    written += emitSmithingTransforms(root)
    written += emitComponentRecipes(root)
    written += emitTemplateAssembly(root)
    println("Phase4LegacyCodegen: wrote $written 1.20.1-format recipe JSONs to ${root.path}")
}

private fun emitSmithingTransforms(root: File): Int {
    var count = 0
    for ((spec, tier) in Weapons.ALL_TIERED) {
        if (tier == Tier.IRON) continue
        val baseId = if (tier == Tier.DIAMOND) {
            spec.id.removeSuffix("_diamond")
        } else {
            spec.id.removeSuffix("_netherite") + "_diamond"
        }
        val (templateId, additionTag) = when (tier) {
            Tier.DIAMOND -> "dndweapons:diamond_weapon_upgrade_template" to "c:gems/diamond"
            Tier.NETHERITE -> "dndweapons:netherite_weapon_upgrade_template" to "c:ingots/netherite"
            Tier.IRON -> error("unreachable")
        }
        val json = """{
  "type": "minecraft:smithing_transform",
  "template_item": "$templateId",
  "base_item":     "dndweapons:$baseId",
  "addition":      { "tag": "$additionTag" },
  "result":        { "item": "dndweapons:${spec.id}" }
}
"""
        File(root, "${spec.id}.json").writeText(json)
        count++
    }
    return count
}

private fun emitComponentRecipes(root: File): Int {
    var count = 0

    // diamond_template_fragment - shaped (4 paper, 4 diamond, 1 flint)
    File(root, "diamond_template_fragment.json").writeText(
        """{
  "type": "minecraft:crafting_shaped",
  "pattern": ["PDP", "FDF", "PDP"],
  "key": {
    "P": { "item": "minecraft:paper" },
    "D": { "tag": "c:gems/diamond" },
    "F": { "item": "minecraft:flint" }
  },
  "result": { "item": "dndweapons:diamond_template_fragment", "count": 4 }
}
"""
    )
    count++

    // weapon_smithing_binding - shaped
    File(root, "weapon_smithing_binding.json").writeText(
        """{
  "type": "minecraft:crafting_shaped",
  "pattern": ["SLS", "LBL", "SLS"],
  "key": {
    "S": { "item": "minecraft:string" },
    "L": { "item": "minecraft:leather" },
    "B": { "item": "minecraft:blaze_rod" }
  },
  "result": { "item": "dndweapons:weapon_smithing_binding", "count": 1 }
}
"""
    )
    count++

    // diamond_template_core - shapeless (4 fragments + 1 binding)
    File(root, "diamond_template_core.json").writeText(
        """{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    { "item": "dndweapons:diamond_template_fragment" },
    { "item": "dndweapons:diamond_template_fragment" },
    { "item": "dndweapons:diamond_template_fragment" },
    { "item": "dndweapons:diamond_template_fragment" },
    { "item": "dndweapons:weapon_smithing_binding" }
  ],
  "result": { "item": "dndweapons:diamond_template_core", "count": 1 }
}
"""
    )
    count++

    // netherite_template_fragment - shaped (obsidian/quartz/ghast_tear/scrap)
    File(root, "netherite_template_fragment.json").writeText(
        """{
  "type": "minecraft:crafting_shaped",
  "pattern": ["OSO", "GNG", "OSO"],
  "key": {
    "O": { "item": "minecraft:obsidian" },
    "S": { "tag": "c:gems/quartz" },
    "G": { "item": "minecraft:ghast_tear" },
    "N": { "item": "minecraft:netherite_scrap" }
  },
  "result": { "item": "dndweapons:netherite_template_fragment", "count": 4 }
}
"""
    )
    count++

    // infernal_binding - shaped
    File(root, "infernal_binding.json").writeText(
        """{
  "type": "minecraft:crafting_shaped",
  "pattern": ["BNB", "SWS", "BNB"],
  "key": {
    "B": { "item": "minecraft:blaze_powder" },
    "N": { "item": "minecraft:nether_star" },
    "S": { "item": "minecraft:soul_sand" },
    "W": { "item": "minecraft:wither_rose" }
  },
  "result": { "item": "dndweapons:infernal_binding", "count": 1 }
}
"""
    )
    count++

    // netherite_template_core - shapeless (4 netherite fragments + 1 infernal binding)
    File(root, "netherite_template_core.json").writeText(
        """{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    { "item": "dndweapons:netherite_template_fragment" },
    { "item": "dndweapons:netherite_template_fragment" },
    { "item": "dndweapons:netherite_template_fragment" },
    { "item": "dndweapons:netherite_template_fragment" },
    { "item": "dndweapons:infernal_binding" }
  ],
  "result": { "item": "dndweapons:netherite_template_core", "count": 1 }
}
"""
    )
    count++

    return count
}

private fun emitTemplateAssembly(root: File): Int {
    var count = 0

    File(root, "diamond_weapon_upgrade_template_assemble.json").writeText(
        """{
  "type": "minecraft:smithing_transform",
  "template_item": "minecraft:netherite_upgrade_smithing_template",
  "base_item":     "dndweapons:diamond_template_core",
  "addition":      { "tag": "c:gems/diamond" },
  "result":        { "item": "dndweapons:diamond_weapon_upgrade_template" }
}
"""
    )
    count++

    File(root, "netherite_weapon_upgrade_template_assemble.json").writeText(
        """{
  "type": "minecraft:smithing_transform",
  "template_item": "minecraft:netherite_upgrade_smithing_template",
  "base_item":     "dndweapons:netherite_template_core",
  "addition":      { "tag": "c:ingots/netherite" },
  "result":        { "item": "dndweapons:netherite_weapon_upgrade_template" }
}
"""
    )
    count++

    return count
}
