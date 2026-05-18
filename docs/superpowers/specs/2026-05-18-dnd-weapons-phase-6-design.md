# DnD Weapons -- Phase 6 Design Specification

**Date:** 2026-05-18
**Status:** Approved (spec drafted by extrapolation from master design Section 10 and Phase 1-5 conventions; user opted to skip a fresh brainstorm)
**Author:** MisterVitoPro
**Parent design:** [2026-05-16-dnd-weapons-design.md](2026-05-16-dnd-weapons-design.md) (Section 10 "Player Wiki")
**Predecessor phase:** [2026-05-18-dnd-weapons-phase-5-design.md](2026-05-18-dnd-weapons-phase-5-design.md) -- Phase 5 shipped the acquisition surface (StructureLoot / MobDrop / VillagerTradeEntry catalog + 3 registrars + 3 gametests). Final verification at [phase-5-verification-final.md](../plans/phase-5-verification-final.md).
**Implementation plan:** `docs/superpowers/plans/2026-05-18-dnd-weapons-phase-6-wiki.md` (next step)

---

## 1. Goal & Scope

**Goal:** Auto-generate a player-facing GitHub Wiki from the existing source-of-truth catalogs (`Weapons.ALL`, `AcquisitionCatalog.*`) so the wiki always matches the latest released code, and ship a one-shot `publishWiki` Gradle task that syncs the rendered output to `<repo>.wiki.git`.

Phase 6 is the last v1.0 phase. It introduces no gameplay code -- it reads existing data and emits Markdown. The novelties are: a single Kotlin codegen entry point (`WikiGen.kt`) following the Phase 5 `Phase5TradeCodegen.kt` pattern, hand-authored narrative pages living under `wiki/handwritten/`, and a publish task that operates on the wiki sidecar git repo without touching the main repo's git state.

### In scope

| Item | Count | Source |
|---|---|---|
| Auto-generated per-weapon pages | 38 | `Weapons.ALL` |
| Auto-generated category index pages | 4 | `Weapons.ALL` grouped by `Category` |
| Hand-authored narrative pages | 9 | hand-written, checked in under `wiki/handwritten/` |
| Wiki navigation (`_Sidebar.md`, `_Footer.md`) | 2 | hand-written under `wiki/handwritten/` |
| `WikiGen.kt` Kotlin codegen (runnable `main`) | 1 | new |
| `generateWiki` Gradle task (`JavaExec`-style) | 1 | added to `build.gradle.kts` |
| `publishWiki` Gradle task (shallow-clone + push) | 1 | added to `build.gradle.kts` |
| GitHub Actions workflow (`v*` tag trigger) | 1 | `.github/workflows/wiki-publish.yml` |
| Unit tests for WikiGen | 1 | `WikiGenTest.kt` (template render correctness, category indexes, vanilla-mapped callout) |
| Wiki output directory | `build/wiki/` | gitignored; regenerated each build |
| **Total new Kotlin LOC (rough)** | **~550** | |
| **Total new Markdown LOC (rough)** | **~600** (handwritten only) | |

### Explicitly out of scope

- **Localization beyond `en_us`** -- master design §11 deferral. The generator reads `displayName` directly from `WeaponSpec`; no i18n lookup.
- **Patchouli / in-game guide book** -- master design §11 deferral.
- **Datapack-extensible catalog reading** -- the generator reads compiled `Weapons.ALL` only; no dynamic JSON catalogs.
- **Sub-page-per-tier** -- Diamond / Netherite variants are documented inline on the base weapon page, not as separate pages.
- **Auto-publishing on every commit to `main`** -- publish triggers only on `v*` tag push, matching CI policy from master design §10.
- **Wiki content for Phase 4 smithing templates** -- the smithing-template *recipes* are linked from the per-weapon "Tiers" section; the templates themselves do not get their own pages.
- **Screenshots / images in auto-generated pages** -- text-only render in v1.0. Hand-authored pages may include images at the author's discretion.
- **Discord embed / per-page diff notifications** -- future work.
- **Multi-MC-version sections per page** -- the catalog is version-agnostic; the wiki renders one canonical form. Version-specific gotchas live in the hand-authored `Version-Notes.md`.

---

## 2. Decisions

| # | Decision | Choice | Rationale |
|---|---|---|---|
| 1 | Implementation language | Kotlin, in the main source set | Matches Phase 5 `Phase5TradeCodegen.kt`. Reuses `Weapons.ALL` and `AcquisitionCatalog` imports directly with no reflection. |
| 2 | Codegen entry point | `src/main/kotlin/com/dndweapons/codegen/WikiGen.kt` with a `main(args)` | One runnable; Gradle invokes it via `JavaExec`. No `buildSrc/` module needed. |
| 3 | Templating | Hand-rolled Kotlin `buildString {}` + string templates | No Mustache / Handlebars / Freemarker dependency. Stays consistent with the rest of the mod (no extra deps). |
| 4 | Output directory | `build/wiki/` (gitignored) | Regenerated every build; never committed to main repo. |
| 5 | Handwritten directory | `wiki/handwritten/` (committed to main repo) | Source-controlled narrative; copied into `build/wiki/` at publish time. |
| 6 | Wiki structure | Flat `Weapons/<Name>.md` + 4 category indexes + 9 narrative pages + sidebar/footer | Matches master design §10 layout. |
| 7 | Vanilla-mapped pages | Use the canonical DnD name (e.g. `Shortsword.md`) with a callout box explaining vanilla mapping | Avoids the "where's the shortsword?" support question (master design §10). |
| 8 | Generate task name | `generateWiki` | Matches master design §10 naming. |
| 9 | Publish task name | `publishWiki` | Matches master design §10 naming. |
| 10 | Publish auth | Env var `WIKI_PUBLISH_TOKEN` (GitHub PAT with `repo` scope) | Standard pattern for wiki sidecar repos. Local dev: dry-run with `--dry-run` flag. |
| 11 | Wiki sidecar repo URL | `git@github.com:MisterVitoPro/dndcraft-weapons.wiki.git` (SSH) when invoked locally; `https://x-access-token:${WIKI_PUBLISH_TOKEN}@github.com/MisterVitoPro/dndcraft-weapons.wiki.git` in CI | Local devs use SSH; CI uses token. The Gradle task picks the URL based on whether `WIKI_PUBLISH_TOKEN` is set. |
| 12 | CI trigger | `push` to tags matching `v*` AND `phase-*` | Releases tag with `v*`; phase milestones tag with `phase-*`. Either triggers a wiki sync. |
| 13 | Generator runs against which Stonecutter fork? | Default fork (whatever `stonecutter.active` points to in `settings.gradle.kts`) -- the catalog data is identical across forks | The wiki content is version-agnostic. No need to run it 5 times. |
| 14 | Acquisition data on per-weapon pages | Inline list of structures / mobs / villager trades pulled from `AcquisitionCatalog` | Single source of truth (decided in Phase 5). Renders as bulleted lists per acquisition surface. |
| 15 | Vanilla-mapped weapons' acquisition section | "Vanilla item -- acquired via the vanilla Minecraft Sword/Bow/etc." callout | They don't appear in our `AcquisitionCatalog`; the callout explains why. |
| 16 | Test coverage | One `WikiGenTest.kt` with 5 tests: every-weapon-renders, category-index-completeness, vanilla-callout-present, acquisition-section-present-when-data-exists, handwritten-file-copy | Smoke tests on the renderer; no need to diff every page. |
| 17 | Failure mode on publish auth failure | Task fails with clear error; main build is unaffected | `generateWiki` is a separate task from `chiseledBuild`. CI can run `generateWiki` without `publishWiki`. |
| 18 | Wiki home page (`Home.md`) | Auto-generated header + hand-authored body | Header has live mod-version + build-time stamp; body is the welcome text from `wiki/handwritten/Home.md`. |

---

## 3. Architecture

```
src/main/kotlin/com/dndweapons/codegen/
  WikiGen.kt                       # runnable main; writes to build/wiki/
  wiki/
    WikiTemplates.kt               # buildString-based templates for each page type
    WikiPaths.kt                   # filename + url-slug helpers
    AcquisitionLookup.kt           # inverse map: weapon_id -> List<AcquisitionFact>
                                   # (derived from AcquisitionCatalog at gen time)

src/test/kotlin/com/dndweapons/codegen/
  WikiGenTest.kt                   # 5 smoke tests

wiki/handwritten/                  # source-controlled narrative pages
  Home.md
  Getting-Started.md
  Combat-Mechanics.md
  Smithing-Upgrade-System.md
  Vanilla-Mapped-Weapons.md
  Acquisition-Guide.md
  Material-Gating.md
  Version-Notes.md
  DnD-Translation-Philosophy.md
  Future-Work.md
  _Sidebar.md
  _Footer.md

build.gradle.kts                   # adds generateWiki + publishWiki tasks

.github/workflows/
  wiki-publish.yml                 # runs on push of v* or phase-* tags

.gitignore                         # add build/wiki/, never commit generated md
```

**`WikiGen.main` flow:**

```
1. Resolve output dir from args[0] (default: build/wiki/)
2. mkdir -p outDir/Weapons/
3. Build AcquisitionLookup (inverse index over STRUCTURE_LOOT, MOB_DROPS, VILLAGER_TRADES)
4. For each WeaponSpec in Weapons.ALL:
     - render via WikiTemplates.renderWeaponPage(spec, acquisition)
     - write to outDir/Weapons/<DisplayName-with-spaces-as-hyphens>.md
5. For each Category in Category.values():
     - render via WikiTemplates.renderCategoryIndex(category, allSpecs)
     - write to outDir/Weapons/<Simple-Melee | Simple-Ranged | Martial-Melee | Martial-Ranged>.md
6. Copy wiki/handwritten/*.md verbatim into outDir/
7. Render Home.md header + concat with handwritten Home.md body
8. Print summary: "<N> weapon pages, 4 category indexes, <M> handwritten pages, total <T> files in <outDir>"
9. Exit 0
```

**`generateWiki` Gradle task** (added to `build.gradle.kts`):

```kotlin
tasks.register<JavaExec>("generateWiki") {
    group = "wiki"
    description = "Generate the player-facing wiki under build/wiki/."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.dndweapons.codegen.WikiGen")
    args(layout.buildDirectory.dir("wiki").get().asFile.absolutePath)
    dependsOn("classes")
}
```

**`publishWiki` Gradle task:**

```kotlin
tasks.register("publishWiki") {
    group = "wiki"
    description = "Sync build/wiki/ to <repo>.wiki.git on a temp clone."
    dependsOn("generateWiki")
    doLast {
        val wikiDir = layout.buildDirectory.dir("wiki").get().asFile
        val cloneDir = layout.buildDirectory.dir("wiki-repo").get().asFile
        val token = System.getenv("WIKI_PUBLISH_TOKEN")
        val url = if (token != null && token.isNotBlank()) {
            "https://x-access-token:$token@github.com/MisterVitoPro/dndcraft-weapons.wiki.git"
        } else {
            "git@github.com:MisterVitoPro/dndcraft-weapons.wiki.git"
        }
        // Clone, wipe, copy, commit, push. Implementation in WikiPublish.kt or inline.
        // Stdout-friendly logging; throw GradleException on git failure.
    }
}
```

**Version drift:** None. `WikiGen.kt` does not call any MC API; it imports `Weapons`, `WeaponSpec`, `Category`, `Property`, `DamageType`, `RangeKind`, `Tier`, and the three `AcquisitionCatalog` maps -- all version-agnostic data classes and objects.

---

## 4. Page templates

### 4.1 Per-weapon page (auto)

Filename: `Weapons/<DisplayName>.md` (spaces -> hyphens, lowercase preserved with title-case from `displayName`)

```markdown
# {{displayName}}

> **Category:** {{category.label}}  |  **Damage type:** {{damageType.label}}  |  **DnD dice:** {{diceText}}{{ versatileDice ? " (versatile " + versatileDice + ")" }}

{{vanillaCallout if isVanillaMapped}}

## Stats

| Property | Value |
|---|---|
| Attack damage | {{attackDamage}}{{ versatileBonus > 0 ? " (+" + versatileBonus + " versatile)" : "" }} |
| Attack speed | {{attackSpeed}} |
| Reach bonus | {{reachBonus}} |
| Knockback bonus | +{{knockbackBonus}} |
| Properties | {{properties joined ", "}} |
| Range | {{ranged.label}} |
| Base durability | {{baseDurability}} |

## Tiers

This weapon supports the full smithing-upgrade ladder (iron -> diamond -> netherite),
except where noted in [Smithing-Upgrade-System](Smithing-Upgrade-System).

{{vanillaTierTreatment if isVanillaMapped: "Vanilla material progression applies. No smithing template required."}}

## Acquisition

{{acquisitionList from AcquisitionLookup, or "Crafting only. Not present in any structure loot, villager trade, or mob drop in v1.0." if no entries}}

## Combat behaviour

Properties translated to vanilla-feeling hooks per the [Combat-Mechanics](Combat-Mechanics) page.
{{propertyHookList: e.g. "- LIGHT: +1 damage when offhand also holds a Light weapon"}}
```

**Vanilla callout (when `isVanillaMapped == true`):**

```markdown
> :information_source:  **This weapon is represented by the vanilla Minecraft {{vanillaItemName}}.** When you craft or pick up a vanilla {{vanillaItemName}}, it carries the {{displayName}} identity: tooltip stat block, combat hooks, role tag. No separate item is registered.
```

### 4.2 Category index (auto)

Filename: `Weapons/<Simple-Melee | Simple-Ranged | Martial-Melee | Martial-Ranged>.md`

```markdown
# {{categoryLabel}}

| Weapon | Damage | Properties | Notes |
|---|---|---|---|
{{for each spec in this category:}}
| [{{displayName}}]({{displayName}}) | {{attackDamage}} | {{properties}} | {{vanillaMappedNote}} |
```

### 4.3 Home page (mixed)

`Home.md` is the only mixed page. The generator emits a small header, then concatenates `wiki/handwritten/Home.md`:

```markdown
<!-- AUTO-GENERATED HEADER -->
**DnD Weapons** for Minecraft -- v{{modVersion}}, build {{buildSha}}.
Supported MC versions: 1.20.1, 1.21.1, 1.21.4, 1.21.11, 26.1.2.

----

<!-- HANDWRITTEN BODY -->
{{contents of wiki/handwritten/Home.md verbatim}}
```

### 4.4 Hand-authored pages (verbatim copy)

The 9 narrative pages plus `_Sidebar.md` and `_Footer.md` are copied byte-for-byte from `wiki/handwritten/` into `build/wiki/`. The generator does not modify them. They are placeholders in v1.0 -- minimum-viable content per page is acceptable.

---

## 5. Testing

`src/test/kotlin/com/dndweapons/codegen/WikiGenTest.kt` -- pure JUnit 5, no MC API dependency. Runs via `chiseledTest` on every version.

| Test | Asserts |
|---|---|
| `everyWeaponInCatalogRendersToAFile` | `WikiGen.main(tmpDir)` produces a `Weapons/<DisplayName>.md` for every spec in `Weapons.ALL` |
| `everyCategoryIndexContainsAllItsWeapons` | Each category index page lists every spec where `spec.category == this category` |
| `vanillaMappedPagesIncludeTheCallout` | Pages for `SHORTSWORD`, `SHORTBOW`, `LIGHT_CROSSBOW`, `TRIDENT` contain the `:information_source:` callout line |
| `acquisitionSectionMatchesCatalogData` | For a known weapon (e.g. `longsword`) the rendered "Acquisition" section lists `stronghold_corridor` and `weaponsmith_level3` (verified against `AcquisitionCatalog`) |
| `handwrittenPagesAreCopiedVerbatim` | After `WikiGen.main(tmpDir)`, every file in `wiki/handwritten/` exists in `tmpDir` with identical bytes |

No gametest is needed. The wiki is a text artifact with no runtime behaviour to verify in-game.

---

## 6. Init wiring

Phase 6 introduces no new init calls in `DndWeaponsMod.onInitialize()`. The wiki generator is a build-time tool, not a runtime hook.

---

## 7. CI integration

`.github/workflows/wiki-publish.yml`:

```yaml
name: Publish Wiki
on:
  push:
    tags:
      - 'v*'
      - 'phase-*'
jobs:
  publish:
    runs-on: ubuntu-latest
    permissions:
      contents: read
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
      - run: ./gradlew :1.21.4:generateWiki publishWiki
        env:
          WIKI_PUBLISH_TOKEN: ${{ secrets.WIKI_PUBLISH_TOKEN }}
```

The `:1.21.4:` prefix selects one Stonecutter fork (any fork would work since catalog data is identical; 1.21.4 picked as the "main" target). The secret is added to the GitHub repo settings ahead of the first tag push.

---

## 8. Acceptance criteria

| Criterion | How verified |
|---|---|
| `./gradlew generateWiki` produces 38 weapon pages | Count files in `build/wiki/Weapons/*.md` minus 4 category index pages |
| `./gradlew generateWiki` produces 4 category indexes | `Simple-Melee.md`, `Simple-Ranged.md`, `Martial-Melee.md`, `Martial-Ranged.md` present |
| All 11 handwritten pages copied verbatim | `diff -r wiki/handwritten/ build/wiki/` (excluding `Home.md` which is mixed) |
| Vanilla-mapped callouts present | `grep -l "represented by the vanilla" build/wiki/Weapons/{Shortsword,Shortbow,Light-Crossbow,Trident}.md` returns 4 hits |
| `./gradlew chiseledTest` passes on all 5 versions | `WikiGenTest`'s 5 cases pass; no regression in existing JUnit |
| `./gradlew publishWiki --dry-run` succeeds locally | (manual smoke; not enforced in CI) |
| Wiki publishes from CI on `v*` tag | First `v0.1.0` tag push triggers workflow; `<repo>.wiki.git` receives new commit |

The phase-6 git tag (`phase-6-wiki`) is applied manually after the user confirms the wiki renders correctly on github.com/MisterVitoPro/dndcraft-weapons/wiki.

---

## 9. Open questions (resolved by extrapolation, flag if user disagrees)

1. **Wiki sidecar URL:** Master design says `<repo>.wiki.git`. Memory says the canonical repo is `git@github.com:MisterVitoPro/dndcraft-weapons.git`. Phase 6 uses `git@github.com:MisterVitoPro/dndcraft-weapons.wiki.git` accordingly.
2. **Hand-authored page initial content:** Each handwritten page ships with a minimal placeholder body (a one-sentence stub) so the generator has something to copy. Real content is filled in post-Phase-6.
3. **Java release target:** Phase 6's `WikiGen.kt` runs on the same JVM as the build (Java 21 per `java_release`). No special toolchain config needed.
4. **`_Sidebar.md` content:** Minimal 5-link sidebar -- Home, Getting Started, Combat Mechanics, Smithing, all-weapons (link to first category index). Author can expand later.

---

## 10. Risks / mitigations

| Risk | Mitigation |
|---|---|
| Wiki repo doesn't exist yet on GitHub | Create the wiki on first repo visit (GitHub auto-initializes when you click "Wiki" tab once). Document this one-time setup step in the README. |
| Hand-authored Markdown has syntax errors | `WikiGenTest`'s "handwrittenPagesAreCopiedVerbatim" only verifies byte-identity; render verification is the user's eyeball check post-publish. Acceptable for v1.0. |
| Generator depends on compiled `Weapons.ALL` | Gradle's `dependsOn("classes")` ensures it. Confirmed: `:1.21.4:classes` is the prereq for `:1.21.4:generateWiki`. |
| Multi-fork ambiguity | Document in the workflow that any single fork suffices because catalog data is version-agnostic. Pin to `:1.21.4:` in CI. |
| Wiki history bloat from re-publishes | `publishWiki` commits with a fixed message format; force-push policy NOT applied. Old commits stay; wiki repo size remains small for text content. |

---

## 11. Plan-runner work plan (summary, for the writing-plans step)

The implementation plan should bucket the Phase 6 work into the following file-disjoint tasks for plan-runner. Approximate wave layout:

- **Wave 1 (3 agents, parallel):** WikiPaths.kt + AcquisitionLookup.kt + WikiTemplates.kt skeleton (data + helpers; no MC imports).
- **Wave 2 (1 agent):** WikiGen.kt (the runnable main; depends on wave 1).
- **Wave 3 (1 agent):** WikiGenTest.kt (depends on wave 2).
- **Wave 4 (parallel up to 6 agents):** Hand-authored placeholder Markdown pages. File-disjoint -- one agent per page or grouped by 2-3 files.
- **Wave 5 (1 agent):** `build.gradle.kts` task wiring (generateWiki + publishWiki).
- **Wave 6 (1 agent):** GitHub Actions workflow + `.gitignore` update for `build/wiki/`.
- **Wave 7 (1 agent):** Run `./gradlew chiseledBuild`, `./gradlew chiseledTest`, `./gradlew :1.21.4:generateWiki`, verify file counts, write `phase-6-verification-final.md`.

Plan-runner will refine these into agent buckets at run time.
