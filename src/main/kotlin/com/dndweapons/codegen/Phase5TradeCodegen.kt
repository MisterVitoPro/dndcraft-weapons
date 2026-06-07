@file:JvmName("Phase5TradeCodegen")
package com.dndweapons.codegen

import com.dndweapons.acquisition.AcquisitionCatalog
import com.dndweapons.acquisition.VillagerTradeEntry
import com.dndweapons.catalog.Weapons
import java.io.File

/**
 * Emits the 9 villager-trade JSON files Minecraft 26.1.2 loads as a trade
 * rebalance data pack. The same trades that
 * [com.dndweapons.trade.WeaponTradeRegistrar] registers via Fabric API on
 * 1.20.1-1.21.11 are emitted here as JSON for 26.1.2.
 *
 * Run via `./gradlew :26.1.2:runPhase5TradeCodegen` (or directly with `kotlin
 * com.dndweapons.codegen.Phase5TradeCodegen`). The output files are committed
 * to the repo; this main is only re-run when [AcquisitionCatalog.VILLAGER_TRADES]
 * is edited.
 */
fun main() {
    // P2-013: validate every trade weapon id against the canonical Weapons.ALL
    // catalog BEFORE emitting JSON. Catches typos / removed entries before they
    // ship as committed JSON files MC silently drops at load time.
    val known: Set<String> = Weapons.ALL.map { it.id }.toSet()
    val unknown = mutableListOf<String>()
    for ((professionId, levels) in AcquisitionCatalog.VILLAGER_TRADES) {
        for ((level, trades) in levels) {
            for (t in trades) {
                if (t.weapon !in known) unknown += "$professionId/$level: ${t.weapon}"
            }
        }
    }
    require(unknown.isEmpty()) {
        "Phase5TradeCodegen: unknown weapon id(s) in AcquisitionCatalog.VILLAGER_TRADES: $unknown"
    }

    val outputRoot = File("src/main/resources/data/dndweapons/villager_trades")
    outputRoot.mkdirs()
    var written = 0
    for ((professionId, levels) in AcquisitionCatalog.VILLAGER_TRADES) {
        val professionSlug = professionId.removePrefix("minecraft:")
        for ((level, trades) in levels) {
            val fileName = "${professionSlug}_level${level}.json"
            File(outputRoot, fileName).writeText(buildJson(professionId, level, trades))
            written++
        }
    }
    println("Phase5TradeCodegen: wrote $written trade JSONs to ${outputRoot.path} (validated against ${known.size} known weapon ids)")
}

private fun buildJson(professionId: String, level: Int, trades: List<VillagerTradeEntry>): String {
    val tradeEntries = trades.joinToString(",\n") { tradeJson(it) }
    return """{
  "profession": "$professionId",
  "level": $level,
  "trades": [
$tradeEntries
  ]
}
"""
}

private fun tradeJson(t: VillagerTradeEntry): String = """    {
      "input": [{ "id": "minecraft:emerald", "count": ${t.emeralds} }],
      "result": { "id": "dndweapons:${t.weapon}", "count": ${t.outputCount} },
      "max_uses": ${t.maxUses},
      "xp": ${t.xp},
      "price_multiplier": 0.05
    }"""
