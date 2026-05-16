# Phase 2a Kotlin Conversion Smoke Test

**Date:** 2026-05-16 14:13:55

## Results on 1.21.4

| Task | Result |
|---|---|
| `:1.21.4:build` | PASS |
| `:1.21.4:test` | PASS (7 tests: 4 WeaponSpecTest + 3 WeaponsTest) |
| `:1.21.4:runGametest` | PASS (`longswordIsRegistered`) |

## Issues caught & fixed inline during smoke test

1. **Gradle 8.10 → 8.12.** `gradle/wrapper/gradle-wrapper.properties` was pinned to Gradle 8.10 (left over from cycle-2's wrapper generation), but fabric-loom 1.10-SNAPSHOT requires Gradle 8.12+. Updated `distributionUrl` to `gradle-8.12-bin.zip`.

2. **FLK version 1.13.5+kotlin.2.0.21 does not exist on Maven.** The plan's best-guess value was wrong. Looked up Fabric Maven directly (https://maven.fabricmc.net/net/fabricmc/fabric-language-kotlin/) — latest is `1.13.11+kotlin.2.3.21`. Updated:
   - `gradle.properties`: `kotlin_version=2.0.21` → `2.3.21`
   - `versions/1.21.4/gradle.properties`: `flk_version=1.13.5+kotlin.2.0.21` → `1.13.11+kotlin.2.3.21`
   - `build.gradle.kts`: Kotlin plugin version `2.0.21` → `2.3.21`

3. **`EntityAttributes.GENERIC_ATTACK_DAMAGE` / `_SPEED` renamed.** Yarn 1.21.4+build.8 dropped the `GENERIC_` prefix. Updated `AttributeCompat.kt`:
   - `GENERIC_ATTACK_DAMAGE` → `ATTACK_DAMAGE`
   - `GENERIC_ATTACK_SPEED` → `ATTACK_SPEED`

4. **`Item.Settings` requires `.registryKey()` before `Item` construction in 1.21.4.** Otherwise the Item constructor throws `NullPointerException: Item id not set` when the translation key is computed. Updated `WeaponRegistrarImpl.kt`:
   - Build `val itemKey = RegistryKey.of(RegistryKeys.ITEM, itemId)` before `Item.Settings()`
   - Call `Item.Settings().registryKey(itemKey)` so the settings know their item key up front
   - Use the new overload `Registry.register(Registries.ITEM, itemKey, item)` taking the `RegistryKey` (instead of the older `Identifier` overload)

## Known issues NOT yet fixed (deferred)

1. **Recipe parse error on 1.21.4.** The shared `src/main/resources/data/dndweapons/recipe/longsword.json` uses the older ingredient form `{"tag": "c:ingots/iron"}`, but 1.21.4 requires either the new ingredient form or a `fabric:type` discriminator. Server log:
   ```
   Couldn't parse data file 'dndweapons:longsword' from 'dndweapons:recipe/longsword.json':
   DataResult.Error['Map entry 'I' : ... Input does not contain a key [fabric:type]'
   ```
   This does NOT block the gametest (which only checks item registration). It DOES affect chiseledRunGametest on 1.21.4 later when recipes load. Phase 2a Task 19 (final verification) will address — likely by updating the recipe JSON to current schema. Tracked.

2. **DndWeaponsMod.kt:45 always-true warning.** `if (item != null)` after `Registries.ITEM.get(...)` — the Yarn-mapped return is non-null in Kotlin's view. Cosmetic; safe to ignore.

## Notes

The smoke test caught 4 real blockers (gradle version, FLK version, attribute rename, Settings.registryKey requirement). All inline-fixed; 1.21.4 now builds + tests + gametests green end-to-end. The Kotlin conversion is functionally validated on 1.21.4. Remaining waves (8-11) add Epoch A directives + 4 more MC subprojects.
