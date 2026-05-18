// src/main/kotlin/com/dndweapons/codegen/wiki/AcquisitionLookup.kt
package com.dndweapons.codegen.wiki

import com.dndweapons.acquisition.AcquisitionCatalog
import com.dndweapons.catalog.Tier

/**
 * Inverse index: for each weapon id (e.g. "longsword"), where can the player
 * acquire it? Built once at WikiGen time from the three AcquisitionCatalog maps.
 *
 * Used by WikiTemplates to render the "Acquisition" section of each weapon page.
 */
class AcquisitionLookup private constructor(
    private val byWeaponId: Map<String, List<AcquisitionFact>>,
) {

    fun factsFor(weaponId: String): List<AcquisitionFact> =
        byWeaponId[weaponId].orEmpty()

    companion object {
        fun build(): AcquisitionLookup {
            val out = linkedMapOf<String, MutableList<AcquisitionFact>>()

            // Structure loot
            for ((tableId, entry) in AcquisitionCatalog.STRUCTURE_LOOT) {
                val tableLabel = friendlyStructure(tableId)
                for (weaponId in entry.weapons) {
                    out.getOrPut(weaponId) { mutableListOf() }
                        .add(
                            AcquisitionFact.StructureChest(
                                tableLabel = tableLabel,
                                chancePct = entry.chancePct,
                                tier = entry.tier,
                                minVersion = entry.minVersion,
                            )
                        )
                }
            }

            // Mob drops
            for ((entityId, drop) in AcquisitionCatalog.MOB_DROPS) {
                val mobLabel = friendlyMob(entityId)
                val ironWeapon = drop.ironWeapon
                if (ironWeapon != null && drop.ironPct > 0) {
                    out.getOrPut(ironWeapon) { mutableListOf() }
                        .add(AcquisitionFact.MobDrop(mobLabel, drop.ironPct, Tier.IRON))
                }
                // Netherite mob drops with a specific weapon are stacked alongside the
                // iron drop; netherite-with-null-weapon (Warden) is a random pick and
                // is documented on every netherite weapon page via NetheriteRandomDrop.
                if (ironWeapon != null && drop.netheritePct > 0) {
                    out.getOrPut(ironWeapon) { mutableListOf() }
                        .add(AcquisitionFact.MobDrop(mobLabel, drop.netheritePct, Tier.NETHERITE))
                }
            }

            // Villager trades
            for ((profession, levels) in AcquisitionCatalog.VILLAGER_TRADES) {
                val profLabel = friendlyProfession(profession)
                for ((level, trades) in levels) {
                    for (trade in trades) {
                        out.getOrPut(trade.weapon) { mutableListOf() }
                            .add(
                                AcquisitionFact.VillagerTrade(
                                    profession = profLabel,
                                    level = level,
                                    emeralds = trade.emeralds,
                                )
                            )
                    }
                }
            }

            return AcquisitionLookup(out)
        }

        private fun friendlyStructure(tableId: String): String = tableId
            .removePrefix("minecraft:chests/")
            .replace('_', ' ')
            .split(' ', '/')
            .joinToString(" ") { it.replaceFirstChar(Char::titlecase) }

        private fun friendlyMob(entityId: String): String = entityId
            .removePrefix("minecraft:entities/")
            .replace('_', ' ')
            .split(' ')
            .joinToString(" ") { it.replaceFirstChar(Char::titlecase) }

        private fun friendlyProfession(profession: String): String = profession
            .removePrefix("minecraft:")
            .replace('_', ' ')
            .split(' ')
            .joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
    }
}

/** Tagged union describing one acquisition source for one weapon. */
sealed interface AcquisitionFact {
    data class StructureChest(
        val tableLabel: String,
        val chancePct: Int,
        val tier: Tier,
        val minVersion: String?,
    ) : AcquisitionFact

    data class MobDrop(
        val mobLabel: String,
        val chancePct: Int,
        val tier: Tier,
    ) : AcquisitionFact

    data class VillagerTrade(
        val profession: String,
        val level: Int,
        val emeralds: Int,
    ) : AcquisitionFact
}
