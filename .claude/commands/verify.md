---
description: Verify phase — run all gates before anything is called done (Aspen pipeline step 5)
---

You are in the **VERIFY** phase for: $ARGUMENTS

Run and report on every gate. Do not declare success until all pass; if one fails, fix or flag, don't paper over it.

1. **Build** — both Android and iOS targets compile.
2. **Tests** — full suite green. Report coverage on any `:shared:domain/safety` or `:shared:domain/consent` code touched; untested branches there are release-blocking.
3. **Copy-lint** — no forbidden tokens (numbers about food/body, shame words, appearance words) in string resources. (See `CLAUDE.md` non-negotiables 1, 2, 5.)
4. **Crisis-freshness gate** — no supported-locale crisis resource is unverified/stale; "NEDA" appears nowhere. (Non-negotiable 7.)
5. **Non-negotiables review** — re-read the `CLAUDE.md` list and confirm the change violates none, in code *and* user-facing copy.
6. **A11y / reduced-motion** — not regressed.

**Output:** a checklist with pass/fail per gate and evidence (command output, test counts). End with an explicit "VERIFIED — ready for human sign-off" or "NOT VERIFIED — blockers: …". The human owns the final "done."
