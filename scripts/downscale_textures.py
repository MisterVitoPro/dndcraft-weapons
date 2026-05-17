#!/usr/bin/env python3
"""
Downscale all PNG textures under src/main/resources/assets/dndweapons/textures/item/
to 16x16 RGBA using nearest-neighbor (point) resampling.

This preserves Minecraft pixel-art fidelity (no bicubic/bilinear blur) and produces
small files (~70 bytes per sprite) matching the canonical longsword.png shipped
with the mod.

Usage:
  python scripts/downscale_textures.py [--check]

  --check     Exit non-zero if any file is not exactly 16x16 (CI-friendly).

Requires: Pillow (PIL fork). Install with: pip install Pillow

Idempotent: re-running on already-downscaled 16x16 PNGs is a no-op (skipped).

Notes:
  - longsword.png is the pre-existing reference sprite; we skip it to avoid
    re-writing it (the file is the source of truth for "correct" output).
  - All other PNGs in the directory are downscaled in-place. Mode is forced to
    RGBA so transparency is preserved.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    sys.stderr.write(
        "Error: Pillow is not installed. Install it with:\n"
        "  pip install Pillow\n"
    )
    sys.exit(2)

REPO_ROOT = Path(__file__).resolve().parent.parent
TEXTURE_DIR = REPO_ROOT / "src" / "main" / "resources" / "assets" / "dndweapons" / "textures" / "item"
TARGET_SIZE = (16, 16)
SKIP_FILES = {"longsword.png"}  # canonical reference sprite -- do not modify


def downscale(path: Path) -> tuple[bool, str]:
    """Return (changed, message)."""
    try:
        with Image.open(path) as img:
            if img.size == TARGET_SIZE:
                return False, f"  skip (already 16x16): {path.name}"
            original_size = img.size
            # Force RGBA so we keep transparency through the resize.
            img = img.convert("RGBA")
            # NEAREST = point sampling -- preserves pixel-art crispness.
            resized = img.resize(TARGET_SIZE, Image.NEAREST)
            resized.save(path, format="PNG", optimize=True)
            return True, f"  downscaled {original_size[0]}x{original_size[1]} -> 16x16: {path.name}"
    except Exception as exc:
        return False, f"  ERROR {path.name}: {exc}"


def check_all() -> int:
    """Return number of files NOT at 16x16."""
    bad = 0
    for png in sorted(TEXTURE_DIR.glob("*.png")):
        with Image.open(png) as img:
            if img.size != TARGET_SIZE:
                print(f"  NOT 16x16 ({img.size[0]}x{img.size[1]}): {png.name}")
                bad += 1
    return bad


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="Exit non-zero if any texture is not 16x16. No mutation.",
    )
    args = parser.parse_args()

    if not TEXTURE_DIR.is_dir():
        sys.stderr.write(f"Error: texture directory not found: {TEXTURE_DIR}\n")
        return 2

    if args.check:
        bad = check_all()
        if bad:
            sys.stderr.write(f"\n{bad} file(s) not at 16x16. Run without --check to fix.\n")
            return 1
        print(f"All textures in {TEXTURE_DIR} are 16x16.")
        return 0

    pngs = sorted(p for p in TEXTURE_DIR.glob("*.png") if p.name not in SKIP_FILES)
    if not pngs:
        print(f"No PNG files to process in {TEXTURE_DIR}")
        return 0

    print(f"Downscaling {len(pngs)} texture(s) in {TEXTURE_DIR}")
    changed = 0
    for path in pngs:
        did_change, msg = downscale(path)
        print(msg)
        if did_change:
            changed += 1
    print(f"\nDone. {changed} file(s) downscaled, {len(pngs) - changed} skipped.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
