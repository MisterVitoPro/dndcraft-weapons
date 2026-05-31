"""Tests for render_textures.py — the JSON pixel-grid -> 16x16 PNG converter.

Run with: python -m pytest scripts/test_render_textures.py -q
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

import pytest
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import render_textures as rt  # noqa: E402


# --------------------------------------------------------------------------- #
# helpers
# --------------------------------------------------------------------------- #

BLANK_ROWS = ["." * 16 for _ in range(16)]


def write_json(path: Path, obj: dict) -> None:
    path.write_text(json.dumps(obj), encoding="utf-8")


@pytest.fixture
def art(tmp_path: Path) -> dict:
    shapes = tmp_path / "shapes"
    palettes = tmp_path / "palettes"
    out = tmp_path / "out"
    shapes.mkdir()
    palettes.mkdir()
    out.mkdir()
    # a couple of baseline palettes
    write_json(palettes / "iron.json", {"colors": {"B": "#C8C8C8", "b": "#8B8B8B", "a": "#6E6E6E"}})
    write_json(palettes / "leather.json", {"colors": {"B": "#5A3E22", "a": "#000000"}})
    write_json(palettes / "leather_diamond.json", {"extends": "leather", "colors": {"a": "#7DF2EE"}})
    return {"shapes": shapes, "palettes": palettes, "out": out}


def diagonal_rows() -> list[str]:
    rows = ["." * 16 for _ in range(16)]
    grid = [list(r) for r in rows]
    for i in range(16):
        grid[i][i] = "B"
    return ["".join(r) for r in grid]


# --------------------------------------------------------------------------- #
# happy path
# --------------------------------------------------------------------------- #

def test_render_produces_16x16_rgba(art):
    shape = {"id": "thing", "size": 16, "outputs": {"thing": "iron"}, "rows": diagonal_rows()}
    write_json(art["shapes"] / "thing.json", shape)
    rt.render_file(art["shapes"] / "thing.json", art["palettes"], art["out"])
    png = art["out"] / "thing.png"
    assert png.exists()
    with Image.open(png) as img:
        assert img.size == (16, 16)
        assert img.mode == "RGBA"
        # diagonal pixel is iron highlight, off-diagonal is transparent
        assert img.getpixel((0, 0)) == (0xC8, 0xC8, 0xC8, 255)
        assert img.getpixel((5, 0)) == (0, 0, 0, 0)


def test_multiple_outputs_one_shape(art):
    write_json(art["palettes"] / "diamond.json", {"colors": {"B": "#7DF2EE", "b": "#4DD2D6", "a": "#6E6E6E"}})
    shape = {"id": "thing", "size": 16,
             "outputs": {"thing": "iron", "thing_diamond": "diamond"},
             "rows": diagonal_rows()}
    write_json(art["shapes"] / "thing.json", shape)
    rt.render_file(art["shapes"] / "thing.json", art["palettes"], art["out"])
    assert (art["out"] / "thing.png").exists()
    with Image.open(art["out"] / "thing_diamond.png") as img:
        assert img.getpixel((0, 0)) == (0x7D, 0xF2, 0xEE, 255)


def test_dot_is_transparent(art):
    shape = {"id": "thing", "size": 16, "outputs": {"thing": "iron"}, "rows": BLANK_ROWS}
    write_json(art["shapes"] / "thing.json", shape)
    rt.render_file(art["shapes"] / "thing.json", art["palettes"], art["out"])
    with Image.open(art["out"] / "thing.png") as img:
        assert img.getpixel((8, 8)) == (0, 0, 0, 0)


def test_8digit_hex_alpha(art):
    write_json(art["palettes"] / "ghost.json", {"colors": {"B": "#C8C8C880"}})
    shape = {"id": "g", "size": 16, "outputs": {"g": "ghost"}, "rows": diagonal_rows()}
    write_json(art["shapes"] / "g.json", shape)
    rt.render_file(art["shapes"] / "g.json", art["palettes"], art["out"])
    with Image.open(art["out"] / "g.png") as img:
        assert img.getpixel((0, 0)) == (0xC8, 0xC8, 0xC8, 0x80)


# --------------------------------------------------------------------------- #
# palette inheritance
# --------------------------------------------------------------------------- #

def test_extends_overrides_single_char(art):
    resolved = rt.resolve_palette("leather_diamond", art["palettes"])
    assert resolved["a"] == "#7DF2EE"   # overridden
    assert resolved["B"] == "#5A3E22"   # inherited


def test_extends_cycle_raises(art):
    write_json(art["palettes"] / "p1.json", {"extends": "p2", "colors": {}})
    write_json(art["palettes"] / "p2.json", {"extends": "p1", "colors": {}})
    with pytest.raises(rt.RenderError, match="cycle"):
        rt.resolve_palette("p1", art["palettes"])


def test_extends_missing_raises(art):
    write_json(art["palettes"] / "p.json", {"extends": "nope", "colors": {}})
    with pytest.raises(rt.RenderError, match="nope"):
        rt.resolve_palette("p", art["palettes"])


# --------------------------------------------------------------------------- #
# strict validation — hard fail
# --------------------------------------------------------------------------- #

def test_bad_hex_raises(art):
    write_json(art["palettes"] / "bad.json", {"colors": {"B": "#ZZZ"}})
    with pytest.raises(rt.RenderError, match="hex"):
        rt.resolve_palette("bad", art["palettes"])


def test_wrong_row_count_raises(art):
    shape = {"id": "x", "size": 16, "outputs": {"x": "iron"}, "rows": ["." * 16 for _ in range(15)]}
    write_json(art["shapes"] / "x.json", shape)
    with pytest.raises(rt.RenderError, match="16 rows"):
        rt.render_file(art["shapes"] / "x.json", art["palettes"], art["out"])


def test_wrong_row_length_raises(art):
    rows = diagonal_rows()
    rows[4] = "." * 17
    shape = {"id": "x", "size": 16, "outputs": {"x": "iron"}, "rows": rows}
    write_json(art["shapes"] / "x.json", shape)
    with pytest.raises(rt.RenderError, match="row 4"):
        rt.render_file(art["shapes"] / "x.json", art["palettes"], art["out"])


def test_undefined_char_raises(art):
    rows = diagonal_rows()
    rows[4] = rows[4][:9] + "x" + rows[4][10:]
    shape = {"id": "x", "size": 16, "outputs": {"x": "iron"}, "rows": rows}
    write_json(art["shapes"] / "x.json", shape)
    with pytest.raises(rt.RenderError, match="'x'"):
        rt.render_file(art["shapes"] / "x.json", art["palettes"], art["out"])


def test_size_not_16_raises(art):
    shape = {"id": "x", "size": 32, "outputs": {"x": "iron"}, "rows": diagonal_rows()}
    write_json(art["shapes"] / "x.json", shape)
    with pytest.raises(rt.RenderError, match="size"):
        rt.render_file(art["shapes"] / "x.json", art["palettes"], art["out"])


def test_id_mismatch_raises(art):
    shape = {"id": "wrong", "size": 16, "outputs": {"wrong": "iron"}, "rows": diagonal_rows()}
    write_json(art["shapes"] / "x.json", shape)
    with pytest.raises(rt.RenderError, match="id"):
        rt.render_file(art["shapes"] / "x.json", art["palettes"], art["out"])


def test_missing_palette_for_output_raises(art):
    shape = {"id": "x", "size": 16, "outputs": {"x": "doesnotexist"}, "rows": diagonal_rows()}
    write_json(art["shapes"] / "x.json", shape)
    with pytest.raises(rt.RenderError, match="doesnotexist"):
        rt.render_file(art["shapes"] / "x.json", art["palettes"], art["out"])


# --------------------------------------------------------------------------- #
# --check / validate-all
# --------------------------------------------------------------------------- #

def test_validate_all_ok(art):
    shape = {"id": "thing", "size": 16, "outputs": {"thing": "iron"}, "rows": diagonal_rows()}
    write_json(art["shapes"] / "thing.json", shape)
    errors = rt.validate_all(art["shapes"], art["palettes"])
    assert errors == []


def test_validate_all_collects_errors(art):
    bad = {"id": "thing", "size": 16, "outputs": {"thing": "iron"}, "rows": ["." * 16 for _ in range(15)]}
    write_json(art["shapes"] / "thing.json", bad)
    errors = rt.validate_all(art["shapes"], art["palettes"])
    assert len(errors) == 1
    assert "thing" in errors[0]


# --------------------------------------------------------------------------- #
# gradients — a palette char maps to {from, to, axis}; converter interpolates
# --------------------------------------------------------------------------- #

def _full_grid(char: str) -> list[str]:
    return [char * 16 for _ in range(16)]


def _single_column(char: str, col: int) -> list[str]:
    grid = [list("." * 16) for _ in range(16)]
    for y in range(16):
        grid[y][col] = char
    return ["".join(r) for r in grid]


def _single_row(char: str, row: int) -> list[str]:
    grid = [list("." * 16) for _ in range(16)]
    for x in range(16):
        grid[row][x] = char
    return ["".join(r) for r in grid]


def test_gradient_y_axis_interpolates(art):
    write_json(art["palettes"] / "grad.json",
               {"colors": {"g": {"from": "#000000", "to": "#FFFFFF", "axis": "y"}}})
    shape = {"id": "gr", "size": 16, "outputs": {"gr": "grad"}, "rows": _single_column("g", 8)}
    write_json(art["shapes"] / "gr.json", shape)
    rt.render_file(art["shapes"] / "gr.json", art["palettes"], art["out"])
    with Image.open(art["out"] / "gr.png") as img:
        assert img.getpixel((8, 0)) == (0, 0, 0, 255)          # t=0 -> from
        assert img.getpixel((8, 15)) == (255, 255, 255, 255)   # t=1 -> to
        assert img.getpixel((8, 8))[0] == round(8 / 15 * 255)  # midpoint channel


def test_gradient_x_axis(art):
    write_json(art["palettes"] / "gx.json",
               {"colors": {"g": {"from": "#000000", "to": "#FFFFFF", "axis": "x"}}})
    shape = {"id": "gx", "size": 16, "outputs": {"gx": "gx"}, "rows": _full_grid("g")}
    write_json(art["shapes"] / "gx.json", shape)
    rt.render_file(art["shapes"] / "gx.json", art["palettes"], art["out"])
    with Image.open(art["out"] / "gx.png") as img:
        assert img.getpixel((0, 5)) == (0, 0, 0, 255)
        assert img.getpixel((15, 5)) == (255, 255, 255, 255)


def test_gradient_diag_axis(art):
    write_json(art["palettes"] / "gd.json",
               {"colors": {"g": {"from": "#000000", "to": "#FFFFFF", "axis": "diag"}}})
    shape = {"id": "gd", "size": 16, "outputs": {"gd": "gd"}, "rows": _full_grid("g")}
    write_json(art["shapes"] / "gd.json", shape)
    rt.render_file(art["shapes"] / "gd.json", art["palettes"], art["out"])
    with Image.open(art["out"] / "gd.png") as img:
        assert img.getpixel((0, 0)) == (0, 0, 0, 255)            # coord x+y = 0 (min)
        assert img.getpixel((15, 15)) == (255, 255, 255, 255)    # coord 30 (max)


def test_gradient_adiag_axis(art):
    write_json(art["palettes"] / "ga.json",
               {"colors": {"g": {"from": "#000000", "to": "#FFFFFF", "axis": "adiag"}}})
    shape = {"id": "ga", "size": 16, "outputs": {"ga": "ga"}, "rows": _full_grid("g")}
    write_json(art["shapes"] / "ga.json", shape)
    rt.render_file(art["shapes"] / "ga.json", art["palettes"], art["out"])
    with Image.open(art["out"] / "ga.png") as img:
        assert img.getpixel((0, 15)) == (0, 0, 0, 255)           # coord x-y = -15 (min)
        assert img.getpixel((15, 0)) == (255, 255, 255, 255)     # coord 15 (max)


def test_gradient_single_line_extent_resolves_to_from(art):
    # a horizontal line under a y-axis gradient: every pixel shares y -> span 0 -> from
    write_json(art["palettes"] / "gl.json",
               {"colors": {"g": {"from": "#112233", "to": "#FFFFFF", "axis": "y"}}})
    shape = {"id": "gl", "size": 16, "outputs": {"gl": "gl"}, "rows": _single_row("g", 7)}
    write_json(art["shapes"] / "gl.json", shape)
    rt.render_file(art["shapes"] / "gl.json", art["palettes"], art["out"])
    with Image.open(art["out"] / "gl.png") as img:
        assert img.getpixel((3, 7)) == (0x11, 0x22, 0x33, 255)


def test_gradient_alpha_interpolates(art):
    write_json(art["palettes"] / "galpha.json",
               {"colors": {"g": {"from": "#FFFFFF00", "to": "#FFFFFFFF", "axis": "y"}}})
    shape = {"id": "gp", "size": 16, "outputs": {"gp": "galpha"}, "rows": _single_column("g", 8)}
    write_json(art["shapes"] / "gp.json", shape)
    rt.render_file(art["shapes"] / "gp.json", art["palettes"], art["out"])
    with Image.open(art["out"] / "gp.png") as img:
        assert img.getpixel((8, 0))[3] == 0
        assert img.getpixel((8, 15))[3] == 255


def test_gradient_flat_colors_still_work(art):
    # regression: a palette mixing flat + gradient chars renders flats unchanged
    write_json(art["palettes"] / "mix.json",
               {"colors": {"B": "#C8C8C8", "g": {"from": "#000000", "to": "#FFFFFF", "axis": "y"}}})
    grid = [list("." * 16) for _ in range(16)]
    grid[0][0] = "B"
    grid[15][15] = "g"
    shape = {"id": "mix", "size": 16, "outputs": {"mix": "mix"}, "rows": ["".join(r) for r in grid]}
    write_json(art["shapes"] / "mix.json", shape)
    rt.render_file(art["shapes"] / "mix.json", art["palettes"], art["out"])
    with Image.open(art["out"] / "mix.png") as img:
        assert img.getpixel((0, 0)) == (0xC8, 0xC8, 0xC8, 255)  # flat unchanged
        # single gradient pixel -> span 0 -> from
        assert img.getpixel((15, 15)) == (0, 0, 0, 255)


def test_gradient_invalid_axis_raises(art):
    write_json(art["palettes"] / "bad.json",
               {"colors": {"g": {"from": "#000000", "to": "#FFFFFF", "axis": "radial"}}})
    with pytest.raises(rt.RenderError, match="axis"):
        rt.resolve_palette("bad", art["palettes"])


def test_gradient_invalid_hex_raises(art):
    write_json(art["palettes"] / "bad2.json",
               {"colors": {"g": {"from": "#ZZZ", "to": "#FFFFFF", "axis": "x"}}})
    with pytest.raises(rt.RenderError, match="hex"):
        rt.resolve_palette("bad2", art["palettes"])


def test_gradient_wrong_keys_raises(art):
    write_json(art["palettes"] / "bad3.json",
               {"colors": {"g": {"from": "#000000", "axis": "x"}}})
    with pytest.raises(rt.RenderError, match="from"):
        rt.resolve_palette("bad3", art["palettes"])
