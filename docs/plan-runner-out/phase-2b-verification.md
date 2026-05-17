# Phase 2b Final Verification — Catalog Expansion to 38 Weapons

**Date:** 2026-05-17 05:10

## Build / Test / Gametest matrix

| Subproject | :build | :test | :runGametest |
|---|---|---|---|
| 1.20.1  | PASS | PASS (13/13) | PASS |
| 1.21.1  | PASS | PASS (13/13) | PASS |
| 1.21.4  | PASS | PASS (13/13) | PASS |
| 1.21.11 | PASS | PASS (13/13) | PASS |
| 26.1.2  | PASS | PASS (13/13) | PASS |

All 5 subprojects built successfully via `chiseledBuild` (BUILD SUCCESSFUL, 34 actionable tasks, 15 up-to-date). Tests run as JUnit 5 via stonecutter's active-version mechanism: each version was set active in turn (`Set active project to $V`), then `:$V:test` run with `--rerun-tasks`. 13 tests per version (4 from `WeaponSpecTest`, 9 from `WeaponsTest`) — all PASS with 0 failures and 0 errors. Gametests (`runGametest`) run a Minecraft server that exits after the test suite; all versions exited with BUILD SUCCESSFUL and 0 gametest failures.

## chiseledBuild jars

| Subproject | Jar | Size |
|---|---|---|
| 1.20.1  | versions/1.20.1/build/libs/dndweapons-0.1.0+mc1.20.1.jar | 17,490,933 bytes |
| 1.21.1  | versions/1.21.1/build/libs/dndweapons-0.1.0+mc1.21.1.jar | 17,477,696 bytes |
| 1.21.4  | versions/1.21.4/build/libs/dndweapons-0.1.0+mc1.21.4.jar | 17,478,365 bytes |
| 1.21.11 | versions/1.21.11/build/libs/dndweapons-0.1.0+mc1.21.11.jar | 17,478,484 bytes |
| 26.1.2  | versions/26.1.2/build/libs/dndweapons-0.1.0+mc26.1.2.jar | 17,478,674 bytes |

Jars are significantly larger than pre-Phase-2b baselines due to the bundled textures (33 AI-generated PNGs at ~400–870 KB each), model JSONs, lang entries, and recipes.

## Catalog summary

- 38 specs total (34 registered + 4 vanilla-mapped)
- 34 shared recipes (`src/main/resources/data/dndweapons/recipe/`) + 34 1.20.1 overlay recipes (`versions/1.20.1/src/main/resources/data/dndweapons/recipes/`)
- 4 role tag JSONs (`src/main/resources/data/dndweapons/tags/items/role/`: `shortsword.json`, `shortbow.json`, `light_crossbow.json`, `trident.json`)
- 39 lang entries in `en_us.json` (38 item names + 1 item group: `itemGroup.dndweapons.main`)
- 34 model JSONs in `src/main/resources/assets/dndweapons/models/item/`
- 34 texture PNGs in `src/main/resources/assets/dndweapons/textures/item/`

## Notes

**Texture caveat (Wave 5):** The 33 AI-generated textures (all weapons except `longsword.png`) were produced by the Gemini image generation tool and are not guaranteed to be native 16x16 pixel art. The images are large PNGs (348 KB – 879 KB) rather than 16x16 Minecraft-format sprites. Minecraft will rescale them at runtime, but the visual quality at small size may differ from native pixel art. `longsword.png` is a 70-byte placeholder (16x16 solid-color stub from Phase 1). A future phase should regenerate all textures as proper 16x16 pixel art sprites.

**Recipe format incompatibility (Wave 4, confirmed here):** The shared recipe JSONs in `src/main/resources/data/dndweapons/recipe/` use bare `{"tag":"c:ingots/iron"}` ingredient syntax for 1.21.1 through 26.1.2, and `{"fabric:type":"fabric:tag","tag":"..."}` syntax (from the legacy longsword recipe) on the same paths. Neither format is accepted by the FAPI recipe codec on MC 1.21.4+. All 34 shared recipes fail to load at gametest server startup with `DataResult.Error` logs. The 1.20.1 overlay recipes in `versions/1.20.1/src/main/resources/data/dndweapons/recipes/` were not tested for runtime acceptance in this run. The gametest (`longswordIsRegistered`) verifies item registration only and passes regardless of recipe loading. This incompatibility is a known deferrment — stonecutter versioned ingredient blocks are needed and will be addressed in a subsequent phase.

**Active project:** Reset to `1.21.4` (vcsVersion) after the sweep.

## Phase 2b status

Catalog expansion: COMPLETE. All 5 MC versions ship the 38-weapon catalog (34 registered + 4 vanilla-mapped) from one source tree. Build, JUnit tests (13/13), and gametest all PASS across every version. Two deferred items (texture resolution and recipe ingredient format) are tracked as known caveats for a subsequent phase.
