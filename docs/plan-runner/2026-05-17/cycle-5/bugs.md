# plan-runner Bug Report
**Date:** 2026-05-17
**Cycle:** 5
**Source plan:** docs/superpowers/plans/2026-05-17-dnd-weapons-phase-4-smithing-ladder.md

## Summary
- P0: 8
- P1: 7
- P2: 2
- P3: 1
- Total: 18

> Note: wave-6-agent-2-bug-1 is a duplicate of wave-8-agent-1-bug-2 (same file, same criterion:
> 4-param LootTableEvents.MODIFY lambda without stonecutter gate). The wave-8 report's P0 severity
> is retained and the wave-6 P1 report is dropped. Final unique bug count: 18.

---

## P0 Bugs

### [wave-8-agent-1-bug-1] chiseledBuild FAILS - SmithingTemplateItems passes wrong constructor arity
**Wave/Agent:** wave-8 agent-1 (task: Full matrix verification + Phase 4 verification doc)
**Category:** missing_requirement
**File:** src/main/kotlin/com/dndweapons/item/SmithingTemplateItems.kt:94
**Expected:** The SmithingTemplateItem constructor arity differs by MC version: 1.20.1 takes 7 args (no Item.Properties); 1.21.1 has a different signature; 1.21.2+ accept Item.Properties via setId. The constructor call must be stonecutter-gated per version.
**Evidence:**
```
SmithingTemplateItem(...8 args including 'props' unconditionally on all versions)
```
**Suggested fix:** Wrap the entire SmithingTemplateItem(...) instantiation in stonecutter version guards. For versions <1.21.2 omit the `props` argument (7-arg form). For 1.21.1 check exact constructor signature. For >=1.21.2 keep 8-arg form.

---

### [wave-8-agent-1-bug-2] chiseledBuild FAILS - SmithingTemplateLootInjector uses ungated 4-param LootTableEvents.MODIFY lambda
**Wave/Agent:** wave-8 agent-1 (task: Full matrix verification + Phase 4 verification doc) [merged from wave-6-agent-2-bug-1]
**Category:** missing_requirement
**File:** src/main/kotlin/com/dndweapons/loot/SmithingTemplateLootInjector.kt:35
**Expected:** Fabric loot API MODIFY event has different lambda arities across versions; 1.20.1/1.21.1 require 3-param form. Lambda signature must be stonecutter-gated.
**Evidence:**
```
LootTableEvents.MODIFY.register { key, builder, source, _ -> ... }
```
**Suggested fix:** Add a stonecutter guard. For versions where MODIFY supplies 3 params use `{ key, builder, source -> ...}`. For versions where it supplies 4 params keep `{ key, builder, source, _ -> }`.

---

### [wave-8-agent-1-bug-3] chiseledBuild FAILS on 26.1.2 - SmithingTemplateLootInjector imports ungated loot v2 API
**Wave/Agent:** wave-8 agent-1 (task: Full matrix verification + Phase 4 verification doc)
**Category:** missing_requirement
**File:** src/main/kotlin/com/dndweapons/loot/SmithingTemplateLootInjector.kt:6
**Expected:** fabric-loot-api module name changed on 26.1.2. Import must be stonecutter-gated.
**Evidence:**
```
import net.fabricmc.fabric.api.loot.v2.LootTableEvents
```
**Suggested fix:** Add a stonecutter guard: for >=26.1.2 import the new qualified name; for older versions keep the v2 import. Guard both import and API call sites.

---

### [wave-8-agent-1-bug-4] chiseledBuild FAILS on 1.21.11 and 26.1.2 - SmithingTemplateTrades passes level Int to registerWanderingTraderOffers
**Wave/Agent:** wave-8 agent-1 (task: Full matrix verification + Phase 4 verification doc)
**Category:** missing_requirement
**File:** src/main/kotlin/com/dndweapons/trade/SmithingTemplateTrades.kt:23
**Expected:** registerWanderingTraderOffers signature changed in 1.21.11 (no longer accepts Int level). Current stonecutter gate `>=1.21.1` groups 1.21.11 incorrectly.
**Evidence:**
```
TradeOfferHelper.registerWanderingTraderOffers(2) { factories -> ... }
```
**Suggested fix:** Split the gate into `>=1.21.1 <1.21.11` (with Int level) and `>=1.21.11` (without level).

---

### [wave-8-agent-1-bug-5] chiseledBuild FAILS on 26.1.2 - SmithingTemplateTrades imports ungated TradeOfferHelper
**Wave/Agent:** wave-8 agent-1 (task: Full matrix verification + Phase 4 verification doc)
**Category:** missing_requirement
**File:** src/main/kotlin/com/dndweapons/trade/SmithingTemplateTrades.kt:6
**Expected:** Fabric trade API moved on 26.1.2. Import unresolved.
**Evidence:**
```
import net.fabricmc.fabric.api.`object`.builder.v1.trade.TradeOfferHelper
```
**Suggested fix:** Add stonecutter guard around the import. For >=26.1.2 use the new import path.

---

### [wave-8-agent-1-bug-6] chiseledBuild FAILS on 1.20.1 - SmithingTemplateLootInjector references unresolved 'pool' builder method
**Wave/Agent:** wave-8 agent-1 (task: Full matrix verification + Phase 4 verification doc)
**Category:** missing_requirement
**File:** src/main/kotlin/com/dndweapons/loot/SmithingTemplateLootInjector.kt:43
**Expected:** Loot builder API differs on 1.20.1; `builder.pool(...)` is unresolved.
**Evidence:**
```
builder.pool(LootPool.lootPool()...build())
```
**Suggested fix:** Stonecutter-gate the loot pool injection block per version. Use the correct 1.20.1 API for that branch.

---

### [wave-8-agent-1-bug-7] chiseledBuild FAILS on 1.20.1 - SmithingTemplateTrades uses TradeListing type which is unresolved on 1.20.1
**Wave/Agent:** wave-8 agent-1 (task: Full matrix verification + Phase 4 verification doc)
**Category:** missing_requirement
**File:** src/main/kotlin/com/dndweapons/trade/SmithingTemplateTrades.kt:46
**Expected:** On 1.20.1 the correct type is VillagerTrades.ItemListing (not TradeListing).
**Evidence:**
```
VillagerTrades.TradeListing { _, _ -> ... }
```
**Suggested fix:** Update the 1.20.1 stonecutter block to use VillagerTrades.ItemListing { entity, random -> ... } with the correct MerchantOffer constructor.

---

### [wave-8-agent-1-bug-8] chiseledBuild FAILS on 1.21.11 - SmithingTemplateLootInjector has unresolved reference 'location' on 1.21.11
**Wave/Agent:** wave-8 agent-1 (task: Full matrix verification + Phase 4 verification doc)
**Category:** missing_requirement
**File:** src/main/kotlin/com/dndweapons/loot/SmithingTemplateLootInjector.kt:37
**Expected:** On 1.21.11, key.location() is renamed or the key type changed.
**Evidence:**
```
val tableId = key.location().toString()
```
**Suggested fix:** Split >=1.21.1 guard into >=1.21.1 <1.21.11 (using .location()) and >=1.21.11 (using the new method).

---

## P1 Bugs

### [wave-1-agent-4-bug-1] Tag ingredients use object format instead of #-prefix string format in diamond_template_fragment.json
**Wave/Agent:** wave-1 agent-4 (task: Hand-author component + core crafting recipes)
**Category:** incorrect_implementation
**File:** src/main/resources/data/dndweapons/recipe/diamond_template_fragment.json:7
**Expected:** Existing recipes in this codebase use the string-prefix syntax, e.g. `"D": "#c:gems/diamond"`. The object form `{ "tag": "..." }` is valid in modern MC data-pack format but the other recipes in this project use the #-prefix string form, and at least the oldest supported MC versions (pre-1.21) may not accept the object tag syntax in crafting_shaped key entries.
**Evidence:**
```
"D": { "tag": "c:gems/diamond" }
```
**Suggested fix:** Replace all `{ "tag": "c:..." }` objects in agent-4's four recipe files (diamond_template_fragment.json key D, netherite_template_fragment.json key S) with the string form `"#c:..."`, matching the convention used by all other recipes in data/dndweapons/recipe/.

---

### [wave-1-agent-4-bug-2] Tag ingredient object format in netherite_template_fragment.json key S
**Wave/Agent:** wave-1 agent-4 (task: Hand-author component + core crafting recipes)
**Category:** incorrect_implementation
**File:** src/main/resources/data/dndweapons/recipe/netherite_template_fragment.json:7
**Expected:** String tag format `"#c:gems/quartz"` consistent with every other recipe in the codebase that uses tags (e.g. `"#c:ingots/iron"`, `"#c:rods/wooden"`).
**Evidence:**
```
"S": { "tag": "c:gems/quartz" }
```
**Suggested fix:** Change to `"S": "#c:gems/quartz"`

---

### [wave-1-agent-5-bug-1] smithing_transform recipe addition uses object tag form instead of #-prefix string form in diamond_weapon_upgrade_template_assemble.json
**Wave/Agent:** wave-1 agent-5 (task: Hand-author template-assembly smithing recipes)
**Category:** incorrect_implementation
**File:** src/main/resources/data/dndweapons/recipe/diamond_weapon_upgrade_template_assemble.json:3
**Expected:** addition field should use `"#c:gems/diamond"` string form consistent with codebase convention.
**Evidence:**
```
"addition": { "tag": "c:gems/diamond" }
```
**Suggested fix:** Change `"addition": { "tag": "c:gems/diamond" }` to `"addition": "#c:gems/diamond"`

---

### [wave-1-agent-5-bug-2] netherite smithing_transform addition uses object tag form in netherite_weapon_upgrade_template_assemble.json
**Wave/Agent:** wave-1 agent-5 (task: Hand-author template-assembly smithing recipes)
**Category:** incorrect_implementation
**File:** src/main/resources/data/dndweapons/recipe/netherite_weapon_upgrade_template_assemble.json:5
**Expected:** `"addition": "#c:ingots/netherite"` - matching the #-prefix string form used by all other tag references in this project's recipe files.
**Evidence:**
```
"addition": { "tag": "c:ingots/netherite" }
```
**Suggested fix:** Change to `"addition": "#c:ingots/netherite"`

---

### [wave-4-agent-2-bug-1] Generated smithing_transform recipes use {item:...} while hand-authored wave-1 recipes use {id:...} - cross-recipe format inconsistency
**Wave/Agent:** wave-4 agent-2 (task: Phase4Codegen - emit recipes, models, lang)
**Category:** incorrect_implementation
**File:** src/main/resources/data/dndweapons/recipe/longsword_diamond.json:3
**Expected:** Format consistency across all smithing_transform recipes in this mod. MC 1.20.1 uses `{item:...}`, MC 1.20.5+ switched to `{id:...}`. Since recipe JSONs are not stonecutter-forked, per-version recipe handling is needed or a single canonical format must be established.
**Evidence:**
```
"template": { "item": "dndweapons:diamond_weapon_upgrade_template" },
  "base":     { "item": "dndweapons:longsword" },
```
**Suggested fix:** Either (a) align all 56 smithing_transform recipes (54 generated + 2 hand-authored) to a single format supported across all 5 MC versions, OR (b) document which MC versions accept which format and ensure compatibility shims exist.

---

### [wave-4-agent-2-bug-2] Generated smithing_transform addition uses "#c:..." string while hand-authored recipes use {"tag":"c:..."} object - conflicting canonical format
**Wave/Agent:** wave-4 agent-2 (task: Phase4Codegen - emit recipes, models, lang)
**Category:** incorrect_implementation
**File:** src/main/resources/data/dndweapons/recipe/longsword_diamond.json:5
**Expected:** A single canonical tag format across all recipe files. Per the wave-1 verifier, the `"#c:..."` string form matches the bulk of pre-existing recipes and should be the standard.
**Evidence:**
```
"addition": "#c:gems/diamond"
```
**Suggested fix:** Pick the `"#c:..."` string form as canonical (per wave-1 verifier observation) and ensure all hand-authored recipes (wave-1 agent-4 and agent-5 files) are also migrated to that format. This bug and wave-1 bugs 1-4 are now a coherent single migration task.

---

### [wave-5-agent-2-bug-1] smithingDiamondUpgradePreservesSpec only checks SpecRegistry, never performs smithing-table craft
**Wave/Agent:** wave-5 agent-2 (task: SmithingGametest - smithingDiamondUpgradePreservesSpec)
**Category:** missing_requirement
**File:** src/main/kotlin/com/dndweapons/test/SmithingGametest.kt:43
**Expected:** Test name 'smithingDiamondUpgradePreservesSpec' implies verifying that an in-world smithing-table upgrade produces a longsword_diamond with correct stats. Current test only verifies registration.
**Evidence:**
```
private fun runDiamondPreservesSpec(ctx: GameTestHelper) { val item = BuiltInRegistries.ITEM.get(itemId)...; val spec = SpecRegistry.lookup(item) ?: throw ...; if (spec.id != "longsword_diamond") ... }
```
**Suggested fix:** Expand to perform an in-world smithing craft: simulate the SmithingTransformRecipe with longsword + diamond_weapon_upgrade_template + diamond, obtain output ItemStack, assert id + SpecRegistry stats.

---

### [wave-8-agent-1-bug-9] chiseledTest and chiseledRunGametest acceptance criteria unmet - blocked by compilation failures
**Wave/Agent:** wave-8 agent-1 (task: Full matrix verification + Phase 4 verification doc)
**Category:** missing_requirement
**File:** src/main/kotlin/com/dndweapons/item/SmithingTemplateItems.kt
**Expected:** Both commands should pass with 13 gametests per version.
**Evidence:**
```
Verification doc: chiseledTest RED, chiseledRunGametest NOT RUN on all 5 versions
```
**Suggested fix:** Fix all P0 compilation bugs first (bugs 1-8), then re-run chiseledTest and chiseledRunGametest.

---

## P2 Bugs

### [wave-2-agent-1-bug-1] Netherite test does not assert fire flag as required by acceptance criterion
**Wave/Agent:** wave-2 agent-1 (task: WeaponSpec.atTier() extension + tests)
**Category:** missing_requirement
**File:** src/test/kotlin/com/dndweapons/catalog/WeaponSpecAtTierTest.kt:29
**Expected:** Acceptance criterion states the netherite test covers 'netherite suffix + damage + fire flag'. The test must assert the fire flag, e.g. `assertTrue(Tier.NETHERITE.fireImmune)`.
**Evidence:**
```
@Test
fun atTierNetheriteSuffixesIdAndAddsTwoDamage() {
    val netherite = Weapons.GREATAXE.atTier(Tier.NETHERITE)
    assertEquals("greataxe_netherite", netherite.id)
    assertEquals("Netherite Greataxe", netherite.displayName)
    assertEquals(Weapons.GREATAXE.attackDamage + 2, netherite.attackDamage)
    assertEquals(2031, netherite.baseDurability)
}
```
**Suggested fix:** Add `assertTrue(Tier.NETHERITE.fireImmune)` inside the atTierNetheriteSuffixesIdAndAddsTwoDamage test.

---

### [wave-8-agent-1-bug-10] Git tag not applied - HOLD until build is green
**Wave/Agent:** wave-8 agent-1 (task: Full matrix verification + Phase 4 verification doc)
**Category:** missing_requirement
**File:** docs/superpowers/plans/phase-4-verification-final.md:169
**Expected:** Tag applied after green build.
**Evidence:**
```
Status: HOLD - tag not applied
```
**Suggested fix:** Apply tag after follow-up cycle resolves P0 bugs and chiseledBuild goes green on all 5 versions.

---

## P3 Bugs

### [wave-5-agent-2-bug-2] AssertionError message hardcodes '+1' instead of using Tier.DIAMOND.damageBonus
**Wave/Agent:** wave-5 agent-2 (task: SmithingGametest - smithingDiamondUpgradePreservesSpec)
**Category:** incorrect_implementation
**File:** src/main/kotlin/com/dndweapons/test/SmithingGametest.kt:65
**Expected:** Use Tier.DIAMOND.damageBonus in the message to stay accurate if enum value ever changes.
**Evidence:**
```
throw AssertionError("Expected attackDamage ${baseSpec.attackDamage + 1}, got ${spec.attackDamage}")
```
**Suggested fix:** Change `${baseSpec.attackDamage + 1}` to `${baseSpec.attackDamage + Tier.DIAMOND.damageBonus}`.
