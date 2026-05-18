# Phase 5 Verification — Acquisition (Loot, Trades, Mob Drops)

**Date:** 2026-05-18 (re-run after Stonecutter fix in plan-runner cycle 2 wave 1)
**Status:** HOLD

## Top-line results

| Version | chiseledBuild | chiseledTest | chiseledRunGametest |
|---|---|---|---|
| 1.20.1  | FAIL (compile error) | NOT RUN | NOT RUN |
| 1.21.1  | FAIL (compile error) | NOT RUN | NOT RUN |
| 1.21.4  | FAIL (compile error) | NOT RUN | NOT RUN |
| 1.21.11 | FAIL (compile error) | NOT RUN | NOT RUN |
| 26.1.2  | FAIL (compile error) | NOT RUN | NOT RUN |

## Commands run

```
./gradlew chiseledBuild
./gradlew chiseledTest
./gradlew chiseledRunGametest
```

All three commands exit non-zero. After the Stonecutter fix in `WeaponLootRegistrar.kt`,
the `setupChiseledBuild` (source-transform) step succeeds on every version, but the
subsequent `:{version}:compileKotlin` task FAILS on every version because of
pre-existing Kotlin compile errors in three other files that were previously masked
by the Stonecutter parse error.

`chiseledTest` and `chiseledRunGametest` cannot execute because the compile step
fails first.

## Failure root cause

The original Stonecutter parse error in `WeaponLootRegistrar.kt:44-49` has been
fixed in plan-runner cycle 2 wave 1 by replacing the invalid `/*value*///?}` inline
pattern and the unsupported `(>=A) & (<B)` compound-condition form with a pure
single-clause else-ladder:

```kotlin
private const val MOD_VERSION_STRING: String =
    //? if >=1.21.11 {
    /*"1.21.11"*/
    //?} else if >=1.21.4 {
    /*"1.21.4"*/
    //?} else if >=1.21.1 {
    /*"1.21.1"*/
    //?} else {
    "1.20.1"
    //?}
```

**Note:** the original fix-plan suggested compound conditions using `& (...)` syntax;
that form is rejected by Stonecutter 0.6 in this project. No other source file in
`src/` uses compound conditions, so the implicit ordering of the else-ladder is the
idiomatic pattern for this codebase.

With Stonecutter parsing fixed, compilation now reveals 16 distinct unresolved
references across three Phase 5 files that were never compile-validated in any prior
verification pass:

### Affected files (16 distinct unresolved references)

1. `src/main/kotlin/com/dndweapons/loot/WitherTrophyHandler.kt`
   - `ServerLivingEntityEvents` (Fabric API)
   - `spawnAtLocation`, `nextInt`, `type`, lambda parameter type inference failures

2. `src/main/kotlin/com/dndweapons/test/AcquisitionGametest.kt`
   - `GameTest`, `lootData`, `getRandomItems`, `descriptionId`, `parse`, `not`

3. `src/main/kotlin/com/dndweapons/trade/WeaponTradeRegistrar.kt`
   - `VillagerProfession`, `VillagerTrades`, `add`, `get`, `getOffer`, `ResourceLocation`
   - All 5 versions affected; argument-type mismatches with `VillagerProfession!`

These are pre-existing Phase 5 implementation defects that escaped detection in
cycle 1 because the Stonecutter parse error short-circuited the build before
`compileKotlin` ever ran.

## New artifacts in Phase 5

- 1 catalog file (~250 lines)
- 4 acquisition data class / helper files (StructureLoot, MobDrop, VillagerTradeEntry, WeaponLookup)
- 1 loot registrar (~250 lines, 5-fork stonecutter) — Stonecutter syntax now valid
- 1 Wither trophy handler — **compile errors against Fabric API on all 5 versions**
- 1 trade registrar (~200 lines, 1.20.1-1.21.11 only) — **compile errors against villager API on all 5 versions**
- 1 codegen main + 9 trade JSONs (26.1.2 only)
- 1 gametest class with 3 @GameTest methods (smoke tests) — **compile errors**
- 1 unit test class with 7 tests (catalog integrity)
- 2 modified files (DndWeaponsMod.kt, fabric.mod.json)

## Test deltas (expected — still not verified due to compile failure)

- Phase 4 baseline: 13 mod gametests per version, ~12 JUnit unit tests
- Phase 5 adds: 3 mod gametests + 7 JUnit unit tests
- Phase 5 total: 16 mod gametests per version, ~19 JUnit unit tests

## Tag command (run only after all 5 versions GREEN)

```bash
git tag phase-5-acquisition
git push origin phase-5-acquisition
```

**Status: HOLD — Stonecutter parse error is resolved; three Phase 5 files now fail
Kotlin compilation on every MC version. Tag stays unapplied until all 5 versions
pass chiseledBuild + chiseledTest + chiseledRunGametest.**

## Known issues / deferrals

1. **WitherTrophyHandler.kt compile failure (blocker, all 5 versions):** References
   `ServerLivingEntityEvents` from the Fabric API but no matching import is present or
   resolvable on any version. Lambda body uses `entity.type`, `entity.spawnAtLocation(...)`,
   and `entity.random.nextInt(...)` which fail type-inference because the surrounding
   event-registration call could not be resolved. Needs per-version stonecutter-guarded
   imports for the correct Fabric API event type and matching SAM signature.

2. **AcquisitionGametest.kt compile failure (blocker, all 5 versions):** Unresolved
   references to `GameTest`, `lootData`, `getRandomItems`, `descriptionId`, `parse`, `not`.
   These are vanilla Minecraft API names that drift across versions (e.g., `getRandomItems`
   renamed to `getRandomLootItems` in newer versions; `descriptionId` may need
   `getDescriptionId()` accessor; `parse` likely needs `ResourceLocation.tryParse` or
   `ResourceLocation.parse` depending on version). Needs version-fork audit.

3. **WeaponTradeRegistrar.kt compile failure (blocker, all 5 versions):** Lines around
   67-69 reference `VillagerTrades.TRADES.get(profession).add(level, factories)`. The
   `VillagerTrades.TRADES` map keying changed between MC versions (raw map -> registry
   holder -> resource key); the `add(level, factories)` call uses a vararg or list shape
   that varies per version. Needs per-version stonecutter-guarded resolution.

4. **chiseledTest / chiseledRunGametest still cannot run:** Same downstream blocker
   as cycle 1, now for a different root cause (compile failure vs. parse failure).

5. **No prior-phase regressions verified:** Because compile fails, prior-phase tests
   cannot run. A future cycle that resolves the three blockers above must execute
   chiseledTest on all 5 versions and confirm the Phase 1-4 baselines still pass.
