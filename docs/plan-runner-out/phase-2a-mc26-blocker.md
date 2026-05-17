# MC 26.x Support — Real Blocker Analysis

**Date:** 2026-05-16
**Status:** 26.1.2 deferred. Not an ecosystem gap — it's a per-import refactor that's bigger than Phase 2a's scope.

## The actual situation (corrected from earlier postmortem)

Earlier I said 26.x is "ecosystem-blocked." That was wrong. **It builds.** NaturesCompass on `fabric-26.1` ships to Modrinth right now (Loom 1.15-SNAPSHOT, Loader 0.18.4, fabric-api 0.144.0+26.1, Java 25). The blocker isn't config — it's source naming.

## What changed at MC 26.x

1. **Mojang stopped obfuscating `client.jar`.** Confirmed by extracting `26.1.2/client.jar`: classes are `com/mojang/blaze3d/Blaze3D.class`, `net/minecraft/world/item/Item.class` — original source-level Mojang names, not `a.class`, `aa.class`.
2. **`piston-meta` no longer publishes `client_mappings`.** Because the JAR itself ships in source-name form, no separate mapping is needed.
3. **Fabric ecosystem switched naming convention** from Yarn (e.g., `net.minecraft.util.Identifier`) to Mojang (e.g., `net.minecraft.resources.Identifier`) for 26.x.
4. **Fabric meta API returns `net.fabricmc:intermediary:0.0.0`** for 26.x — a 578-byte stub that just satisfies Loom's mappings-config requirement.

## What "supporting 26.x" actually requires

The Loom build config is solvable (NaturesCompass proves it). But our codebase has Yarn naming everywhere:

| Our file | Yarn name (current) | Mojang name (26.x needs) |
|---|---|---|
| `WeaponRegistrarImpl.kt` | `net.minecraft.util.Identifier` | `net.minecraft.resources.Identifier` |
| `WeaponRegistrarImpl.kt` | `net.minecraft.item.Item` | `net.minecraft.world.item.Item` |
| `WeaponRegistrarImpl.kt` | `net.minecraft.registry.Registry`, `Registries`, `RegistryKey`, `RegistryKeys` | `net.minecraft.core.Registry`, `core.registries.BuiltInRegistries`, `resources.ResourceKey`, `core.registries.Registries` |
| `AttributeCompat.kt` | `net.minecraft.entity.attribute.EntityAttribute` | `net.minecraft.world.entity.ai.attributes.Attribute` |
| `AttributeCompat.kt` | `net.minecraft.entity.attribute.EntityAttributes.ATTACK_DAMAGE` | `net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE` |
| `AttributeCompat.kt` | `net.minecraft.component.DataComponentTypes.ATTRIBUTE_MODIFIERS` | `net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS` |
| `AttributeCompat.kt` | `net.minecraft.component.type.AttributeModifierSlot.MAINHAND` | `net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND` |
| `AttributeCompat.kt` | `net.minecraft.entity.attribute.EntityAttributeModifier` | `net.minecraft.world.entity.ai.attributes.AttributeModifier` |
| `DndWeaponsMod.kt` | `net.minecraft.text.Text` | `net.minecraft.network.chat.Component` |
| `DndWeaponsMod.kt` | `net.minecraft.item.ItemGroup`, `ItemStack` | `net.minecraft.world.item.CreativeModeTab`, `ItemStack` |
| `DndWeaponsMod.kt` | `net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup` | `net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents` (API also changed) |
| `RegistrationGametest.kt` | `net.minecraft.test.TestContext`, `GameTest` | `net.minecraft.gametest.framework.GameTestHelper`, `GameTest` |

This is **every Minecraft API reference** in the mod source. Plus several Fabric API package renames. Each one needs a Stonecutter `//?` fork OR per-version source overlay.

## Doable but big

The way to do it in our architecture:
1. Add per-import `//?` directives — every file with `net.minecraft.*` import gets forked. Roughly ~30 imports across 5 files for Phase 2a's surface area.
2. Method/field reference sites inside the bodies also need to be forked where types differ (e.g., `EntityAttributes.ATTACK_DAMAGE` vs `Attributes.ATTACK_DAMAGE` at the use-site — Kotlin won't auto-resolve across packages).
3. Add `versions/26.1.2/` overlays for files that diverge enough that inline `//?` is unreadable (e.g., the entire `DndWeaponsMod.onInitialize` body changes shape because `FabricItemGroup` → `CreativeModeTabEvents`).
4. Update `build.gradle.kts`: conditional on `mc.startsWith("26.")`, use `mappings("net.fabricmc:intermediary:0.0.0:v2")` + Java 25 toolchain + Loom 1.15-SNAPSHOT for that subproject only.
5. Update `versions/26.1.2/gradle.properties`: `minecraft_version=26.1`, `loader_version=0.18.4`, `fabric_version=0.144.0+26.1`, `java_release=25`. Drop `yarn_mappings`.

That's a focused mini-phase (call it **Phase 2a-3: MC 26.x support**) — needs its own brainstorm + plan + cycle.

## Reference: NaturesCompass fabric-26.1

For the next person picking this up, the working reference is https://github.com/MattCzyr/NaturesCompass/tree/fabric-26.1. Their `build.gradle` is single-project (no Stonecutter), but the Loom config + dependency declarations work as-is.

## Conclusion for Phase 2a

4 of 5 originally-planned MC versions ship and are validated. 26.1.2 is *not* fundamentally blocked — it needs a focused phase that adds a comprehensive yarn-to-mojang naming fork across every source file. Deferred.
