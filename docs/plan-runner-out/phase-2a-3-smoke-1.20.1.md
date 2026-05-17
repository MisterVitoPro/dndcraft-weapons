# Phase 2a-3 Mojang-Naming Smoke Test (1.20.1)
**Date:** 2026-05-16 19:50:00

## Results
| Task | Result |
|---|---|
| :1.20.1:build | PASS |
| :1.20.1:test | PASS (7/7) |
| :1.20.1:runGametest | PASS |

## Inline fixes applied
- **AttributeCompat.kt**: Removed `//? if >=1.21.2` conditional around attribute field declarations.
  All Mojang-mapped versions use `Attributes.ATTACK_DAMAGE` / `ATTACK_SPEED`; no `GENERIC_` prefix exists
  in Mojang official mappings for any version.
- **DndWeaponsMod.kt**: Changed `//? if >=1.21.2 && <1.21.11` to `//? if >=1.21.2` (same fix as above
  versions, irrelevant for 1.20.1 since the <1.20.5 code paths dominate, but needed for correctness).
- **DndWeaponItem.kt**: Corrected method name in the `//? if <1.20.5` override from `getAttributeModifiers`
  to `getDefaultAttributeModifiers`. In 1.20.1 Mojang mappings, `Item` exposes
  `getDefaultAttributeModifiers(EquipmentSlot)`, not `getAttributeModifiers(ItemStack, EquipmentSlot)`.
  The old method name did not override anything and caused a compile error.

## Notes
- 1.20.1 is Epoch A (<1.20.5): uses UUID-keyed `AttributeModifier` via `AttributeCompat.modifiersFor()`.
  `Attributes.ATTACK_DAMAGE` is `Attribute` (not `Holder<Attribute>`) in 1.20.1 Mojang mappings.
  `AttributeModifier.Operation.ADDITION` exists and compiles correctly.
- `BuiltInRegistries.ITEM.get(ResourceLocation)` returns `T` (Item) directly in 1.20.1; the `else`
  branch in `DndWeaponsMod.kt` correctly activates.
- `ResourceLocation(namespace, path)` constructor (pre-1.21 path) activates via `//? if <1.21` else
  branch in `rl()` helper functions.
- `Item.Properties()` has no `setId()` in 1.20.1; the `//? if >=1.21.2 {` guard in `WeaponRegistrarImpl`
  correctly excludes it.
- `getDefaultAttributeModifiers(EquipmentSlot)` override activates and returns `ImmutableMultimap` from
  `AttributeCompat.modifiersFor()`. Weapon attack damage/speed modifiers are applied at runtime via this
  override.
- One gametest (`longswordIsRegistered`) passed against the 1.20.1 server. Server launched cleanly.
- Java toolchain 17 (Amazon Corretto 25 JDK used with `-release 17` target). Compiled and ran without issue.
