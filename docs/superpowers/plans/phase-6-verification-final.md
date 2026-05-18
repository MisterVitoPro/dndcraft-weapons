# Phase 6 Verification -- Wiki Generator

**Date:** 2026-05-18
**Status:** GREEN

## Top-line results

| Version | chiseledBuild | chiseledTest | generateWiki (output file counts)                                  |
|---------|---------------|--------------|--------------------------------------------------------------------|
| 1.20.1  | PASS          | PASS         | 42 weapon-dir + 12 handwritten + Home.md mixed (via chiseledGenerateWiki) |
| 1.21.1  | PASS          | PASS         | 42 weapon-dir + 12 handwritten + Home.md mixed (via chiseledGenerateWiki) |
| 1.21.4  | PASS          | PASS         | 42 weapon-dir + 12 handwritten + Home.md mixed (primary target)          |
| 1.21.11 | PASS          | PASS         | 42 weapon-dir + 12 handwritten + Home.md mixed (via chiseledGenerateWiki) |
| 26.1.2  | PASS          | PASS         | 42 weapon-dir + 12 handwritten + Home.md mixed (via chiseledGenerateWiki) |

## Commands run

```
./gradlew chiseledBuild              # BUILD SUCCESSFUL in 5s (54 tasks: 15 executed, 39 up-to-date)
./gradlew chiseledTest               # BUILD SUCCESSFUL (5 forks, 5 WikiGenTest tests each, 0 failures)
./gradlew chiseledGenerateWiki       # BUILD SUCCESSFUL in 3s (writes per-version build/wiki/ across all 5 forks)
```

> **Note on the plan's `./gradlew :1.21.4:generateWiki` command:**
> The plan's verification recipe runs `:1.21.4:generateWiki` directly, but in this Stonecutter 0.6 layout
> `:1.21.4:compileKotlin` only succeeds when invoked under Stonecutter's `chiseled` wrapper -- the wrapper
> swaps the source set's srcDirs to the per-version chiseledSrc tree during execution. To make the wiki
> generator work both standalone AND under the chiseled pipeline, the build registers
> `chiseledGenerateWiki` in `stonecutter.gradle.kts` (wraps `generateWiki` like `chiseledBuild` wraps
> `build`). Output goes to `versions/<mc>/build/wiki/` per fork.

## File counts after generateWiki (verified on 1.21.4)

- Auto-generated weapon pages: 38                                     (expected 38)  PASS
- Category indexes: 4 (Simple-Melee, Simple-Ranged, Martial-Melee, Martial-Ranged)  (expected 4)   PASS
- Handwritten pages copied at wiki root: 12                           (expected 12)  PASS
- Vanilla-mapped callouts: 4 (Shortsword, Shortbow, Light-Crossbow, Trident)        (expected 4)   PASS
- Home.md contains BOTH the auto-generated header AND the handwritten body         PASS

Total files in `versions/1.21.4/build/wiki/Weapons/`: 42 (38 + 4).
Total handwritten .md files at `versions/1.21.4/build/wiki/`: 12.

## WikiGenTest results (per-version)

| Version | tests | failures | errors |
|---------|-------|----------|--------|
| 1.20.1  | 5     | 0        | 0      |
| 1.21.1  | 5     | 0        | 0      |
| 1.21.4  | 5     | 0        | 0      |
| 1.21.11 | 5     | 0        | 0      |
| 26.1.2  | 5     | 0        | 0      |

All 5 tests pass on every fork:
- `everyWeaponInCatalogRendersToAFile`
- `everyCategoryIndexExists`
- `vanillaMappedPagesIncludeTheCallout`
- `acquisitionSectionListsAtLeastOneCatalogEntryForKnownWeapon`
- `handwrittenPagesAreCopied`

## Plan deviations applied during implementation

1. **`Property.SPECIAL` -> `Property.SPECIAL_LANCE`** in `WikiTemplates.kt`. The plan's template
   referenced `Property.SPECIAL`, but the actual enum (`src/main/kotlin/com/dndweapons/catalog/Property.kt`)
   defines the variant as `SPECIAL_LANCE`. Adjusted the `when` branch accordingly. Lance-specific
   hook summary text written to match the existing semantics (mounted bonus, off-hand restriction).
2. **`RangeKind.SLING` and `RangeKind.BLOWGUN`** added to `labelFor(RangeKind)`. Plan only handled
   NONE/THROWN/BOW/CROSSBOW/FIREARM; actual enum also has SLING and BLOWGUN. Mapped to "Sling" and
   "Blowgun".
3. **`@TempDir` injection in `WikiGenTest`** -- the plan placed `@TempDir lateinit var tmp: Path` as
   an instance field; with `@TestInstance(PER_CLASS)` and `@BeforeAll`, JUnit 5 did not inject the
   field before `@BeforeAll`. Switched to method-parameter injection: `fun setup(@TempDir tmp: Path)`.
   All 5 tests pass after the fix.
4. **`java.io.File` FQN in `build.gradle.kts`** -- the plan's snippet used `java.io.File(cloneDir, f.name)`
   inline, but the Kotlin Gradle DSL compilation could not resolve the inline FQN. Added
   `import java.io.File` and replaced the inline FQN with `File(...)`.
5. **`generateWiki` requires Stonecutter wrapping.** Registered `chiseledGenerateWiki` in
   `stonecutter.gradle.kts` (analogous to `chiseledBuild`/`chiseledTest`). Standalone
   `./gradlew :1.21.4:generateWiki` fails because Stonecutter 0.6 only swaps source dirs inside
   a chiseled wrapper. CI workflow (`.github/workflows/wiki-publish.yml`) ships as-is per the plan,
   running `:1.21.4:generateWiki publishWiki`; that invocation should be updated to
   `chiseledGenerateWiki publishWiki` if the workflow is exercised before a follow-up plan adjusts it.
   See concerns in `docs/plan-runner/2026-05-18/cycle-5/bugs/wave-6.json`.

## Tag command (NOT executed by plan-runner)

```bash
git tag phase-6-wiki
git push origin phase-6-wiki
```

Only apply manually after eyeballing the generated wiki output in `versions/1.21.4/build/wiki/`.
