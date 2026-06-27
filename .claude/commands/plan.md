---
description: Plan phase — propose a change against the spec, then STOP for human approval (Aspen pipeline step 2)
---

You are in the **PLAN** phase for: $ARGUMENTS

Assume research is done (run `/research` first if not). Produce a plan — not code.

The plan must include:
1. **Goal** — one or two sentences, tied to the relevant `docs/` spec section.
2. **Non-negotiables touched** — list which of the `CLAUDE.md` hard constraints this work must honour, and how.
3. **Files** — what you'll create/modify, by module (`:shared:...`, `:androidApp`, etc.).
4. **Tests first** — the tests that define "correct," especially for safety/consent/logic. State the coverage expectation.
5. **Definition of Done** — concrete, checkable. Include the verification gates from `CLAUDE.md`.
6. **Risks / decisions needed** — anything for the human or advisors.

Then **STOP**. Print: *"Awaiting approval before implementing."* Do not write code until the human approves. This gate is real.
