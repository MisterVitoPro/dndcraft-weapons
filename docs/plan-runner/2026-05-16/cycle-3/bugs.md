# plan-runner Bug Report (Cycle 3)
**Date:** 2026-05-16
**Cycle:** 3
**Source plan:** D:/minecraft/mods/dnd-weapons/docs/superpowers/plans/2026-05-16-dnd-weapons-phase-2a-versions-and-kotlin.md

## Resolved inline during pipeline

The following 4 bugs from wave 7 were detected during smoke testing and inline-fixed by the orchestrator before continuing to subsequent waves:

- **wave-7-agent-1-bug-1** (P1) — Gradle wrapper version 8.10 too old; bumped to 8.12
- **wave-7-agent-1-bug-2** (P1) — FLK version 1.13.5+kotlin.2.0.21 not on Maven; bumped to 1.13.11+kotlin.2.3.21
- **wave-7-agent-1-bug-3** (P0) — Yarn attribute field rename; GENERIC_ATTACK_DAMAGE → ATTACK_DAMAGE, GENERIC_ATTACK_SPEED → ATTACK_SPEED
- **wave-7-agent-1-bug-4** (P0) — Item.Settings registryKey() sequencing; orchestrator reordered settings construction

## Summary (unresolved bugs)
- P0: 0
- P1: 4
- P2: 1
- P3: 1
- **Total unresolved: 6**

## P1 Bugs

### wave-11-agent-1-bug-1
**Stonecutter chiseledBuild fails to switch between MC versions**

**Wave/Agent:** wave-11 agent-1  
**Category:** missing_requirement  
**File:** settings.gradle.kts

**Expected:** chiseledBuild produces 5 jars; sibling subprojects compile the shared src/main/kotlin/ tree with //? directives applied

**Evidence:**
```
java.lang.Exception: Failed to switch from 1.21.4 to 1.21.1 (and similar for 1.20.1).
Direct :sub:build commands succeed structurally but show NO-SOURCE on every Kotlin task 
because the chiseledBuild mechanism that distributes processed source to sibling subprojects is broken.
```

**Suggested fix:** Investigate Stonecutter 0.6's kotlinController=true behavior with our //? directive placement. Possible workarounds: (a) try Stonecutter 0.7+, (b) simplify //? blocks to avoid nested imports inside /* */, (c) fall back to git-branches-per-version per the design doc Section 7.

---

### wave-11-agent-1-bug-2
**MC 26.1.2 requires Java 25 but build JDK is 21**

**Wave/Agent:** wave-11 agent-1  
**Category:** incorrect_implementation  
**File:** versions/26.1.2/gradle.properties:6

**Expected:** 26.1.2 subproject builds on the system's installed JDK

**Evidence:**
```
IllegalStateException: Minecraft 26.1.2 requires Java 25 but Gradle is using 21
```

**Suggested fix:** Install JDK 25 on the build machine; set versions/26.1.2/gradle.properties java_release=25; verify Kotlin 2.3.21 supports jvmTarget=25. Defer to Phase 2a-2 if JDK 25 not available.

---

### wave-11-agent-1-bug-3
**Fabric Loom 1.10-SNAPSHOT does not support MC 1.21.11 unpick format**

**Wave/Agent:** wave-11 agent-1  
**Category:** incorrect_implementation  
**File:** build.gradle.kts:5

**Expected:** Loom version supporting 1.21.11 intermediary/unpick format

**Evidence:**
```
UnsupportedOperationException: Unsupported unpick version (during :1.21.11 configuration)
```

**Suggested fix:** Bump Loom to 1.11-SNAPSHOT (or newer) globally, or use a Stonecutter directive on the Loom plugin version for MC >= 1.21.11. Verify all 5 versions' compatibility with chosen Loom release.

---

### wave-11-agent-1-bug-5
**Plan-specified FLK version for 1.20.1 (1.10.20+kotlin.2.0.21) does not exist on Maven**

**Wave/Agent:** wave-11 agent-1  
**Category:** incorrect_implementation  
**File:** versions/1.20.1/gradle.properties:5

**Expected:** Valid FLK Maven coordinate for 1.20.1

**Evidence:**
```
Maven 404. The 1.10.x FLK line ships only with Kotlin 1.9.x bundled stdlib; 
2.0+ requires FLK 1.11.x or 1.13.x which don't support MC 1.20.1.
```

**Suggested fix:** Corrected to 1.10.20+kotlin.1.9.24 inline (latest published 1.10.x). Note: this Kotlin 1.9.24 FLK stdlib may conflict at runtime with the Kotlin 2.3.21 stdlib our Kotlin compiler bundles. Validate at runtime before declaring 1.20.1 fully supported.

---

## P2 Bugs

### wave-7-agent-1-bug-5
**Longsword recipe JSON fails to parse on 1.21.4 (missing fabric:type discriminator)**

**Wave/Agent:** wave-7 agent-1  
**Category:** incorrect_implementation  
**File:** src/main/resources/data/dndweapons/recipe/longsword.json

**Expected:** Recipe loads cleanly on MC 1.21.4 datapack reload

**Evidence:**
```
Server log: 'Map entry I : Input does not contain a key [fabric:type]: MapLike[{tag:c:ingots/iron}]'
```

**Suggested fix:** Either upgrade the ingredient schema to include fabric:type discriminator, or use raw vanilla minecraft:ingredient form. Address in Wave 11 (final verification) once full schema is confirmed via runtime testing.

---

## P3 Bugs

### wave-11-agent-1-bug-4
**Two MC versions removed from settings.gradle.kts to allow other versions to configure**

**Wave/Agent:** wave-11 agent-1  
**Category:** missing_requirement  
**File:** settings.gradle.kts:18

**Expected:** All 5 versions declared per design spec

**Evidence:**
```
versions("1.20.1", "1.21.1", "1.21.4") — 26.1.2 and 1.21.11 omitted; 
gradle configures all subprojects upfront so one bad subproject blocks every task
```

**Suggested fix:** Re-add 26.1.2 and 1.21.11 after their respective blockers (bugs 2 and 3) are resolved.

---

## Notes

- Wave 7 smoke testing caught 5 bugs (4 P0/P1 inline-fixed, 1 P2 deferred)
- Wave 11 final verification discovered 5 more blockers during chiseledBuild / cross-version testing
- Kotlin conversion itself is complete: 1.21.4 builds and tests pass end-to-end
- Multi-version expansion (5 MC versions) reveals deeper toolchain constraints than anticipated
