# Plan-runner Cycle 5 -- Bug Report

**Plan:** `docs/superpowers/plans/2026-05-18-dnd-weapons-phase-6-wiki.md`
**Date:** 2026-05-18
**Cycle:** 5

## Summary

| Severity | Count |
|----------|-------|
| P0       | 0     |
| P1       | 0     |
| P2       | 1     |
| P3       | 0     |
| **Total**| **1** |

Phase 6 implementation is **GREEN** in the local matrix; one P2 follow-up bug is flagged for the CI
workflow command line.

## Bugs

### wave-6-bug-1 (P2) -- CI workflow command line will fail without Stonecutter wrapper

- **File:** `.github/workflows/wiki-publish.yml:33`
- **Category:** plan_vs_reality_drift
- **Evidence:** Running `./gradlew :1.21.4:generateWiki` directly fails with
  `:1.21.4:compileKotlin` FAILED on `CombatHooksGametest.kt:278` (the per-version `startRiding`
  signature in unprocessed shared source). Stonecutter 0.6 only swaps the source-set srcDirs to
  `chiseledSrc` inside the `chiseled` task wrapper. The CI workflow's
  `run: ./gradlew :1.21.4:generateWiki publishWiki` will hit the same failure mode in CI.
- **Expected:** The CI workflow should invoke a chiseled-aware task so Stonecutter source
  replacement is active during `compileKotlin`.
- **Suggested fix:** Change the CI workflow `run:` line to
  `./gradlew chiseledGenerateWiki :1.21.4:publishWiki`
  -- this uses the new `chiseledGenerateWiki` wrapper (registered in `stonecutter.gradle.kts`
  alongside `chiseledBuild`/`chiseledTest`) which builds the wiki for every fork. Alternatively,
  factor `publishWiki` to consume the chiseled output without re-triggering compileKotlin from
  raw shared sources.

## Concerns (informational; not bugs)

- Plan's `WikiGenTest` used `@TempDir lateinit var tmp: Path` as an instance field with
  `@TestInstance(PER_CLASS)`/`@BeforeAll`. JUnit 5 does not inject the field before `@BeforeAll`
  runs in that lifecycle, so all tests crashed at setup. Fixed in-cycle by switching to
  method-parameter injection: `fun setup(@TempDir tmp: Path)`. All 5 tests now pass on every fork.
- Plan's `build.gradle.kts` snippet used `java.io.File(...)` inline; the Kotlin Gradle DSL could
  not resolve the inline FQN. Fixed in-cycle by adding `import java.io.File`.
- Plan's `WikiTemplates.kt` referenced `Property.SPECIAL`; actual enum variant is
  `Property.SPECIAL_LANCE`. Also missed `RangeKind.SLING` and `RangeKind.BLOWGUN` in
  `labelFor(RangeKind)`. Fixed in-cycle per the plan's own line-443 guidance (inspect actual
  enums, don't invent variants).
- Plan's `./gradlew :1.21.4:generateWiki` verification step does not work standalone in this
  Stonecutter 0.6 layout. The cycle worked around this by registering `chiseledGenerateWiki` and
  using it for verification; the same fix needs to apply to the CI workflow (above bug).

## Acceptance criteria

All Phase 6 acceptance criteria met:

| Criterion                                              | Result                                                                |
|--------------------------------------------------------|-----------------------------------------------------------------------|
| chiseledBuild PASS on all 5 forks                      | PASS                                                                  |
| chiseledTest PASS on all 5 forks (5 WikiGenTest tests) | PASS                                                                  |
| generateWiki produces 38 weapon pages                  | PASS (38 .md files in `versions/1.21.4/build/wiki/Weapons/`)          |
| generateWiki produces 4 category indexes               | PASS (Simple-Melee, Simple-Ranged, Martial-Melee, Martial-Ranged)     |
| generateWiki produces 12 handwritten pages             | PASS (Home.md mixed; 11 other handwritten copied verbatim)            |
| 4 vanilla-mapped callouts                              | PASS (Shortsword, Shortbow, Light-Crossbow, Trident)                  |
| Home.md contains auto-gen header + handwritten body    | PASS                                                                  |

Phase 6 status: **GREEN**.
