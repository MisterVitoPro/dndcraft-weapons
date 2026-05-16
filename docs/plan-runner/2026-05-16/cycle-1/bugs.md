# plan-runner Bug Report
**Date:** 2026-05-16
**Cycle:** 1
**Source plan:** D:/minecraft/mods/dnd-weapons/docs/superpowers/plans/2026-05-16-dnd-weapons-phase-1-foundation.md

## Summary
- P0: 0
- P1: 2
- P2: 0
- P3: 2
- Total: 4

## P1 Bugs

### W1-001 gradlew shell script not created
**Wave/Agent:** wave-1 agent-2 (task: Task 1: Gradle Bootstrap + Stonecutter Setup)
**Category:** missing_requirement
**File:** D:/minecraft/mods/dnd-weapons/gradlew
**Expected:** Unix shell wrapper script gradlew must exist so CI and developers can invoke Gradle without a pre-installed installation
**Evidence:**
```
ls: cannot access 'D:/minecraft/mods/dnd-weapons/gradlew': No such file or directory
```
**Suggested fix:** Run `gradle wrapper --gradle-version 8.10` in the project root, or manually create gradlew from the standard Gradle wrapper template and make it executable

### W1-002 gradlew.bat Windows batch script not created
**Wave/Agent:** wave-1 agent-2 (task: Task 1: Gradle Bootstrap + Stonecutter Setup)
**Category:** missing_requirement
**File:** D:/minecraft/mods/dnd-weapons/gradlew.bat
**Expected:** Windows batch wrapper gradlew.bat must exist so Windows developers and CI pipelines can invoke Gradle without a pre-installed installation
**Evidence:**
```
ls: cannot access 'D:/minecraft/mods/dnd-weapons/gradlew.bat': No such file or directory
```
**Suggested fix:** Run `gradle wrapper --gradle-version 8.10` in the project root, or manually create gradlew.bat from the standard Gradle wrapper template

## P3 Bugs

### W1-003 gradle-wrapper.jar binary not created
**Wave/Agent:** wave-1 agent-2 (task: Task 1: Gradle Bootstrap + Stonecutter Setup)
**Category:** missing_requirement
**File:** D:/minecraft/mods/dnd-weapons/gradle/wrapper/gradle-wrapper.jar
**Expected:** gradle-wrapper.jar must be present alongside gradle-wrapper.properties for the Gradle wrapper to bootstrap itself
**Evidence:**
```
ls: cannot access 'D:/minecraft/mods/dnd-weapons/gradle/wrapper/gradle-wrapper.jar': No such file or directory
```
**Suggested fix:** Run `gradle wrapper --gradle-version 8.10` in the project root to generate both the JAR and shell scripts; alternatively copy the JAR from any Gradle 8.10 distribution

### W1-004 gradle.properties missing minecraft_version, yarn_mappings, loader_version, and fabric_version properties
**Wave/Agent:** wave-1 agent-2 (task: Task 1: Gradle Bootstrap + Stonecutter Setup)
**Category:** missing_requirement
**File:** D:/minecraft/mods/dnd-weapons/gradle.properties
**Expected:** gradle.properties should include minecraft_version=1.21.4, yarn_mappings=<appropriate yarn build>, loader_version=<fabricloader version >=0.16.0>, and fabric_version=<fabric-api version> so the build resolves dependencies correctly
**Evidence:**
```
gradle.properties contains only modId, modGroup, modVersion, and JVM/parallel flags. build.gradle.kts references property("minecraft_version"), property("yarn_mappings"), property("loader_version"), and property("fabric_version") which will cause a runtime error when Gradle evaluates the dependencies block.
```
**Suggested fix:** Note: This is a downgraded P3 false-positive. The plan deliberately places minecraft_version, yarn_mappings, loader_version, and fabric_version in versions/1.21.4/gradle.properties (created in wave 2 Task 2), not root gradle.properties. Root gradle.properties correctly contains only modId, modGroup, and modVersion. Stonecutter will materialize the per-version properties at build time.
