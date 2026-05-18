# Plan-runner Cycle 5 -- Fix Plan

> **Source bugs:** `docs/plan-runner/2026-05-18/cycle-5/bugs.md`
> **Prior cycle:** plan-runner cycle 5 (1 P2 bug flagged; Phase 6 otherwise GREEN)

## File Map

**Modified files:**
- `.github/workflows/wiki-publish.yml` -- update CI command line to use the chiseled wrapper

---

## Task 1: Update CI workflow to use chiseledGenerateWiki

**Files:**
- Modify: `.github/workflows/wiki-publish.yml`

The CI workflow currently runs `./gradlew :1.21.4:generateWiki publishWiki`. In this Stonecutter 0.6
layout, `:1.21.4:generateWiki` triggers `:1.21.4:compileKotlin` against the raw shared source root
(`src/main/kotlin/`), which still contains unprocessed `//? if >=1.21.11` Stonecutter directives and
fails to compile. The Stonecutter wrapper task `chiseledGenerateWiki` (added in cycle 5) swaps
srcDirs to the per-version chiseledSrc tree before compileKotlin runs and produces the wiki output
under `versions/<mc>/build/wiki/` for every fork.

`publishWiki` then needs to consume the 1.21.4 output specifically.

- [ ] **Step 1: Update the `run:` line**

Edit `.github/workflows/wiki-publish.yml` and change:

```yaml
      - name: Generate + publish wiki
        env:
          WIKI_PUBLISH_TOKEN: ${{ secrets.WIKI_PUBLISH_TOKEN }}
        run: ./gradlew :1.21.4:generateWiki publishWiki
```

to:

```yaml
      - name: Generate + publish wiki
        env:
          WIKI_PUBLISH_TOKEN: ${{ secrets.WIKI_PUBLISH_TOKEN }}
        run: ./gradlew chiseledGenerateWiki :1.21.4:publishWiki
```

- [ ] **Step 2: Update the artifact upload path**

The artifact-upload step currently points at `1.21.4/build/wiki/`. With `chiseledGenerateWiki`, the
1.21.4 output is at `versions/1.21.4/build/wiki/`. Edit the path:

```yaml
      - name: Upload generated wiki as workflow artifact
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: generated-wiki
          path: versions/1.21.4/build/wiki/    # was: 1.21.4/build/wiki/
          if-no-files-found: warn
          retention-days: 14
```

- [ ] **Step 3: Verify the YAML still parses**

Run a YAML parser check locally (no test command exists in the repo; the CI provider's first
real run on the next tag push will surface any remaining issues).

```
python -c "import yaml; yaml.safe_load(open('.github/workflows/wiki-publish.yml')); print('YAML OK')"
```

Expected: `YAML OK`.

- [ ] **Step 4: (Optional, follow-up) Consider tightening publishWiki**

`publishWiki` is registered at the root `build.gradle.kts` and currently has `dependsOn("generateWiki")`.
After this fix, the CI flow becomes: `chiseledGenerateWiki` (generates for all 5 forks) then
`:1.21.4:publishWiki` (publishes the 1.21.4 output). The `dependsOn("generateWiki")` in
`publishWiki` is still satisfied because `chiseledGenerateWiki` includes `:1.21.4:generateWiki`
as one of its delegated subtasks. No build.gradle.kts change required for this task.

If a future cycle wants to publish from a non-1.21.4 fork (e.g. switch CI to `:1.21.11:publishWiki`),
that change is independent of this fix-plan.

---

## Out of scope

- Re-architecting `publishWiki` to be Stonecutter-aware (it already runs after generation; the
  task body operates on `versions/1.21.4/build/wiki/` via Gradle's `layout.buildDirectory`, which
  resolves correctly under the `:1.21.4:` project).
- Modifying `generateWiki` itself -- it works correctly under the chiseled wrapper and is left as-is.
