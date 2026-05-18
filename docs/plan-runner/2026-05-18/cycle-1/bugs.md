# plan-runner Bug Report
**Date:** 2026-05-18
**Cycle:** 1
**Source plan:** docs/superpowers/plans/2026-05-18-dnd-weapons-phase-5-acquisition.md

## Summary
- P0: 2
- P1: 0
- P2: 0
- P3: 0
- Total: 2

## P0 Bugs

### [wave-9-agent-1-bug-1] WeaponLootRegistrar.kt: invalid Stonecutter 0.6 syntax on MOD_VERSION_STRING causes chiseledBuild to fail on all 5 MC versions
**Wave/Agent:** wave-9 agent-1 (task: Full matrix verification + Phase 5 verification doc)
**Category:** broken_existing
**File:** src/main/kotlin/com/dndweapons/loot/WeaponLootRegistrar.kt:44
**Expected:** Stonecutter 0.6 parses MOD_VERSION_STRING fork correctly so chiseledBuild succeeds on all 5 versions (1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2). This was the acceptance criterion for wave-3-agent-2: 'File compiles on all 5 MC versions with chiseledBuild'.
**Evidence:**
```
    private const val MOD_VERSION_STRING: String =
        //? if >=1.21.11 { /*"1.21.11"*///?}
        //? if (>=1.21.4) & (<1.21.11) { /*"1.21.4"*///?}
        //? if (>=1.21.1) & (<1.21.4) { /*"1.21.1"*///?}
        //? if <1.21.1 {
        "1.20.1"
        //?}
```
**Suggested fix:** Replace the inline `/*"value"*///?}` pattern with a multi-line else-ladder that Stonecutter 0.6 accepts:

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

Each conditional branch must be on its own line; the block-comment value token must appear on a separate line after the opening directive, and the close `//?}` must be on its own line before the next else-if. The combined `/*value*///?}` token on a single line is not valid Stonecutter 0.6 syntax.

---

### [wave-9-agent-1-bug-2] chiseledTest and chiseledRunGametest not run: Phase 5 test results entirely unverified across all 5 MC versions
**Wave/Agent:** wave-9 agent-1 (task: Full matrix verification + Phase 5 verification doc)
**Category:** missing_requirement
**File:** docs/superpowers/plans/phase-5-verification-final.md
**Expected:** Verification matrix must show GREEN for all 5 MC versions across chiseledBuild, chiseledTest, and chiseledRunGametest. The WeaponLootRegistrar.kt parse error prevents any of this from being verified. Status is HOLD, not GREEN.
**Evidence:**
```
| 1.20.1  | FAIL (parse error) | NOT RUN | NOT RUN |
| 1.21.1  | FAIL (parse error) | NOT RUN | NOT RUN |
| 1.21.4  | FAIL (parse error) | NOT RUN | NOT RUN |
| 1.21.11 | FAIL (parse error) | NOT RUN | NOT RUN |
| 26.1.2  | FAIL (parse error) | NOT RUN | NOT RUN |
```
**Suggested fix:** Fix the Stonecutter syntax in WeaponLootRegistrar.kt (see bug-1), then re-run `./gradlew chiseledBuild && ./gradlew chiseledTest && ./gradlew chiseledRunGametest` and update the verification doc with actual pass/fail results before promoting to GREEN status or applying the `phase-5-acquisition` git tag.
