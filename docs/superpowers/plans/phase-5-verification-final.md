# Phase 5 Verification — Acquisition (Loot, Trades, Mob Drops)

**Date:** 2026-05-18
**Status:** HOLD

## Top-line results

| Version | chiseledBuild | chiseledTest | chiseledRunGametest |
|---|---|---|---|
| 1.20.1  | FAIL (parse error) | NOT RUN | NOT RUN |
| 1.21.1  | FAIL (parse error) | NOT RUN | NOT RUN |
| 1.21.4  | FAIL (parse error) | NOT RUN | NOT RUN |
| 1.21.11 | FAIL (parse error) | NOT RUN | NOT RUN |
| 26.1.2  | FAIL (parse error) | NOT RUN | NOT RUN |

## Commands run

```
./gradlew chiseledBuild
```

Exit code: 1. All 5 versions failed at the `setupChiseledBuild` step before any compilation occurred.
`chiseledTest` and `chiseledRunGametest` were NOT run because the build step failed.

## Failure root cause

All 5 versions fail at `setupChiseledBuild` (the Stonecutter source-transform step) with the error:

```
Failed to parse D:\minecraft\mods\dnd-weapons\src\main\kotlin\com\dndweapons\loot\WeaponLootRegistrar.kt
    Code comment not properly closed:
    ? if >=1.21.11 { /*"1.21.11"*///?}
                     ~~~~~~~~~~~~~~~~~
    Expected comment to end:
    ? if >=1.21.11 { /*"1.21.11"*///?}
                     ~
    Unknown token:
    ? if (>=1.21.4) & (<1.21.11) { /*"1.21.4"*///?}
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    Unexpected token:
    ? if (>=1.21.4) & (<1.21.11) { /*"1.21.4"*///?}
    ...
```

**File:** `src/main/kotlin/com/dndweapons/loot/WeaponLootRegistrar.kt`, lines 44-49

**Problematic code:**
```kotlin
private const val MOD_VERSION_STRING: String =
    //? if >=1.21.11 { /*"1.21.11"*///?}
    //? if (>=1.21.4) & (<1.21.11) { /*"1.21.4"*///?}
    //? if (>=1.21.1) & (<1.21.4) { /*"1.21.1"*///?}
    //? if <1.21.1 {
    "1.20.1"
    //?}
```

**Explanation:** Stonecutter 0.6 uses a specific syntax for "true branch is in a block comment" form.
The pattern `/*"1.21.11"*///?}` attempts to combine a block comment containing the active string
with an inline `//? }` close, but Stonecutter's parser does not recognise `/*...*///?}` as a
valid composite token. When the `/*"1.21.11"*/` block comment opens, the parser expects a
standard `*/` close followed by a newline before any further `//? ...` directive — the `//`
after `*/` is instead parsed as starting an unknown-token sequence.

The valid Stonecutter pattern to put string literals in a commented branch looks like:
```kotlin
private const val MOD_VERSION_STRING: String =
    //? if >=1.21.11 {
    /*"1.21.11"*/
    //?} else if (>=1.21.4) & (<1.21.11) {
    /*"1.21.4"*/
    //?} else if (>=1.21.1) & (<1.21.4) {
    /*"1.21.1"*/
    //?} else {
    "1.20.1"
    //?}
```

This is a Phase 5 implementation defect. No earlier phase file uses this pattern so it was not
caught by prior-phase verification.

## New artifacts in Phase 5

- 1 catalog file (~250 lines)
- 4 acquisition data class / helper files (StructureLoot, MobDrop, VillagerTradeEntry, WeaponLookup)
- 1 loot registrar (~250 lines, 5-fork stonecutter) — **contains parse error**
- 1 Wither trophy handler
- 1 trade registrar (~200 lines, 1.20.1-1.21.11 only)
- 1 codegen main + 9 trade JSONs (26.1.2 only)
- 1 gametest class with 3 @GameTest methods (smoke tests)
- 1 unit test class with 7 tests (catalog integrity)
- 2 modified files (DndWeaponsMod.kt, fabric.mod.json)

## Test deltas (expected — not verified due to build failure)

- Phase 4 baseline: 13 mod gametests per version, ~12 JUnit unit tests
- Phase 5 adds: 3 mod gametests + 7 JUnit unit tests
- Phase 5 total: 16 mod gametests per version, ~19 JUnit unit tests

## Tag command (run only after all 5 versions GREEN)

```bash
git tag phase-5-acquisition
git push origin phase-5-acquisition
```

**Status: HOLD — do NOT apply tag until `WeaponLootRegistrar.kt` Stonecutter syntax is fixed and all 5 versions pass chiseledBuild + chiseledTest + chiseledRunGametest**

## Known issues / deferrals

1. **WeaponLootRegistrar.kt parse error (blocker):** Lines 44-49 use invalid Stonecutter 0.6 syntax
   for the `MOD_VERSION_STRING` constant. The `/*"value"*///?}` inline comment+close pattern is not
   recognised by the Stonecutter parser. Must be rewritten as a multi-line `//? if ... { / *value* /
   //?} else ...` ladder before any build can succeed.

2. **chiseledTest / chiseledRunGametest not run:** Since all versions fail at the source-transform
   step, neither the unit tests nor the gametests could be executed. Results for those columns will
   need to be captured in a re-verification run after the syntax fix.

3. **No prior-phase regressions confirmed:** Because the build fails before compilation, it is
   unknown whether any prior-phase test regressions were introduced in this wave. The fix to
   `WeaponLootRegistrar.kt` must be applied and then all three gradle tasks re-run.
