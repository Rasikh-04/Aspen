# 14 — Git Workflow (branch → PR → merge), cross-platform

> How the two devs push branches, open PRs, review, and merge to `main` — with the Linux/Windows split handled so it doesn't produce noisy diffs or broken builds. Trunk-based: short-lived feature branches off `main`, integrated via PR. `main` is always green and releasable.

---

## 0. One-time cross-platform setup (do this first — it prevents the classic pain)

The Linux ↔ Windows split causes three predictable problems: **line endings**, **path length**, and the **gradlew executable bit**. Handle them once.

### 0.1 `.gitattributes` (commit at repo root — fixes line endings for everyone)
This normalises line endings in the repo to **LF** regardless of OS, so Windows checkouts don't turn every file into a whitespace diff:

```gitattributes
# Normalise all text to LF in the repo; tools handle native endings on checkout
* text=auto eol=lf

# Scripts that MUST stay LF (gradlew breaks with CRLF)
gradlew         text eol=lf
*.sh            text eol=lf

# Windows batch must stay CRLF
*.bat           text eol=crlf
*.cmd           text eol=crlf

# Binary — never touch
*.png binary
*.jpg binary
*.webp binary
*.ttf binary
*.otf binary
*.jar binary
*.keystore binary
*.jks binary
```

> This file is the real fix. With it committed, neither dev needs fragile `core.autocrlf` gymnastics.

### 0.2 Per-machine git config
**Both:**
```bash
git config --global init.defaultBranch main
git config --global pull.rebase true          # keep feature-branch history linear
git config --global core.autocrlf input       # (harmless alongside .gitattributes)
```
**Dev B (Windows) — also:**
```bash
git config --global core.longpaths true        # Gradle/KMP paths exceed 260 chars
```
Enable Windows long paths at the OS level too (Win+R → `gpedit` → Enable Win32 long paths, or the registry `LongPathsEnabled=1`) — KMP build/cache paths are deep.

### 0.3 gradlew executable bit (Linux/macOS need it; git can drop it)
If `./gradlew` ever loses its exec bit after a Windows commit:
```bash
git update-index --chmod=+x gradlew
git commit -m "chore: keep gradlew executable"
```

### 0.4 Don't fight the IDE on case
Linux is case-sensitive, Windows isn't. **Never** create two files differing only in case; it corrupts the Windows checkout. Pick one canonical name.

### 0.5 `.gitignore` (commit at root)
```gitignore
.gradle/
build/
local.properties
*.keystore
*.jks
.idea/
*.iml
.kotlin/
.DS_Store
# secrets / env
*.env
secrets.properties
```

---

## 1. Branch model (trunk-based, 2-person)

```
main ──●────────●─────────●──────────●───▶   (always green & releasable; protected; tagged for releases)
        \        \         \
         feat/…   fix/…     chore/…           (short-lived; one feature; off main; PR back into main)
```

- **`main`** — protected. No direct pushes. Always builds, always passes gates. Tag releases (`v0.1.0`, …).
- **Feature branches** — short-lived (hours–days, not weeks), one feature each, branched from latest `main`, merged back via PR. Delete after merge.
- **No long-lived `develop`/divergent branches.** Two people + rigorous PRs don't need gitflow; long branches just create merge pain.

### Branch naming (matches Conventional Commits)
```
feat/grounding-breathing
feat/onboarding-questionnaire-ui
fix/overlay-fullscreen-suspend
chore/version-catalog-bump
docs/update-status
```

---

## 2. The per-feature loop (one branch = one feature through the pipeline)

A feature branch *is* one trip through **Research → Plan → Approve → Implement → Debug/Refactor → Verify → Commit**. The PR is where **Verify** is enforced and the **Commit** to `main` happens.

```bash
# 1. Start from fresh main
git checkout main
git pull --rebase origin main
git checkout -b feat/grounding-breathing

# 2. Work in small commits (Conventional Commits)
git add -A
git commit -m "feat(grounding): add paced-breathing timer use case + tests"
# ... more commits ...

# 3. Keep the branch current (rebase onto main; linear history)
git fetch origin
git rebase origin/main          # resolve any conflicts, then:
# git rebase --continue

# 4. Push
git push -u origin feat/grounding-breathing
```

### Commit message style (Conventional Commits — pairs with ECC + clean history)
```
feat(scope): …      fix(scope): …      refactor(scope): …
test(scope): …      docs(scope): …     chore(scope): …
```
e.g. `feat(safety): region-aware crisis resolver with offline fallback`

---

## 3. Open the PR (use `gh` — both have it)

```bash
gh pr create \
  --base main \
  --head feat/grounding-breathing \
  --title "feat(grounding): paced-breathing timer" \
  --body "Implements docs/06 Flow A breathing tool.

## What / why
- …
## Spec
- docs/06 §3 (Flow A)
## Definition of Done (docs/13 §4)
- [x] builds Android+shared; iOS configures
- [x] tests added; gates green (copy-lint per language, a11y, RTL)
- [x] non-negotiables checked in code + copy
- [x] STATUS.md updated
"
```

PR rules:
- **Small and single-purpose.** One feature. A giant PR is itself a reject reason (`docs/13` §5).
- The PR body **must** state which `docs/` spec it implements and tick the DoD.
- Link the issue if you track issues (`gh issue`).

---

## 4. Review (rigorous, no self-merge — `docs/13` §5)

The **other** developer reviews. Approval means "I verified this," not "looks fine."

```bash
# Reviewer pulls and actually runs it
gh pr checkout 42
./gradlew build            # build Android + shared
./gradlew test             # run tests
# exercise the changed flow; eyeball copy for non-negotiables; confirm gates
```

- Reviewer **requests changes** for any failing/missing test, any non-negotiable violation (code *or* copy), a hardcoded string, an unapproved contract change, or missing DoD items.
- **No self-approval, no self-merge — including the lead.** A's PRs need B's approval; B's need A's.
- Safety/AI/consent/crisis code gets the slower second pass.

---

## 5. Merge to `main` (squash; green + approved only)

Merge only when **CI gates are green** *and* the reviewer approved.

```bash
# Squash-merge → one clean commit on main per feature; delete the branch
gh pr merge 42 --squash --delete-branch
```

- **Squash merge** keeps `main` history one-clean-commit-per-feature.
- After merge, both devs refresh:
  ```bash
  git checkout main && git pull --rebase origin main
  ```
- The lead holds final merge authority to `main`, but the **review gate applies to everyone**.

---

## 6. Branch protection on `main` (set once, on the host — GitHub)

Configure on the remote so the rules are enforced, not just agreed:
- Require a pull request before merging; **require 1 approving review**; **dismiss stale approvals** on new commits.
- **Require status checks to pass** (CI: build, tests, copy-lint, crisis-freshness, NEDA-deny, RTL/a11y).
- **Require branches up to date** before merge.
- **No direct pushes**; **no force-push** to `main`; (optionally) include administrators so the lead is held to the same bar — matches `docs/13` "no edge given."

```bash
# (optional) inspect / verify protection via gh api
gh api repos/:owner/:repo/branches/main/protection
```

---

## 7. Conflicts & keeping in sync

- Rebase your feature branch onto `main` **before** opening/finishing the PR (§2 step 3) so the merge is clean.
- Conflict during rebase: fix the files, `git add`, `git rebase --continue`. If it gets messy, `git rebase --abort` and re-approach in smaller steps.
- Because branches are short-lived and PRs small, conflicts stay rare. The main cross-platform conflict source (line endings) is killed by `.gitattributes` (§0.1).
- **Never force-push a branch someone else is reviewing/using** without telling them. Force-push is fine on your *own* un-shared feature branch after a rebase: `git push --force-with-lease`.

---

## 8. Releases

When a phase's work is on `main`, green, and (for launch) advisor-signed:
```bash
git checkout main && git pull --rebase origin main
git tag -a v0.1.0 -m "Aspen v0.1.0 — Phase 2 spine + Phase 3 features"
git push origin v0.1.0
gh release create v0.1.0 --title "Aspen v0.1.0" --notes "…"
```
Tag per meaningful milestone; the tag is a reproducible point (gates were green at that commit).

---

## 9. Daily cheat-sheet

```bash
# start work
git checkout main && git pull --rebase origin main && git checkout -b feat/x

# save work
git add -A && git commit -m "feat(x): …"

# sync with main
git fetch origin && git rebase origin/main

# publish + PR
git push -u origin feat/x
gh pr create --base main --title "feat(x): …" --body "…"

# review someone's PR
gh pr checkout <n> && ./gradlew build test   # then approve or request changes on the host

# merge (after green + approval)
gh pr merge <n> --squash --delete-branch
```

> The PR is the gate. A branch merges to `main` only when it's complete, green, and approved by the other dev — no exceptions, no edge given (`docs/13` §5).
