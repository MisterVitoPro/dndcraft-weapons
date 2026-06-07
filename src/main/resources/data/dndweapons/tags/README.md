# Tag directory layout

Minecraft renamed the data-pack tag root from `tags/items/` (plural, pre-1.21)
to `tags/item/` (singular, 1.21+) between MC 1.20.x and 1.21.x. The mod ships
the same 4 role tags (light_crossbow, shortbow, shortsword, trident) on every
supported version, but the on-disk path differs by version:

| MC version | Tag root | Where the JSONs live |
|---|---|---|
| 1.20.1     | `data/dndweapons/tags/items/role/`  | `versions/1.20.1/src/main/resources/...` (per-version overlay) |
| 1.21.x     | `data/dndweapons/tags/item/role/`   | `src/main/resources/...` (shared) |
| 26.1.2     | `data/dndweapons/tags/item/role/`   | `src/main/resources/...` (shared) |

The four role JSONs are identical content across both locations. When editing a
role tag, **edit both copies** or future contributors will silently see one MC
version diverge from another.

If a future MC version drops the singular `tags/item/` path or renames it
again, fork the shared `tags/item/` folder into a per-version overlay (the
1.20.1 pattern below) and add a row to the table above.
