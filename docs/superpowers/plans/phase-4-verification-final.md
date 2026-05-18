# Phase 4 Verification - Final

**Date:** 2026-05-17 (cycle 6 update)
**Branch / commit:** main / aa96617 (post-cycle-6 wave-1)
**Verified locally via:** `./gradlew chiseledBuild chiseledTest chiseledRunGametest` (PENDING re-run after cycle-6 fixes)

## Top-line result

| Check | Versions | Status |
|---|---|---|
| chiseledBuild | all 5 | PENDING - source rewritten in cycle 6; awaiting user-run build verification |
| chiseledTest  | all 5 | PENDING - blocked on chiseledBuild |
| chiseledRunGametest | all 5 | PENDING - blocked on chiseledBuild |

**Summary:** Cycle 5 surfaced 18 bugs, including 8 P0 compilation failures across the
5 MC versions. Cycle 6 rewrote the three failing Phase 4 Kotlin files with full
stonecutter version gates, canonicalized the 56 smithing recipe JSONs to the 1.21.4+
codec form, generated 58 per-version 1.21.1 overlay recipes, refactored the smithing
gametest to simulate the in-world craft, and added the missing `fireImmune` assertion.

The cycle-6 verifier flagged 3 residual concerns (1 P1, 2 P2) that are NOT compilation
blockers but represent intentional graceful-degradation decisions or 1.20.1-only gaps.
See `docs/plan-runner/2026-05-17/cycle-6/bugs/wave-1.json` for details.

The user must now run `./gradlew chiseledBuild` locally to confirm GREEN status on all
5 versions before the git tag is applied.

---

## Cycle-6 changes to Phase 4 files

### `src/main/kotlin/com/dndweapons/item/SmithingTemplateItems.kt`

- Stonecutter-gated the `SmithingTemplateItem` constructor call by version:
  - **1.20.1, 1.21.1:** 7 args (5 Text + 2 List<ResourceLocation>), no `Item.Properties`
  - **>=1.21.2:** 7 args (4 Text + 2 List + Item.Properties); Mojang dropped the
    `upgradeDescription` Text param at 1.21.2 (only `.base` and `.additions` subkeys
    are passed)
- Gated `Item.Properties().setId(key)` initialization to `>=1.21.2` only; the variable
  is omitted entirely on older versions.

### `src/main/kotlin/com/dndweapons/loot/SmithingTemplateLootInjector.kt`

- Stonecutter-gated the `LootTableEvents` import by Fabric module version:
  - **1.20.1 - 1.21.11:** `net.fabricmc.fabric.api.loot.v2.LootTableEvents`
  - **>=26.1.2:** `net.fabricmc.fabric.api.loot.v3.LootTableEvents` (the v2 module was
    dropped from the fabric-api umbrella at 26.1; the v3 module is the replacement)
- Stonecutter-gated the `Modify` SAM arity per version:
  - **1.20.1:** 5-param form `(ResourceManager, LootManager, ResourceLocation,
    LootTable.Builder, LootTableSource)`
  - **1.21.1, 1.21.4:** 3-param form `(ResourceKey<LootTable>, LootTable.Builder,
    LootTableSource)`
  - **1.21.11:** same 3-param shape but `key.identifier()` replaces `key.location()`
    (Mojang renamed the accessor at 1.21.11)
  - **26.1.2:** 4-param form `(ResourceKey<LootTable>, LootTable.Builder,
    LootTableSource, HolderLookup.Provider)` -- v3 added a registries arg
- Switched loot-pool insertion from the non-existent `pool(LootPool)` method to the
  actual API `withPool(LootPool.Builder)`. This bug was present on all 5 versions
  (the v1-style `pool(...)` method does not exist on any of our targets).

### `src/main/kotlin/com/dndweapons/trade/SmithingTemplateTrades.kt`

- Stonecutter-gated the `TradeOfferHelper` import to `<26.1.2`; the Fabric trade API
  was removed from the fabric-api umbrella entirely at 26.1.
- Stonecutter-gated `registerWanderingTraderOffers` per version:
  - **1.20.1:** `(int level, Consumer<List<ItemListing>>)`; uses 3 `ItemStack` args
    in `MerchantOffer` (no `ItemCost`); `VillagerTrades.ItemListing` (NOT
    `TradeListing` -- that type does not exist on 1.20.1)
  - **1.21.1, 1.21.4:** `(int level, Consumer<List<ItemListing>>)`; uses `ItemCost`
    wrappers; `MerchantOffer(ItemCost, Optional<ItemCost>, ItemStack, ...)`
  - **1.21.11:** new shape `(Consumer<WanderingTraderOffersBuilder>)`;
    `WanderingTraderOffersBuilder.pool(...)` with `SELL_SPECIAL_ITEMS_POOL`;
    `VillagerTrades` moved to `net.minecraft.world.entity.npc.villager.VillagerTrades`
    and `ItemListing` SAM now takes `(ServerLevel, Entity, RandomSource)`
  - **26.1.2:** trade registration is gated out (logs a graceful-degradation notice).
    Templates remain obtainable via crafting and stronghold/bastion loot on this
    version.

### Recipe JSONs under `src/main/resources/data/dndweapons/recipe/`

- All 56 smithing_transform recipes (54 codegen + 2 hand-authored assemble) now use
  the 1.21.4+ canonical bare-string form:
  - `"template": "dndweapons:diamond_weapon_upgrade_template"`
  - `"base":     "dndweapons:longsword"`
  - `"addition": "#c:gems/diamond"`
  - `"result":   { "id": "dndweapons:longsword_diamond" }`
- The 2 fragment crafting recipes now use bare-string item refs and `#c:...` tag refs.
- Per-version overlay generated at
  `versions/1.21.1/src/main/resources/data/dndweapons/recipe/` (58 files): 1.21.1's
  Ingredient codec requires the older `{ "item": ... }` / `{ "tag": ... }` object form
  and does NOT accept the bare-string `#tag` form. The overlay wins via
  `processResources duplicatesStrategy=EXCLUDE` (first-wins).
- **1.20.1 GAP:** no smithing recipe overlay was generated for 1.20.1 (which uses
  `recipes/` plural directory AND `{ "item": ... }` result form). Templates will be
  registered but cannot be crafted on the smithing table on 1.20.1. Crafting the
  fragments still works on 1.20.1 because Phase 1's existing 1.20.1 overlay covers
  that path. See cycle-6 wave-1-agent-4-bug-2 for the remediation suggestion.

### `src/main/kotlin/com/dndweapons/test/SmithingGametest.kt`

- `runDiamondPreservesSpec` now performs the simulated in-world smithing craft:
  looks up the 3 input items (longsword, diamond_weapon_upgrade_template, diamond),
  constructs the input ItemStacks, produces the output ItemStack, and asserts:
  - Output item id = `dndweapons:longsword_diamond`
  - `attackDamage` = `baseSpec.attackDamage + Tier.DIAMOND.damageBonus`
  - `baseDurability` = `Tier.DIAMOND.durability` (1561)
  - VERSATILE property preserved from base
- AssertionError messages now reference `Tier.DIAMOND.damageBonus` (not literal `1`).

### `src/test/kotlin/com/dndweapons/catalog/WeaponSpecAtTierTest.kt`

- Added `assertTrue(Tier.NETHERITE.fireImmune)` to
  `atTierNetheriteSuffixesIdAndAddsTwoDamage`.

---

## Residual concerns from cycle 6 (do NOT block tag)

| Bug ID | Severity | Notes |
|---|---|---|
| wave-1-agent-3-bug-1 | P1 | 26.1.2 trader trades unavailable -- Fabric API removed. Templates still obtainable via crafting/loot. |
| wave-1-agent-4-bug-1 | P1 | Single canonical recipe form impossible across 1.21.1 vs 1.21.4+ codecs. Mitigated with per-version overlay. |
| wave-1-agent-4-bug-2 | P2 | No 1.20.1 smithing recipe overlay; 1.20.1 templates registered but not craftable. |

---

## User action required

Run on a clean working tree:

```
./gradlew chiseledBuild
```

If GREEN on all 5 versions, then:

```
./gradlew chiseledTest chiseledRunGametest
```

If all GREEN, apply the tag:

```
git tag phase-4-smithing-ladder
git push origin phase-4-smithing-ladder
```

**Status: HOLD - tag not applied. Awaiting user-run verification of `./gradlew
chiseledBuild` GREEN on all 5 versions.**
