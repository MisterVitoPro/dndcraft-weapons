# DnD Weapons — Phase 4 Design Specification

**Date:** 2026-05-17
**Status:** Approved (brainstorm complete, ready for implementation planning)
**Author:** brainstorm session with the user
**Parent design:** [2026-05-16-dnd-weapons-design.md](2026-05-16-dnd-weapons-design.md)
**Predecessor phase:** [2026-05-17-dnd-weapons-phase-3-design.md](2026-05-17-dnd-weapons-phase-3-design.md) — Phase 3 shipped the combat-hook mixin, tooltip injection, and the 4 vanilla-mapped role tags. Final verification at [phase-3-verification-final.md](../plans/phase-3-verification-final.md).
**Implementation plan:** TBD (next step — invoke `superpowers:writing-plans`)

---

## 1. Goal & Scope

**Goal:** Add a smithing upgrade ladder (iron → diamond → netherite) to the 27 registered melee + thrown DnD weapons, with two custom upgrade templates acquired through loot, wandering-trader trades, or a multi-step end-game crafting recipe.

### In scope

| Item | Count | Source |
|---|---|---|
| Tiered weapon items (diamond + netherite variants of 27 base specs) | 54 | new registrations |
| Smithing upgrade templates (diamond + netherite) | 2 | new registrations |
| Smithing-component items (4 fragments/bindings + 2 cores) | 6 | new registrations |
| **Total new items** | **62** | |
| `smithing_transform` recipes (27 weapons × 2 upgrades) | 54 | data |
| Component crafting recipes (4 fragments/bindings + 2 cores) | 6 | data |
| Template assembly recipes (smithing) | 2 | data |
| **Total new recipes** | **62** | |
| Loot-table injections (stronghold-library, bastion-treasure) | 2 | data + code |
| Wandering-trader trade injections | 2 | code |
| Gametests covering tier upgrades + fire immunity + Phase 3 carryover | 3 | code |

Plus all the supporting infrastructure: `Tier` enum and `WeaponSpec.atTier()` helper, 66 new lang entries (see §8), 62 textures via Gemini (see §7), 62 item-model JSONs auto-emitted from a template.

### Explicitly out of scope

- **Vanilla-mapped weapons** (shortsword, shortbow, light_crossbow, trident) — they keep vanilla's existing material progression. No `diamond_shortsword` is introduced. This matches the master spec's row "Vanilla mapping treatment: smithing upgrade NOT applied".
- **Ranged DnD weapons** (longbow, heavy_crossbow, hand_crossbow, blowgun, sling, musket, pistol) — no tier ladder. The "diamond longbow" fantasy is a stretch and the per-weapon art burden doubles for marginal value.
- **Mob drops, advancement triggers, villager (non-wandering) trades** — Phase 5.
- **Wiki entries for tiered items** — Phase 6.
- **Per-tier 3D model overrides** — the model JSON shape stays `item/generated` with one `layer0` PNG, only the texture differs per tier.
- **Tier rebalance for the base iron stats** — the existing 21 melee + 6 thrown specs are unchanged. The diamond/netherite specs are computed from them, not the other way around.

---

## 2. Decisions

| # | Decision | Choice | Rationale |
|---|---|---|---|
| 1 | Tier set | Iron base → Diamond → Netherite | Master spec §"Upgrade ladder". All 3 materials exist in every target MC version (1.20.1–26.1.2). |
| 2 | Tier scope | 27 weapons (21 melee + 6 thrown) | Brainstorm decision. Ranged weapons (bow/crossbow/firearm/sling/blowgun) keep their single-tier treatment; tiering them adds 14 more items and dilutes the "weapon-as-blade" fantasy. |
| 3 | Stat scaling | Vanilla pattern: `+1 damage` per tier above iron, durability follows vanilla material, netherite is fire-immune | Brainstorm decision. Matches vanilla MC sword/axe progression so it feels native. |
| 4 | Attack speed, reach, properties | Unchanged across tiers | Tier is a pure power-level upgrade; the weapon's identity (FINESSE, HEAVY, etc.) stays. Simplifies Phase 3 mixin reuse. |
| 5 | Item ID convention | `<base_id>_<tier>` (lowercase), e.g. `longsword_diamond`, `longsword_netherite` | Matches the existing convention used by Phase 2b for compound IDs (`hand_crossbow`, `heavy_crossbow`). |
| 6 | Display-name convention | `<Tier> <Base>`, e.g. "Diamond Longsword", "Netherite Greataxe" | Matches vanilla naming ("Diamond Sword"). |
| 7 | Upgrade mechanic | `minecraft:smithing_transform` | Master spec §"Upgrade mechanic". Works on all target versions; familiar UX. |
| 8 | Template acquisition | Loot + wandering-trader trades + multi-step crafting (all three) | Brainstorm decision. Loot/trader give early discovery; crafting unblocks creative / skyblock progression. |
| 9 | Craft difficulty | Multi-step component crafting (4 components + smithing assembly) | Brainstorm decision. Vanilla-style duplication wouldn't satisfy "epic and challenging"; multi-step gives a tangible progression arc. |
| 10 | Catalog wiring | `Tier` enum + `WeaponSpec.atTier()` extension function, `Weapons.ALL_TIERED` exposes the cartesian product | Brainstorm decision (Approach B). One source of truth in code; recipes/lang derive from it. |
| 11 | Asset generation | Gemini via `minecraft-asset-generator` agent (per-weapon, per-tier prompts) | Brainstorm decision. Avoids 54 hand-drawn textures. |
| 12 | Model JSONs | Auto-emitted at build (or once, committed to repo) from a single `item/generated` template | Every tiered item has the same model shape; checking in 54 near-identical JSONs is fine but a small data-gen pass is cleaner. |
| 13 | Phase 3 compatibility | Each tiered item is its own `WeaponSpec` registered via `SpecRegistry.bindRegistered`. Mixin behavior is identical. | Each tier shares the parent's `properties`, so the FINESSE/HEAVY/LIGHT/VERSATILE/SPECIAL_LANCE bonuses apply unchanged to higher tiers. |
| 14 | Common-tag references | `c:gems/diamond`, `c:ingots/netherite` for smithing additions | Master spec §"Material references". Lets other mods' diamond/netherite ingredients work as upgrade material. |
| 15 | **Master spec override (QA Swarm 2026-05-18, P1-001, P1-002)** | Phase 4 governs tier stats: Diamond=`base + 1` damage, Netherite=`base + 2` damage, HEAVY knockback is flat `1.0` at every tier (no tier-keyed Netherite knockback bonus) | Master spec §7 originally specified +2/+3 damage and a Netherite Heavy `+1` knockback column. Row 3 above (vanilla pattern) explicitly overrides the damage progression to match MC swords/axes; the netherite knockback bonus was never implemented because tiers should be pure power-level upgrades (row 4: "Attack speed, reach, properties unchanged across tiers" - knockback is a property-derived attribute and stays unchanged too). The master spec §7 table has been updated to match this implementation; this row is the authoritative decision record. |

---

## 3. Tier specification

```kotlin
enum class Tier(val suffix: String, val displayPrefix: String, val damageBonus: Int, val durability: Int, val fireImmune: Boolean) {
    IRON      (suffix = "",          displayPrefix = "",         damageBonus = 0, durability = 250,  fireImmune = false),
    DIAMOND   (suffix = "_diamond",  displayPrefix = "Diamond ", damageBonus = 1, durability = 1561, fireImmune = false),
    NETHERITE (suffix = "_netherite",displayPrefix = "Netherite ",damageBonus = 2, durability = 2031, fireImmune = true),
}
```

`durability` numbers match vanilla MC `Tiers.IRON`/`Tiers.DIAMOND`/`Tiers.NETHERITE` (per `net.minecraft.world.item.Tiers` enum). Numeric values are pinned in the enum, not derived from MC's `Tier.getUses()`, so the spec doesn't depend on the per-version `Tiers` class shape.

### Damage progression example

| Base weapon | Iron | Diamond | Netherite |
|---|---|---|---|
| `longsword` (base 6) | 6 | 7 | 8 |
| `greataxe` (base 8) | 8 | 9 | 10 |
| `dagger` (base 4) | 4 | 5 | 6 |
| `lance` (base 7) | 7 | 8 | 9 |
| `rapier` (base 6) | 6 | 7 | 8 |

> **P3-002 correction (QA Swarm 2026-05-18):** the `longsword` base damage is
> 6 (not 5); the example table previously listed `5 | 6 | 7` which conflicted
> with `Weapons.LONGSWORD.attackDamage = 6` (rounded from the dice formula
> `round(4.5 + 1.5) = 6`). Implementation is unchanged; doc-only fix.

The DnD combat properties stack on top:
- `Netherite Longsword` versatile-empty: `8 + 1 = 9` damage (same +1 versatile bonus, applied after tier).
- `Netherite Rapier` finesse-sprint: `8 × 1.20 = 9.6` damage.

### Fire immunity (netherite tier)

`DndWeaponItem.Properties` for netherite-tier items calls `.fireResistant()` (the Mojang-named `Item.Properties.fireResistant()` method that flags the item as immune to fire damage when in entity form). This matches vanilla `Items.NETHERITE_SWORD` / `Items.NETHERITE_INGOT` behavior. All 5 MC target versions expose this method with the same signature.

---

## 4. Catalog wiring (Approach B)

### `WeaponSpec` additions

`WeaponSpec` already has `attackDamage: Int` and `baseDurability: Int` fields that fit cleanly. No schema change to `WeaponSpec` is required. Add an extension function in `Weapons.kt`:

```kotlin
fun WeaponSpec.atTier(t: Tier): WeaponSpec = copy(
    id           = if (t == Tier.IRON) id else "$id${t.suffix}",
    displayName  = if (t == Tier.IRON) displayName else "${t.displayPrefix}$displayName",
    attackDamage = attackDamage + t.damageBonus,
    baseDurability = t.durability,
)
```

### `Weapons.ALL_TIERED`

```kotlin
val ALL_TIERED: List<Pair<WeaponSpec, Tier>> = ALL
    .filter { it.vanillaRoleTag == null && it.ranged in setOf(RangeKind.NONE, RangeKind.THROWN) }
    .flatMap { base -> Tier.values().map { tier -> base.atTier(tier) to tier } }
```

This produces 27 × 3 = 81 entries, of which the 27 IRON entries are equivalent to the existing `Weapons.ALL` content (so `WeaponRegistrarImpl.register` would double-register without deduping). Deduplication strategy: registration code switches to iterating `ALL_TIERED` and uses the existing `WeaponSpec.id` collision check in `SpecRegistry.bindRegistered`. The IRON entries from `ALL_TIERED` are reference-equal to the existing ones; the registrar treats them as the canonical iron-tier spec and the existing `ALL` list is replaced or aliased to `ALL_TIERED.filter { (_, t) -> t == Tier.IRON }.map { it.first }`.

### `DndWeaponItem.Properties` wiring

`WeaponRegistrarImpl.register(spec, tier)` is updated to take a Tier parameter and conditionally call `properties.fireResistant()` when `tier.fireImmune == true`. The existing `register(spec)` signature stays as an `register(spec, Tier.IRON)` shim for callers outside the new loop (none expected after Phase 4 lands).

### Backward compatibility

No `RangeKind` or `Property` enum changes. The tier is **not** an enum value on `WeaponSpec` — it's a construction-time parameter folded into the resulting spec's `id`, `displayName`, `attackDamage`, and `baseDurability`. This keeps `WeaponSpec`'s data shape unchanged from Phase 2b, so Phase 3 tests, `SpecRegistry`, `WeaponAttackHandler`, `WeaponTooltipBuilder`, and any external consumers compile and behave identically.

---

## 5. Smithing system

### Per-weapon transforms (54 recipes)

```json
{
  "type": "minecraft:smithing_transform",
  "template": { "id": "dndweapons:diamond_weapon_upgrade_template" },
  "base":     { "id": "dndweapons:longsword" },
  "addition": { "tag": "c:gems/diamond" },
  "result":   { "id": "dndweapons:longsword_diamond" }
}
```

```json
{
  "type": "minecraft:smithing_transform",
  "template": { "id": "dndweapons:netherite_weapon_upgrade_template" },
  "base":     { "id": "dndweapons:longsword_diamond" },
  "addition": { "tag": "c:ingots/netherite" },
  "result":   { "id": "dndweapons:longsword_netherite" }
}
```

Recipes are emitted at build time by a small Kotlin data-generation script (one-off iteration over `Weapons.ALL_TIERED` filtered to non-IRON entries, paired with the prior-tier spec). Implementation may instead choose to hand-emit them; the spec is agnostic.

**Recipe path:** `data/dndweapons/recipe/<base_id>_<tier>.json`. (1.21.1+ uses `recipe/` singular per the MC 1.21 data-pack convention; pre-1.21 uses `recipes/`. Stonecutter forks the directory at build time — same pattern as the role-tag `tags/item/` vs `tags/items/` split already in the repo per Phase 3 fix.)

### Template items

| Template | Item ID | Smithing role |
|---|---|---|
| Diamond | `dndweapons:diamond_weapon_upgrade_template` | Template slot of any base→diamond transform |
| Netherite | `dndweapons:netherite_weapon_upgrade_template` | Template slot of any diamond→netherite transform |

Both extend `SmithingTemplateItem` (vanilla MC class, available on all target versions with a 5-version-stable constructor that takes display-name components and slot icons). The constructor parameters differ slightly between 1.20.1 and 1.21+; the implementation will fork the construction path via stonecutter (similar to the @ModifyArg target string forks in `PlayerAttackMixin`).

### Component items (multi-step craft)

| Component | Item ID | Tier |
|---|---|---|
| Diamond template fragment | `dndweapons:diamond_template_fragment` | mid-game |
| Weapon smithing binding | `dndweapons:weapon_smithing_binding` | mid-game |
| Netherite template fragment | `dndweapons:netherite_template_fragment` | end-game |
| Infernal binding | `dndweapons:infernal_binding` | end-game |

All four extend `Item` directly with default `Item.Properties()`. No combat behavior, no enchantments. They are pure crafting ingredients.

### Component crafting recipes

```
diamond_template_fragment (×4 output per craft):
  [P][D][P]
  [F][D][F]
  [P][D][P]
  P = paper, D = c:gems/diamond, F = flint
  → 4 × diamond_template_fragment

weapon_smithing_binding (×1):
  [S][L][S]
  [L][B][L]
  [S][L][S]
  S = string, L = leather, B = blaze rod
  → 1 × weapon_smithing_binding

netherite_template_fragment (×4 output per craft):
  [O][S][O]
  [G][N][G]
  [O][S][O]
  O = obsidian, S = c:gems/quartz, G = ghast tear, N = netherite scrap
  → 4 × netherite_template_fragment

infernal_binding (×1):
  [B][N][B]
  [S][*][S]
  [B][N][B]
  B = blaze powder, N = nether star (yes, one per craft of the binding), S = soul sand, * = wither rose
  → 1 × infernal_binding
```

These costs are intentionally heavy. The diamond binding is mid-game cost (blaze rod is the rarest single ingredient). The netherite binding consumes a nether star and a wither rose per craft — every netherite template requires a Wither kill.

### Template assembly recipes (smithing, not crafting)

Final assembly uses the smithing table to reinforce the "smithing system" theme. Smithing requires 3 slots (template + base + addition) so the recipe shape is:

```
Diamond weapon upgrade template:
  template: any vanilla smithing template (e.g. minecraft:netherite_upgrade_smithing_template) — consumed as a "catalyst"
  base:     dndweapons:weapon_smithing_binding (×1)
  addition: dndweapons:diamond_template_fragment (×4 in a stack; smithing_transform consumes 1 — see note)
  result:   dndweapons:diamond_weapon_upgrade_template
```

**Note on smithing-transform stack semantics:** vanilla `smithing_transform` consumes exactly 1 item from each input slot. To require 4 fragments, we either:
  (a) use a custom recipe type that consumes N from a slot (introduces a new recipe-serializer; bigger blast radius);
  (b) use 4 individual single-fragment smithing crafts and require the player to perform the smithing 4× (clunky UX, requires intermediate state — discouraged); or
  (c) **chosen**: introduce an intermediate `dndweapons:diamond_template_core` crafting recipe (3×3 in a regular crafting table: 4 fragments + 1 binding → 1 core), and the smithing-table assembly consumes 1 core + 1 catalyst-template + 1 binding-stub.

The implementation plan will lock in option (c): the spec mandates that the player's path from "raw materials" to "diamond template" requires both a crafting-table step (assemble fragments + binding into a core) AND a smithing-table step (apply the core with a vanilla template catalyst to produce the final template).

Concretely:

```
crafting_table:
  4× diamond_template_fragment + 1× weapon_smithing_binding (any 3×3 layout, shapeless)
  → 1× dndweapons:diamond_template_core

smithing_table:
  template: minecraft:netherite_upgrade_smithing_template   (returns to player after smithing — vanilla preserves the template slot)
  base:     dndweapons:diamond_template_core   (consumed)
  addition: c:gems/diamond                     (consumed)
  → 1× dndweapons:diamond_weapon_upgrade_template
```

The 2 core items (`diamond_template_core`, `netherite_template_core`) are already counted in the §1 total of 62.

Mirror structure for netherite tier:
- `crafting_table`: 4 netherite fragments + 1 infernal binding → 1 `netherite_template_core`
- `smithing_table`: vanilla netherite_upgrade_smithing_template (catalyst) + netherite_template_core + `c:ingots/netherite` → 1 netherite template

---

## 6. Template acquisition

### a. Loot injection

Use the version-gated Loot API shim (already in the repo from Phase 2b — `LootTableEvents` v2 on ≤1.21, v3 on 1.21.1+; one stonecutter fork file).

| Loot table | Pool | Item | Weight | Notes |
|---|---|---|---|---|
| `minecraft:chests/stronghold_library` | new pool | `dndweapons:diamond_weapon_upgrade_template` | 5 (low) | Library books fit the "ancient knowledge" theme |
| `minecraft:chests/bastion_treasure` | new pool | `dndweapons:netherite_weapon_upgrade_template` | 3 (lower) | Matches vanilla netherite-template distribution in bastions |

Both pools use a single roll with `count: 1` and a `quality: 2` to slightly bias toward looting-enchanted chests.

### b. Wandering-trader trades

Inject via Fabric's `TradeOfferHelper.registerWanderingTraderOffers(level, ...)`. Two RARE trades (level 2):

| Trade | Cost | Item |
|---|---|---|
| Diamond template | 32 emeralds + 1 emerald block | 1 × `dndweapons:diamond_weapon_upgrade_template` |
| Netherite template | 48 emeralds + 1 diamond block | 1 × `dndweapons:netherite_weapon_upgrade_template` |

Trades are uses-capped at 1 per spawn (max-uses = 1) so each trader-spawn only offers the trade once.

### c. Multi-step craft

Specified in §5 above. Six total recipes (4 components + 2 cores + 2 final smithing assemblies) for both templates combined.

---

## 7. Asset strategy

### Textures

Generated via the `minecraft-asset-generator` agent at `D:/minecraft/.claude/agents/minecraft-asset-generator.md`, which uses Gemini Image. Per-tier prompts follow this template:

```
Minecraft texture, 16x16 pixel art, single item icon, transparent background.
Subject: a <tier> <weapon-display-name>.
Style: matches the iron-tier <weapon-display-name> shape exactly (preserve hilt, guard, blade silhouette);
       only the blade material color shifts to <tier-color>:
       - diamond: cyan (#5DECF5) blade with light gray (#A9A9A9) accents
       - netherite: dark grey-brown (#443A3B) blade with faint purple (#5B2A5C) highlights
Lighting: top-left light source, single-pixel shading on the blade edge.
Output: PNG, 16x16, no antialiasing.
```

The implementation plan dispatches the agent in batches (e.g. 5 weapons at a time × 2 tiers) and writes results to `src/main/resources/assets/dndweapons/textures/item/<base_id>_<tier>.png`.

The 8 smithing-system textures (4 fragments/bindings + 2 cores + 2 templates) need their own one-off prompts:

| Asset | Prompt subject |
|---|---|
| Diamond template fragment | "shard of a glowing diamond rune, broken edge" |
| Netherite template fragment | "shard of a dark netherite rune with faint purple glow" |
| Weapon smithing binding | "spool of waxed cord wrapped around a brass rivet" |
| Infernal binding | "spool of nether-charred cord around a blackstone rivet" |
| Diamond template core | "circular medallion engraved with the diamond rune complete" |
| Netherite template core | "circular medallion engraved with the netherite rune complete" |
| Diamond weapon upgrade template | "ornate cyan smithing template, vanilla template silhouette, diamond-rune center" |
| Netherite weapon upgrade template | "ornate dark smithing template, vanilla template silhouette, netherite-rune center" |

### Model JSONs

Auto-generated once and committed (54 weapon tier models + 8 component models). Every model is the standard:

```json
{ "parent": "minecraft:item/generated", "textures": { "layer0": "dndweapons:item/<id>" } }
```

A small gradle task (or a one-shot Kotlin main) writes them to `src/main/resources/assets/dndweapons/models/item/<id>.json` at spec-implementation time. The repo-committed result is the source of truth thereafter; the task does not run on every build.

---

## 8. Lang strings

All new strings land in `src/main/resources/assets/dndweapons/lang/en_us.json`.

| Key prefix | Count | Example |
|---|---|---|
| `item.dndweapons.<base>_diamond` | 27 | `"item.dndweapons.longsword_diamond": "Diamond Longsword"` |
| `item.dndweapons.<base>_netherite` | 27 | `"item.dndweapons.longsword_netherite": "Netherite Longsword"` |
| `item.dndweapons.<component>` (4 fragments/bindings + 2 cores) | 6 | `"item.dndweapons.diamond_template_fragment": "Diamond Template Fragment"` |
| `item.dndweapons.<template>` | 2 | `"item.dndweapons.diamond_weapon_upgrade_template": "Diamond Weapon Upgrade Template"` |
| `tooltip.dndweapons.smithing.<template>.applies_to` | 2 | smithing-table UI tooltip line (vanilla pattern) |
| `tooltip.dndweapons.smithing.<template>.ingredients` | 2 | second tooltip line |

Total: **66 new lang entries.**

The generator that emits item-model JSONs also emits lang entries to keep them in sync.

---

## 9. Phase 3 compatibility

The Phase 3 `PlayerAttackMixin` calls `SpecRegistry.lookup(self.mainHandItem.item)`. Each tiered item is its own registered `Item` with its own `WeaponSpec` bound in `SpecRegistry.byItem`. The mixin treats `dndweapons:longsword_diamond` identically to `dndweapons:longsword` — the spec's `properties` are shared, so FINESSE/HEAVY/LIGHT/VERSATILE/SPECIAL_LANCE bonuses fire on every tier.

The tier's `damageBonus` is baked into the `WeaponSpec.attackDamage` field, which `AttributeCompat` turns into the `minecraft:generic.attack_damage` attribute modifier on the item. So the +1 / +2 tier bonus reaches `Player.attack()` through the vanilla attribute pipeline, **not** through the mixin. The DnD bonuses stack on top via the mixin's `@ModifyArg`. Tier × DnD multipliers compose multiplicatively/additively as `WeaponAttackHandler.modifyDamage` already does — the tier just shifts the input damage upward.

`WeaponTooltipBuilder` is unchanged: each tier's spec produces the same stat-block + bonus lines as the iron base. The new items inherit the entire Phase 3 tooltip pipeline for free.

---

## 10. Testing

Three new gametests in `src/main/kotlin/com/dndweapons/test/SmithingGametest.kt`:

### `smithingDiamondUpgradePreservesSpec`

Strategy:
1. `SpecRegistry.lookup(Items.getById("dndweapons:longsword_diamond"))` returns a non-null spec.
2. Returned spec's `attackDamage` is `longsword_iron.attackDamage + 1` = `6 + 1 = 7`.
3. Returned spec's `properties` equals `longsword_iron.properties` (set-equality).
4. Returned spec's `id` is `"longsword_diamond"`.

Pure registry inspection — no smithing-table simulation needed. The smithing-table data recipe is validated structurally by being read into the recipe registry at server startup; a malformed JSON would crash the server before tests run.

### `netheriteFireImmunityFires`

Strategy:
1. Spawn an `ItemEntity` holding a `longsword_netherite` ItemStack in the test world.
2. Place it inside a `Blocks.LAVA` block.
3. Run the server for 60 ticks.
4. Assert the `ItemEntity` is still alive (`!entity.isRemoved`).
5. Repeat with `longsword` (iron) and assert it IS removed within 30 ticks (sanity check on the fire-immune logic — iron should burn).

### `tieredItemTriggersDnDMixin`

Strategy:
1. Mock player with `longsword_diamond` mainhand, empty offhand (VERSATILE path from Phase 3).
2. `primeAttackStrength(player)` + `applyMainHandModifiers(player)`.
3. `player.attack(pig)`.
4. Assert `dealt` ≈ `(longsword base 6 + diamond +1) + versatileBonus 1` = `8`, within `±0.5` tolerance.

This proves the tier bonus passes through the vanilla attribute pipeline AND the DnD mixin fires correctly on the tiered item.

### CI

`.github/workflows/build.yml` already runs `chiseledRunGametest` across all 5 versions. The three new tests slot in automatically.

---

## 11. File-by-file change list (forward-looking)

| Path | New / Modified | Purpose |
|---|---|---|
| `src/main/kotlin/com/dndweapons/catalog/Tier.kt` | new | The `Tier` enum |
| `src/main/kotlin/com/dndweapons/catalog/Weapons.kt` | modified | `atTier()` extension, `ALL_TIERED` list |
| `src/main/kotlin/com/dndweapons/item/SmithingTemplateItems.kt` | new | The 2 template-item classes + 2 core items + 4 component items |
| `src/main/kotlin/com/dndweapons/registry/WeaponRegistrarImpl.kt` | modified | Takes Tier param, calls `.fireResistant()` for netherite |
| `src/main/kotlin/com/dndweapons/DndWeaponsMod.kt` | modified | Calls `register(spec, tier)` for every entry in `ALL_TIERED` instead of `register(spec)` for `ALL` |
| `src/main/kotlin/com/dndweapons/loot/SmithingTemplateLootInjector.kt` | new | Loot-table injection for stronghold-library + bastion-treasure |
| `src/main/kotlin/com/dndweapons/trade/SmithingTemplateTrades.kt` | new | Wandering-trader trade registration |
| `src/main/kotlin/com/dndweapons/test/SmithingGametest.kt` | new | The 3 gametests above |
| `src/main/resources/data/dndweapons/recipe/<base>_diamond.json` | new × 27 | Iron→diamond smithing-transform recipes |
| `src/main/resources/data/dndweapons/recipe/<base>_netherite.json` | new × 27 | Diamond→netherite smithing-transform recipes |
| `src/main/resources/data/dndweapons/recipe/<component>.json` | new × 6 | Fragment / binding / core crafting recipes |
| `src/main/resources/data/dndweapons/recipe/<template>_assemble.json` | new × 2 | Final smithing-table template assembly |
| `src/main/resources/data/dndweapons/tags/item/role/...` | unchanged | No role tag changes; tiered items live alongside their base |
| `src/main/resources/assets/dndweapons/lang/en_us.json` | modified | 68 new entries |
| `src/main/resources/assets/dndweapons/models/item/<id>.json` | new × 62 | Model JSONs (auto-generated, committed) |
| `src/main/resources/assets/dndweapons/textures/item/<id>.png` | new × 62 | Textures (Gemini-generated via agent) |
| `src/main/resources/fabric.mod.json` | (no change) | Existing entries already cover the new items |
| `docs/superpowers/plans/2026-05-1?-dnd-weapons-phase-4-...md` | new | Implementation plan (next step) |

---

## 12. Open questions / risks

| # | Topic | Risk | Mitigation |
|---|---|---|---|
| 1 | Smithing-transform consumes 1 from each slot | Vanilla recipe semantics don't allow "4 fragments per craft" in smithing slots | Spec §5 locks in the crafting-table-cores intermediate step; smithing-table only consumes 1 core + 1 catalyst-template + 1 addition |
| 2 | `SmithingTemplateItem` constructor signature differs between 1.20.1 and 1.21+ | Compile/runtime break on one fork | Stonecutter forks the construction path; mirror the pattern already used in `PlayerAttackMixin` for `@At` target descriptors |
| 3 | Loot-table API version skew (v2 ≤1.21 vs v3 ≥1.21.1) | One-version regression | Reuse the v2/v3 shim from Phase 2b (already proven) |
| 4 | Wandering-trader trade injection API may differ across versions | Same as #3 | Fabric API has provided `TradeOfferHelper.registerWanderingTraderOffers` since 1.20.x; behaviorally stable, but the trade-builder type names sometimes shift |
| 5 | Gemini-generated textures may not match the existing iron-tier art style | Visual incoherence | Implementation plan dispatches the agent with the iron texture attached as a style reference (the agent supports image inputs) |
| 6 | 62 new items pushes us toward MC's per-mod registry size limits | Negligible — limit is in the thousands | Document the new total in the implementation plan |
| 7 | Phase 3 gametests do not exercise tiered items | Regressions in Phase 4 could quietly invalidate Phase 3 assumptions | The new `tieredItemTriggersDnDMixin` test covers this; iron-tier tests continue to run unchanged |
| 8 | The recipes shipped in this phase require finding a vanilla netherite_upgrade_smithing_template as the "catalyst" in template assembly | Players may not have one yet | Loot/trader paths exist for early bootstrap; the catalyst returns to the player after assembly (vanilla smithing-template-slot behavior) |

---

## 13. Approved by

User approval received on 2026-05-17 during brainstorm session. Spec written 2026-05-17.

Next step: `superpowers:writing-plans` to produce the implementation plan.
