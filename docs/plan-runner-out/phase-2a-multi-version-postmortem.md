# Phase 2a Multi-Version Postmortem

**Date:** 2026-05-16
**Outcome:** Multi-version expansion blocked; Phase 2a ships with 1.21.4 only (Kotlin conversion fully validated).

## What worked

- Kotlin conversion from Phase 1 Java (all source files, tests, gametest) — fully validated on 1.21.4.
- Gradle wrapper bump 8.10 → 8.12 → 8.14.
- Fabric Loom bump 1.10-SNAPSHOT → 1.11-SNAPSHOT.
- Kotlin plugin + FLK runtime dependency wiring (Kotlin 2.3.21 + FLK 1.13.11+kotlin.2.3.21).
- fabric.mod.json Kotlin entrypoint adapter.
- 1.21.4 yarn renames: `EntityAttributes.ATTACK_DAMAGE`/`ATTACK_SPEED` (not `GENERIC_*`).
- 1.21.4 runtime requirement: `Item.Settings.registryKey()` before Item construction.

## Why multi-version got blocked

Multiple compounding issues, each requiring deeper investment than an agent cycle can deliver:

### 1. Stonecutter directive parsing is fragile (the deepest blocker)

Cycle 8 added `//? if MC >= 1.21 { ... //?} else { /* ... */ //?}` directives to `AttributeCompat.kt` and `DndWeaponItem.kt`. After cycle 4's "directive normalization" altered the syntax, every Stonecutter operation that reads the directives — including `Set active project to X`, `Refresh active project`, and `chiseledBuild` — errors with `java.lang.Exception: Failed to switch from <X> to <Y>`. The underlying parse exception is swallowed; only the generic wrapper surfaces.

Stripping all `//?` directives from those two files **immediately unblocks** Stonecutter switching. The directives ARE the issue, but isolating which character / line trips the parser would require either:
- Running Stonecutter's `StonecutterTask.run` under a debugger to see the swallowed cause, or
- Reading the Stonecutter 0.6 source (`dev.kikugie.stonecutter.process.StonecutterTask` line 70) and reverse-engineering the directive grammar.

Neither is feasible mid-session.

### 2. Real per-version API differences need directives

Once directives are stripped, per-version compile errors surface:

| MC version | API gap |
|---|---|
| 1.21.1 | `EntityAttributes.ATTACK_DAMAGE` / `_SPEED` don't exist; need `GENERIC_*` prefix. `Item.Settings.registryKey()` doesn't exist. |
| 1.20.1 | `DataComponentTypes.ATTRIBUTE_MODIFIERS` doesn't exist; need UUID-keyed `EntityAttributeModifier` returned via `Item.getAttributeModifiers(EquipmentSlot)` override. |
| 26.1.2 | Requires Java 25 runtime; build JDK is 21 (JDK 25 not installed). Also moved to Mojang mappings. |
| 1.21.11 | Loom 1.11 fabric-api remap fails with `Javadoc provided by mod (fabric-content-registries-v0) must be have an intermediary source namespace`. Likely needs Loom 1.12+ or a specific fabric-api variant. |

Without Stonecutter directives working, there's no way to express these differences in one shared source tree.

### 3. JDK 25 / Loom 1.12+ / Stonecutter debug — all external

Each remaining blocker is an external prerequisite, not an agent task:
- 26.1.2: needs JDK 25 installed on the build host
- 1.21.11: needs newer Loom or a known-good fabric-api version that hasn't been published yet
- Stonecutter: needs reading the library source or upgrading to 0.7+ if/when released

## Recommended next steps (for whoever picks up Phase 2a-3)

1. **Investigate Stonecutter 0.6 directive grammar.** Read `dev.kikugie.stonecutter.process.StonecutterTask.kt` line ~70 and surrounding parsing code. Or wait for 0.7 release. The directive syntax we used follows the published examples but apparently triggers an internal parse failure.

2. **Once directives parse, the per-version API matrix is documented above.** Each MC version's `AttributeCompat.kt` / `WeaponRegistrarImpl.kt` branch is small (~20 lines) and the necessary code shapes are known.

3. **External prereqs to unblock 26.1.2 and 1.21.11.** Install JDK 25 (Adoptium / Corretto). For 1.21.11, wait for fabric-api ecosystem to stabilize (Loom 1.12+ or known-good fabric-api version).

4. **Alternative: drop Stonecutter, use git branches per MC version.** The Phase 2a design (Section 7) explicitly listed this fallback. One MC version per branch, full source duplication, no `//?` magic. Higher maintenance cost but predictable.

## What ships in Phase 2a

- `src/main/kotlin/` tree (DndWeaponsMod object, Weapons + WeaponSpec data class, DndWeaponItem, WeaponRegistrar + Impl, AttributeCompat, RegistrationGametest)
- `src/test/kotlin/` tree (WeaponSpecTest, WeaponsTest)
- `build.gradle.kts` with Kotlin 2.3.21 plugin + FLK + Loom 1.11
- `gradle/wrapper/` pinned to Gradle 8.14
- `versions/1.21.4/gradle.properties` only
- `settings.gradle.kts` declaring 1.21.4 only
- Longsword: registered, recipe loads, gametest passes, mod boots in client

Phase 2a's "5 MC versions" goal is **deferred to Phase 2a-3** pending the unblockers above. Phase 2b (full weapon catalog on 1.21.4) is unblocked and can proceed immediately.
