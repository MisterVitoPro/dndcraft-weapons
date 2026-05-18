# Phase 3 Verification — Final (all GREEN)

**Date:** 2026-05-17
**Branch / commit:** main / `0a17796`
**Verified locally via:** `./gradlew chiseledBuild chiseledTest chiseledRunGametest` across all 5 MC versions

## Top-line result

| Check | Versions | Status |
|---|---|---|
| `./gradlew chiseledBuild` | 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2 | GREEN |
| `./gradlew chiseledTest` | 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2 | GREEN |
| `./gradlew chiseledRunGametest` | 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2 | GREEN (55/55 mod gametests + 2 vanilla `minecraft:always_pass` on 1.21.11/26.1.2) |

Per-version gametest tally:
- 1.20.1: 11 / 11
- 1.21.1: 11 / 11
- 1.21.4: 11 / 11
- 1.21.11: 12 / 12 (includes `minecraft:always_pass`)
- 26.1.2: 12 / 12 (includes `minecraft:always_pass`)

## 14 spec acceptance criteria

| # | Criterion | Status | Evidence |
|---|---|---|---|
| 1 | SpecRegistry exists with expected API | GREEN | `SpecRegistry.kt` unchanged from cycle 2; lookups exercised by 11 gametests per version |
| 2 | WeaponAttackHandler.modifyDamage correct | GREEN | `chiseledTest` green across all 5 versions |
| 3 | PlayerAttackMixin injects on every version | GREEN | Mixin observed firing via `combathooksgametest.*` results: rapier+sprint dealt 7.2, lance on foot dealt 3.5, light dual dealt 5.0 — all matching the mixin's modifyDamage formula |
| 4 | AttributeCompat emits ATTACK_KNOCKBACK for Heavy | GREEN | `combathooksgametest.heavyknockbackbonusapplies` passes on all 5 versions |
| 5 | Tooltip injection on registered items | GREEN | `tooltipinjectiongametest.registereditemtooltipcontainsstatblock` passes on all 5 versions |
| 6 | Tooltip injection on vanilla iron sword | GREEN | `tooltipinjectiongametest.vanillaironswordtooltipcontainsstatblock` passes on all 5 versions (relies on the `dndweapons:role/shortsword` item tag, which now lives at both `tags/items/` and `tags/item/` data paths) |
| 7 | Finesse on registered item | GREEN | `combathooksgametest.finessesprintbonusfires` passes on all 5 versions; rapier dealt 7.2 (= 6 * 1.20) |
| 8 | Finesse on vanilla item | GREEN | `combathooksgametest.vanillaironswordcarriesfinessehook` passes on all 5 versions |
| 9 | Light dual-wield | GREEN | `combathooksgametest.lightdualwieldbonusfires` passes; iron sword + dagger offhand dealt base+1 |
| 10 | Versatile offhand-empty | GREEN | `combathooksgametest.versatileemptyoffhandfires` passes; longsword empty offhand dealt base+1 |
| 11 | Lance on-foot halves, mounted does not | GREEN | `combathooksgametest.lanceonfoothalves` and `lancemountedfulldamage` both pass on all 5 versions |
| 12 | All 5 MC versions pass build/test/gametest | GREEN | See top-line table |
| 13 | `en_us.json` keys present | GREEN | All 8 required keys present with `tooltip.dndweapons.` prefix; audited in cycle-3 wave-1 |
| 14 | Phase 1 + Phase 2 gametests still pass | GREEN | `registrationgametest.longswordisregistered` passes; no regressions |

**Tally: 14 GREEN / 0 YELLOW / 0 RED.** Phase 3 is shippable.

## Notable fixes between cycle-3 doc and this one

The cycle-3 verification doc reported 4 GREEN / 2 YELLOW / 8 RED with the gametest infrastructure intentionally not exercised. After running the gradle matrix locally we found four orthogonal bugs that the plan-runner pipeline couldn't catch (it never invokes gradle):

1. `loom { accessWidenerPath }` used a subproject-relative path. Stonecutter's subproject root is `versions/<v>/`, so loom resolved the widener at `versions/<v>/src/main/resources/...` and threw `FileNotFoundException` on configuration. Fix: anchor at `rootProject.file(...)`. — commit 26eccbb
2. The widener targeted `Player.attackStrengthTicker` but the field is declared on `LivingEntity`. Validator failed on every version. Fix: widener on `LivingEntity` + helper function with a `LivingEntity` parameter so Kotlin emits `PUTFIELD LivingEntity` rather than `PUTFIELD Player`. — commit 26eccbb
3. `GameTestHelper.makeMockPlayer` returns a Player whose AttributeMap has no equipment modifiers — `setItemInHand` only updates the inventory; weapon ATTACK_DAMAGE is normally folded in by `LivingEntity.aiStep -> collectEquipmentChanges` on the next tick. Apply held-item modifiers explicitly. — commit 26eccbb
4. `WeaponTooltipBuilder` puts the damage type as an arg of the `stat_block` line (rendered client-side via `Component.translatable`). The test asserted on `translationKey` only. Widen the check to scan string args. — commit 0a17796
5. MC 1.21 moved data-pack item tags from `data/.../tags/items/` (plural) to `data/.../tags/item/` (singular). The 4 role tags only existed at the plural path so iron sword never bound to the SHORTSWORD spec on 1.21.x. Mirror to both paths. — commit 0a17796
6. The cycle-4 sweep test used rapier + sprint; vanilla `Player.attack` disables the sweep loop entirely while sprinting. DnD weapons extend `Item`, not `SwordItem`, so they never trigger vanilla sweep anyway. Switch to iron sword + dagger offhand (LIGHT+LIGHT → +1 deterministic bonus) with sprint disabled. — commit 0a17796
7. The mock player spawns at world (0.5, 1.0, 0.5) but `ctx.spawn(EntityType, BlockPos)` resolves through the test structure's origin, which can be millions of blocks away. Single-hit tests work because `player.attack(target)` calls `Entity.hurt` directly with no range check, but vanilla sweep does `level.getEntitiesOfClass` around the PLAYER's bounding box and finds nothing. Teleport the player to `primary.pos + (0,0,1)` before attacking. — commit 0a17796

## Tag command

Once this doc is committed alongside the fixes:

```
git tag phase-3-combat-hooks
git push origin phase-3-combat-hooks    # manual; CI does not push tags
```

## Audit details (criterion 13)

All 8 required translation keys are present in `src/main/resources/assets/dndweapons/lang/en_us.json` (all under the `tooltip.dndweapons.` prefix):

- `tooltip.dndweapons.stat_block`
- `tooltip.dndweapons.damage_type.slashing`
- `tooltip.dndweapons.damage_type.piercing`
- `tooltip.dndweapons.damage_type.bludgeoning`
- `tooltip.dndweapons.bonus.finesse_sprint`
- `tooltip.dndweapons.bonus.light_dual`
- `tooltip.dndweapons.bonus.versatile_empty`
- `tooltip.dndweapons.bonus.lance_foot`

No additions needed.
