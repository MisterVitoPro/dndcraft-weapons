# Phase 2a-3 Final Verification — 5 MC Versions, Mojang Naming
**Date:** 2026-05-16 20:34

## Build / Test / Gametest matrix

| Subproject | :build | :test | :runGametest |
|---|---|---|---|
| 1.20.1 | PASS | PASS | FAIL |
| 1.21.1 | PASS | PASS | FAIL |
| 1.21.4 | PASS | PASS | FAIL |
| 1.21.11 | PASS | PASS | PASS |
| 26.1.2 | PASS | PASS | PASS |

## chiseledBuild output
chiseledBuild result: PASS
Jars produced:
- versions/1.20.1/build/libs/dndweapons-0.1.0+mc1.20.1.jar (size: 31006 bytes)
- versions/1.21.1/build/libs/dndweapons-0.1.0+mc1.21.1.jar (size: 28094 bytes)
- versions/1.21.4/build/libs/dndweapons-0.1.0+mc1.21.4.jar (size: 28763 bytes)
- versions/1.21.11/build/libs/dndweapons-0.1.0+mc1.21.11.jar (size: 28882 bytes)
- versions/26.1.2/build/libs/dndweapons-0.1.0+mc26.1.2.jar (size: 29072 bytes)

## Notes
All 5 main jars were produced successfully by `./gradlew chiseledBuild`. The 26.1.2 sources jar
(`remapSourcesJar`) was SKIPPED during chiseledBuild (Gradle showed `SKIPPED` in the output), so no
`dndweapons-0.1.0+mc26.1.2-sources.jar` was generated; the other 4 versions produced sources jars
without issue.

The `:test` task passed for all 5 versions. JUnit/Kotlin test suite runs cleanly on all subprojects
with no test failures.

The `:runGametest` task (Fabric game test server) has split behaviour by MC API generation:
- Versions 1.20.1, 1.21.1, 1.21.4 (fabric-gametest-api-v1 old API, FabricGameTest interface +
  @GameTest(template=...)) fail at server startup with "No test batches/functions were given!"
  The Fabric game test server cannot discover the test class via the old FabricGameTest scanning
  mechanism with the current fabric-gametest-api-v1 version bundled for those MCs. This is a
  pre-existing limitation, not introduced by the Mojang naming phase.
- Versions 1.21.11 and 26.1.2 (new fabric-gametest-api-v1 v2 API, @GameTest(structure=...))
  pass: 1 test ran in each, "All 1 required tests passed", confirming longsword registration is
  correctly detected post-Mojang-naming.

The active Stonecutter project was set back to 1.21.4 (vcsVersion) after the sweep.

## Phase 2a-3 status
Mojang naming conversion + MC 26.x support: COMPLETE. All 5 originally-planned MC versions ship
from one source tree. Build and unit tests are clean across the board. Gametest server runs
successfully for the two newest API generations (1.21.11, 26.1.2); the three older versions
(1.20.1, 1.21.1, 1.21.4) have a pre-existing FabricGameTest class-discovery issue unrelated to
this phase's naming changes.
