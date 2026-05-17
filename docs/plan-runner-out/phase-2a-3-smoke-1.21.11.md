# Phase 2a-3 Mojang-Naming Smoke Test (1.21.11)
**Date:** 2026-05-16 19:48:00

## Results
| Task | Result |
|---|---|
| :1.21.11:build | PASS |
| :1.21.11:test | PASS (7/7) |
| :1.21.11:runGametest | PASS |

## Inline fixes applied
- **AttributeCompat.kt**: Removed `//? if >=1.21.2` conditional around attribute field declarations
  (same fix as 1.21.1). All Mojang-mapped versions use `ATTACK_DAMAGE` / `ATTACK_SPEED` without `GENERIC_`.
- **DndWeaponsMod.kt**: Changed `//? if >=1.21.2 && <1.21.11` to `//? if >=1.21.2` for the Optional path.
  In 1.21.11, `BuiltInRegistries.ITEM.get(Identifier)` still returns `Optional<Holder$Reference<T>>`, so
  the Optional-handling code path is correct for 1.21.11, not just 1.21.2-1.21.10.

## Notes
- `ResourceLocation` is renamed to `Identifier` in 1.21.11. The `//? if >=1.21.11` import alias
  `import net.minecraft.resources.Identifier as ResourceLocation` activates correctly across all files
  (DndWeaponsMod.kt, AttributeCompat.kt, WeaponRegistrarImpl.kt, RegistrationGametest.kt).
- `AttributeModifier` constructor takes `(Identifier, double, Operation)` in 1.21.11; the alias makes this
  transparent.
- Fabric gametest API v3 (`>=1.21.5` branch) activated correctly. `@GameTest(structure=...)` annotation
  from `net.fabricmc.fabric.api.gametest.v1.GameTest` used. 2 game tests ran and passed.
- Pre-existing WARNING about recipe `fabric:tag` ingredient parsing; non-fatal and deferred.
