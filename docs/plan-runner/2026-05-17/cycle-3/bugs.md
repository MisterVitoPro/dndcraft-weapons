# plan-runner Bug Report
**Date:** 2026-05-17
**Cycle:** 3
**Source plan:** docs/plan-runner/2026-05-17/cycle-2/fix-plan.md

## Summary
- P0: 0
- P1: 3
- P2: 1
- P3: 1
- Total: 5

## P0 Bugs
(none)

## P1 Bugs

### [wave-1-agent-1-bug-1] No access widener for Player.attackStrengthTicker; direct Kotlin field assignment will fail to compile
**Wave/Agent:** wave-1 agent-1 (task: Reset mock-player attack-strength in combat gametests)
**Category:** incorrect_implementation
**File:** src/main/kotlin/com/dndweapons/test/CombatHooksGametest.kt:124
**Expected:** attackStrengthTicker is a protected field on net.minecraft.world.entity.player.Player. For Kotlin to set it directly, an access widener entry must exist in src/main/resources/dndweapons.accesswidener AND fabric.loom.accessWidenerPath must point to that file in build.gradle.kts. Neither the .accesswidener file nor the gradle declaration exists anywhere in the project tree, so the build will fail with an IllegalAccessError or a compile-time visibility error on all 5 MC versions.
**Evidence:**
```
player.attackStrengthTicker = 100
```
**Suggested fix:** 1) Create src/main/resources/dndweapons.accesswidener with content: 'accessWidener v2 named\naccessible field net/minecraft/world/entity/player/Player attackStrengthTicker I'. 2) In build.gradle.kts inside the loom {} block add: accessWidenerPath = file("src/main/resources/dndweapons.accesswidener"). 3) Add '"accessWidener": "dndweapons.accesswidener"' to fabric.mod.json at the top level. Alternative: use player.resetAttackStrengthTicker() (option a from acceptance criteria) which is a public method and needs no widener.

---

### [wave-2-agent-1-bug-1] Primary damage assertion cannot distinguish mixin-fired from mixin-never-fired because HEAVY has no damage multiplier
**Wave/Agent:** wave-2 agent-1 (task: Add sweep-behavior gametest and verify runtime correctness of sweep guard)
**Category:** incorrect_implementation
**File:** src/main/kotlin/com/dndweapons/test/CombatHooksGametest.kt:353
**Expected:** The test should use a weapon where the DnD modifier produces a measurably different value on the primary target so that a broken mixin (not firing) produces a detectable difference. A FINESSE weapon with isSprinting=true (e.g. rapier or scimitar, multiplier 1.20x) would make primaryDealt differ from base by ~20%, giving a real signal. Alternatively add a HEAVY damage multiplier to WeaponAttackHandler and update the assertion.
**Evidence:**
```
WeaponAttackHandler.modifyDamage handles VERSATILE, LIGHT, FINESSE, SPECIAL_LANCE only. For a greataxe (Property.HEAVY + Property.TWO_HANDED), modifyDamage returns base unchanged. The assertion |primaryDealt - base| <= 0.5 will pass whether the mixin fires correctly, fires but does nothing, or never fires at all. The acceptance criterion 'primary damage equals base * HeavyMultiplier' is satisfied trivially and tests nothing about mixin correctness.
```
**Suggested fix:** Change the weapon to a FINESSE weapon (e.g. dndItem("rapier")) with player.isSprinting = true. The primary assertion becomes |primaryDealt - base * 1.20f| <= 0.5f. The secondary assertion remains secondaryDealt > 0 && secondaryDealt < primaryDealt - tol. This proves both that the mixin fires on the primary AND that the sweep guard prevents the 1.20x multiplier from applying to the secondary.

---

### [wave-2-agent-1-bug-2] Secondary sweep range is borderline: pig at BlockPos(2,1,1) is exactly 1.0 block from primary at BlockPos(2,1,2), risking sweep miss on versions with strict < 1.0 check
**Wave/Agent:** wave-2 agent-1 (task: Add sweep-behavior gametest and verify runtime correctness of sweep guard)
**Category:** incorrect_implementation
**File:** src/main/kotlin/com/dndweapons/test/CombatHooksGametest.kt:334
**Expected:** Place the secondary pig 0.5 blocks from the primary to be safely within the sweep range on all 5 versions. Use a fractional offset spawn API.
**Evidence:**
```
Vanilla sweep code checks distance to sweep candidate against a threshold. The primary pig spawns at the center of (2,1,2) and the secondary at center of (2,1,1), giving a center-to-center distance of exactly 1.0. Some MC versions use a strict less-than (< 1.0) for the sweep range check, meaning the secondary pig is precisely on the boundary and may not be swept. If sweep never fires, secondaryDealt <= 0 throws the 'sweep did not fire' assertion, causing the test to fail spuriously.
```
**Suggested fix:** Replace val secondaryPos = BlockPos(2, 1, 1) with a spawn call that positions the secondary entity at a fractional coordinate within 0.5 blocks of the primary, for example using ctx.spawnAt(EntityType.PIG, Vec3(2.0, 1.0, 1.5)) or equivalent API so the inter-entity gap is 0.5 rather than 1.0 blocks.

## P2 Bugs

### [wave-2-agent-1-bug-3] Sweep guard test cannot verify flag-broken scenario when HEAVY modifier is identity (1.0x)
**Wave/Agent:** wave-2 agent-1 (task: Add sweep-behavior gametest and verify runtime correctness of sweep guard)
**Category:** incorrect_implementation
**File:** src/main/kotlin/com/dndweapons/test/CombatHooksGametest.kt:372
**Expected:** The test should use a weapon with a non-identity DnD damage modifier so primary and secondary tests are independently meaningful.
**Evidence:**
```
If the strike-once ThreadLocal guard broke and the @ModifyArg fired for both primary and sweep hurt calls, for a HEAVY-only weapon modifyDamage returns base unchanged. So a broken guard would still result in secondary=base instead of vanilla 1.0 (caught by secondaryDealt >= primaryDealt - tol). But this only works coincidentally — same root issue as bug-1: with identical primary and modified-secondary values both = base, the test cannot independently verify primary-fires and guard-works.
```
**Suggested fix:** Same fix as wave-2-agent-1-bug-1: use a FINESSE weapon with sprinting to get a 1.20x multiplier on primary.

## P3 Bugs

### [wave-2-agent-1-bug-4] Acceptance criterion wording 'secondary pig damage equals vanilla sweep damage WITHOUT the Heavy bonus' is misleading; HEAVY has no damage bonus
**Wave/Agent:** wave-2 agent-1 (task: Add sweep-behavior gametest and verify runtime correctness of sweep guard)
**Category:** missing_requirement
**File:** src/main/kotlin/com/dndweapons/test/CombatHooksGametest.kt:307
**Expected:** The acceptance criterion should be reworded to 'Asserts secondary pig damage is positive and strictly less than primary pig damage, confirming the DnD @ModifyArg did not fire on the sweep INVOKE.'
**Evidence:**
```
Since HEAVY has no damage bonus, 'without the Heavy bonus' is vacuously true and the criterion provides no meaningful coverage guarantee. The KDoc comment in the code is accurate but the plan's wording is misleading.
```
**Suggested fix:** Update the KDoc comment to remove the implication that HEAVY contributes a damage multiplier and clarify the test validates the strike-once guard by showing secondary < primary. No code change beyond the comment.
