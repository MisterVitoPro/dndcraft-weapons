# Phase 3 Acceptance Verification (cycle 3)

**Date:** 2026-05-17
**Cycle:** 3
**Source spec:** docs/superpowers/specs/2026-05-17-dnd-weapons-phase-3-design.md (Section 7)
**Source plan:** docs/superpowers/plans/2026-05-17-dnd-weapons-phase-3-combat-hooks.md
**Runner:** plan-runner cycle 3
**Overall status:** DONE_WITH_CONCERNS

---

## Context

Cycle 3 was the repair cycle for the 7 failing criteria carried over from cycle 2. Three dev waves
ran:

- **Wave 1** (commit `2dbd43afe0855d76f2a6438c7547cda7b55cf8d2`): three agents — reset
  attack-strength cooldown in combat gametests (agent-1), rewrite tooltip gametests to bypass
  server-side `ItemTooltipCallback` (agent-2), and audit `en_us.json` keys (agent-3).
- **Wave 2** (commit `21bb636280374c289b47043f45faf8fd492d4e28`): one agent added the sweep-guard
  gametest.
- **Wave 3** (this document): tagging and final verification gate.

Both wave-1 and wave-2 verifiers returned `BUGS_FOUND`. The Gradle build has **not been run** in
cycle 3; all status assessments below are static-analysis-based.

---

## Matrix runtime results

> NOTE: No Gradle invocation occurred in cycle 3. Statuses reflect static code analysis and
> verifier-reported bugs. The table below shows the expected outcome if bugs are resolved.

| Task | All 5 versions | Status |
|---|---|---|
| `./gradlew chiseledBuild` | 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2 | RED (will not compile — see P1 bugs below) |
| `./gradlew chiseledTest` | 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2 | UNKNOWN (blocked by build failure) |
| `./gradlew chiseledRunGametest` | 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2 | UNKNOWN (blocked by build failure) |

---

## Unresolved bugs blocking the git tag

The following bugs were reported by cycle-3 wave verifiers and have NOT been fixed as of this
document. The git tag `phase-3-combat-hooks` was **NOT created** in cycle 3 because of these
open P1 issues.

### P1 bugs (build-blocking or test-invalidating)

**wave-1-agent-1-bug-1** — No access widener for `Player.attackStrengthTicker`

- **File:** `src/main/kotlin/com/dndweapons/test/CombatHooksGametest.kt` line 124
- **Evidence:** `player.attackStrengthTicker = 100` — `attackStrengthTicker` is a protected field
  on `net.minecraft.world.entity.player.Player`. No `.accesswidener` file exists and no
  `loom.accessWidenerPath` declaration exists in `build.gradle.kts`. The build will fail with a
  compile-time visibility error on all 5 MC versions.
- **Fix options:**
  1. Use `player.resetAttackStrengthTicker()` (public method, zero config needed).
  2. Create `src/main/resources/dndweapons.accesswidener`, declare
     `accessible field net/minecraft/world/entity/player/Player attackStrengthTicker I`, and wire
     it in `build.gradle.kts` inside `loom {}` plus add `"accessWidener"` to `fabric.mod.json`.

**wave-2-agent-1-bug-1** — Sweep-guard test uses HEAVY weapon with no damage multiplier, so
primary assertion is trivially true whether the mixin fires or not

- **File:** `src/main/kotlin/com/dndweapons/test/CombatHooksGametest.kt` line 353
- **Evidence:** `WeaponAttackHandler.modifyDamage` has no multiplier branch for `Property.HEAVY`.
  A broken mixin that never fires produces identical primary damage to a correctly-firing mixin,
  so the assertion `|primaryDealt - base| <= 0.5` provides no real signal.
- **Fix:** Switch weapon to a FINESSE weapon (e.g. `dndItem("rapier")`) with
  `player.isSprinting = true`. Update primary assertion to
  `|primaryDealt - base * 1.20f| <= 0.5f`.

**wave-2-agent-1-bug-2** — Secondary pig is at exactly 1.0 block distance, on the boundary of
vanilla sweep range; may not be swept on versions using a strict `< 1.0` check

- **File:** `src/main/kotlin/com/dndweapons/test/CombatHooksGametest.kt` line 334
- **Evidence:** Primary pig at block-center (2,1,2), secondary at block-center (2,1,1) gives
  center-to-center distance = 1.0. Some MC versions use `distance < 1.0`, making the secondary
  pig unreachable and causing spurious test failure.
- **Fix:** Place the secondary pig at a fractional coordinate 0.5 blocks from the primary, e.g.
  `ctx.spawnAt(EntityType.PIG, Vec3(2.0, 1.0, 1.5))`.

### P2 bugs (test logic weaknesses, not build-blocking)

**wave-2-agent-1-bug-3** — Sweep guard cannot independently verify the flag-broken scenario
because HEAVY is an identity multiplier

- **File:** `src/main/kotlin/com/dndweapons/test/CombatHooksGametest.kt` line 372
- **Fix:** Same as bug-1 above (switch to FINESSE weapon).

### P3 bugs (documentation / wording only)

**wave-2-agent-1-bug-4** — Acceptance criterion wording "secondary pig damage equals vanilla
sweep damage WITHOUT the Heavy bonus" is misleading; HEAVY contributes no damage bonus

- **File:** `src/main/kotlin/com/dndweapons/test/CombatHooksGametest.kt` line 307 (KDoc)
- **Fix:** Update KDoc to: "Asserts secondary pig damage is positive and strictly less than primary
  pig damage, confirming the DnD @ModifyArg did not fire on the sweep INVOKE."

---

## 14 acceptance criteria (per spec Section 7)

| # | Criterion | Result | Evidence / Notes |
|---|---|---|---|
| 1 | SpecRegistry exists with expected API (`bindRegistered`, `bindRoleTag`, `lookup`, `invalidateRoleCache`) | GREEN | Confirmed present in `SpecRegistry.kt` by cycle-2 verifier; no changes in cycle 3 |
| 2 | WeaponAttackHandler.modifyDamage correct (12 assertions x 5 versions) | GREEN | `WeaponAttackHandlerTest` passed in cycle-2 chiseledTest run; no changes to `WeaponAttackHandler` in cycle 3 |
| 3 | PlayerAttackMixin injects on every version | YELLOW | Mixin loaded without `MixinTransformerError` in cycle 2; cycle-3 wave-1 adds `resetAttackStrengthTicker()` but the access-widener bug (wave-1-bug-1) means the build currently fails before mixin injection can be verified |
| 4 | AttributeCompat emits ATTACK_KNOCKBACK for Heavy on both epochs | YELLOW | Confirmed in cycle-2 gametest output (Epoch C); Epoch A (1.20.1) in-game spot-check remains pending manual verification |
| 5 | Tooltip injection on registered items | YELLOW | Wave-1-agent-2 rewrote `TooltipInjectionGametest` to bypass server-side `ItemTooltipCallback`; approach DONE_WITH_CONCERNS per agent status; unverified by build |
| 6 | Tooltip injection on vanilla iron sword | YELLOW | Same as criterion 5 |
| 7 | Finesse on registered item (`finesseSprintBonusFires`) | RED | Wave-1-agent-1 added cooldown reset, but the access-widener P1 bug prevents compilation and therefore verification |
| 8 | Finesse on vanilla item (`vanillaIronSwordCarriesFinesseHook`) | RED | Same blocker as criterion 7 |
| 9 | Light dual-wield (`lightDualWieldBonusFires`) | RED | Same blocker as criterion 7 |
| 10 | Versatile offhand-empty (`versatileEmptyOffhandFires`) | RED | Same blocker as criterion 7 |
| 11 | Lance on-foot halves; mounted does not | RED | Same blocker as criterion 7 |
| 12 | All 5 MC versions pass build/test/gametest | RED | Build will fail due to access-widener P1 bug; test and gametest cannot run until build is fixed |
| 13 | `en_us.json` keys present | GREEN | Wave-1-agent-3 audited: all 8 required keys are present with no missing entries (see audit below) |
| 14 | Phase 1 + Phase 2 gametests still pass (no regression) | GREEN | `RegistrationGametest.longswordIsRegistered` passed in cycle 2; no changes to Phase 1/2 code in cycle 3 |

**Summary:** 4 GREEN, 2 YELLOW (infrastructure done, unverified), 5 RED (blocked by P1 bugs), 3 RED (blocked by P1 build failure). The phase cannot be considered complete.

---

## en_us.json audit (wave-1-agent-3)

Wave-1-agent-3 status: **CLEAN** (no bugs found).

All 8 required Phase 3 translation keys are present in
`src/main/resources/assets/dndweapons/lang/en_us.json`:

| Key | Value |
|---|---|
| `tooltip.dndweapons.stat_block` | `"%s %s%s"` |
| `tooltip.dndweapons.damage_type.slashing` | `"slashing"` |
| `tooltip.dndweapons.damage_type.piercing` | `"piercing"` |
| `tooltip.dndweapons.damage_type.bludgeoning` | `"bludgeoning"` |
| `tooltip.dndweapons.bonus.finesse_sprint` | `"+20%% damage while sprinting"` |
| `tooltip.dndweapons.bonus.light_dual` | `"+1 damage when dual-wielding Light"` |
| `tooltip.dndweapons.bonus.versatile_empty` | `"+%d damage when offhand empty"` |
| `tooltip.dndweapons.bonus.lance_foot` | `"Half damage when on foot"` |

No additions were required.

---

## phase-3-combat-hooks git tag

**NOT CREATED in cycle 3.**

The git tag was deliberately withheld because the following P1 bugs remain unresolved:

1. **wave-1-agent-1-bug-1** — `Player.attackStrengthTicker` access-widener missing; build will
   not compile on any of the 5 MC versions.
2. **wave-2-agent-1-bug-1** — Sweep-guard gametest uses HEAVY weapon with no damage multiplier;
   primary damage assertion is trivially satisfiable and provides no real mixin-correctness signal.
3. **wave-2-agent-1-bug-2** — Secondary pig at exactly 1.0 block distance is on the sweep-range
   boundary; may cause spurious test failure on strict-`< 1.0`-check versions.

Per the spec, the tag should only be placed once all 14 acceptance criteria are GREEN and the full
Gradle matrix (`chiseledBuild` + `chiseledTest` + `chiseledRunGametest`) passes on all 5 MC
versions.

### Manual tag commands (for the user to run after a future cycle resolves the bugs)

```bash
git tag phase-3-combat-hooks
git push origin phase-3-combat-hooks
```

Run these from the root of the repository after confirming the Gradle matrix is fully green.

---

## Carry-over to cycle 4

The following items must be resolved before the tag can be created:

| Priority | Bug ID | Fix required |
|---|---|---|
| P1 | wave-1-agent-1-bug-1 | Replace `player.attackStrengthTicker = 100` with `player.resetAttackStrengthTicker()` in `CombatHooksGametest.kt` |
| P1 | wave-2-agent-1-bug-1 | Switch sweep-guard test weapon from `greataxe` (HEAVY) to `rapier` (FINESSE) with `player.isSprinting = true`; update primary assertion to `base * 1.20f` |
| P1 | wave-2-agent-1-bug-2 | Move secondary pig to fractional coordinate 0.5 blocks from primary (e.g. `Vec3(2.0, 1.0, 1.5)`) |
| P2 | wave-2-agent-1-bug-3 | Resolved by same fix as bug-1 above |
| P3 | wave-2-agent-1-bug-4 | Update KDoc wording (no code change) |
| Manual | — | Epoch A (1.20.1) in-game ATTACK_KNOCKBACK spot-check (criterion 4) |
