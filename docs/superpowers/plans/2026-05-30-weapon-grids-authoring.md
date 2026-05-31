# Weapon Shape-Grid Authoring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Author the remaining 95 `art/shapes/<id>.json` pixel grids so every dnd-weapons item texture is generated from JSON by the existing converter, replacing the pre-existing placeholder/noisy PNGs with clean 16x16 sprites.

**Architecture:** The pipeline already exists (`scripts/render_textures.py` renders `art/shapes/*.json` + `art/palettes/*.json` into 16x16 RGBA PNGs). This plan only adds shape grids — one JSON file per weapon — in themed batches by weapon family. No converter, palette, or skill changes are required (palettes already cover every material). Each task authors a family of grids, renders them, verifies them, and commits.

**Tech Stack:** Python 3.14 + Pillow 12.1 (already installed); pytest for the converter's own tests; the `pixel-art-generator` skill for authoring guidance.

---

## Background every task depends on

Read these once before starting any task:

- **Spec:** `docs/superpowers/specs/2026-05-30-weapon-asset-generator-design.md`
- **Skill (authoring guidance):** `.claude/skills/pixel-art-generator/SKILL.md` — contains the
  semantic char convention, composition rules, the Shape Lookup, and the per-weapon
  palette override mapping. This plan does NOT repeat the char table; follow the skill.
- **Reference grid:** `art/shapes/longsword.json` — the committed quality bar. New grids
  match its style (2px-wide forms, upper-left highlight, stair-stepped diagonals,
  handle bottom-left / head top-right).

### Shape file contract (every grid must satisfy)

```json
{
  "id": "<weapon_id>",          // MUST equal the filename stem
  "size": 16,                    // MUST be 16
  "outputs": { "<png_basename>": "<palette_name>", ... },
  "rows": [ /* exactly 16 strings of exactly 16 chars; '.' = transparent */ ]
}
```

### Semantic chars (from the skill — use only these unless you add to the skill+palette first)

`.` transparent · `B`/`b`/`s` primary hi/mid/shadow · `W`/`w` wood hi/mid · `k` wrap ·
`f` fitting · `p` pommel · `a` tier accent · `e` ember (netherite) · `d` drawstring ·
`R`/`r`/`t` brass hi/mid/shadow (flintlock) · `i` inlay-mid (diamond shard).

Every non-`.` char used in a grid MUST be defined in EVERY palette that grid's
`outputs` reference. If a needed role is missing, add it to the palette file(s) and
the skill's char table FIRST, then author the grid (do not inline a hex).

### Definition of done for a single grid (the "test" for pixel-art tasks)

A grid is done when ALL of these hold:

1. `python scripts/render_textures.py --check` reports it valid (16x16, all chars
   defined, palettes resolve).
2. `python scripts/render_textures.py --only <id>` writes one PNG per `outputs` entry.
3. Each output PNG is exactly 16x16 and mode RGBA (verified by the snippet below).
4. The ASCII preview reads as the intended weapon silhouette (handle bottom-left,
   head/blade top-right, except the documented sling/blowgun/longbow exceptions).

### Reusable verification snippet (used in every task's verify step)

Save nothing — paste inline. Replace the id list per task.

```bash
python - <<'PY'
from PIL import Image
ids = ["dagger","greatsword","rapier","scimitar"]   # <-- edit per task
import glob, json, os
base = "src/main/resources/assets/dndweapons/textures/item"
for sid in ids:
    shape = json.load(open(f"art/shapes/{sid}.json", encoding="utf-8"))
    for out in shape["outputs"]:
        p = f"{base}/{out}.png"
        img = Image.open(p).convert("RGBA")
        assert img.size == (16,16), f"{out} is {img.size}"
        print(f"--- {out} ({img.mode}) ---")
        for y in range(16):
            print("".join("  " if img.getpixel((x,y))[3]==0 else "##" for x in range(16)))
PY
```

---

## Task 1: Blades family (dagger, greatsword, rapier, scimitar)

`longsword` is already done; this task does the four remaining bladed swords.

**Files:**
- Create: `art/shapes/dagger.json`
- Create: `art/shapes/greatsword.json`
- Create: `art/shapes/rapier.json`
- Create: `art/shapes/scimitar.json`

All four use the standard metal tiers. Each `outputs` is exactly:

```json
"outputs": { "<id>": "iron", "<id>_diamond": "diamond", "<id>_netherite": "netherite" }
```

Per-weapon silhouette (from the skill Shape Lookup), chars to use:

- **dagger** — short double-edged blade ~5 cells upper-right (`B`/`b`), 2-cell
  crossguard (`f`), 3-cell grip (`k`), tiny pommel (`p`). Compact, ~9 cells diagonal.
- **greatsword** — blade nearly fills the diagonal, 10+ cells (`B`/`b`/`s`), wide
  4-cell crossguard (`f`), long grip (`k`), prominent round pommel (`p`).
- **rapier** — very thin 8-cell blade (`B`/`b`), ornate swept guard 3 cells at base
  (`f`, stepped quillons), wrapped grip (`k`), small pommel (`p`). Slim profile.
- **scimitar** — single-edged curved blade arcing the diagonal, stair-stepped ~10
  cells (`B`/`b`, shadow `s` on the inner curve), slim crossguard (`f`), grip (`k`),
  small pommel (`p`).

- [ ] **Step 1: Author the four shape files**

For each id, create `art/shapes/<id>.json` matching the contract above, hand-placing
the semantic chars into 16 rows of 16 chars per the silhouette notes. Match
`longsword.json` style. Use only chars `B b s f k p .`.

- [ ] **Step 2: Validate all shapes**

Run: `python scripts/render_textures.py --check`
Expected: `All N shape(s) ... are valid.` (no errors). If a grid is off-size or uses
an undefined char, the converter names the file/row/col — fix the grid, do not relax
the converter.

- [ ] **Step 3: Render the family**

Run: `python scripts/render_textures.py --only dagger && python scripts/render_textures.py --only greatsword && python scripts/render_textures.py --only rapier && python scripts/render_textures.py --only scimitar`
Expected: 3 `rendered <id>*.png` lines per weapon (12 PNGs total).

- [ ] **Step 4: Verify 16x16 + visual silhouette**

Run the reusable verification snippet with `ids = ["dagger","greatsword","rapier","scimitar"]`.
Expected: every PNG asserts 16x16 RGBA; each ASCII preview reads as the intended
sword. If a silhouette is unreadable, revise the grid and re-render.

- [ ] **Step 5: Commit**

```bash
git add art/shapes/dagger.json art/shapes/greatsword.json art/shapes/rapier.json art/shapes/scimitar.json \
  src/main/resources/assets/dndweapons/textures/item/dagger.png src/main/resources/assets/dndweapons/textures/item/dagger_diamond.png src/main/resources/assets/dndweapons/textures/item/dagger_netherite.png \
  src/main/resources/assets/dndweapons/textures/item/greatsword.png src/main/resources/assets/dndweapons/textures/item/greatsword_diamond.png src/main/resources/assets/dndweapons/textures/item/greatsword_netherite.png \
  src/main/resources/assets/dndweapons/textures/item/rapier.png src/main/resources/assets/dndweapons/textures/item/rapier_diamond.png src/main/resources/assets/dndweapons/textures/item/rapier_netherite.png \
  src/main/resources/assets/dndweapons/textures/item/scimitar.png src/main/resources/assets/dndweapons/textures/item/scimitar_diamond.png src/main/resources/assets/dndweapons/textures/item/scimitar_netherite.png
git commit -m "feat(textures): author blade-family shape grids (dagger, greatsword, rapier, scimitar)"
```

---

## Task 2: Axes family (battleaxe, greataxe, handaxe)

**Files:**
- Create: `art/shapes/battleaxe.json`
- Create: `art/shapes/greataxe.json`
- Create: `art/shapes/handaxe.json`

Standard metal tiers; `outputs` = `{ "<id>": "iron", "<id>_diamond": "diamond", "<id>_netherite": "netherite" }`.

- **battleaxe** — broad single-bit head ~5 cells tall top-right (`B`/`b`/`s`), short
  straight haft ~8 cells down the diagonal (`w`, wrap `k` optional), small iron butt
  cap (`f`).
- **greataxe** — long shaft (`w`), oversized head ~6 tall x 4 wide top-right
  (`B`/`b`/`s`) dominating the silhouette.
- **handaxe** — short 4-cell haft (`w`), single-bit head 3 cells top-right
  (`B`/`b`), butt cap (`f`). Compact ~8 cells.

Note: axe heads are metal (`B`/`b`/`s`); shafts are wood (`w`, with `W` highlight if
desired). All these chars exist in iron/diamond/netherite palettes.

- [ ] **Step 1: Author `battleaxe.json`, `greataxe.json`, `handaxe.json`** per the
  notes above, 16x16, chars from `B b s w W k f .`.

- [ ] **Step 2: Validate** — Run: `python scripts/render_textures.py --check` — Expected: all valid.

- [ ] **Step 3: Render** — Run: `python scripts/render_textures.py --only battleaxe && python scripts/render_textures.py --only greataxe && python scripts/render_textures.py --only handaxe` — Expected: 9 PNGs.

- [ ] **Step 4: Verify** — Run the snippet with `ids = ["battleaxe","greataxe","handaxe"]`. Expected: all 16x16 RGBA; silhouettes read as axes.

- [ ] **Step 5: Commit**

```bash
git add art/shapes/battleaxe.json art/shapes/greataxe.json art/shapes/handaxe.json \
  src/main/resources/assets/dndweapons/textures/item/battleaxe.png src/main/resources/assets/dndweapons/textures/item/battleaxe_diamond.png src/main/resources/assets/dndweapons/textures/item/battleaxe_netherite.png \
  src/main/resources/assets/dndweapons/textures/item/greataxe.png src/main/resources/assets/dndweapons/textures/item/greataxe_diamond.png src/main/resources/assets/dndweapons/textures/item/greataxe_netherite.png \
  src/main/resources/assets/dndweapons/textures/item/handaxe.png src/main/resources/assets/dndweapons/textures/item/handaxe_diamond.png src/main/resources/assets/dndweapons/textures/item/handaxe_netherite.png
git commit -m "feat(textures): author axe-family shape grids (battleaxe, greataxe, handaxe)"
```

---

## Task 3: Hammers & picks family (mace, maul, warhammer, war_pick, light_hammer, morningstar)

**Files:**
- Create: `art/shapes/mace.json`, `art/shapes/maul.json`, `art/shapes/warhammer.json`,
  `art/shapes/war_pick.json`, `art/shapes/light_hammer.json`, `art/shapes/morningstar.json`

Standard metal tiers; `outputs` = `{ "<id>": "iron", "<id>_diamond": "diamond", "<id>_netherite": "netherite" }`.

- **mace** — 5-cell haft (`w`/`k`), flanged metal head 3 cells top-right (`B`/`b`/`s`,
  stepped flanges).
- **maul** — 8-cell shaft (`w`), square head 4 wide x 3 tall top-right (`B`/`b`/`s`).
- **warhammer** — 5-cell haft (`w`), square flat head 2-3 cells top-right (`B`/`b`),
  optional small back-spike (`s`).
- **war_pick** — 5-cell haft (`w`), curved downward beak 3 cells top-right (`B`/`b`),
  optional 1-cell back-hammer (`f`).
- **light_hammer** — 4-cell haft (`w`), square head 2 cells top-right (`B`/`b`),
  small butt cap (`f`).
- **morningstar** — 5-cell haft (`w`/`k`), RIGID spiked ball head 3 cells top-right
  (`B`/`b`/`s`), spikes in cardinal/diagonal cells. No chain.

- [ ] **Step 1: Author all six shape files** per notes, 16x16, chars from `B b s w W k f .`.

- [ ] **Step 2: Validate** — Run: `python scripts/render_textures.py --check` — Expected: all valid.

- [ ] **Step 3: Render** — Run: `python scripts/render_textures.py` (renders every shape; faster than six `--only` calls) — Expected: lines for every weapon including this family.

- [ ] **Step 4: Verify** — Run the snippet with `ids = ["mace","maul","warhammer","war_pick","light_hammer","morningstar"]`. Expected: all 16x16 RGBA; silhouettes read as hammers/picks.

- [ ] **Step 5: Commit**

```bash
git add art/shapes/mace.json art/shapes/maul.json art/shapes/warhammer.json art/shapes/war_pick.json art/shapes/light_hammer.json art/shapes/morningstar.json \
  "src/main/resources/assets/dndweapons/textures/item/mace.png" "src/main/resources/assets/dndweapons/textures/item/mace_diamond.png" "src/main/resources/assets/dndweapons/textures/item/mace_netherite.png" \
  "src/main/resources/assets/dndweapons/textures/item/maul.png" "src/main/resources/assets/dndweapons/textures/item/maul_diamond.png" "src/main/resources/assets/dndweapons/textures/item/maul_netherite.png" \
  "src/main/resources/assets/dndweapons/textures/item/warhammer.png" "src/main/resources/assets/dndweapons/textures/item/warhammer_diamond.png" "src/main/resources/assets/dndweapons/textures/item/warhammer_netherite.png" \
  "src/main/resources/assets/dndweapons/textures/item/war_pick.png" "src/main/resources/assets/dndweapons/textures/item/war_pick_diamond.png" "src/main/resources/assets/dndweapons/textures/item/war_pick_netherite.png" \
  "src/main/resources/assets/dndweapons/textures/item/light_hammer.png" "src/main/resources/assets/dndweapons/textures/item/light_hammer_diamond.png" "src/main/resources/assets/dndweapons/textures/item/light_hammer_netherite.png" \
  "src/main/resources/assets/dndweapons/textures/item/morningstar.png" "src/main/resources/assets/dndweapons/textures/item/morningstar_diamond.png" "src/main/resources/assets/dndweapons/textures/item/morningstar_netherite.png"
git commit -m "feat(textures): author hammer/pick-family shape grids (mace, maul, warhammer, war_pick, light_hammer, morningstar)"
```

---

## Task 4: Polearms & thrown points family (glaive, halberd, spear, pike, lance, javelin, dart)

**Files:**
- Create: `art/shapes/glaive.json`, `art/shapes/halberd.json`, `art/shapes/spear.json`,
  `art/shapes/pike.json`, `art/shapes/lance.json`, `art/shapes/javelin.json`,
  `art/shapes/dart.json`

Standard metal tiers; `outputs` = `{ "<id>": "iron", "<id>_diamond": "diamond", "<id>_netherite": "netherite" }`.

All polearms have a long wooden shaft (`w`) running the full diagonal; only the head
differs (metal `B`/`b`/`s`). `dart` is the small thrown sibling:

- **glaive** — single broad curved blade ~5 cells top-right, arcing away from shaft.
- **halberd** — axe-head one side (~3 cells), spike top-right (~2 cells), back-hook
  opposite.
- **spear** — leaf spearhead 3 cells top-right, short crossbar (`f`) just below head.
- **pike** — thin shaft full diagonal, tiny 2-cell spearhead at the very top-right.
- **lance** — thin tapered shaft, conical tip ~3 cells top-right, circular vamplate
  ~2 cells (`f`) partway down.
- **javelin** — thin shaft full diagonal, 2-3 cell leaf head top-right, no crossbar.
- **dart** — small finned throwing dart: 2-cell pointed metal head top-right
  (`B`/`b`), 4-cell shaft (`w`), 2-cell V fletching at the base (`k`). Fills ~8 cells.

- [ ] **Step 1: Author all seven shape files** per notes, 16x16, chars from `B b s w W k f .`.

- [ ] **Step 2: Validate** — Run: `python scripts/render_textures.py --check` — Expected: all valid.

- [ ] **Step 3: Render** — Run: `python scripts/render_textures.py` — Expected: lines for this family.

- [ ] **Step 4: Verify** — Run the snippet with `ids = ["glaive","halberd","spear","pike","lance","javelin","dart"]`. Expected: all 16x16 RGBA; long diagonal polearm silhouettes plus a small finned dart.

- [ ] **Step 5: Commit**

```bash
git add art/shapes/glaive.json art/shapes/halberd.json art/shapes/spear.json art/shapes/pike.json art/shapes/lance.json art/shapes/javelin.json art/shapes/dart.json \
  src/main/resources/assets/dndweapons/textures/item/glaive*.png \
  src/main/resources/assets/dndweapons/textures/item/halberd*.png \
  src/main/resources/assets/dndweapons/textures/item/spear*.png \
  src/main/resources/assets/dndweapons/textures/item/pike*.png \
  src/main/resources/assets/dndweapons/textures/item/lance*.png \
  src/main/resources/assets/dndweapons/textures/item/javelin*.png \
  src/main/resources/assets/dndweapons/textures/item/dart*.png
git commit -m "feat(textures): author polearm/thrown-point shape grids (glaive, halberd, spear, pike, lance, javelin, dart)"
```

---

## Task 5: Wooden bludgeons family (club, greatclub, quarterstaff)

These use the HARDWOOD override palette. Tier variation is an accent cell, not a
recolor of the whole body, so `outputs` reference the hardwood tier palettes.

**Files:**
- Create: `art/shapes/club.json`, `art/shapes/greatclub.json`, `art/shapes/quarterstaff.json`

`outputs` for each:

```json
"outputs": { "<id>": "hardwood", "<id>_diamond": "hardwood_diamond", "<id>_netherite": "hardwood_netherite" }
```

- **club** — knobbly wooden cylinder thicker at the head top-right (`B`/`b`/`s`)
  tapering to a thinner grip. Diamond/netherite: 1-2 accent cells (`a`, and `e` for
  netherite ember) embedded in the head. Iron tier = pure hardwood (no `a`/`e`).
- **greatclub** — oversized two-handed club flaring from a 2-cell grip to a 5-cell
  head top-right. Diamond/netherite: 2-3 accent cells (`a`/`e`) across the face.
- **quarterstaff** — simple long pole full diagonal (`B`/`b`/`s`), banded metal caps
  1 cell each end (`f`); diamond/netherite add an inlay accent (`a`/`e`) on the caps.
  No blade.

IMPORTANT: because iron-tier hardwood has no `a`/`e` cells but diamond/netherite do,
the accent pixels must be authored into the grid (they render transparent under the
plain `hardwood` palette only if `a`/`e` are undefined there — but they ARE used, so
`hardwood.json` must define `a`). The plain `hardwood` palette already defines `a` as
iron-grey `#6E6E6E`; if you want the iron tier to show NO inlay, simply do not place
any `a`/`e` cells in the grid — but then diamond/netherite also lack them.

To get "iron = no inlay, diamond/netherite = inlay" from a single shared grid, place
the accent cells and rely on the palette: ensure `hardwood.json` maps `a` to the SAME
color as the surrounding hardwood body (so the inlay is invisible at iron tier) while
`hardwood_diamond`/`hardwood_netherite` map `a` to the shard color. Verify this before
authoring:

- [ ] **Step 0: Make iron-tier inlay invisible**

Confirm `art/palettes/hardwood.json` defines `a` to a hardwood body color (e.g. the
midtone `#4A2E18`) so accent cells vanish at iron tier. If it currently maps `a` to
`#6E6E6E` (grey), change it to `#4A2E18` and re-run `python scripts/render_textures.py --check`.
This keeps club/greatclub iron heads pure wood while diamond/netherite show shards
from the SAME grid.

- [ ] **Step 1: Author `club.json`, `greatclub.json`, `quarterstaff.json`** per notes,
  16x16, chars from `B b s f a e .`. Place `a`/`e` accent cells where shards/inlays go.

- [ ] **Step 2: Validate** — Run: `python scripts/render_textures.py --check` — Expected: all valid.

- [ ] **Step 3: Render** — Run: `python scripts/render_textures.py --only club && python scripts/render_textures.py --only greatclub && python scripts/render_textures.py --only quarterstaff` — Expected: 9 PNGs.

- [ ] **Step 4: Verify** — Run the snippet with `ids = ["club","greatclub","quarterstaff"]`. Expected: all 16x16 RGBA; iron previews show plain wood, diamond/netherite show the accent cells (compare the three previews per weapon — the accent pixels differ).

- [ ] **Step 5: Commit**

```bash
git add art/palettes/hardwood.json art/shapes/club.json art/shapes/greatclub.json art/shapes/quarterstaff.json \
  src/main/resources/assets/dndweapons/textures/item/club*.png \
  src/main/resources/assets/dndweapons/textures/item/greatclub*.png \
  src/main/resources/assets/dndweapons/textures/item/quarterstaff*.png
git commit -m "feat(textures): author wooden-bludgeon shape grids (club, greatclub, quarterstaff)"
```

---

## Task 6: Flexible & curved family (flail, whip, sickle)

**Files:**
- Create: `art/shapes/flail.json` (standard metal tiers)
- Create: `art/shapes/whip.json` (LEATHER override tiers)
- Create: `art/shapes/sickle.json` (standard metal tiers)

`outputs`:

```json
// flail.json, sickle.json
"outputs": { "<id>": "iron", "<id>_diamond": "diamond", "<id>_netherite": "netherite" }
// whip.json
"outputs": { "whip": "leather", "whip_diamond": "leather_diamond", "whip_netherite": "leather_netherite" }
```

- **flail** — 4-cell wooden handle bottom-left (`w`/`k`), 3-cell stair-stepped chain as
  DISCRETE cells (`f` and `s` alternating to suggest links), 3-cell spiked iron ball
  top-right (`B`/`b`/`s`). Chain is cells, never a curve.
- **whip** — NOT rigid. 3-cell wooden handle bottom-left (`w`) with iron cap (`f`),
  then a stair-stepped coiled leather lash looping up-right ~10 cells (`B`/`b`/`s` =
  leather hi/mid/shadow from the leather palette), single tier accent cell (`a`) at
  the tip. Iron `a`=grey, diamond `a`=cyan, netherite `a`=obsidian (palette handles it).
- **sickle** — 3-cell wooden handle bottom-left (`w`/`k`), sharply hooked inward blade
  arcing 4 cells toward the top (`B`/`b`/`s`), like a question mark.

- [ ] **Step 1: Author `flail.json`, `whip.json`, `sickle.json`** per notes, 16x16.
  flail/sickle chars from `B b s w W k f .`; whip chars from `B b s w k f a .`.

- [ ] **Step 2: Validate** — Run: `python scripts/render_textures.py --check` — Expected: all valid.

- [ ] **Step 3: Render** — Run: `python scripts/render_textures.py --only flail && python scripts/render_textures.py --only whip && python scripts/render_textures.py --only sickle` — Expected: 9 PNGs.

- [ ] **Step 4: Verify** — Run the snippet with `ids = ["flail","whip","sickle"]`. Expected: all 16x16 RGBA; flail shows discrete chain cells, whip shows a coiled lash with a distinct tip color across tiers, sickle shows a hooked curve.

- [ ] **Step 5: Commit**

```bash
git add art/shapes/flail.json art/shapes/whip.json art/shapes/sickle.json \
  src/main/resources/assets/dndweapons/textures/item/flail*.png \
  src/main/resources/assets/dndweapons/textures/item/whip*.png \
  src/main/resources/assets/dndweapons/textures/item/sickle*.png
git commit -m "feat(textures): author flexible/curved shape grids (flail, whip, sickle)"
```

---

## Task 7: Ranged family (blowgun, hand_crossbow, heavy_crossbow, longbow, sling, musket, pistol)

No tier variants — each has a SINGLE `outputs` entry. Follow the documented
orientation exceptions (sling, longbow, blowgun do NOT use the standard diagonal).

**Files & `outputs`:**
- `art/shapes/blowgun.json` — `{ "blowgun": "hardwood" }`
- `art/shapes/hand_crossbow.json` — `{ "hand_crossbow": "iron" }`
- `art/shapes/heavy_crossbow.json` — `{ "heavy_crossbow": "iron" }`
- `art/shapes/longbow.json` — `{ "longbow": "lightwood" }`
- `art/shapes/sling.json` — `{ "sling": "tan_leather" }`
- `art/shapes/musket.json` — `{ "musket": "flintlock" }`
- `art/shapes/pistol.json` — `{ "pistol": "flintlock" }`

Silhouettes & chars:

- **blowgun** — long thin hollow tube full diagonal (`B`/`b`/`s` hardwood), flared
  mouthpiece 2 cells bottom-left. No metal.
- **hand_crossbow** — horizontal bow limbs 3 cells top-right (`B`/`b` iron), vertical
  wood grip 3 cells below (`w`), small trigger guard (`f`).
- **heavy_crossbow** — thick limbs 5 cells top-right (`B`/`b`/`s`), long wood stock to
  bottom-left (`w`/`W`), drawstring 1-cell line (`k` or `f`), trigger group (`f`).
- **longbow** — recurved bow as a stair-stepped C-curve along the diagonal (`B`/`b`/`s`
  lightwood), grip in the middle, drawstring (`d`) a thin straight line connecting the
  limb tips. Vertical-ish C, not the standard diagonal.
- **sling** — two cords forming a stair-stepped V from top-left and top-right meeting
  at a small oval pouch ~2 cells lower-center (`B`/`b`/`s` tan_leather). Symmetric
  vertical V — override the diagonal rule.
- **musket** — long octagonal barrel ~9 cells (steel `B`/`b`/`s`), wooden stock +
  shoulder butt bottom-left (`W`/`w`/`k` walnut), lockplate near grip (`B`/`b`), brass
  trigger guard/butt cap (`R`/`r`/`t`).
- **pistol** — short barrel 4 cells top-right (steel `B`/`b`), curved wood grip 4 cells
  to bottom-left (`W`/`w`/`k`), brass lockplate/trigger guard (`R`/`r`/`t`). ~9 cells.

- [ ] **Step 1: Author all seven shape files** per notes, 16x16. Use only chars
  defined in each weapon's referenced palette (e.g. `d` exists only in `lightwood`;
  `R`/`r`/`t` only in `flintlock`).

- [ ] **Step 2: Validate** — Run: `python scripts/render_textures.py --check` — Expected: all valid. (If you used a char not in that weapon's single palette, the converter names it — fix the grid.)

- [ ] **Step 3: Render** — Run: `python scripts/render_textures.py` — Expected: 7 single PNGs for this family.

- [ ] **Step 4: Verify** — Run the snippet with `ids = ["blowgun","hand_crossbow","heavy_crossbow","longbow","sling","musket","pistol"]`. Expected: all 16x16 RGBA; sling is a symmetric V, longbow a C-curve, blowgun a straight tube, firearms read as barrel+stock.

- [ ] **Step 5: Commit**

```bash
git add art/shapes/blowgun.json art/shapes/hand_crossbow.json art/shapes/heavy_crossbow.json art/shapes/longbow.json art/shapes/sling.json art/shapes/musket.json art/shapes/pistol.json \
  src/main/resources/assets/dndweapons/textures/item/blowgun.png \
  src/main/resources/assets/dndweapons/textures/item/hand_crossbow.png \
  src/main/resources/assets/dndweapons/textures/item/heavy_crossbow.png \
  src/main/resources/assets/dndweapons/textures/item/longbow.png \
  src/main/resources/assets/dndweapons/textures/item/sling.png \
  src/main/resources/assets/dndweapons/textures/item/musket.png \
  src/main/resources/assets/dndweapons/textures/item/pistol.png
git commit -m "feat(textures): author ranged-family shape grids (blowgun, crossbows, longbow, sling, musket, pistol)"
```

---

## Task 8: Templates & bindings (8 items)

Two of these (`infernal_binding`, `weapon_smithing_binding`) need palettes that do not
exist yet. Add them first.

**Files:**
- Create: `art/palettes/infernal.json` — occult black-red rope
  ```json
  { "extends": null, "colors": { "B": "#5A1A14", "b": "#3A120E", "s": "#1E0A08", "e": "#6B2A1A" } }
  ```
- Create: `art/shapes/diamond_template_core.json` — `{ "diamond_template_core": "diamond" }`
- Create: `art/shapes/diamond_template_fragment.json` — `{ "diamond_template_fragment": "diamond" }`
- Create: `art/shapes/diamond_weapon_upgrade_template.json` — `{ "diamond_weapon_upgrade_template": "diamond" }`
- Create: `art/shapes/netherite_template_core.json` — `{ "netherite_template_core": "netherite" }`
- Create: `art/shapes/netherite_template_fragment.json` — `{ "netherite_template_fragment": "netherite" }`
- Create: `art/shapes/netherite_weapon_upgrade_template.json` — `{ "netherite_weapon_upgrade_template": "netherite" }`
- Create: `art/shapes/infernal_binding.json` — `{ "infernal_binding": "infernal" }`
- Create: `art/shapes/weapon_smithing_binding.json` — `{ "weapon_smithing_binding": "tan_leather" }`

Silhouettes & chars:

- **diamond_template_core** / **netherite_template_core** — ~6x6-cell center plate
  (`B`/`b`/`s`), faceted gem inlaid center (use `i` for diamond mid-inlay, or `f`/`s`
  for netherite bronze). Mimics the vanilla smithing-template silhouette.
- **diamond_template_fragment** / **netherite_template_fragment** — jagged shard ~5
  cells (`B`/`b`/`s`) with stair-stepped broken edges; netherite adds sparse `e` ember.
- **diamond_weapon_upgrade_template** / **netherite_weapon_upgrade_template** — tall
  scroll/plate ~6 wide x 10 tall (`B`/`b` border, `s` dark interior) with a small
  weapon silhouette in the center.
- **infernal_binding** — coiled black-red rope ~8 cells in a stair-stepped knot
  (`B`/`b`/`s`), ember-glow `e` cells along the cord. Occult feel.
- **weapon_smithing_binding** — coiled leather strap ~6 cells in a tidy loop
  (`B`/`b`/`s` tan_leather). Mundane counterpart.

Note: the netherite template cores need a bronze inlay color. `netherite.json` already
defines `f`/`p` as dull bronze `#5A3E2A` and `e` as ember — use those; do not add new
chars unless a role is genuinely missing.

- [ ] **Step 1: Create `art/palettes/infernal.json`** exactly as shown above.

- [ ] **Step 2: Author the eight shape files** per notes, 16x16. Use only chars defined
  in each item's referenced palette.

- [ ] **Step 3: Validate** — Run: `python scripts/render_textures.py --check` — Expected: all valid.

- [ ] **Step 4: Render** — Run: `python scripts/render_textures.py` — Expected: 8 single PNGs for this family.

- [ ] **Step 5: Verify** — Run the snippet with `ids = ["diamond_template_core","diamond_template_fragment","diamond_weapon_upgrade_template","netherite_template_core","netherite_template_fragment","netherite_weapon_upgrade_template","infernal_binding","weapon_smithing_binding"]`. Expected: all 16x16 RGBA; templates read as plates/scrolls, bindings as coiled rope/strap.

- [ ] **Step 6: Commit**

```bash
git add art/palettes/infernal.json \
  art/shapes/diamond_template_core.json art/shapes/diamond_template_fragment.json art/shapes/diamond_weapon_upgrade_template.json \
  art/shapes/netherite_template_core.json art/shapes/netherite_template_fragment.json art/shapes/netherite_weapon_upgrade_template.json \
  art/shapes/infernal_binding.json art/shapes/weapon_smithing_binding.json \
  src/main/resources/assets/dndweapons/textures/item/diamond_template_core.png \
  src/main/resources/assets/dndweapons/textures/item/diamond_template_fragment.png \
  src/main/resources/assets/dndweapons/textures/item/diamond_weapon_upgrade_template.png \
  src/main/resources/assets/dndweapons/textures/item/netherite_template_core.png \
  src/main/resources/assets/dndweapons/textures/item/netherite_template_fragment.png \
  src/main/resources/assets/dndweapons/textures/item/netherite_weapon_upgrade_template.png \
  src/main/resources/assets/dndweapons/textures/item/infernal_binding.png \
  src/main/resources/assets/dndweapons/textures/item/weapon_smithing_binding.png
git commit -m "feat(textures): author template + binding shape grids (8 items) and infernal palette"
```

---

## Task 9: Full-set verification

After all families are authored, confirm the whole set is JSON-sourced and 16x16.

**Files:** none created; verification only.

- [ ] **Step 1: Validate every shape**

Run: `python scripts/render_textures.py --check`
Expected: `All 42 shape(s) in ... are valid.` (42 shape files: 27 melee + 7 ranged + 8 templates/bindings.)

- [ ] **Step 2: Re-render everything from JSON (idempotent)**

Run: `python scripts/render_textures.py`
Expected: a `rendered <name>.png` line for all 96 outputs, `Done. 96 texture(s) written`.

- [ ] **Step 3: Confirm shape-file and output counts**

Run:
```bash
python - <<'PY'
import glob, json
shapes = glob.glob("art/shapes/*.json")
outputs = []
for s in shapes:
    outputs += list(json.load(open(s, encoding="utf-8"))["outputs"].keys())
print("shape files:", len(shapes), "-> output PNGs:", len(outputs))
assert len(shapes) == 42, f"expected 42 shape files, got {len(shapes)}"
assert len(outputs) == 96, f"expected 96 output PNGs, got {len(outputs)}"
PY
```
Expected: `shape files: 42 -> output PNGs: 96`, both assertions pass.

- [ ] **Step 4: Confirm all shipped textures are 16x16**

Run: `python scripts/downscale_textures.py --check`
Expected: `All textures in ... are 16x16.`

- [ ] **Step 5: Confirm converter unit tests still pass**

Run: `python -m pytest scripts/test_render_textures.py -q`
Expected: `16 passed`.

- [ ] **Step 6: Commit any remaining re-rendered PNGs**

```bash
git add src/main/resources/assets/dndweapons/textures/item/*.png
git commit -m "chore(textures): re-render full 96-item set from JSON shape grids" || echo "nothing to commit"
```

---

## Self-review notes (author)

- **Spec coverage:** the spec's only deferred work was authoring the remaining grids.
  Tasks 1-8 cover every remaining item: 26 melee (27 minus longsword) across Tasks 1-6
  — Task1=4, Task2=3, Task3=6, Task4=7 (incl. dart), Task5=3, Task6=3 = 26 — plus 7
  ranged (Task 7) and 8 templates/bindings (Task 8). That is 41 remaining shape files
  producing 93 PNG outputs, alongside longsword's already-committed shape file (3
  PNGs): 42 shape files / 96 PNGs total. Task 9 verifies the full set.
- **No new converter/skill work** is required; palettes are complete except
  `infernal` (added in Task 8) and the `hardwood` `a`-invisibility tweak (Task 5
  Step 0), both called out explicitly.
- **Palette/char consistency:** every char named in a task is defined in that task's
  referenced palette per `art/palettes/*.json` and the skill char table; flintlock-only
  (`R`/`r`/`t`) and lightwood-only (`d`) chars are scoped to their weapons.
