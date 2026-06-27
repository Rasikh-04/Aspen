# 08 — Identity, Account, Linkage & Consent Architecture

> Account/identity model, the data-key trade-off, and the clinician-linkage roadmap. Written now — even though most of it ships later — because you asked to plan it so future features slot in **without breaking earlier ones**. The throughline is a single **scoped, revocable consent model** that everything else hangs off.

---

## 1. Identity model

**Anonymous by default (confirmed).** Core features — questionnaire, grounding, reflection, logging, safety, in-app companion, Android overlay — work with **no account, no email, nothing.** This is a hard rule (`03` FR-9); the most vulnerable user must be able to use Aspen leaving no trace.

**Optional account, app-native, not federated-dependent.** If a user wants their data backed up / recoverable / synced across devices (or later, linked to a clinician), they can create an account that is **entirely Aspen's own** — a unique identity in our database, **not dependent on Google/Apple identity**.

**Login methods (user's choice, layerable):**
- **Email** — optional, used for recoverable credentials / login.
- **"Continue with Google" (or Apple)** — a *convenience* that creates a first-class Aspen account (no password needed to start). It is **not** a dependency: the account exists in our DB on its own.
- **Add-a-password-later / change method** — even after a Google login, the user can **set a password** (or switch methods) to harden their data. Auth methods are *attached to* the Aspen account, not the identity of the account.

**Design rule:** the Aspen account is the root; external providers are just *one way to authenticate into it*. This means a user is never locked out because they lost a Google account, and we never depend on a third party to know who our user is. It also means we can add/remove auth methods over time without migrating identities.

---

## 2. Data, encryption & the key model (decided: true E2E)

Two zones:

| Zone | What's in it | Key model | Property |
|---|---|---|---|
| **Local (default)** | reflections, food/behaviour logs, profile, AI history | **device/user-held keys** (Keystore/Keychain; optional biometric/PIN) | Truly private; we can't read it. |
| **Cloud (opt-in: sync / backup / export-import)** | encrypted blobs only | **true end-to-end** — key derived from the user's password/passphrase, **never sent to the server** | Server stores ciphertext it **cannot decrypt**. Even a full server breach exposes no readable content. |

**Decision (confirmed): true E2E for maximum privacy and security.** The decrypt key is derived on-device from the user's credential and **never leaves the device**. The cloud holds only **ciphertext**. This is the strongest posture and the right one for the most sensitive thing a person can write down.

### The export/import + "cloud verification" pattern (how to keep E2E intact)

You said exports/imports require cloud verification. The critical rule: **the server verifies *authorization*, never *plaintext*.** Concretely:
- **What the cloud does:** authenticates the account (who you are), confirms you're allowed to pull *your* encrypted blob, issues/validates the transfer, rate-limits, audits. It handles **identity and access control** — not content.
- **What the cloud never does:** see plaintext, hold the key, or decrypt. The blob is E2E-encrypted in transit and at rest; **only the device decrypts**, using the user's key.
- **Import** = pull the ciphertext blob after auth, decrypt locally with the user's key. **Export** = encrypt locally, hand the user (or push to cloud) a ciphertext blob; verification gates the *transfer*, not the *reading*.

This gives you cloud-verified, account-bound export/import **and** true E2E — they're compatible as long as verification stays at the auth layer.

### The honest trade-off you're accepting (design for it deliberately)

True E2E means **if the user loses their key/passphrase with no backup, their cloud data is genuinely unrecoverable** — by design, because we *can't* recover what we can't decrypt. That's the cost of maximum privacy. So you must design a **deliberate key-backup/recovery UX**, e.g.:
- A **recovery passphrase/code** shown once at setup ("write this down; it's the only way to recover — we can't"), optionally stored in the OS keychain / user's own password manager.
- Optional **device-to-device key transfer** for multi-device, without the server ever seeing the key.
- Clear, repeated, honest messaging that recovery depends on the user keeping their key — never imply we can reset it.

**[DECISION NEEDED]** Confirm the recovery mechanism (recovery code vs. device-transfer vs. both). This is the one real UX consequence of choosing E2E, and it's better decided now than after someone loses data.

---

## 3. The consent model (the spine — build this early, even if linkage is later)

Everything in §4 (clinician linkage) and any future sharing runs through **one** mechanism, so you never have to retrofit consent later:

**A `consent_grant` is:**
- **Scoped** — to specific *data categories* (e.g. "reflections," "food/behaviour logs," "safety events," "profile") — granular, not all-or-nothing.
- **Directed** — to a specific *recipient* (a linked clinician, an affiliated doctor, a trusted person).
- **Time-boxed** (optional) — can auto-expire.
- **Revocable instantly** — revoke cuts access immediately; revocation is one tap and always available.
- **Auditable** — the user can always see *who can see what, granted when*, and a history.
- **Default deny** — nothing is shared unless an explicit grant exists.

> Building this consent primitive in **Phase 2-ish (alongside safety)**, even with no recipients yet, is the single best thing you can do for "won't have to break something later." Every later sharing feature becomes "issue a grant," not "design sharing from scratch."

---

## 4. Clinician linkage roadmap (your 3 tiers) — scoped honestly

You described three tiers. They differ **enormously** in operational/legal weight. Here's the honest scoping, ordered easiest→heaviest, all gated on §3 consent.

### Tier 1 — Directory of nearby / relevant professionals *(lightest; earliest)*
- A curated, region-aware **directory** the user can browse to *find* help (like Rise Up + Recover's ConnectED). No data sharing — just discovery.
- **Weight:** low. It's content + search. Main cost is *curating and verifying* the directory per region (the non-technical teammate's job, with advisor input).
- **When:** can come relatively early, after the core single-user app is solid.

### Tier 3 — Link to the user's *own* existing clinician *(medium; do before Tier 2)*
- User links a clinician/dietitian/support person **they already have**, and grants scoped, revocable access to chosen data (e.g. reflections + logs) — *or* simply shares an encrypted **export**.
- **Weight:** medium. This is sharing *the user's own data with someone the user already trusts*, on the user's initiative. No clinical service liability for Aspen — Aspen is a conduit, not the provider. The §3 consent model does the heavy lifting.
- **When:** after consent model + accounts exist. This is the natural "connect with my team" feature.

### Tier 2 — Aspen-affiliated doctors providing *immediate* support (voice/video/messaging) *(heaviest; a separate product)*
- On-demand real-time clinical support from doctors *affiliated with Aspen*.
- **Weight: very high — this is effectively launching a telehealth service.** Be clear-eyed:
  - **Clinical governance:** credentialing/vetting clinicians, scope-of-practice, supervision, safeguarding protocols, what happens in an emergency on a call.
  - **Legal/regulatory:** real-time clinical advice is often a *regulated health service*, jurisdiction by jurisdiction (PK/DE/UK/US each differ). Likely needs legal counsel, possibly licensing, liability insurance, and per-country compliance.
  - **Health-data law:** live clinical interactions raise the data-protection bar sharply (GDPR special-category data in DE/UK; etc.).
  - **Safety:** real-time crisis on a video call is a serious responsibility with its own protocols.
  - **Ops:** scheduling, availability, identity verification, payment/sustainability of paying clinicians on a free app.
- **When:** a **distinct future phase with its own approval, legal review, and clinical-governance design** — not a feature bolted onto v1. *Plan the data/consent model to accommodate it (done, via §3), but do not scope it into the near-term build.*

**Recommended order:** core single-user app → consent model → **Tier 1 (directory)** → **Tier 3 (link own clinician)** → *much later, separately* → **Tier 2 (affiliated telehealth)**.

---

## 5. What to build now vs. later (so nothing breaks)

| Build **now** (so later slots in) | Defer |
|---|---|
| Anonymous-first; app-native account root; layerable auth | Federated edge cases, multi-device polish |
| Local device-key encryption (default) | — |
| The **scoped/revocable consent primitive** (§3), even with zero recipients | Actual recipients |
| Data-category tagging (reflections / logs / profile / safety) so grants can be scoped | — |
| Clear key-model disclosure UX | E2E "max privacy" mode (if you choose to offer it) |
| Directory data *schema* (region-aware) | Tier 1 content curation |
| — | Tier 3 linkage UI |
| — | **Tier 2 telehealth — entire separate phase + legal/clinical review** |

---

## 6. [DECISIONS] — status

1. **Cloud key model:** ✅ **Decided — true E2E** (server stores ciphertext it can't decrypt; verification is auth-only). One open sub-decision: **recovery mechanism** (recovery code vs. device-to-device transfer vs. both) — §2.
2. **Tier 2 (affiliated telehealth):** ✅ **Decided — explicitly-later, separately-governed phase**, started only after confirmation from your USA healthcare contacts.
3. **Directory (Tier 1) regions at launch:** same as crisis regions (PK/DE/UK, US later) — assumed yes; flag if not.
