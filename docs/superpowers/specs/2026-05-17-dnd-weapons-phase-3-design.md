# DnD Weapons — Phase 3 Design Specification

**Date:** 2026-05-17
**Status:** Approved (brainstorm complete, ready for implementation planning)
**Author:** brainstorm session with the user
**Parent design:** [2026-05-16-dnd-weapons-design.md](2026-05-16-dnd-weapons-design.md)
**Predecessor phase:** [2026-05-16-dnd-weapons-phase-2b-design.md](2026-05-16-dnd-weapons-phase-2b-design.md) — Phase 2b shipped the full 38-weapon catalog with crafting recipes, models, lang, textures, and the 4 vanilla-mapped role tags (data only).
**Implementation plan:** TBD (next step — invoke `superpowers:writing-plans`)

---

## 1. Goal & Scope

**Goal:** Make 5 DnD combat properties actually do something at runtime, and inject a DnD stat-block tooltip line on every weapon — registered or vanilla-mapped — across all 5 MC versions (1.20.1, 1.21.1, 1.21.4, 26.1.2, 1.21.11).

### In scope

The 5 combat properties from the master design's phase plan:

| Property | Mechanic | Implementation surface |
|---|---|---|
| Versatile | +`versatileBonus` damage when offhand is empty | mixin → `WeaponAttackHandler` |
| Finesse | ×1.20 damage when attacker is sprinting | mixin → `WeaponAttackHandler` |
| Light | +1 damage when offhand is also a LIGHT weapon | mixin → `WeaponAttackHandler` |
| Heavy | +1.0 `ATTACK_KNOCKBACK` attribute baked into the item | `AttributeCompat` extension |
| Special (Lance) | ×0.50 damage when attacker has no vehicle | mixin → `WeaponAttackHandler` |

Plus tooltip injection: an italic DnD stat-block line + zero-or-more italic conditional-bonus lines, inserted right after the item display name, on **both** registered DnD items and the 4 vanilla-mapped items (any vanilla sword, `minecraft:bow`, `minecraft:crossbow`, `minecraft:trident`) carrying a role tag.

Concrete deliverables:

- New `SpecRegistry` (resolves `Item → WeaponSpec?` for registered items and via role-tag walk for vanilla-mapped items)
- New `WeaponAttackHandler` (pure function `(base, mainhandSpec, ctx) → Float`)
- New `WeaponTooltipBuilder` + `WeaponTooltipInjector` (`ItemTooltipCallback`)
- New `PlayerAttackMixin` injecting into `net.minecraft.world.entity.player.Player#attack(Entity)`
- New `dndweapons.mixins.json` + `fabric.mod.json` reference
- `AttributeCompat` modified to emit `ATTACK_KNOCKBACK +1.0` for HEAVY weapons (both epochs)
- `WeaponRegistrarImpl` modified to register specs into `SpecRegistry` on both the registered and vanilla-mapped branches
- `DndWeaponsMod.onInitialize` modified to wire `SpecRegistry.init()` and `WeaponTooltipInjector.register()`
- `en_us.json` entries for stat-block, property labels, and conditional-bonus strings
- JUnit suites for the three pure modules (Handler, TooltipBuilder, SpecRegistry)
- Per-mechanic Fabric gametests (sprint/light/versatile/lance-foot/lance-mounted/vanilla-iron-sword)
- 2 tooltip gametests (registered + vanilla-mapped)
- All 5 MC versions pass `:build`, `:test`, `:runGametest`

### Out of scope (deferred)

| Concern | Deferred to |
|---|---|
| Reach property (`entity_interaction_range` / Reach Entity Attributes) | A future runtime-property phase |
| Two-Handed offhand-block (per-tick check) | A future runtime-property phase |
| Thrown right-click throw (`DndThrownWeaponItem` subclass) | A custom-item phase |
| Ammunition / Loading custom item subclasses (Sling, Crossbows, Blowgun, Firearms) | A custom-item phase |
| Smithing upgrade ladder (iron → diamond → netherite) | Phase 4 |
| Loot tables, villager trades, mob drops | Phase 5 |
| Wiki generator | Phase 6 |
| Per-mob damage-type vulnerabilities (Slashing/Piercing/Bludgeoning) | Not planned for v1.0 (master non-goal) |
| Mastery properties | Not planned for v1.0 (master non-goal) |

---

## 2. Decision Log

| # | Decision | Choice | Rationale |
|---|---|---|---|
| 1 | Phase 3 scope | Strict — 5 properties + tooltips only | Matches master design's phase list verbatim. Keeps the plan-runner cycle right-sized; Reach/Two-Handed are small but expand the per-tick surface, Thrown/Ammunition/Loading need custom item subclasses |
| 2 | Heavy knockback mechanism | `Attributes.ATTACK_KNOCKBACK` modifier in `AttributeCompat` | Identical pipeline to ATTACK_DAMAGE/ATTACK_SPEED, already proven on all 5 versions. Visible in vanilla attribute tooltip. Stacks with Knockback enchantment naturally |
| 3 | Damage-modification mechanism | Mixin into `Player.attack(Entity)` | Fabric API has no "modify damage" event. AttackEntityCallback fires too early to modify damage and can only allow/block. Alternatives (per-tick transient modifier; re-implement vanilla hit in `AttackEntityCallback`) either lie in the vanilla attribute tooltip or require re-doing knockback/sweep/sound/cooldown by hand. The master design's section-5 sketch ("Wired into AttackEntityCallback") is a design gap; this phase resolves it |
| 4 | Tooltip layout | Stat-block line + bonus line above vanilla attribute block, italic gray | Cleanest separation; bonus is visually paired with the DnD identity it explains. Italic gray matches vanilla flavor-text convention |
| 5 | `SpecRegistry` shape | Single registry; one `Map<Item, WeaponSpec>` for registered + one `Map<TagKey, WeaponSpec>` for role tags; lazy flattened cache for role-tag lookups | Hot path (lookup) is O(1) via flattened cache. Cache invalidation on `CommonLifecycleEvents.TAGS_LOADED` makes it reload-safe |
| 6 | Testing depth | Unit-heavy + per-mechanic gametest (6 mechanics gametests + 2 tooltip gametests) | Pure-function math gets exhaustive unit coverage; gametests prove the mixin wiring and SpecRegistry role-tag resolution end-to-end on every version |
| 7 | Mixin compatibility level | `JAVA_17` | Works on the 1.20.1 subproject (Java 17 build) and on 1.21+ (Java 21 is a superset). No per-epoch fork |
| 8 | Mixin "required" | `"required": true`, `defaultRequire: 1` | If the mixin fails to apply on any version, mod load fails loudly. Better than silent damage-not-modified |

---

## 3. Architecture

### File inventory

```
src/main/kotlin/com/dndweapons/
  registry/
    SpecRegistry.kt              # NEW - Item → WeaponSpec resolver (registered + role tags)
    WeaponRegistrarImpl.kt       # MODIFY - register into SpecRegistry on both branches
  combat/
    WeaponAttackHandler.kt       # NEW - pure modifyDamage(base, spec, ctx) → Float
  tooltip/
    WeaponTooltipBuilder.kt      # NEW - pure builder, WeaponSpec → List<Component>
    WeaponTooltipInjector.kt     # NEW - registers ItemTooltipCallback
  mixin/
    PlayerAttackMixin.kt         # NEW - @ModifyVariable into Player.attack(Entity)
  compat/
    AttributeCompat.kt           # MODIFY - emit ATTACK_KNOCKBACK for HEAVY weapons (both epochs)
  DndWeaponsMod.kt               # MODIFY - call SpecRegistry.init() + WeaponTooltipInjector.register()

src/main/resources/
  dndweapons.mixins.json         # NEW
  fabric.mod.json                # MODIFY - add "mixins": ["dndweapons.mixins.json"]
  assets/dndweapons/lang/en_us.json   # MODIFY - tooltip translation keys

src/test/kotlin/com/dndweapons/
  combat/WeaponAttackHandlerTest.kt     # NEW - ~12 tests
  tooltip/WeaponTooltipBuilderTest.kt   # NEW - ~10 tests
  registry/SpecRegistryTest.kt          # NEW - ~5 tests (pure-logic surface)

src/main/kotlin/com/dndweapons/test/
  CombatHooksGametest.kt         # NEW - 6 per-mechanic gametests
  TooltipInjectionGametest.kt    # NEW - 2 tooltip gametests
```

Net: 7 new Kotlin files + 1 new JSON + 4 modifications. No new dependencies (mixin support is already provided by Loom).

### Component sketches

#### `SpecRegistry.kt`

```kotlin
package com.dndweapons.registry

import com.dndweapons.catalog.WeaponSpec
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item

object SpecRegistry {
    private val byItem = mutableMapOf<Item, WeaponSpec>()
    private val byRoleTag = mutableMapOf<TagKey<Item>, WeaponSpec>()
    @Volatile private var roleCache: Map<Item, WeaponSpec>? = null

    fun init() {
        // Subscribe to CommonLifecycleEvents.TAGS_LOADED to invalidate cache.
        // Concrete API call inserted at implementation time (see Section 4).
    }

    fun bindRegistered(item: Item, spec: WeaponSpec) {
        byItem[item] = spec
    }

    fun bindRoleTag(spec: WeaponSpec) {
        val tagStr = spec.vanillaRoleTag ?: return
        byRoleTag[parseTagKey(tagStr)] = spec
        roleCache = null
    }

    fun lookup(item: Item): WeaponSpec? {
        byItem[item]?.let { return it }
        return (roleCache ?: buildRoleCacheAndStore()).get(item)
    }

    fun invalidateRoleCache() { roleCache = null }

    private fun buildRoleCacheAndStore(): Map<Item, WeaponSpec> {
        val out = mutableMapOf<Item, WeaponSpec>()
        for ((tag, spec) in byRoleTag) {
            for (holder in BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                out[holder.value()] = spec
            }
        }
        roleCache = out
        return out
    }

    private fun parseTagKey(s: String): TagKey<Item> { /* "ns:path" → TagKey */ }
}
```

Notes:
- `byItem` and `byRoleTag` are mutated only during mod init (single-threaded). `roleCache` is volatile because `lookup` is called from server tick thread; the read-build-write race is benign (worst case: two threads each build the same cache once, last write wins, both copies are correct).
- `getTagOrEmpty(tag)` returns an `Iterable<Holder<Item>>` on all 5 versions. Mojang names are stable.

#### `WeaponAttackHandler.kt`

```kotlin
package com.dndweapons.combat

import com.dndweapons.catalog.Property.*
import com.dndweapons.catalog.WeaponSpec

object WeaponAttackHandler {
    data class Context(
        val attackerSprinting: Boolean,
        val attackerHasVehicle: Boolean,
        val offhandIsEmpty: Boolean,
        val offhandSpec: WeaponSpec?,
    )

    fun modifyDamage(base: Float, mainhand: WeaponSpec, ctx: Context): Float {
        var dmg = base
        // Additive bonuses applied first, then multiplicative scales.
        // This makes Finesse boost the post-Versatile/Light damage uniformly,
        // and keeps the math conventional (additives compose, multipliers stack).
        if (VERSATILE in mainhand.properties && ctx.offhandIsEmpty) {
            dmg += mainhand.versatileBonus
        }
        if (LIGHT in mainhand.properties &&
            ctx.offhandSpec != null && LIGHT in ctx.offhandSpec.properties) {
            dmg += 1.0f
        }
        if (FINESSE in mainhand.properties && ctx.attackerSprinting) {
            dmg *= 1.20f
        }
        if (SPECIAL_LANCE in mainhand.properties && !ctx.attackerHasVehicle) {
            dmg *= 0.50f
        }
        return dmg
    }
}
```

Pure function. No MC types in the signature except `WeaponSpec`. Fully unit-testable in plain JVM.

**Stacking order is load-bearing.** Additive bonuses (Versatile, Light) apply before multiplicative scales (Finesse, Lance) so a sprinting Scimitar with a Dagger offhand yields `(base + 1) × 1.20`, not `base × 1.20 + 1`. The unit test `finesseAndLightStackOnOneStrike` pins this contract.

#### `PlayerAttackMixin.kt`

```kotlin
package com.dndweapons.mixin

import com.dndweapons.combat.WeaponAttackHandler
import com.dndweapons.combat.WeaponAttackHandler.Context
import com.dndweapons.registry.SpecRegistry
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.ModifyVariable

@Mixin(Player::class)
abstract class PlayerAttackMixin {

    // @At target descriptor resolved at plan time by reading decompiled Player.class.
    // Target is the LivingEntity#hurt(DamageSource, float) invocation inside Player#attack(Entity).
    @ModifyVariable(
        method = "attack(Lnet/minecraft/world/entity/Entity;)V",
        at = At(value = "INVOKE", target = "<RESOLVED_AT_PLAN_TIME>"),
        // name="f" by default; per-epoch //? fork via ordinal= if any version differs
    )
    private fun dndweapons$modifyAttackDamage(damage: Float, target: Entity): Float {
        val self = this as Player
        val mainhandItem = self.mainHandItem.item
        val spec = SpecRegistry.lookup(mainhandItem) ?: return damage

        val offhandStack = self.offhandItem
        val ctx = Context(
            attackerSprinting = self.isSprinting,
            attackerHasVehicle = self.isPassenger,
            offhandIsEmpty = offhandStack.isEmpty,
            offhandSpec = SpecRegistry.lookup(offhandStack.item),
        )
        return WeaponAttackHandler.modifyDamage(damage, spec, ctx)
    }
}
```

Per-epoch concern: only the local-variable name/ordinal can shift across the 5 versions. Mitigated by the Wave-1 smoke task that compiles and runs the mixin on 1.21.4 alone before per-version fan-out. If a version breaks, a single-line `//?` fork on the `@ModifyVariable` parameters fixes it.

**Implementation update (QA Swarm 2026-05-18, P1-005):** The shipped mixin uses
`@ModifyArg` on the `hurt`/`hurtOrSimulate` INVOKE site (index=1) instead of the
`@ModifyVariable` sketched above. Three mixin methods cooperate:

1. `@Inject(at = HEAD)` `dndweapons_captureTarget` records the primary target
   Entity into a per-instance `ThreadLocal`.
2. `@ModifyArg(target = hurt/hurtOrSimulate, index = 1)` `dndweapons_modifyAttackDamage`
   reads the ThreadLocal, applies the DnD modifier only on the FIRST invocation
   (the primary strike), then clears the flag so subsequent INVOKEs (sweep
   damage) skip the modifier.
3. `@Inject(at = TAIL)` `dndweapons_clearTarget` clears the ThreadLocal on every
   method exit including exception paths.

Why this design over the spec sketch:
- **LVT immunity**: `@ModifyArg` keys on the call-site descriptor, not on a
  local-variable name/ordinal. Robust across 5 MC versions without per-version
  forks on the LVT.
- **Sweep-entity guard**: vanilla `Player.attack` issues `hurt`/`hurtOrSimulate`
  once for the primary target and again per neighbouring entity in the sweep
  loop. Without the ThreadLocal flag the DnD modifier would multiply collateral
  damage. The spec scopes the modifier to one deliberate strike.
- **TAIL not RETURN**: `@Inject(at = RETURN)` only fires on normal returns. If
  any downstream mod or future MC code throws inside `Player.attack` between
  HEAD and RETURN, the ThreadLocal would retain a stale Entity reference until
  the worker thread next executes the method. TAIL fires on every exit
  including exception paths, eliminating the leak.

The fork on the INVOKE descriptor (hurt vs hurtOrSimulate) is still required
and lives on the `@ModifyArg` annotation: `<1.21.4` uses `hurt`, `>=1.21.4`
uses `hurtOrSimulate`. Both have identical `(DamageSource, float) -> boolean`
signatures so the index=1 parameter is the same in both.

#### `AttributeCompat.kt` — HEAVY extension

In the Epoch C (`>= 1.20.5`) `applyTo` branch, after the speed modifier and before `.build()`, add:

```kotlin
if (Property.HEAVY in spec.properties) {
    builder.add(
        Attributes.ATTACK_KNOCKBACK,
        AttributeModifier(
            ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, "base_attack_knockback_${spec.id}"),
            1.0,
            AttributeModifier.Operation.ADD_VALUE,
        ),
        EquipmentSlotGroup.MAINHAND,
    )
}
```

In the Epoch A (`< 1.20.5`) `storeFor` branch, extend `CachedMods` with a nullable `knockback: AttributeModifier?` and add it to the returned `Multimap` in `modifiersFor` when present:

```kotlin
private data class CachedMods(
    val damage: AttributeModifier,
    val speed: AttributeModifier,
    val knockback: AttributeModifier?,
)

// in storeFor:
knockback = if (Property.HEAVY in spec.properties) {
    AttributeModifier(
        UUID.nameUUIDFromBytes("dndweapons:kb:${spec.id}".toByteArray()),
        "Weapon base attack knockback",
        1.0,
        AttributeModifier.Operation.ADDITION,
    )
} else null

// in modifiersFor: when cached.knockback != null, .put(Attributes.ATTACK_KNOCKBACK, cached.knockback)
```

`Attributes.ATTACK_KNOCKBACK` exists on all 5 versions under Mojang names.

#### `WeaponTooltipBuilder.kt` / `WeaponTooltipInjector.kt`

`WeaponTooltipBuilder.build(spec: WeaponSpec): List<Component>` returns:

1. **Stat-block line** (always one):
   - Format: `<diceText> <damageTypeLabel>[ · <propertyTags joined by " · ">]`
   - Examples:
     - Mace: `"1d6 bludgeoning"`
     - Rapier: `"1d8 piercing · Finesse"`
     - Longsword: `"1d8 slashing · Versatile (1d10)"`
     - Lance: `"1d10 piercing · Heavy · Reach · Two-Handed · Special"`
   - Italic gray (`Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true)`)
   - Built from translation keys: `tooltip.dndweapons.damage_type.slashing`, `tooltip.dndweapons.property.finesse`, etc.

2. **Conditional-bonus line** (zero or more, in property-enum declaration order):
   - FINESSE → `"+20% damage while sprinting"` (key: `tooltip.dndweapons.bonus.finesse_sprint`)
   - LIGHT → `"+1 damage when dual-wielding Light"` (key: `tooltip.dndweapons.bonus.light_dual`)
   - VERSATILE → `"+N damage when offhand empty"` with N = `spec.versatileBonus` (key: `tooltip.dndweapons.bonus.versatile_empty` with arg)
   - SPECIAL_LANCE → `"Half damage when on foot"` (key: `tooltip.dndweapons.bonus.lance_foot`)
   - HEAVY, REACH, TWO_HANDED, THROWN, AMMUNITION, LOADING: no bonus line in Phase 3 (HEAVY's effect is the attribute itself; REACH/TWO_HANDED/THROWN/AMMUNITION/LOADING are out of Phase 3 scope and have no runtime effect to advertise yet)
   - Italic gray, same style as stat-block

`WeaponTooltipInjector.register()`:

```kotlin
ItemTooltipCallback.EVENT.register { stack, ctx, type, lines ->
    val spec = SpecRegistry.lookup(stack.item) ?: return@register
    val toInsert = WeaponTooltipBuilder.build(spec)
    if (toInsert.isNotEmpty()) lines.addAll(1, toInsert)  // after display name
}
```

`ItemTooltipCallback` signature is identical across the 5 versions.

#### `DndWeaponsMod.onInitialize()` additions

```kotlin
override fun onInitialize() {
    // ... existing logging + registrar.registerAll(...) ...

    SpecRegistry.init()
    WeaponTooltipInjector.register()

    // ... existing creative-tab wiring ...
}
```

#### `WeaponRegistrarImpl.register` additions

After the early-return in the vanilla-mapped branch:

```kotlin
override fun register(spec: WeaponSpec) {
    if (spec.isVanillaMapped) {
        SpecRegistry.bindRoleTag(spec)
        return
    }
    // ... existing registration ...
    SpecRegistry.bindRegistered(item, spec)
    DndWeaponsMod.LOGGER.info("Registered weapon: {}", itemId)
}
```

#### `dndweapons.mixins.json`

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.dndweapons.mixin",
  "compatibilityLevel": "JAVA_17",
  "mixins": ["PlayerAttackMixin"],
  "injectors": { "defaultRequire": 1 }
}
```

`fabric.mod.json` gains `"mixins": ["dndweapons.mixins.json"]`. No per-version overlay needed.

---

## 4. Per-Epoch Concerns

Mojang official mappings on all 5 versions (Phase 2a-3 outcome) collapse most cross-version differences. The full per-epoch matrix:

| Concern | 1.20.1 | 1.21.1 | 1.21.4 | 26.1.2 | 1.21.11 | Notes |
|---|---|---|---|---|---|---|
| `ItemTooltipCallback` | v1 | v1 | v1 | v1 | v1 | Fabric API, identical signature all 5 |
| `CommonLifecycleEvents.TAGS_LOADED` | v1 | v1 | v1 | v1 | v1 | Stable since Fabric API 0.85 |
| `Attributes.ATTACK_KNOCKBACK` | UUID | Id | Id | Id | Id | Already split by `AttributeCompat` A/C branches |
| `Player.attack(Entity)` signature | stable | stable | stable | stable | stable | Mojang names — `attack(Lnet/minecraft/world/entity/Entity;)V` |
| Local `float f` in `Player.attack` | check | check | check | check | check | Real per-version risk; mitigated by Wave-1 smoke test |
| `BuiltInRegistries.ITEM.getTagOrEmpty(TagKey)` | yes | yes | yes | yes | yes | Same Mojang signature on all 5 |
| Mixin schema | 0.8 | 0.8 | 0.8 | 0.8 | 0.8 | Loom-provided MixinExtras; no version drift |

### Edge cases

1. **Mixin fires only for player attackers.** Mobs swinging items use different code paths and don't trigger the mixin. Correct: DnD properties are player-facing.

2. **Crit interaction.** Vanilla applies a 1.5× crit multiplier on the local `f` before our injection point. Sprinting + critting Rapier = `base × 1.5 × 1.20`. Acceptable.

3. **Sweep edge.** Vanilla sweeping-edge follow-ups call `hurt` again per sweep entity, each going through our mixin. Sprinting Rapier sweeps with the Finesse bonus. Acceptable.

4. **Cooldown scaling.** Vanilla scales damage by attack-cooldown progress before our injection. Our multipliers compound with cooldown — spam-clicked Lance on foot deals `cooldown × base × 0.5`. Intentional ("not the right way to use a Lance").

5. **Offhand item is a vanilla bow.** `SpecRegistry.lookup(bow)` returns Shortbow spec. Shortbow has only TWO_HANDED — `modifyDamage` no-ops (no LIGHT/FINESSE/VERSATILE/LANCE check is triggered). Same for Light Crossbow. Trident gets VERSATILE on melee swings (correct DnD behavior).

6. **Items with no spec.** `SpecRegistry.lookup` returns null → mixin early-returns → vanilla damage unchanged. Tooltip injector likewise no-ops.

7. **Tooltip in creative search / recipe book / advancement screens.** `ItemTooltipCallback` covers all uniformly.

8. **Missing lang keys.** Vanilla renders the raw key, ugly but non-fatal. Acceptance criterion: all required keys present.

9. **Heavy ∩ vanilla-mapped.** Empty by construction (none of Shortsword/Shortbow/Light Crossbow/Trident carry HEAVY). Knockback extension never applies to vanilla items. Safe.

10. **Mixin load failure.** `"required": true` + `defaultRequire: 1` → mod load fails loudly. Better than silent damage-not-modified. Gametest catches it on every version.

11. **`TAGS_LOADED` timing.** Title-screen creative-tab tooltips (no world loaded) → role tags empty → vanilla iron sword shows vanilla tooltip on title screen. Correct: no world, no tag data, no DnD identity to assert. Once a world loads, tooltips light up.

12. **`SpecRegistry` thread safety.** Reads happen on the server thread (mixin) and on the client thread (tooltip injector). Writes happen during mod init (single-threaded). `roleCache` rebuild from `lookup` may race between threads; result is correct under last-write-wins and the cache is content-equivalent for both threads, so the race is benign.

---

## 5. Data Flow

### Mod init

1. `DndWeaponsMod.onInitialize` runs.
2. `WeaponRegistrarImpl().registerAll(Weapons.ALL)`. For each spec:
   - vanilla-mapped → `SpecRegistry.bindRoleTag(spec)` and return.
   - registered → existing pipeline (attribute compat, registry key, `Registry.register`); then `SpecRegistry.bindRegistered(item, spec)`.
3. `SpecRegistry.init()` subscribes to `CommonLifecycleEvents.TAGS_LOADED` to invalidate the role cache when tag data reloads (datapack reload, server start).
4. `WeaponTooltipInjector.register()` subscribes to `ItemTooltipCallback.EVENT`.
5. Existing creative-tab wiring runs unchanged.
6. Mixin is auto-loaded by Loader via `dndweapons.mixins.json`; class is transformed at class-load time.

### Runtime: player attacks zombie with Rapier while sprinting

1. Player left-clicks zombie. Vanilla `Player.attack(zombie)` runs.
2. Vanilla reads `ATTACK_DAMAGE` from Rapier's item-attribute component (6.0), applies cooldown multiplier, sums enchantment damage, applies crit multiplier if applicable. Result lands in local `float f`.
3. Just before vanilla's `zombie.hurt(damageSource, f)` invocation, our `@ModifyVariable` mixin fires.
4. `SpecRegistry.lookup(rapierItem)` → `byItem` hit → returns Rapier spec (FINESSE).
5. `WeaponAttackHandler.modifyDamage(f, rapierSpec, ctx)` with `ctx.attackerSprinting=true` → returns `f × 1.20`.
6. Vanilla calls `zombie.hurt(damageSource, modified)`. Knockback, sound, particles, durability all stock vanilla.

### Runtime: player attacks zombie with vanilla iron sword while sprinting

1. Same path through `Player.attack`.
2. Vanilla computes `f` from iron sword's base damage = 6.0.
3. Mixin fires. `SpecRegistry.lookup(iron_sword)` → `byItem` miss → `roleCache` hit (built lazily on first miss after `TAGS_LOADED`) → returns Shortsword spec (FINESSE + LIGHT).
4. Handler applies Finesse multiplier (`× 1.20`); Light check fails (offhand empty); returns `7.2`.
5. Vanilla `zombie.hurt(source, 7.2)`. The vanilla iron sword behaves DnD-correctly with no item replacement.

### Runtime: tooltip render

1. Player hovers a stack in inventory. MC builds the tooltip line list, then fires `ItemTooltipCallback.EVENT`.
2. Our injector's callback gets `(stack, ctx, type, lines)`.
3. `SpecRegistry.lookup(stack.item)` returns the spec (or null).
4. `WeaponTooltipBuilder.build(spec)` produces 1–2 italic gray Components.
5. Injector inserts them at index 1 (right after the display name line at index 0). Vanilla attribute lines render below.

### Runtime: datapack reload mid-server

1. Tag data reloads. Fabric API fires `CommonLifecycleEvents.TAGS_LOADED`.
2. Our subscriber calls `SpecRegistry.invalidateRoleCache()`.
3. Next `lookup` of a vanilla item rebuilds the cache from current tag membership.

---

## 6. Testing Strategy

### Unit (JUnit 5, Kotlin) — runs on all 5 subprojects via `chiseledTest`

**`WeaponAttackHandlerTest.kt`** — 12 tests:

| Test | Inputs | Expect |
|---|---|---|
| `noPropertiesReturnsBase` | Mace, default ctx | base |
| `versatileWithEmptyOffhandAddsBonus` | Longsword, offhandEmpty=true | base + 1 |
| `versatileWithFilledOffhandUnchanged` | Longsword, offhandEmpty=false | base |
| `finesseWhileSprintingMultiplies` | Rapier, sprinting=true | base × 1.20 |
| `finesseWhileWalkingUnchanged` | Rapier, sprinting=false | base |
| `lightWithLightOffhandAddsOne` | Dagger main, Scimitar offhand | base + 1 |
| `lightWithNonLightOffhandUnchanged` | Dagger main, Longsword offhand | base |
| `lightWithEmptyOffhandUnchanged` | Dagger main, no offhand | base |
| `lanceOnFootHalves` | Lance, hasVehicle=false | base × 0.5 |
| `lanceMountedFullDamage` | Lance, hasVehicle=true | base |
| `finesseAndLightStackOnOneStrike` | Scimitar + Dagger offhand, sprinting | (base + 1) × 1.20 |
| `lanceWithFinesseMultipliersChain` | synthetic LANCE+FINESSE spec, on foot, sprinting | base × 1.20 × 0.5 |

**`WeaponTooltipBuilderTest.kt`** — 10 tests covering each line format variant and one spec per property to confirm bonus-line content (see Section 3.6 for the format spec).

**`SpecRegistryTest.kt`** — 5 tests for the in-process logic (bind/lookup/null/multiple). Role-tag flattening from `BuiltInRegistries` is gametest-only (needs a live MC registry).

### Fabric Gametest — runs per version via `chiseledRunGametest`

**`CombatHooksGametest.kt`** — 6 tests in `fabric-gametest-api-v1:empty`:

| Test | Setup | Verify |
|---|---|---|
| `finesseSprintBonusFires` | player + pig, give rapier, force sprint, attack | pig health drop ≈ 7 (6 × 1.2 rounded by vanilla damage application) |
| `lightDualWieldBonusFires` | dagger main + dagger off, attack pig | drop ≈ base + 1 |
| `versatileEmptyOffhandFires` | longsword, empty offhand, attack pig | drop ≈ base + 1 |
| `lanceOnFootHalves` | lance, no vehicle, attack pig | drop ≈ 4 |
| `lanceMountedFullDamage` | lance, mount second pig, attack third pig | drop ≈ 7 |
| `vanillaIronSwordCarriesFinesseHook` | iron sword, sprint, attack pig | drop ≈ vanilla iron base × 1.2 |

**`TooltipInjectionGametest.kt`** — 2 tests:

- `registeredItemTooltipContainsStatBlock` — Longsword `ItemStack`, get tooltip lines via `Item.getTooltipLines` (or equivalent per-version API resolved at plan time), assert at least one line text contains "slashing".
- `vanillaIronSwordTooltipContainsStatBlock` — vanilla iron sword stack, assert at least one line contains "piercing" and one contains "Finesse".

Two tooltip gametests is enough: unit tests cover every format variant exhaustively; the gametests prove the injector is subscribed and `SpecRegistry` resolves vanilla-mapped items at runtime.

---

## 7. Acceptance Criteria

| # | Criterion | Verification |
|---|---|---|
| 1 | `SpecRegistry.kt` exists; `bindRegistered`, `bindRoleTag`, `lookup`, `invalidateRoleCache` operate per design | `SpecRegistryTest` + tooltip gametest for vanilla-mapped lookup |
| 2 | `WeaponAttackHandler.modifyDamage` produces correct results for all property combinations | `WeaponAttackHandlerTest` (12 tests pass) |
| 3 | `PlayerAttackMixin` injects successfully on every version (`defaultRequire: 1` would fail otherwise) | `chiseledBuild` succeeds; mod loads cleanly on all 5 |
| 4 | `AttributeCompat` emits `ATTACK_KNOCKBACK +1.0` for HEAVY weapons on both epochs | Epoch C: any Greataxe stack's `ItemAttributeModifiers` contains the modifier (gametest assertion); Epoch A: `AttributeCompat.modifiersFor(GREATAXE, MAINHAND)` returns the modifier (unit-test against Epoch A branch) |
| 5 | `WeaponTooltipInjector` registers on init; tooltips on registered items contain the stat-block | `TooltipInjectionGametest.registeredItemTooltipContainsStatBlock` |
| 6 | Tooltip injection fires on vanilla iron sword (Shortsword role) | `TooltipInjectionGametest.vanillaIronSwordTooltipContainsStatBlock` |
| 7 | Finesse sprint bonus fires end-to-end on a registered item (Rapier) | gametest |
| 8 | Finesse sprint bonus fires end-to-end on a vanilla item (iron sword) | gametest |
| 9 | Light dual-wield bonus fires (Dagger + Dagger) | gametest |
| 10 | Versatile offhand-empty bonus fires (Longsword) | gametest |
| 11 | Lance on-foot halves; mounted does not | gametest (2 cases) |
| 12 | All 5 MC versions pass `:build`, `:test`, `:runGametest` | `chiseledBuild`, `chiseledTest`, `chiseledRunGametest` green |
| 13 | `en_us.json` contains tooltip keys: stat-block format + 3 damage-type labels + 10 property labels + 4 bonus strings (finesse_sprint, light_dual, versatile_empty, lance_foot) | grep |
| 14 | Phase 1 + Phase 2 gametests still pass (no regression to registration) | included in `chiseledRunGametest` |

### Plan-runner risk surfaces

1. **Wave 1 — mixin smoke on 1.21.4 only.** Compile `PlayerAttackMixin` against 1.21.4, run `:1.21.4:runGametest` with `finesseSprintBonusFires` only. If `@ModifyVariable` resolves the wrong local or the `@At` target doesn't match, the failure surfaces before per-version fan-out. Wave 1 also lands the `AttributeCompat` HEAVY extension.

2. **Wave 2 — `SpecRegistry` + tooltip injector + remaining mechanic tests on 1.21.4.** Confirms the whole Phase 3 pipeline on the canonical version.

3. **Wave 3 — per-version fan-out (build + test + gametest on the remaining four versions).** Any per-epoch failure (most likely a mixin local-name shift) gets a `//?` fork in `PlayerAttackMixin.kt` and re-runs in a tight loop.

---

## 8. Resolved at Plan Time

These are intentionally not pinned here; the plan-writing step resolves them:

- **Exact `@At` target descriptor** for the `LivingEntity#hurt` invocation in `Player#attack` (Mojang names). Lookup: read the decompiled `Player.class` in `versions/1.21.4/.gradle/loom-cache/` or the equivalent.
- **Exact local-variable name (or ordinal)** for the damage float in `Player.attack` on each of the 5 versions. Same lookup; if it shifts across versions, the per-epoch `//?` fork is generated then.
- **Final translation key spelling** for tooltip strings. Drafted in Section 3.6 (`tooltip.dndweapons.stat_block`, `tooltip.dndweapons.bonus.finesse_sprint`, etc.); the writing-plans agent finalizes and ensures all keys land in `en_us.json`.
- **`CommonLifecycleEvents.TAGS_LOADED` exact import path and event-name** per version (Fabric API has reorganized this once or twice). Resolved at plan time per version.
- **`TagKey` parsing helper** — whether to use `TagKey.create(Registries.ITEM, ResourceLocation.parse(s))` or per-version fork. Resolved at plan time.

---

## 9. QA Swarm 2026-05-18 Implementation Notes

These notes capture deviations from the original spec made during Phase 3 and
discovered during the 2026-05-18 QA swarm. See `docs/qa-swarm/2026-05-18-*.md`
for the underlying findings.

### 9.1 WeaponTooltipInjector lives in ClientModInitializer (P2-002)

The original §5 sequence wired `WeaponTooltipInjector.register()` into
`DndWeaponsMod.onInitialize`. The shipped implementation moves it to
`DndWeaponsClientMod.onInitializeClient()` because `ItemTooltipCallback` is a
client-side API unavailable on dedicated servers - registering it from the main
entrypoint would throw `NoClassDefFoundError` on server-only environments.

The split entrypoints are declared in `fabric.mod.json` under `entrypoints.main`
(DndWeaponsMod) and `entrypoints.client` (DndWeaponsClientMod). DndWeaponsMod
retains an inline comment at the spot where the original spec would have called
the injector, pointing at the client-side wiring.

### 9.2 SpecRegistry.byRoleTag is keyed by String, not TagKey<Item> (P2-005)

The §3 sketch declared `byRoleTag: Map<TagKey<Item>, WeaponSpec>`. The shipped
implementation uses `Map<String, WeaponSpec>` keyed by the raw `"ns:path"`
string. Constructing a `TagKey<Item>` at `bindRoleTag()` time would force
`Registries.<clinit>` and transitively `BuiltInRegistries.<clinit>` to fire,
which throws `IllegalArgumentException("Not bootstrapped")` from JVM unit
tests that don't bootstrap Minecraft's registry. Deferring `TagKey`
construction to `buildRoleCacheAndStore()` (called only at first lookup, which
is guaranteed post-bootstrap on the server thread) avoids the pitfall and
keeps the unit tests free of MC runtime dependencies.

The behaviour is otherwise identical: validation of the `"ns:path"` shape
happens eagerly at bind time so a malformed spec fails fast.

### 9.3 SmithingItemRegistrar facade in Phase 4 (P2-007 - cross-phase)

Phase 4 adds `com.dndweapons.registry.SmithingItemRegistrar` as a one-line
facade over `SmithingComponentItems.registerAll()` + `SmithingTemplateItems
.registerAll()`. This is not in any phase spec but is harmless - it just keeps
`DndWeaponsMod.onInitialize` to a single call site for smithing registration.
Treat it as an undocumented convenience wrapper.

### 9.4 PlayerAttackMixin design (P1-005)

See the "Implementation update" callout in §3 under `PlayerAttackMixin.kt` for
the @ModifyArg + ThreadLocal + TAIL design that ships instead of the original
@ModifyVariable sketch.
