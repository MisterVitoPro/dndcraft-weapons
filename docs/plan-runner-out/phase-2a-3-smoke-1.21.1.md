# Phase 2a-3 Mojang-Naming Smoke Test (1.21.1)
**Date:** 2026-05-16 19:47:00

## Results
| Task | Result |
|---|---|
| :1.21.1:build | PASS |
| :1.21.1:test | PASS (7/7) |
| :1.21.1:runGametest | PASS |

## Inline fixes applied
- **AttributeCompat.kt**: Removed the `//? if >=1.21.2` conditional around `damageAttr`/`speedAttr`.
  The directive originally used `Attributes.GENERIC_ATTACK_DAMAGE` for <1.21.2, but with Mojang official
  mappings all supported versions (1.20.1 through 1.21.11) use `Attributes.ATTACK_DAMAGE` / `ATTACK_SPEED`
  without any `GENERIC_` prefix. The conditional was replaced with an unconditional declaration.
- **DndWeaponsMod.kt**: Changed `//? if >=1.21.2 && <1.21.11` to `//? if >=1.21.2` in both `iconStack()`
  and `addToEntries()`. The `&&<1.21.11` upper bound was unnecessary because 1.21.11 also uses the
  `Optional<Holder$Reference<T>>` return type from `BuiltInRegistries.ITEM.get()`, same as 1.21.2-1.21.10.

## Notes
- 1.21.1 uses `loom.officialMojangMappings()` (layered). `BuiltInRegistries.ITEM.get(ResourceLocation)`
  returns `T` (Item) directly in 1.21.1, not `Optional`. The `else` branch in `iconStack()` and
  `addToEntries()` correctly activates for 1.21.1.
- Compiler warning `Condition is always 'true'` at `if (item != null)` in the else branch is expected and
  benign: with a non-nullable `Item` return type in 1.21.1, the null guard is a no-op.
- One gametest (`longswordIsRegistered`) passed. Server launched and shut down cleanly.
- Pre-existing WARNING about `dndweapons:longsword` recipe using `fabric:tag` ingredient syntax; not
  critical to registration or gametest and deferred.
