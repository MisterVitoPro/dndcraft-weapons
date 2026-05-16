# plan-runner Bug Report
**Date:** 2026-05-16
**Cycle:** 4
**Source plan:** D:/minecraft/mods/dnd-weapons/docs/plan-runner/2026-05-16/cycle-3/fix-plan.md

## Summary
- P0: 0
- P1: 3
- P2: 3
- P3: 2
- Total: 8

## P0 Bugs

(none)

## P1 Bugs

### [wave-1-agent-1-bug-1] Residual inconsistent //? directive spacing in build.gradle.kts not normalized
**Wave/Agent:** wave-1 agent-1 (task: Resolve Stonecutter chiseledBuild switching error)
**Category:** missing_requirement
**File:** build.gradle.kts:35
**Expected:** Directive markers `//?}` and `//?} else {` should be normalized to `//? }` and `//? } else {` to match the canonical Stonecutter form applied in AttributeCompat.kt / DndWeaponItem.kt; inconsistent spacing inside the central script is a candidate trigger for the controller 'Failed to switch from X to Y' error.
**Evidence:**
```
//? if MC >= 26.1 {
/*mappings(loom.officialMojangMappings())*/
//?} else {
mappings("net.fabricmc:yarn:...")
//?}
```
**Suggested fix:** Normalize lines 33-37 of build.gradle.kts to use `//? }` (with space) consistently. Note build.gradle.kts was not in agent-1's owned_files but IS the central script for kotlinController; this needs to be addressed in a follow-up wave or by widening agent-1's scope.

### [wave-1-agent-1-bug-2] chiseledBuild fix not verified end-to-end
**Wave/Agent:** wave-1 agent-1 (task: Resolve Stonecutter chiseledBuild switching error)
**Category:** incorrect_implementation
**File:** src/main/kotlin/com/dndweapons/compat/AttributeCompat.kt:67
**Expected:** AC2 'Stonecutter does not error with Failed to switch from X to Y' — must be empirically verified by running `./gradlew chiseledBuild` and observing no switch errors.
**Evidence:**
```
Directive spacing normalized from `//?}` to `//? }` on lines 67 and 120, plus DndWeaponItem.kt line 26. Dev agent could not execute chiseledBuild from build environment.
```
**Suggested fix:** Run `./gradlew clean chiseledBuild` after wave commit; if errors persist, apply fallback per agent concerns: split AttributeCompat into per-epoch source files under versions/<mc>/src/main/kotlin/, removing all //? directives from the shared file.

### [wave-2-agent-1-bug-1] Dev agent BLOCKED: JDK 25 not installed on build machine
**Wave/Agent:** wave-2 agent-1 (task: Enable MC 26.1.2 support with JDK 25)
**Category:** missing_requirement
**File:** versions/26.1.2/gradle.properties
**Expected:** JDK 25 installed, :26.1.2:build succeeds with BUILD SUCCESSFUL, Kotlin 2.3.21 compiles to jvmTarget=25 without warnings.
**Evidence:**
```
Dev agent could not satisfy AC1 'JDK 25 installed on build machine' because the build environment has JDK 21 only; orchestrator constraint forbids installing JDK 25 in this cycle.
```
**Suggested fix:** Defer MC 26.1.2 build verification to Phase 3 per plan Notes section. Track JDK 25 install as a prerequisite ticket. In the interim, consider scoping settings.gradle.kts to exclude 26.1.2 via a feature flag (e.g., -Pinclude26=true) or document the expected failure in README.

## P2 Bugs

### [wave-1-agent-2-bug-1] Loom 1.11-SNAPSHOT availability and backward compatibility unverified
**Wave/Agent:** wave-1 agent-2 (task: Upgrade Fabric Loom to support MC 1.21.11)
**Category:** incorrect_implementation
**File:** build.gradle.kts:5
**Expected:** AC2 ':1.21.11:build succeeds (unpick format recognized)' and AC3 'All 5 subprojects configure without UnsupportedOperationException' — both require empirical configuration check on each version.
**Evidence:**
```
id("fabric-loom") version "1.11-SNAPSHOT"  // upgraded from 1.10-SNAPSHOT
```
**Suggested fix:** Run `./gradlew :1.20.1:tasks :1.21.1:tasks :1.21.4:tasks :1.21.11:tasks` and confirm each subproject configures. If Loom 1.11 fails on legacy versions, gate plugin version with Stonecutter directive per plan suggestion 5.

### [wave-1-agent-3-bug-1] Kotlin stdlib mismatch on 1.20.1 unresolved at runtime
**Wave/Agent:** wave-1 agent-3 (task: Resolve FLK version mismatch for MC 1.20.1)
**Category:** incorrect_implementation
**File:** versions/1.20.1/gradle.properties:5
**Expected:** AC3 'no ClassCastException or NoSuchMethodError from Kotlin stdlib conflicts' at runtime.
**Evidence:**
```
flk_version=1.10.20+kotlin.1.9.24  // FLK ships Kotlin 1.9.24 stdlib while compiler is 2.3.21
```
**Suggested fix:** Add a constraint to dependencies to force Kotlin 2.3.21 stdlib at the top of the dependency graph for 1.20.1, OR fall back to FLK 1.13.x with Kotlin 1.9.24 compiler scoped to 1.20.1 only via Stonecutter directive. Defer empirical verification until runGametest can be run on 1.20.1.

### [wave-2-agent-2-bug-1] Re-adding 26.1.2 to versions() will fail configuration without JDK 25
**Wave/Agent:** wave-2 agent-2 (task: Re-add removed MC versions to settings.gradle.kts)
**Category:** incorrect_implementation
**File:** settings.gradle.kts:18
**Expected:** AC3 './gradlew tasks -q succeeds and lists subprojects (may show skipped tasks for unresolved bugs, but no configuration errors)' — adding 26.1.2 to the list with no JDK 25 toolchain WILL cause a configuration error, not just skipped tasks.
**Evidence:**
```
versions("1.20.1", "1.21.1", "1.21.4", "1.21.11", "26.1.2") — :26.1.2 subproject will trigger 'Minecraft 26.1.2 requires Java 25 but Gradle is using 21' at configuration time.
```
**Suggested fix:** Either (a) gate 26.1.2 via a project property (e.g., conditional `if (project.findProperty("include26") == "true") versions += "26.1.2"`), OR (b) keep 26.1.2 in the list and add a `java.toolchain { languageVersion.set(JavaLanguageVersion.of(javaRelease)) }` block so Gradle auto-provisions JDK 25 from Adoptium.

## P3 Bugs

### [wave-1-agent-1-bug-3] settings.gradle.kts not updated to declare all 5 versions
**Wave/Agent:** wave-1 agent-1 (task: Resolve Stonecutter chiseledBuild switching error)
**Category:** missing_requirement
**File:** settings.gradle.kts:18
**Expected:** AC1 explicitly: 'chiseledBuild succeeds when settings.gradle.kts declares all 5 versions'.
**Evidence:**
```
versions("1.20.1", "1.21.1", "1.21.4")  // 26.1.2 and 1.21.11 still omitted at wave-1 time
```
**Suggested fix:** Resolved in wave-2-agent-2 commit — settings.gradle.kts now declares all 5 versions. Bug retained for audit trail.

### [wave-2-agent-2-bug-2] 1.21.11 added to versions() without confirming Loom 1.11-SNAPSHOT availability
**Wave/Agent:** wave-2 agent-2 (task: Re-add removed MC versions to settings.gradle.kts)
**Category:** incorrect_implementation
**File:** settings.gradle.kts:18
**Expected:** AC3 './gradlew tasks -q succeeds' on the full 5-version set.
**Evidence:**
```
versions("1.20.1", "1.21.1", "1.21.4", "1.21.11", "26.1.2") — :1.21.11 depends on the Loom 1.11 bump (wave-1-agent-2) which is unverified.
```
**Suggested fix:** Verify Loom 1.11-SNAPSHOT resolution by running `./gradlew :1.21.11:tasks --refresh-dependencies` after wave 2 commit. If Loom 1.11 is not yet published, pin to last-known-good (1.10) and revisit when 1.11 lands.
