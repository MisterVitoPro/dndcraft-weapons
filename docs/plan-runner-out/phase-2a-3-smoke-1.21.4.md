# Phase 2a-3 Mojang-Naming Smoke Test (1.21.4)

**Date:** 2026-05-17 02:38:00

## Results
| Task | Result |
|---|---|
| :1.21.4:build | PASS |
| :1.21.4:test | PASS (7/7) |
| :1.21.4:runGametest | PASS |

## Inline fixes applied
- None. All source files compiled and ran without modification.

## Notes
All three Gradle tasks passed cleanly on 1.21.4 with no inline fixes required. The Stonecutter version-conditional forks in RegistrationGametest.kt, DndWeaponsMod.kt, AttributeCompat.kt, and WeaponRegistrarImpl.kt resolved correctly for 1.21.4 (Epoch C, `>=1.21.2 && <1.21.11` branch active). The 7 unit tests (4 in WeaponSpecTest, 3 in WeaponsTest) all passed. The runGametest task launched a 1.21.4 server, registered dndweapons:longsword, and ran the longswordIsRegistered gametest successfully ("All 1 required tests passed"). One non-fatal WARNING was observed: the longsword recipe JSON uses `fabric:tag` ingredient syntax which could not be parsed by the 1.21.4 vanilla recipe system; this is a pre-existing data-pack issue (recipe not critical to registration or gametest) and is deferred to a future task for recipe data migration.
