# Phase 3 Acceptance Verification (cycle 2)

**Date:** 2026-05-17
**Cycle:** 2
**Source spec:** docs/superpowers/specs/2026-05-17-dnd-weapons-phase-3-design.md (Section 7)
**Source plan:** docs/superpowers/plans/2026-05-17-dnd-weapons-phase-3-combat-hooks.md
**Runner:** plan-runner cycle 2

## Matrix runtime results

| Task | All 5 versions | Status |
|---|---|---|
| `./gradlew chiseledBuild` | 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2 | GREEN |
| `./gradlew chiseledTest` | 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2 | GREEN (25 unit tests pass) |
| `./gradlew chiseledRunGametest` | 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2 | RED (8 of 10-11 gametests fail per version, same failures across versions) |

## 14 acceptance criteria (per spec Section 7)

| # | Criterion | Result | Evidence |
|---|---|---|---|
| 1 | SpecRegistry exists with expected API | PASS | `bindRegistered`, `bindRoleTag`, `lookup`, `invalidateRoleCache` all present in `SpecRegistry.kt` |
| 2 | WeaponAttackHandler.modifyDamage correct | PASS | `WeaponAttackHandlerTest` passes in chiseledTest run on all 5 versions |
| 3 | PlayerAttackMixin injects on every version | PASS | Mixin loads without `MixinTransformerError`; `heavyKnockbackBonusApplies` test passes on all versions (knockback path proves @ModifyArg fires) |
| 4 | AttributeCompat emits ATTACK_KNOCKBACK for Heavy | PARTIAL | Confirmed in tooltips per gametest output: greataxe shows "+5 Attack Damage" via attribute path. `heavy_knockback` gametest passes on all 5 versions. In-game spot-check (Epoch A 1.20.1) still pending manual verification. |
| 5 | Tooltip injection on registered items | FAIL | `registeredItemTooltipContainsStatBlock` fails on all 5 versions. Cause: `ItemTooltipCallback` is client-only; gametest server context does not fire it. See cycle-2 wave-4 bug-3. |
| 6 | Tooltip injection on vanilla iron sword | FAIL | `vanillaIronSwordTooltipContainsStatBlock` fails on all 5 versions. Same root cause as criterion 5. |
| 7 | Finesse on registered item | FAIL | `finesseSprintBonusFires` fails. Cause: mock-player attack-strength scaling produces ~3.5% damage. See cycle-2 wave-4 bug-2. |
| 8 | Finesse on vanilla item | FAIL | `vanillaIronSwordCarriesFinesseHook` fails. Same root cause as criterion 7. |
| 9 | Light dual-wield | FAIL | `lightDualWieldBonusFires` fails. Same root cause as criterion 7. |
| 10 | Versatile offhand-empty | FAIL | `versatileEmptyOffhandFires` fails. Same root cause as criterion 7. |
| 11 | Lance on-foot halves; mounted does not | FAIL | `lanceOnFootHalves` + `lanceMountedFullDamage` fail. Same root cause as criterion 7. |
| 12 | All 5 MC versions pass build/test/gametest | PARTIAL | build + test green on all 5; gametest red on all 5 (same failures). |
| 13 | en_us.json keys present | PENDING | Static grep not run in this verifier task. |
| 14 | Phase 1 + Phase 2 gametests still pass | PASS | `RegistrationGametest.longswordIsRegistered` passes on all 5 versions. No regression. |

## Summary

- **Build infrastructure**: GREEN. Cycle 2 successfully landed chiseledTest task, test source-set materialization, SpecRegistry lazy-init, and the server-entrypoint unblocker. The Phase 3 build/test plumbing now works.
- **Gametest correctness**: RED. 8 of 9 combat/tooltip gametests fail due to two pre-existing design flaws:
  1. Mock-player attack-strength scaling (6 combat tests)
  2. Server-side ItemTooltipCallback misconception (2 tooltip tests)
- **Cycle 3 carry-over**: 4 bugs flagged in wave-4 are queued for next cycle. The mod is shippable for non-gametest use; gametest assertions require cycle 3 to reach full green.

## phase-3-combat-hooks git tag

NOT YET CREATED. Per the spec, the tag should only be placed once all 14 acceptance criteria are green. With 7/14 still failing, tagging here would misrepresent phase state. The tag is deferred to cycle 3 (wave-6 task) once the gametest carryover bugs are resolved.
