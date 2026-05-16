# Phase 2a Verification

**Date:** 2026-05-16 14:31:43

## Build / Test / Gametest matrix

| Subproject | :build | :test | :runGametest |
|---|---|---|---|
| 1.20.1 | PASS (no Kotlin source picked up — see Notes) | NO-SOURCE | NOT-RUN |
| 1.21.1 | PASS (no Kotlin source picked up — see Notes) | NO-SOURCE | NOT-RUN |
| 1.21.4 | PASS | PASS (7 tests: 4 WeaponSpecTest + 3 WeaponsTest) | PASS (`longswordIsRegistered`) |
| 26.1.2 | BLOCKED (removed from settings.gradle.kts) | n/a | n/a |
| 1.21.11 | BLOCKED (removed from settings.gradle.kts) | n/a | n/a |

## Phase 2a status

**5 of 15 cells pass; 4 of 5 versions blocked by issues outside the original plan's scope.** The Kotlin conversion itself (the core scope of Phase 2a) is fully validated on 1.21.4: code compiles, unit tests pass, gametest passes, mod loads. The multi-version expansion turned out to be substantially deeper than the design doc anticipated — see Notes for each blocker. Recommend shipping the Kotlin conversion now and treating multi-version support as a follow-up phase (Phase 2a-2) addressing each blocker individually.

## Notes per failed / blocked cell

### 1.20.1 / 1.21.1: Stonecutter chiseledBuild source distribution broken

**Root cause:** Stonecutter's `setupChiseledBuild` task fails with `java.lang.Exception: Failed to switch from 1.21.4 to <version>` when run across all subprojects. Running `:1.20.1:build` or `:1.21.1:build` directly produces `BUILD SUCCESSFUL` but with every Kotlin source-set task showing `NO-SOURCE` — the sibling subprojects never see the shared `src/main/kotlin/` tree because the `chiseledBuild` mechanism that copies + applies `//?` directives is broken.

**Output tail:**
```
* What went wrong:
Execution failed for task ':1.21.1:setupChiseledBuild'.
> java.lang.Exception: Failed to switch from 1.21.4 to 1.21.1
```

**Recommended fix:**
- Investigate Stonecutter 0.6's `kotlinController = true` interaction with our `//?` directive placement in `AttributeCompat.kt` and `DndWeaponItem.kt`. The error message suggests a state-machine issue in the controller when files contain directive blocks.
- Possible workaround: try Stonecutter 0.7+ (if released) or revert to plain text directive blocks without nested Kotlin imports inside `/*` blocks.
- Fallback: per the Phase 2a design (Section 7), if Stonecutter blocks at structural level, fall back to git-branches-per-version (one branch per MC, no shared `src/`).

### 26.1.2: Requires Java 25, build JDK is 21

**Root cause:** `Failed to setup Minecraft, java.lang.IllegalStateException: Minecraft 26.1.2 requires Java 25 but Gradle is using 21`. MC 26.1+ moved to Java 25 as its runtime requirement. The plan's "Java 21 for everything except 1.20.1" matrix is wrong for 26.1.x — that version line needs Java 25.

**Recommended fix:**
- Install JDK 25 (Adoptium / Corretto / Temurin) on the build machine.
- Add `versions/26.1.2/gradle.properties` `java_release=25` (already set to 21; change to 25).
- Update `build.gradle.kts` Kotlin `jvmTarget` to handle 25 — Kotlin 2.3.21 may not yet support `jvmTarget = "25"`; verify Kotlin compiler compatibility.
- This is a Java/Kotlin toolchain upgrade, not a quick fix. Defer to Phase 2a-2.

### 1.21.11: Loom unpick incompatibility

**Root cause:** `Failed to setup Minecraft, java.lang.UnsupportedOperationException: Unsupported unpick version`. Fabric Loom 1.10.x doesn't understand 1.21.11's intermediary/unpick format. The Loom version pinned in Phase 1 (`1.10-SNAPSHOT`) predates 1.21.11.

**Recommended fix:**
- Bump Loom to `1.11-SNAPSHOT` (or newer) for 1.21.11 specifically.
- Either via Stonecutter directive on the Loom version in `build.gradle.kts` (selecting 1.11 when MC >= 1.21.11), or by upgrading Loom globally if all other versions support 1.11.
- Verify all 5 versions' compatibility with the chosen Loom release before committing.
- Defer to Phase 2a-2.

## Inline fixes applied during this verification

In addition to the 4 fixes from Wave 7's smoke test (gradle 8.12, FLK 1.13.11, ATTACK_DAMAGE rename, Settings.registryKey), this wave applied:

5. **FLK for 1.20.1:** `1.10.20+kotlin.2.0.21` (plan default — doesn't exist on Maven) → `1.10.20+kotlin.1.9.24` (latest published 1.10.x release on Fabric Maven; bundles Kotlin 1.9 stdlib).

6. **settings.gradle.kts scope reduction:** versions list cut from 5 to 3 (removed `26.1.2` and `1.21.11`) so the configuration phase can resolve. Both are documented above for Phase 2a-2.

## Acceptance vs plan

The Phase 2a design Section 6 lists 14 acceptance criteria. Status:

| # | Criterion | Status |
|---|---|---|
| 1 | Phase 1 Java tree removed | PASS |
| 2 | Equivalent .kt files exist with same identifiers | PASS |
| 3 | WeaponSpec is a Kotlin data class with init-block validation | PASS |
| 4 | Weapons is a Kotlin object | PASS |
| 5 | fabric.mod.json uses Kotlin entrypoint adapter | PASS |
| 6 | build.gradle.kts applies Kotlin plugin + FLK dep | PASS |
| 7 | All 5 versions/<mc>/gradle.properties files exist | PASS (all 5 created; 26.1.2 + 1.21.11 not actively in settings) |
| 8 | settings.gradle.kts lists all 5 versions | PARTIAL (3 of 5 active; 26.1.2 + 1.21.11 deferred) |
| 9 | AttributeCompat.kt with //? directives | PASS |
| 10 | Per-version recipe overlay for 1.20.1 | PASS |
| 11 | chiseledBuild succeeds | FAIL (Stonecutter state-machine error) |
| 12 | chiseledTest succeeds | FAIL (depends on chiseledBuild) |
| 13 | chiseledRunGametest succeeds | FAIL (depends on chiseledBuild) |
| 14 | Master design doc code examples are Kotlin | PASS (verified pre-Phase 2a) |

**10 of 14 criteria pass.** The 4 fails (criteria 8, 11, 12, 13) all trace back to Stonecutter chiseledBuild + per-version Loom/Java requirements, which need their own focused investigation rather than incremental fixes.
