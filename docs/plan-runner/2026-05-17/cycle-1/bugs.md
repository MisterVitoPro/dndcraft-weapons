# plan-runner Bug Report
**Date:** 2026-05-17
**Cycle:** 1
**Source plan:** docs/superpowers/plans/2026-05-17-dnd-weapons-phase-3-combat-hooks.md

## Summary
- P0: 4
- P1: 6
- P2: 6
- P3: 1
- Total: 17

> Duplicates removed: wave-2-agent-2-bug-1 (same makeMockPlayer compile error as wave-2-agent-1-bug-1) and wave-4-agent-1-bug-4 (same SpecRegistry bootstrap failure as wave-3-agent-1-bug-1).

---

## P0 Bugs

### [wave-2-agent-1-bug-1] makeMockPlayer() called without GameType arg on 5 call sites; fails to compile on MC 1.21.4
**Wave/Agent:** wave-2 agent-1 (task: CombatHooks + TooltipInjection gametests + fabric.mod.json)
**Category:** incorrect_implementation
**File:** src/main/kotlin/com/dndweapons/test/CombatHooksGametest.kt (lines 100, 123, 145, 167, 190)
**Expected:** Each of the 5 call sites should be Stonecutter-gated: `//? if >=1.21.2 { ctx.makeMockPlayer(GameType.SURVIVAL) //?} else { ctx.makeMockPlayer() //?}` and `net.minecraft.world.level.GameType` must be imported under the same version gate.
**Evidence:**
```
Lines 100, 123, 145, 167, 190 all call ctx.makeMockPlayer() with no arguments.
MC 1.21.4 (>=1.21.2) requires makeMockPlayer(GameType). The file has no stonecutter
version gate around these calls and no import for net.minecraft.world.level.GameType,
so the module fails to compile on the 1.21.4 subproject. Confirmed by wave-2-agent-2's
build failure report.
```
**Suggested fix:** Add `import net.minecraft.world.level.GameType` under a `//? if >=1.21.2 { ... //?}` block near the top imports. Wrap all 5 makeMockPlayer() calls (lines 100, 123, 145, 167, 190) with stonecutter guards.

---

### [wave-4-agent-1-bug-1] chiseledTest Stonecutter task does not exist
**Wave/Agent:** wave-4 agent-1 (task: chiseledTest - fix per-version unit-test issues)
**Category:** missing_requirement
**File:** stonecutter.gradle.kts (and stonecutter.gradle)
**Expected:** `./gradlew chiseledTest` invokes the test task across all 5 subprojects.
**Evidence:**
```
stonecutter.gradle.kts and stonecutter.gradle both define only chiseledBuild and
chiseledRunGametest. No chiseledTest task is registered.
```
**Suggested fix:** Add a `chiseledTest` task to both `stonecutter.gradle.kts` and `stonecutter.gradle` mirroring the `chiseledBuild` pattern using `ofTask("test")`.

---

### [wave-4-agent-1-bug-3] Test sources not picked up by Stonecutter — chiseledSrc/test not populated
**Wave/Agent:** wave-4 agent-1 (task: chiseledTest - fix per-version unit-test issues)
**Category:** missing_requirement
**File:** build.gradle.kts
**Expected:** The `src/test` source set is registered with Stonecutter so per-version `chiseledSrc/test` directories are populated and version gates in test sources are processed.
**Evidence:**
```
src/test/kotlin tree exists with 5 JUnit test files but no per-version
build/chiseledSrc/test directory is created. Stonecutter only processes source
sets registered with it.
```
**Suggested fix:** Register the test source set in `build.gradle.kts` so Stonecutter stamps it per-version.

---

### [wave-4-agent-2-bug-1] stonecutter.gradle.kts and stonecutter.gradle modified outside owned_files without disclosure
**Wave/Agent:** wave-4 agent-2 (task: chiseledRunGametest - fix per-version mixin/runtime issues)
**Category:** scope_drift
**File:** stonecutter.gradle.kts (and stonecutter.gradle)
**Expected:** Files outside `owned_files` must be listed in `files_unexpectedly_modified` with reasoning in `concerns`.
**Evidence:**
```
wave-4-agent-2's owned_files were PlayerAttackMixin.kt, CombatHooksGametest.kt,
WeaponTooltipInjector.kt. The agent also wrote stonecutter.gradle.kts and
stonecutter.gradle (adding chiseledRunGametest task) and listed them in files_written
but NOT in files_unexpectedly_modified.
```
**Suggested fix:** Process violation only — no code fix needed. The additive task registration is correct and not breaking. Note for future agent-discipline tracking.

---

## P1 Bugs

### [wave-2-agent-1-bug-2] Only 8 gametests implemented; acceptance criterion requires 9
**Wave/Agent:** wave-2 agent-1 (task: CombatHooks + TooltipInjection gametests + fabric.mod.json)
**Category:** missing_requirement
**File:** src/main/kotlin/com/dndweapons/test/CombatHooksGametest.kt
**Expected:** 9 gametest methods total across `CombatHooksGametest.kt` and `TooltipInjectionGametest.kt`.
**Evidence:**
```
CombatHooksGametest.kt defines 6 test methods.
TooltipInjectionGametest.kt defines 2 test methods.
Total = 8 gametests. Acceptance criteria for wave-2-agent-1 and wave-2-agent-2 both
state 'All 9 gametests pass'.
```
**Suggested fix:** Add the missing 9th gametest. The plan likely intended a `heavy_knockback` test in `CombatHooksGametest.kt`; verify against plan lines 1274-1601.

---

### [wave-3-agent-1-bug-1] SpecRegistry.parseItemTagKey triggers BuiltInRegistries bootstrap, causing 'Not bootstrapped' in 1.20.1 test runtime
**Wave/Agent:** wave-3 agent-1 (task: chiseledBuild - fix per-version compilation issues)
**Category:** incorrect_implementation
**File:** src/main/kotlin/com/dndweapons/registry/SpecRegistry.kt:78
**Expected:** SpecRegistryTest passes on 1.20.1 without requiring MC Bootstrap.
**Evidence:**
```
TagKey.create(Registries.ITEM, loc) at line 78 forces BuiltInRegistries.<clinit> to run.
In the 1.20.1 test environment the registries are not bootstrapped before SpecRegistryTest,
so any call path through parseItemTagKey (e.g. bindRoleTag) throws a 'Not bootstrapped'
error at runtime. This was masked by compile failures before wave 3.
```
**Suggested fix:** Either (a) lazy-resolve the `Registries.ITEM` reference into a companion-object property only touched post-bootstrap, or (b) accept `TagKey<Item>` directly in `bindRoleTag` and move string parsing to call sites that are guaranteed to run post-bootstrap. Blocks wave 4 1.20.1 test execution.

---

### [wave-4-agent-1-bug-2] startRiding() argument count compile error on 1.21.4 — status unclear
**Wave/Agent:** wave-4 agent-1 (task: chiseledTest - fix per-version unit-test issues)
**Category:** incorrect_implementation
**File:** src/main/kotlin/com/dndweapons/test/CombatHooksGametest.kt:223
**Expected:** `CombatHooksGametest.kt` compiles on all 5 versions.
**Evidence:**
```
Wave-4-agent-1 reported a compile failure at line 223 for 1.21.4. Reading the file
shows lines 222-226 ARE correctly Stonecutter-forked. Either the report was stale OR
Stonecutter substitution isn't being applied to test sources. Unverified without a
build run.
```
**Suggested fix:** Re-run `./gradlew chiseledBuild` and `./gradlew :1.21.4:build` to confirm status. If failure persists, debug Stonecutter substitution of test/gametest sources. (Likely resolves with wave-4-agent-1-bug-3 fix.)

---

### [wave-4-agent-2-bug-2] PlayerAttackMixin target rewritten to Entity.hurt/hurtOrSimulate @ModifyArg — unverified at runtime
**Wave/Agent:** wave-4 agent-2 (task: chiseledRunGametest - fix per-version mixin/runtime issues)
**Category:** incorrect_implementation
**File:** src/main/kotlin/com/dndweapons/mixin/PlayerAttackMixin.kt
**Expected:** Mixin injection fires and modifies damage on all 5 MC versions; gametests verify.
**Evidence:**
```
Mixin architecturally rewritten: target now @ModifyArg(index=1) on Entity.hurt (<1.21.4)
/ Entity.hurtOrSimulate (>=1.21.4) inside Player.attack(Entity). Plausible per static
bytecode analysis but no gametest run confirms injection fires.
```
**Suggested fix:** Run `chiseledRunGametest`. Add a logger/breakpoint to confirm `@ModifyArg` fires. Verify on 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2.

---

### [wave-4-agent-2-bug-3] chiseledRunGametest never actually executed — acceptance criterion unmet
**Wave/Agent:** wave-4 agent-2 (task: chiseledRunGametest - fix per-version mixin/runtime issues)
**Category:** missing_requirement
**File:** (no specific file — task-level acceptance criterion)
**Expected:** `./gradlew chiseledRunGametest` is run and all 9 gametests pass on all 5 versions (45 passing total).
**Evidence:**
```
Acceptance criterion: './gradlew chiseledRunGametest succeeds across all 5 subprojects'
and 'All 9 gametests pass on all 5 versions (45 passing tests total)'. Agent explicitly
states 'chiseledRunGametest was not actually run — fixes are static-bytecode-inspection-based.'
```
**Suggested fix:** Run `./gradlew chiseledRunGametest`. Triage and fix any runtime failures.

---

### [wave-4-agent-3-bug-1] Criterion 12 unverified — build/test/gametest matrix not confirmed green
**Wave/Agent:** wave-4 agent-3 (task: Final acceptance - verify all 14 spec criteria)
**Category:** missing_requirement
**File:** (no specific file — full-matrix acceptance criterion)
**Expected:** Full matrix passes: `chiseledBuild` + `chiseledTest` + `chiseledRunGametest` across all 5 versions.
**Evidence:**
```
Criterion 12 requires all 5 MC versions pass chiseledBuild, chiseledTest, and
chiseledRunGametest. chiseledTest task missing (P0); chiseledRunGametest not run (P1).
Phase structurally present, not runtime-verified.
```
**Suggested fix:** After fixing P0 bugs (`chiseledTest` task, test source set), run full matrix. Iterate fixes until green.

---

## P2 Bugs

### [wave-2-agent-1-bug-3] vanillaIronSwordTooltipContainsStatBlock depends on shortsword tag file not guaranteed to exist
**Wave/Agent:** wave-2 agent-1 (task: CombatHooks + TooltipInjection gametests + fabric.mod.json)
**Category:** missing_requirement
**File:** src/main/kotlin/com/dndweapons/test/TooltipInjectionGametest.kt:73
**Expected:** `data/dndweapons/tags/items/role/shortsword.json` exists and contains `minecraft:iron_sword`.
**Evidence:**
```
vanillaIronSwordTooltipContainsStatBlock asserts Iron Sword tooltip contains 'piercing'
and 'Finesse', which requires tag-driven tooltip injection to map Iron Sword into the
shortsword role. The tag file is not guaranteed to exist.
```
**Suggested fix:** Verify the tag file exists. If missing, create `data/dndweapons/tags/items/role/shortsword.json` with `minecraft:iron_sword` as part of this fix wave.

---

### [wave-2-agent-2-bug-2] In-game spot-check (criterion 4) cannot be automated and was deferred
**Wave/Agent:** wave-2 agent-2 (task: 1.21.4 acceptance - full build + test + gametest)
**Category:** missing_requirement
**File:** (no specific file — manual verification criterion)
**Expected:** Manual verification: all 34 weapons visible, tooltips render correctly, Greataxe shows Attack Knockback.
**Evidence:**
```
wave-2-agent-2 deferred: 'all 34 weapons visible, tooltips render correctly, Greataxe
shows Attack Knockback'. Cannot launch interactive Minecraft from agent context.
```
**Suggested fix:** Schedule a manual in-game spot-check; this cannot be resolved by the agent swarm.

---

### [wave-3-agent-1-bug-2] PlayerAttackMixin @At target uses hardcoded DamageSource descriptor with no per-version fork
**Wave/Agent:** wave-3 agent-1 (task: chiseledBuild - fix per-version compilation issues)
**Category:** incorrect_implementation
**File:** src/main/kotlin/com/dndweapons/mixin/PlayerAttackMixin.kt:30
**Expected:** Mixin injection succeeds in all 5 environments OR a version-conditional descriptor fork is in place.
**Evidence:**
```
The @At target descriptor 'Lnet/minecraft/world/damagesource/DamageSource;F' is a
single string with no stonecutter/preprocessor fork. If any of the 5 subprojects uses
a mapping set where the intermediary name differs, the mixin will silently fail to inject
at runtime without a compile error. No static evidence of breakage was found, but the
absence of a version fork here is inconsistent with the defensive forking applied elsewhere.
```
**Suggested fix:** Run a targeted mixin injection test across all 5 subprojects, or add a stonecutter fork on the target descriptor if decompilation reveals a descriptor change between versions.

---

### [wave-4-agent-2-bug-4] Sweep entities receive DnD modifier — potential unintended behavior
**Wave/Agent:** wave-4 agent-2 (task: chiseledRunGametest - fix per-version mixin/runtime issues)
**Category:** incorrect_implementation
**File:** src/main/kotlin/com/dndweapons/mixin/PlayerAttackMixin.kt
**Expected:** Determination of intended behavior; if sweep entities should NOT receive the modifier, a guard is added.
**Evidence:**
```
@ModifyArg on the INVOKE inside Player.attack(Entity) fires for every entity hit,
including sweep targets. Spec does not explicitly state whether sweep entities should
get the modifier.
```
**Suggested fix:** Consult the design spec. If sweep entities should be exempt, switch to `@ModifyVariable` on the primary damage local, or guard the modification with a check against the primary target.

---

### [wave-4-agent-2-bug-5] 26.1.2 identity mapping for mixin bytecode descriptor is novel and untested
**Wave/Agent:** wave-4 agent-2 (task: chiseledRunGametest - fix per-version mixin/runtime issues)
**Category:** incorrect_implementation
**File:** src/main/kotlin/com/dndweapons/mixin/PlayerAttackMixin.kt
**Expected:** Mixin works on 26.1.2 the same as other versions.
**Evidence:**
```
26.1.2 uses identity tiny v2 stub (no intermediary remapping). Mixin descriptor
resolution at runtime may behave differently on this novel mapping config.
```
**Suggested fix:** Smoke-test the mixin on 26.1.2 specifically. May need an additional descriptor fork if behavior differs.

---

### [wave-4-agent-3-bug-2] phase-3-combat-hooks git tag not created
**Wave/Agent:** wave-4 agent-3 (task: Final acceptance - verify all 14 spec criteria)
**Category:** missing_requirement
**File:** (no specific file — git tag operation)
**Expected:** `phase-3-combat-hooks` tag exists at the tip of the Phase 3 work.
**Evidence:**
```
Criterion 14 requires phase tagged 'phase-3-combat-hooks'. Agent-3 is static-read-only
and could not create the tag.
```
**Suggested fix:** After full matrix is green, manually run: `git tag phase-3-combat-hooks && git push --tags`.

---

## P3 Bugs

### [wave-4-agent-3-bug-3] Uncommitted manifest.json modification and untracked logs/ directory
**Wave/Agent:** wave-4 agent-3 (task: Final acceptance - verify all 14 spec criteria)
**Category:** scope_drift
**File:** (manifest.json + versions/1.20.1/logs/)
**Expected:** Clean working tree at end of phase.
**Evidence:**
```
Working tree has a modified manifest.json and an untracked versions/1.20.1/logs/ directory.
```
**Suggested fix:** Add `versions/*/logs/` to `.gitignore`. Commit or revert `manifest.json` changes.
