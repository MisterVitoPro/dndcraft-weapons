# plan-runner Bug Report
**Date:** 2026-05-18
**Cycle:** 3
**Source plan:** docs/plan-runner/2026-05-18/cycle-2/fix-plan.md

## Summary
- P0: 0
- P1: 0
- P2: 2
- P3: 0
- Total: 2 (both scope_drift onto pre-existing Phase 4 issues; not Phase 5 regressions)

## Convergence

| Cycle | Bug count | Notes |
|---|---|---|
| 1 | 2 (1 P0) | Stonecutter parse error |
| 2 | 6 (4 P0) | Compile errors revealed |
| 3 | 2 (0 P0, 0 P1) | All Phase 5 scope GREEN; only pre-existing Phase 4 fails remain |

**Phase 5 scope: GREEN (zero in-scope bugs).**

## P2 Bugs (scope drift onto pre-existing Phase 4 issues)

### [wave-2-agent-1-bug-1] Three pre-existing Phase 4 gametest regressions
**Wave/Agent:** wave-2 agent-1 (task: Re-run verification matrix)
**Category:** scope_drift
**Files:** src/main/kotlin/com/dndweapons/test/CombatHooksGametest.kt, src/main/kotlin/com/dndweapons/test/TooltipInjectionGametest.kt
**Expected:** 16/16 gametests pass per version (Task 4 acceptance criterion)
**Evidence:**
- `combathooksgametest.heavysweepguarddoesnotboostsecondary`: fails on 1.20.1, 1.21.1, 1.21.4, 26.1.2 with `Sweep guard (primary): dealt=6.0 expected~7.0 (base=6.0)`.
- `combathooksgametest.vanillaironswordcarriesfinessehook`: fails on 1.20.1, 1.21.1, 1.21.4, 26.1.2 with `Finesse sprint: dealt=6.0 expected~7.2 (base=6.0)`.
- `tooltipinjectiongametest.vanillaironswordtooltipcontainsstatblock`: fails on 1.20.1, 1.21.1, 1.21.4, 26.1.2 with `Iron Sword has no WeaponSpec in SpecRegistry`.
- 1.21.11 passes 12/12 (these tests are not present in the 1.21.11 set, by virtue of the Fabric gametest annotation drift at 1.21.5+).
**Suggested fix:** Open a Phase 4 follow-up plan. NOT addressable by re-running the Phase 5 plan; requires touching Phase 1-3 combat-mixin and role-tag binding code, all outside the cycle-2 fix-plan's owned-files set.

### [wave-2-agent-1-bug-2] 26.1.2-only smithing gametest regression
**Wave/Agent:** wave-2 agent-1 (task: Re-run verification matrix)
**Category:** scope_drift
**File:** src/main/kotlin/com/dndweapons/test/SmithingGametest.kt
**Expected:** 16/16 gametests on 26.1.2
**Evidence:** `dndweapons:smithing_gametest_netherite_fire_immunity_fires` fails on 26.1.2 only. Phase 4 smithing concern.
**Suggested fix:** Phase 4 26.1.2 smithing-template fork audit; outside Phase 5 plan scope.

## Phase 5 acceptance criteria status

| Criterion | Status |
|---|---|
| WitherTrophyHandler.kt compiles on all 5 versions | GREEN |
| AcquisitionGametest.kt compiles on all 5 versions | GREEN |
| WeaponTradeRegistrar.kt compiles on all 5 versions | GREEN |
| chiseledTest (JUnit, 19 unit tests) passes on all 5 versions | GREEN |
| Phase 5 acquisition gametests (3) pass on every version that runs them | GREEN |
| chiseledBuild BUILD SUCCESSFUL on all 5 versions | GREEN |
| Full chiseledRunGametest 16/16 pass per version | HOLD (Phase 4 fails) |
| phase-5-verification-final.md updated with actual results | DONE |
| git tag phase-5-acquisition applied | NOT EXECUTED (orchestrator policy: never tag/push) |

## Recommendation

Do NOT re-run this plan automatically. The remaining 2 bugs are out-of-scope for
the Phase 5 acquisition plan and cannot be resolved by repeating the same task list.
The user should open a separate Phase 4 follow-up plan to address the 3 pre-existing
combat-hook/tooltip regressions and the 1 26.1.2 smithing regression. After those
fixes land, the chiseledRunGametest matrix should reach 16/16 GREEN on every
version, and `git tag phase-5-acquisition` can be applied.
