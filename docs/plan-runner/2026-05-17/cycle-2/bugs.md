# plan-runner Bug Report
**Date:** 2026-05-17
**Cycle:** 2
**Source plan:** docs/plan-runner/2026-05-17/cycle-1/fix-plan.md

## Summary
- P0: 0
- P1: 3
- P2: 2
- P3: 1
- Total: 6

> Duplicates removed: wave-4-agent-1-bug-1 (DndWeaponsMod server crash) was a P0 raised AND resolved within wave 4 itself; the unblocker fix is committed in 8d1d19c. Similarly, wave-1-agent-1-bug-1 (chiseledTest runtime) and wave-3-agent-2-bug-1 (26.1.2 mixin runtime) were both P1 placeholders whose runtime acceptance was satisfied during the wave-4 matrix run (chiseledBuild + chiseledTest green on all 5 versions; chiseledRunGametest reaches all 5 versions, mixin path proved by heavyKnockbackBonusApplies passing on all 5). wave-1-agent-1-bug-2 (test source set redirect) likewise resolved - chiseledTest ran successfully so the source-set materialization works. wave-4-agent-1-bug-4 (sweep guard runtime correctness) is collapsed into wave-2-agent-2-bug-2 (same concern, different wording). wave-5-agent-1-bug-1 is a meta-rollup of wave-4 gametest failures and is collapsed into wave-4-agent-1-bug-2 + wave-4-agent-1-bug-3.

---

## P1 Bugs

### [wave-4-agent-1-bug-2] 6 combat gametests fail: mock-player attack-strength scaling produces fractional damage (~3.5% of expected)
**Wave/Agent:** wave-4 agent-1 (task: Verify PlayerAttackMixin injection fires and execute chiseledRunGametest)
**Category:** broken_existing
**File:** src/main/kotlin/com/dndweapons/test/CombatHooksGametest.kt
**Expected:** Each combat gametest should observe full-damage hits (player.attack(pig) deals ~base*multiplier).
**Evidence:**
```
Finesse sprint: dealt=0.20800018 expected~7.2 (base=6.0)
Versatile empty: dealt=1.2080002 expected~7.0 (base=6.0)
All 6 single-hit combat tests across all 5 MC versions report the same ~3.5% damage ratio:
- finesseSprintBonusFires
- lightDualWieldBonusFires
- versatileEmptyOffhandFires
- lanceOnFootHalves
- lanceMountedFullDamage
- vanillaIronSwordCarriesFinesseHook
```
**Suggested fix:** GameTestHelper.makeMockPlayer() spawns a player at zero attack-strength scale (cooldown not reset). Vanilla Player.attack multiplies damage by (0.2 + cooldown*0.8) on a fresh swing. Before each player.attack(pig) call, the gametest should set player.attackStrengthTicker to a high value (e.g. 100) and/or call resetAttackStrengthTicker() to ensure full-damage swings. Without this, the per-test damage observations are ~20% of base instead of base*spec_multiplier.

---

### [wave-4-agent-1-bug-3] 2 tooltip gametests fail: ItemTooltipCallback is client-side only and does not fire under stack.getTooltipLines() in server gametest context
**Wave/Agent:** wave-4 agent-1 (task: Verify PlayerAttackMixin injection fires and execute chiseledRunGametest)
**Category:** broken_existing
**File:** src/main/kotlin/com/dndweapons/test/TooltipInjectionGametest.kt:65
**Expected:** Tooltip injection lines (slashing/piercing/Finesse/Versatile) appear in the rendered tooltip during gametest.
**Evidence:**
```
AssertionError: Longsword tooltip missing 'slashing'. Got: Longsword||When in Main Hand:|+5 Attack Damage|-2.5 Attack Speed
AssertionError: Iron sword tooltip missing 'piercing'. Got: Iron Sword||When in Main Hand:|+5 Attack Damage|-2.4 Attack Speed
Both failures occur on all 5 MC versions.
```
**Suggested fix:** The collectTooltip helper's comment 'ItemTooltipCallback fires server-side too' is incorrect; ItemTooltipCallback is purely client-side. Options:
(a) Call WeaponTooltipBuilder.build(spec) directly in the gametest and inspect its output rather than going through stack.getTooltipLines();
(b) Register the tooltip injection through a server-aware DataComponentType hook so the lines are baked into the stack on registration;
(c) Restructure tests to only run in a client gametest context (uses Fabric's client-gametest harness if available).

---

### [wave-6-agent-1-bug-1] phase-3-combat-hooks git tag intentionally NOT created - Phase 3 acceptance not fully green
**Wave/Agent:** wave-6 agent-1 (task: Create git tag and clean up working tree)
**Category:** missing_requirement
**File:** (git tag)
**Expected:** phase-3-combat-hooks tag created at the tip of green Phase 3 work.
**Evidence:**
```
phase-3-verification.md shows 7 of 14 spec acceptance criteria FAIL (criteria 5-11 all hinge on
the gametest path which is blocked by mock-player attack-strength + client-only tooltip issues).
Tagging here would misrepresent phase state.
```
**Suggested fix:** Defer tag creation until cycle 3 (or later) once chiseledRunGametest reaches 9/9 pass per version (45/45 total). The tag must wait on wave-4-agent-1-bug-2 and wave-4-agent-1-bug-3 being resolved.

---

## P2 Bugs

### [wave-2-agent-2-bug-1] Sweep-behavior gametest not added (file ownership conflict in wave plan)
**Wave/Agent:** wave-2 agent-2 (task: Determine sweep-entity modifier behavior)
**Category:** missing_requirement
**File:** src/main/kotlin/com/dndweapons/test/CombatHooksGametest.kt
**Expected:** A relevant gametest asserts the correct sweep behavior.
**Evidence:**
```
Wave-2-agent-2's owned_files list contains only PlayerAttackMixin.kt. AC4 requires
'A relevant gametest asserts the correct sweep behavior' which mandates editing
CombatHooksGametest.kt, owned by wave-2-agent-1 in this same wave.
```
**Suggested fix:** Add a sweep-behavior gametest in a follow-up wave that has file ownership over CombatHooksGametest.kt. The test should: (a) place a primary pig and a secondary pig within sweep range, (b) attack with greataxe (Heavy), (c) verify primary pig takes the Heavy-modified damage and the secondary pig takes only vanilla sweep damage without the Heavy bonus. Requires bug-2 (mock-player attack-strength) to be fixed first so the damage comparison is meaningful.

---

### [wave-2-agent-2-bug-2] Sweep guard uses 'strike-once' flag - runtime correctness unverified
**Wave/Agent:** wave-2 agent-2 (task: Determine sweep-entity modifier behavior)
**Category:** incorrect_implementation
**File:** src/main/kotlin/com/dndweapons/mixin/PlayerAttackMixin.kt:96
**Expected:** Sweep guard reliably distinguishes primary hit from sweep hits in all scenarios.
**Evidence:**
```kotlin
val primary = dndweapons_primaryTarget.get() ?: return damage
// Consume the flag so subsequent INVOKEs in the same call (sweep) skip the
// modifier. Cleared by dndweapons_clearTarget on RETURN as a safety net.
dndweapons_primaryTarget.set(null)
```
**Suggested fix:** The current strike-once flag works for the standard vanilla call sequence (primary hurt FIRST, then sweep loop). If any mod or future MC version reorders the calls so sweep entities are hurt first, the modifier would apply to a sweep target rather than the primary. The runtime gametest from wave-2-agent-2-bug-1 must verify this assumption holds; if not, switch to a per-entity tracking strategy using @Redirect on the hurt call site instead of @ModifyArg.

---

## P3 Bugs

### [wave-5-agent-1-bug-2] en_us.json key audit (criterion 13) not executed by static verifier
**Wave/Agent:** wave-5 agent-1 (task: Phase 3 matrix verification)
**Category:** missing_requirement
**File:** src/main/resources/assets/dndweapons/lang/en_us.json
**Expected:** Confirm tooltip.dndweapons.stat_block, damage_type.{slashing,piercing,bludgeoning}, bonus.{finesse_sprint,light_dual,versatile_empty,lance_foot} all present.
**Evidence:**
```
verifier did not run the for-loop grep specified in phase-3 plan lines 1975-1980.
```
**Suggested fix:** Run the audit grep in cycle 3 wave-5 (or fold into wave-6 cleanup task).
