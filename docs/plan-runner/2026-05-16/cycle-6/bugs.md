# plan-runner Bug Report
**Date:** 2026-05-16
**Cycle:** 6
**Source plan:** docs/superpowers/plans/2026-05-16-dnd-weapons-phase-2b-catalog-expansion.md

## Summary
- P0: 0
- P1: 1
- P2: 1
- P3: 0
- Total: 2

## P0 Bugs
(none)

## P1 Bugs

### [wave-4-agent-1-bug-1] Recipe tag-ingredient format incompatible with FAPI on 1.21.4, 1.21.11, and 26.1.2 -- expanded-catalog recipes fail to load at runtime
**Wave/Agent:** wave-4 agent-1 (task: Per-version smoke before texture generation (5 MC versions))
**Category:** incorrect_implementation
**File:** versions/1.21.4/src/main/resources/data/dndweapons/recipes/ (and equivalent paths under versions/1.21.11 and versions/26.1.2)
**Expected:** All expanded-catalog weapon recipes should load without ERROR on all 5 MC versions. The recipe ingredient tag serializer format must be adapted per-version (versioned stonecutter blocks or conditional ingredient schemas) so recipes are functional in-game, not merely absent from the parse error log.
**Evidence:**
```
During :runGametest on 1.21.4, 1.21.11, and 26.1.2, recipe-loading ERRORs were logged for all
expanded-catalog weapon recipes (~34 weapons). The tag ingredient format used
(bare {"tag":"c:ingots/iron"} or {"fabric:type":"fabric:tag","tag":"c:ingots/iron"}) is not
accepted by the FAPI version active on those MC targets. Recipes fail to parse and are silently
skipped; gametests pass only because registration checks do not exercise recipe lookup. 1.20.1
and 1.21.1 showed no such errors, suggesting either the format is compatible there or the recipes
were absent for those runs.
```
**Suggested fix:** Introduce versioned recipe source sets or Gradle copy-filter logic that writes the correct ingredient format per MC target: use the flat {"tag":"..."} form only where the FAPI RecipeSerializer accepts it, and use the {"fabric:type":"fabric:tag","tag":"..."} (or the vanilla 1.21+ {"ingredient":{"tag":"..."}} wrapper) for other targets. Verify fix by asserting non-zero recipe counts in a gametest or via /recipe command in runClient for each version.

## P2 Bugs

### [wave-5-bug-1] Generated textures are high-res (340-858 KB each, not 16x16) -- Minecraft will rescale them which may produce blurry/non-pixel-art appearance
**Wave/Agent:** wave-5 agent-1 through agent-6 (task: Generate textures batches 1-6)
**Category:** incorrect_implementation
**File:** src/main/resources/assets/dndweapons/textures/item/*.png
**Expected:** Files should be valid 16x16 pixel PNG images per acceptance criterion; a true 16x16 RGBA PNG would be under 1 KB.
**Evidence:**
```
club.png is 492.9 KB and rapier.png is 529.5 KB -- visual inspection confirms high-resolution
output (estimated 768x768 or 1024x1024). The pre-existing longsword.png is 0.1 KB, consistent
with a true 16x16 pixel-art sprite. All 33 gemini-generated textures range from 340 KB
(sling.png) to 858 KB (javelin.png), clearly not 16x16.
```
**Suggested fix:** Post-process gemini outputs with PIL/Pillow or ImageMagick to downscale to 16x16 with nearest-neighbor filtering (e.g. `convert input.png -filter point -resize 16x16 output.png`). Alternatively replace with handcrafted 16x16 pixel art in a future iteration.

## P3 Bugs
(none)
