# plan-runner Bug Report
**Date:** 2026-05-18
**Cycle:** 2
**Source plan:** docs/plan-runner/2026-05-18/cycle-1/fix-plan.md

## Summary
- P0: 4
- P1: 2
- P2: 0
- P3: 0
- Total: 6

## P0 Bugs

### [wave-1-agent-1-bug-1] chiseledBuild still fails: pre-existing Kotlin compile errors in files outside Wave 1 scope
**Wave/Agent:** wave-1 agent-1 (task: Fix WeaponLootRegistrar.kt MOD_VERSION_STRING Stonecutter 0.6 syntax)
**Category:** missing_requirement
**File:** multiple
**Expected:** chiseledBuild BUILD SUCCESSFUL on all 5 MC versions: 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2
**Evidence:**
```
After fixing MOD_VERSION_STRING Stonecutter syntax, chiseledBuild advances past Stonecutter parsing on all 5 MC versions but :{version}:compileKotlin FAILED on every version. Unresolved references include: ServerLivingEntityEvents, VillagerTrades, VillagerProfession, descriptionId, lootData, getRandomItems, spawnAtLocation, ResourceLocation, GameTest, parse, nextInt, type, add, get, getOffer, not.
Affected files: src/main/kotlin/com/dndweapons/loot/WitherTrophyHandler.kt, src/main/kotlin/com/dndweapons/test/AcquisitionGametest.kt, src/main/kotlin/com/dndweapons/trade/WeaponTradeRegistrar.kt.
```
**Suggested fix:** Pre-existing Phase 5 breakage outside the cycle-1 fix-plan's scope. A cycle 3 fix-plan should add tasks to repair each broken file with the correct per-version Stonecutter-guarded imports/API usage.

### [wave-2-agent-1-bug-1] WitherTrophyHandler.kt fails compileKotlin on all 5 versions (ServerLivingEntityEvents unresolved)
**Wave/Agent:** wave-2 agent-1 (task: Re-run verification matrix and update phase-5-verification-final.md)
**Category:** missing_requirement
**File:** src/main/kotlin/com/dndweapons/loot/WitherTrophyHandler.kt:6
**Expected:** WitherTrophyHandler.kt compiles on all 5 MC versions with correct per-version stonecutter-guarded imports for Fabric ServerLivingEntityEvents and matching SAM signatures.
**Evidence:**
```
Unresolved reference 'ServerLivingEntityEvents' (Fabric API import missing/wrong), plus lambda type-inference cascade failures on entity.type, entity.spawnAtLocation, entity.random.nextInt.
```
**Suggested fix:** Add stonecutter-guarded import block for ServerLivingEntityEvents (Fabric API module path may differ per version) and verify the SAM signature matches the version's API.

### [wave-2-agent-1-bug-2] AcquisitionGametest.kt fails compileKotlin on all 5 versions (multiple vanilla-API drifts)
**Wave/Agent:** wave-2 agent-1 (task: Re-run verification matrix and update phase-5-verification-final.md)
**Category:** missing_requirement
**File:** src/main/kotlin/com/dndweapons/test/AcquisitionGametest.kt:60
**Expected:** AcquisitionGametest.kt compiles on all 5 MC versions with correct per-version accessors.
**Evidence:**
```
Unresolved references across all 5 versions: GameTest annotation, MinecraftServer.lootData accessor (1.21.4+), LootTable.getRandomItems (renamed across versions), Item.descriptionId (accessor changed), ResourceLocation.parse (added in newer versions).
```
**Suggested fix:** Stonecutter-guard each affected accessor: lootData vs getServer().reloadableRegistries(), getRandomItems vs getRandomLootItems, descriptionId vs getDescriptionId(), ResourceLocation.parse vs ResourceLocation.tryParse vs new ResourceLocation(String).

### [wave-2-agent-1-bug-3] WeaponTradeRegistrar.kt fails compileKotlin on all 5 versions (villager trade API drift)
**Wave/Agent:** wave-2 agent-1 (task: Re-run verification matrix and update phase-5-verification-final.md)
**Category:** missing_requirement
**File:** src/main/kotlin/com/dndweapons/trade/WeaponTradeRegistrar.kt:67
**Expected:** WeaponTradeRegistrar.kt compiles on all 5 MC versions with correct per-version stonecutter-guarded access to VillagerTrades.TRADES and the add/append shape.
**Evidence:**
```
Lines around 67-69: VillagerTrades.TRADES.get(profession).add(level, factories) -- 'add' unresolved, 'profession' argument type mismatch (Any vs VillagerProfession!), factories parameter cannot be inferred. The vanilla VillagerTrades.TRADES type signature drifts across versions (raw map, registry-holder map, Int2ObjectMap of List<ItemListing>, etc.).
```
**Suggested fix:** Either (a) per-version stonecutter forks of the registration block, or (b) use TradeOfferHelper / Fabric API trade-registration where available to abstract over the vanilla map drift.

## P1 Bugs

### [wave-1-agent-1-bug-2] Fix-plan prescribed compound Stonecutter condition syntax that is not supported by this project's Stonecutter 0.6 configuration
**Wave/Agent:** wave-1 agent-1 (task: Fix WeaponLootRegistrar.kt MOD_VERSION_STRING Stonecutter 0.6 syntax)
**Category:** incorrect_implementation
**File:** docs/plan-runner/2026-05-18/cycle-1/fix-plan.md:35
**Expected:** Use single-clause else-if branches that exploit the ordering of the ladder (e.g., `else if >=1.21.4` implicitly means `>=1.21.4 AND <1.21.11` because the prior branch caught >=1.21.11).
**Evidence:**
```
The fix-plan's example `//?} else if (>=1.21.4) & (<1.21.11) {` produced Stonecutter errors: `Unknown token: & (<1.21.11) {` and `CONDITION closes unmatched scope of null`. No existing source file in src/ uses the `&` combinator; all version guards use single-clause conditions.
```
**Suggested fix:** Already applied in wave 1: MOD_VERSION_STRING now uses `if >=1.21.11 / else if >=1.21.4 / else if >=1.21.1 / else` ladder.

### [wave-2-agent-1-bug-4] phase-5-verification-final.md previously attributed all failures to Stonecutter parse error; the actual scope is broader
**Wave/Agent:** wave-2 agent-1 (task: Re-run verification matrix and update phase-5-verification-final.md)
**Category:** stale_documentation
**File:** docs/superpowers/plans/phase-5-verification-final.md
**Expected:** Verification doc accurately reflects all blockers, not just the cycle 1 surface error.
**Evidence:**
```
Cycle 1's verification doc documented one root cause (Stonecutter parse) and three Known Issues. Cycle 2 reveals at least three additional pre-existing compile blockers across three separate files. The Phase 5 verification status remains HOLD with a broader Known Issues set; the doc has been rewritten in this wave to reflect that.
```
**Suggested fix:** Already applied in wave 2 -- the verification doc now lists 5 Known Issues including the three compile-failure blockers and notes that no prior-phase regressions are confirmed.
