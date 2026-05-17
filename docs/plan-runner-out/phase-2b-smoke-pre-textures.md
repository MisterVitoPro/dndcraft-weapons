# Phase 2b Pre-Texture Smoke Test

**Date:** 2026-05-16 21:52

## Results

| Subproject | :build | :test (N/13) | :runGametest |
|---|---|---|---|
| 1.20.1  | PASS | PASS (13/13) | PASS (1 gametest) |
| 1.21.1  | PASS | PASS (13/13) | PASS (1 gametest) |
| 1.21.4  | PASS | PASS (13/13) | PASS (1 gametest) |
| 1.21.11 | PASS | PASS (13/13) | PASS (2 gametests) |
| 26.1.2  | PASS | PASS (13/13) | PASS (2 gametests) |

## Notes

All 5 MC versions built successfully via `chiseledBuild` (BUILD SUCCESSFUL, 5 mod jars produced under `versions/*/build/libs/`). All 65 JUnit unit tests passed (13 per version across `WeaponSpecTest` and `WeaponsTest`). All gametests passed on server exit.

During `runGametest`, recipe-loading ERRORs were observed for all expanded-catalog weapon recipes on versions 1.21.4, 1.21.11, and 26.1.2. The errors indicate that the new recipe JSONs use tag ingredients in a format (bare `{"tag":"c:ingots/iron"}` or `{"fabric:type":"fabric:tag","tag":"c:ingots/iron"}`) that the current FAPI version on those MC targets does not accept at runtime. Recipes fail to parse but the server continues, and the gametest assertions (item registration checks) pass regardless. This is a known recipe ingredient format divergence across MC versions that will need stonecutter versioned blocks when recipe generation is addressed in a subsequent phase. Missing-texture warnings were not observed (no textures assigned yet — purple-checker interim state is acceptable at this stage). Active project was reset to 1.21.4 after the sweep.
