#!/usr/bin/env python3
"""
Render JSON pixel-grid sources into 16x16 RGBA PNG item textures.

Claude (or a human) authors a tier-independent shape grid under art/shapes/<id>.json
using semantic characters, plus reusable color palettes under art/palettes/<name>.json
that map each character to a hex color. This converter cross-products a shape against
the palettes named in its `outputs` map and writes one 16x16 PNG per output into
  src/main/resources/assets/dndweapons/textures/item/<output>.png

This replaces the old "paste a prompt into an external image LLM, then downscale and
chroma-key by hand" workflow with a deterministic, reproducible pipeline.

Usage:
  python scripts/render_textures.py                 # render every shape in art/shapes/
  python scripts/render_textures.py --only longsword
  python scripts/render_textures.py --check          # validate all shapes+palettes, write nothing

Exit codes:
  0  success / all valid
  1  validation failure (see messages)
  2  environment error (Pillow missing, dirs missing)

Shape file schema (art/shapes/<id>.json):
  {
    "id": "longsword",            # must equal the filename stem
    "size": 16,                   # must be 16
    "outputs": {                  # output PNG basename -> palette name
      "longsword": "iron",
      "longsword_diamond": "diamond",
      "longsword_netherite": "netherite"
    },
    "rows": ["................", ...]   # exactly 16 strings of exactly 16 chars; '.' = transparent
  }

Palette file schema (art/palettes/<name>.json):
  {
    "extends": "leather",         # optional base palette to inherit from
    "colors": { "B": "#C8C8C8", "a": "#7DF2EE", "x": null }  # char -> #RRGGBB / #RRGGBBAA / null
  }

Validation is strict and hard-fails (no silent padding/cropping): a grid that is not
exactly 16x16, references an undefined palette char, uses a malformed hex, names a
missing palette, or forms an `extends` cycle is rejected with a precise message.

Requires: Pillow (PIL fork). Install with: pip install Pillow
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Optional

try:
    from PIL import Image
except ImportError:
    sys.stderr.write(
        "Error: Pillow is not installed. Install it with:\n  pip install Pillow\n"
    )
    sys.exit(2)

REPO_ROOT = Path(__file__).resolve().parent.parent
ART_DIR = REPO_ROOT / "art"
SHAPES_DIR = ART_DIR / "shapes"
PALETTES_DIR = ART_DIR / "palettes"
TEXTURE_DIR = REPO_ROOT / "src" / "main" / "resources" / "assets" / "dndweapons" / "textures" / "item"

SIZE = 16
TRANSPARENT = (0, 0, 0, 0)
HEX_RE = re.compile(r"^#(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")
TRANSPARENT_CHAR = "."
MAX_EXTENDS_DEPTH = 16
GRADIENT_AXES = ("x", "y", "diag", "adiag")


class RenderError(Exception):
    """Raised on any invalid shape or palette. Carries a human-readable message."""


# --------------------------------------------------------------------------- #
# palettes
# --------------------------------------------------------------------------- #

def hex_to_rgba(value: Optional[str]) -> tuple[int, int, int, int]:
    """Convert '#RRGGBB' / '#RRGGBBAA' (or None) to an RGBA tuple."""
    if value is None:
        return TRANSPARENT
    h = value.lstrip("#")
    r, g, b = int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)
    a = int(h[6:8], 16) if len(h) == 8 else 255
    return (r, g, b, a)


def validate_gradient(palette_name: str, char: str, obj: dict) -> None:
    """Strict-validate a gradient palette value {from, to, axis}. Raises RenderError."""
    keys = set(obj.keys())
    if keys != {"from", "to", "axis"}:
        raise RenderError(
            f"palette '{palette_name}': char '{char}' gradient must have exactly keys "
            f"from/to/axis, got {sorted(keys)}"
        )
    for endpoint in ("from", "to"):
        val = obj[endpoint]
        if not isinstance(val, str) or not HEX_RE.match(val):
            raise RenderError(
                f"palette '{palette_name}': char '{char}' gradient '{endpoint}' "
                f"has invalid hex '{val}'"
            )
    if obj["axis"] not in GRADIENT_AXES:
        raise RenderError(
            f"palette '{palette_name}': char '{char}' gradient axis '{obj['axis']}' "
            f"must be one of {'/'.join(GRADIENT_AXES)}"
        )


def axis_coord(x: int, y: int, axis: str) -> int:
    """Scalar position of a pixel along a linear gradient axis."""
    if axis == "x":
        return x
    if axis == "y":
        return y
    if axis == "diag":      # top-left -> bottom-right
        return x + y
    return x - y            # adiag: bottom-left -> top-right


def lerp_rgba(c0: tuple[int, int, int, int], c1: tuple[int, int, int, int], t: float) -> tuple[int, int, int, int]:
    """Per-channel linear interpolation between two RGBA colors at fraction t in [0, 1]."""
    return tuple(round(c0[i] + t * (c1[i] - c0[i])) for i in range(4))  # type: ignore[return-value]


def resolve_palette(name: str, palettes_dir: Path, _seen: Optional[list[str]] = None) -> dict[str, object]:
    """Load a palette by name, resolving `extends` inheritance into a flat char->value map.

    A value is a hex string, ``None`` (transparent), or a gradient object
    ``{from, to, axis}``. Raises RenderError on a missing file, an `extends` cycle,
    excessive depth, a malformed hex value, or an invalid gradient object.
    """
    _seen = _seen or []
    if name in _seen:
        chain = " -> ".join(_seen + [name])
        raise RenderError(f"palette '{name}': extends cycle ({chain})")
    if len(_seen) > MAX_EXTENDS_DEPTH:
        raise RenderError(f"palette '{name}': extends chain exceeds depth {MAX_EXTENDS_DEPTH}")

    path = palettes_dir / f"{name}.json"
    if not path.is_file():
        raise RenderError(f"palette '{name}' not found at {path}")
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise RenderError(f"palette '{name}': invalid JSON: {exc}") from exc

    colors: dict[str, object] = {}
    base = data.get("extends")
    if base:
        colors.update(resolve_palette(base, palettes_dir, _seen + [name]))

    own = data.get("colors", {})
    if not isinstance(own, dict):
        raise RenderError(f"palette '{name}': 'colors' must be an object")
    for char, value in own.items():
        if isinstance(value, dict):
            validate_gradient(name, char, value)
        elif value is not None and not HEX_RE.match(str(value)):
            raise RenderError(f"palette '{name}': char '{char}' has invalid hex '{value}'")
        colors[char] = value
    return colors


# --------------------------------------------------------------------------- #
# shapes
# --------------------------------------------------------------------------- #

def load_shape(path: Path) -> dict:
    """Read and structurally validate a shape file. Raises RenderError on any problem."""
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise RenderError(f"{path.name}: invalid JSON: {exc}") from exc

    stem = path.stem
    if data.get("id") != stem:
        raise RenderError(f"{path.name}: id '{data.get('id')}' must match filename stem '{stem}'")
    if data.get("size") != SIZE:
        raise RenderError(f"{path.name}: size must be {SIZE}, got {data.get('size')}")

    rows = data.get("rows")
    if not isinstance(rows, list) or len(rows) != SIZE:
        raise RenderError(f"{path.name}: 'rows' must be exactly {SIZE} rows, got {len(rows) if isinstance(rows, list) else type(rows).__name__}")
    for y, row in enumerate(rows):
        if not isinstance(row, str) or len(row) != SIZE:
            raise RenderError(f"{path.name} row {y}: must be exactly {SIZE} chars, got {len(row) if isinstance(row, str) else type(row).__name__}")

    outputs = data.get("outputs")
    if not isinstance(outputs, dict) or not outputs:
        raise RenderError(f"{path.name}: 'outputs' must be a non-empty object")
    return data


def render_shape(shape: dict, palettes_dir: Path) -> dict[str, "Image.Image"]:
    """Build one RGBA image per output. Validates char coverage against each palette.

    Flat chars (hex / null) paint a single color. A char whose palette value is a
    gradient object is collected and painted in a second pass: its color is interpolated
    along the gradient axis across the extent of that char's own cells (so the ramp fills
    the form wherever the char is placed). A single-line extent resolves to `from`.
    """
    rows: list[str] = shape["rows"]
    images: dict[str, Image.Image] = {}
    for output_name, palette_name in shape["outputs"].items():
        palette = resolve_palette(palette_name, palettes_dir)
        img = Image.new("RGBA", (SIZE, SIZE), TRANSPARENT)
        px = img.load()
        gradient_pixels: dict[str, list[tuple[int, int]]] = {}
        for y, row in enumerate(rows):
            for x, char in enumerate(row):
                if char == TRANSPARENT_CHAR:
                    continue
                if char not in palette:
                    raise RenderError(
                        f"{shape['id']}.json row {y} col {x}: char '{char}' not defined in resolved palette '{palette_name}'"
                    )
                value = palette[char]
                if isinstance(value, dict):
                    gradient_pixels.setdefault(char, []).append((x, y))
                else:
                    px[x, y] = hex_to_rgba(value)
        for char, pixels in gradient_pixels.items():
            grad = palette[char]
            axis = grad["axis"]
            c_from = hex_to_rgba(grad["from"])
            c_to = hex_to_rgba(grad["to"])
            coords = [axis_coord(x, y, axis) for x, y in pixels]
            cmin, cmax = min(coords), max(coords)
            span = cmax - cmin
            for x, y in pixels:
                t = 0.0 if span == 0 else (axis_coord(x, y, axis) - cmin) / span
                px[x, y] = lerp_rgba(c_from, c_to, t)
        images[output_name] = img
    return images


def render_file(shape_path: Path, palettes_dir: Path, out_dir: Path, write: bool = True) -> list[str]:
    """Validate + render one shape file, writing PNGs into out_dir. Returns output names."""
    shape = load_shape(shape_path)
    images = render_shape(shape, palettes_dir)
    written: list[str] = []
    for output_name, img in images.items():
        if write:
            out_dir.mkdir(parents=True, exist_ok=True)
            img.save(out_dir / f"{output_name}.png", format="PNG", optimize=True)
        written.append(output_name)
    return written


def validate_all(shapes_dir: Path, palettes_dir: Path) -> list[str]:
    """Validate every shape (and the palettes it references) without writing. Returns error messages."""
    errors: list[str] = []
    for shape_path in sorted(shapes_dir.glob("*.json")):
        try:
            render_file(shape_path, palettes_dir, out_dir=Path("."), write=False)
        except RenderError as exc:
            errors.append(str(exc))
    return errors


# --------------------------------------------------------------------------- #
# CLI
# --------------------------------------------------------------------------- #

def main() -> int:
    parser = argparse.ArgumentParser(description="Render art/shapes/*.json into 16x16 PNG textures.")
    parser.add_argument("--only", metavar="ID", help="render a single shape by id (filename stem)")
    parser.add_argument("--check", action="store_true", help="validate all shapes+palettes; write nothing")
    args = parser.parse_args()

    if not SHAPES_DIR.is_dir():
        sys.stderr.write(f"Error: shapes directory not found: {SHAPES_DIR}\n")
        return 2
    if not PALETTES_DIR.is_dir():
        sys.stderr.write(f"Error: palettes directory not found: {PALETTES_DIR}\n")
        return 2

    if args.check:
        errors = validate_all(SHAPES_DIR, PALETTES_DIR)
        if errors:
            for e in errors:
                sys.stderr.write(f"  {e}\n")
            sys.stderr.write(f"\n{len(errors)} shape(s) invalid.\n")
            return 1
        count = len(list(SHAPES_DIR.glob("*.json")))
        print(f"All {count} shape(s) in {SHAPES_DIR} are valid.")
        return 0

    if args.only:
        shape_path = SHAPES_DIR / f"{args.only}.json"
        if not shape_path.is_file():
            sys.stderr.write(f"Error: shape not found: {shape_path}\n")
            return 2
        shape_paths = [shape_path]
    else:
        shape_paths = sorted(SHAPES_DIR.glob("*.json"))
        if not shape_paths:
            print(f"No shape files in {SHAPES_DIR}")
            return 0

    total = 0
    try:
        for shape_path in shape_paths:
            written = render_file(shape_path, PALETTES_DIR, TEXTURE_DIR)
            for name in written:
                print(f"  rendered {name}.png")
            total += len(written)
    except RenderError as exc:
        sys.stderr.write(f"Error: {exc}\n")
        return 1

    print(f"\nDone. {total} texture(s) written to {TEXTURE_DIR}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
