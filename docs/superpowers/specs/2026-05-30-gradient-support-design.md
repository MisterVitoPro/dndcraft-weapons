# Gradient Support + Longbow Fix — Design

**Date:** 2026-05-30
**Status:** Approved (design)
**Author:** MisterVitoPro

## Problem

Two related gaps in the JSON texture pipeline (`scripts/render_textures.py`):

1. **No gradient support.** The converter maps every grid char to one flat hex color
   (`render_shape` → `hex_to_rgba`). Smooth shading can only be faked by hand-placing
   discrete `B`/`b`/`s` bands, which reads as blocky stripes at 16x16. We want
   genuinely interpolated gradients for rounded wood/metal forms.
2. **The longbow doesn't read as a bow.** The committed `longbow.json` grid is a sharp
   `>` chevron (two straight diagonal segments) with the drawstring `d` floating ~5px
   to the right, unconnected to the limb tips. It looks like a bent stick beside a
   line, not a bow.

## Goals

- Add converter-computed linear gradients as a first-class palette feature.
- Re-author the longbow as a recognizable recurve bow, using the new gradient for
  wood depth.
- Document both in the `pixel-art-generator` skill so future grids can use gradients.

## Non-goals (YAGNI)

- N-stop gradients. v1 supports exactly two stops (`from`/`to`).
- Radial / per-region gradients. Only the four linear axes.
- Converting any other existing grid to gradients. Only the longbow adopts one now.

## Design

### 1. Gradient feature (converter)

A palette char value may now take a third form, alongside the existing `"#RRGGBB"` /
`"#RRGGBBAA"` string and `null`:

```json
"g": { "from": "#D8B080", "to": "#6B4A22", "axis": "y" }
```

**Axes** (all linear; `coord` is the scalar position along the axis for a pixel at
`(x, y)`):

| axis | direction | coord |
|---|---|---|
| `x` | left → right | `x` |
| `y` | top → bottom | `y` |
| `diag` | top-left → bottom-right | `x + y` |
| `adiag` | bottom-left → top-right | `x - y` |

**Rendering semantics.** For each output image, and for each gradient char used in the
grid:

1. Collect the set of pixels `(x, y)` whose grid char equals this char.
2. Compute `cmin` / `cmax` = the min / max `coord` over that pixel set.
3. For each such pixel: `t = (coord - cmin) / (cmax - cmin)`, clamped to `[0, 1]`.
   If `cmax == cmin` (single line along the axis), `t = 0.0` (resolves to `from`).
4. Pixel color = per-channel linear interpolation `round(from + t * (to - from))`
   over R, G, B, **and A** (so `from`/`to` may carry alpha; `null` is not permitted
   inside a gradient — use `#RRGGBB00` for transparent endpoints if ever needed).

The extent is measured per char over its own cells, so a gradient auto-fills whatever
form uses it, wherever it sits in the grid.

**Inheritance.** A gradient value participates in `extends` exactly like a hex/null
value: a child palette's char entry (gradient or flat) fully replaces the parent's.

**Validation (strict hard-fail, matching the converter's existing style).** A gradient
object must:
- be a JSON object with exactly the keys `from`, `to`, `axis` (no more, no less);
- have `from` and `to` each matching the hex regex (`#RRGGBB` or `#RRGGBBAA`); `null`
  is rejected inside a gradient;
- have `axis` ∈ `{x, y, diag, adiag}`.

Any violation raises `RenderError` with a precise message naming the palette and char,
consistent with the converter's other messages (it never silently degrades).

**Backward compatibility.** Flat hex strings and `null` behave exactly as before. None
of the 42 existing shape grids reference a gradient char, so every current PNG renders
byte-identically. The 16 existing pytest cases stay green.

**New tests (~7).** One render per axis (`x`/`y`/`diag`/`adiag`) asserting interpolated
endpoint + midpoint pixels; single-line extent resolves to `from`; invalid `axis`
hard-fails; malformed gradient hex hard-fails; a flat-color regression confirming
non-gradient palettes are unchanged.

### 2. Longbow redesign

Replace the chevron with a stair-stepped **C-curve stave** (belly bulging left) ending
in **recurve tips** that hook back toward the string at top and bottom, plus the
drawstring `d` as a straight vertical **chord connecting the two tips**. The stave is
authored with a new gradient char `g` using the `y` axis — light at the upper limb,
dark toward the lower limb — for rounded-wood depth. Target silhouette (`g` = gradient
wood stave, `d` = drawstring):

```
...........gg...
.........gg.d...
.......gg...d...
......gg....d...
.....gg.....d...
....gg......d...
....gg......d...
...gg.......d...
...gg.......d...
....gg......d...
....gg......d...
.....gg.....d...
......gg....d...
.......gg...d...
.........gg.d...
...........gg...
```

(Exact pixel placement is finalized during implementation and verified by rendering
the PNG + ASCII preview; the design fixes the *shape*: left-bulging recurve C-curve,
vertical string chord on the right, `y`-gradient stave.)

### 3. Skill + palette + data updates

- **`art/palettes/lightwood.json`** — add char `g` mapped to the wood-stave gradient
  (`from` light `#D8B080`, `to` `#6B4A22`, `axis` `y`). Keep `B`/`b`/`s`/`d` unchanged.
- **`art/shapes/longbow.json`** — re-author to the silhouette above.
- **`.claude/skills/pixel-art-generator/SKILL.md`**:
  - New "Gradients" subsection under Palettes: the gradient-object schema, the four
    axes, the per-char-extent rule, and when to reach for it (rounded wood/metal vs.
    flat bands).
  - Add `g` to the semantic-char table as "palette-defined gradient fill".
  - Rewrite the `longbow` shape note to specify the recurve C-curve + string chord +
    `y`-gradient stave (the old note — "stair-stepped C-curve ... drawstring connecting
    the tips" — was too vague and the authored grid drifted into a chevron).

## Architecture / boundaries

The change is localized to the palette layer of the converter:

- `resolve_palette` already returns `dict[char -> value]`; values become
  `str | None | dict` (the gradient object). A small `validate_gradient(obj)` helper
  enforces the schema at resolve time.
- `render_shape` gains a pre-pass per output: for gradient chars, compute `cmin`/`cmax`
  over the char's pixels, then fill those pixels via a `lerp_rgba(from, to, t)` helper.
  Flat chars keep the existing single-`hex_to_rgba` path.
- No change to the shape-file schema, the CLI, or the output contract (still strict
  16x16 RGBA). Gradients are a palette-only concern, so shape grids stay
  tier-independent and a gradient is reusable by any grid that references the palette.

## Testing

- Extend `scripts/test_render_textures.py` with the ~7 cases above.
- Full-set regression: `python scripts/render_textures.py --check` (42 valid) and a
  full render (96 PNGs) must still pass; only `longbow.png` changes bytes.
- Visual: render the longbow and confirm the ASCII silhouette reads as a recurve bow.
