# 05 — Companion Design (On-Screen Overlay)

> The animated companion that can live on the phone screen — a "desktop pet for the phone." This is Aspen's most distinctive feature and also the one with the most real risk, so this spec covers the interaction model, the overlay architecture, **and** the guardrails that keep it from becoming intrusive or a dependency.

---

## 1. The idea, and where it comes from

You described a small character (animal/cartoon) that:
- appears from the side of the screen and offers presence;
- retreats when the user is doing something else (screen "interrupted");
- can be tapped to come forth and play (hide behind icons, be dragged around);
- can be summoned by an accessibility-style floating button;
- is configurable: home-screen-only, or system-wide overlay.

This is the **desktop-mascot / "Shimeji"** lineage — characters that wander the screen, can be picked up and dragged, climb/hide around UI, and pause when a fullscreen app is in use. It existed on old desktops and in browser extensions, and there are shipping **Android** implementations using the system overlay permission, with drag, tap-to-interact, "pass-through" modes, and on-demand sprite downloads [Shimeji apps]. So the *mechanism* is proven. Aspen's contribution is **purpose**: turning a novelty pattern into **low-pressure, opt-in companionship for the between-session gap** — and doing it safely.

---

## 2. Why a companion, for *this* population specifically

- **Loneliness is the load-bearing problem at 9:40pm** (`01` §7). A small living presence on the screen is a form of *ambient, non-demanding company* — it asks nothing, tracks nothing, judges nothing.
- It's **the opposite of a feed.** Where social apps pull the user into comparison, the companion just *is there*, like a pet. No content, no scroll, no others.
- It can be a **gentle on-ramp** to the helpful flows ("tap me if it's hard"), without nagging.

But — and this is the spec's spine — *a presence that's always there can also become something the user leans on instead of people, or an intrusion that adds anxiety.* So every design choice below is filtered through: **does this keep the companion supportive rather than obstructive?** (the same continuum the Recovery Record research surfaced, `01` §5).

---

## 3. Hard guardrails (these define the feature as much as the fun does)

1. **Off by default. Fully optional. One-tap dismiss, anytime, anywhere.** The user can banish it instantly and it stays gone until summoned.
2. **It never nags.** No guilt, no "you haven't talked to me," no streaks, no "I'm sad you left." (`03` SR-4). It does not punish absence.
3. **No appearance talk, no food talk, no numbers.** Same non-negotiables as the rest of the app (`01` §6). The companion never comments on the user's body, eating, or "progress."
4. **It is not the AI therapist and not a crisis responder.** If a user in distress engages it, it offers warmth and a **one-tap route to the real safety flow** (`06`) and to a human — it does not try to "handle" a crisis itself.
5. **Presence, not pressure.** Default behaviour is *ambient and quiet*. Proactive check-ins are (a) off by default, (b) rare, (c) gentle, (d) never notification-spam, and (e) fully configurable/disable-able.
6. **Reduced-motion / calm mode disables it** or reduces it to a still, slow presence (`03` NFR-a11y, SR-6). Some users find motion activating; respect that.
7. **No data harvested via the overlay.** It cannot and does not read screen content (see §6 on *why we avoid AccessibilityService*).
8. **Scope is the user's choice and clearly explained:** home-screen-only vs system-wide overlay, with a plain-language explanation of what the overlay permission means.

> Design test, every behaviour: *"Is this a kind, optional presence — or is it engagement-bait wearing a cute costume?"* If the latter, cut it.

---

## 4. Interaction model

### States
- **Hidden** — not on screen. Summon via the in-app toggle or the optional floating "summon" button.
- **Ambient/idle** — sits or rests at a screen edge; tiny, slow, low-fps micro-animations (breathing, blinking). Costs almost nothing (`04` §6).
- **Playful** — after a tap: walks, hides behind/peeks around app icons, can be **dragged** by the user and dropped; small idle games. Time-boxed; returns to ambient on its own.
- **Gentle presence (distress)** — if the user opens Aspen's hard-moment flow, the companion can appear alongside as calm company, and offers the route to grounding/safety. Never blocks the safety exit.
- **Suspended** — when a fullscreen/immersive app is foreground (video, game, camera), the companion **fully pauses and hides** (battery + non-intrusion, `04` §6).

### Summoning
- **In-app toggle** (Settings / Home).
- **Optional floating summon button** (an accessibility-style bubble) — itself optional and movable; off by default.
- **Configurable home:** "only on my home screen / launcher" vs "across apps (system overlay)."

### Gestures
- **Tap** → playful state / gentle prompt.
- **Long-press / drag** → pick up and move; drop anywhere; it settles.
- **Fling to edge / swipe away** → dismiss to hidden.
- **Double-tap (configurable)** → open Aspen, or open the hard-moment flow directly.

### "Common companion questions" (your note), done safely
Desktop pets often have little canned interactions. Aspen's are **emotionally supportive and non-prying**, e.g.:
- "Want to sit together for a minute?" → starts a breathing/grounding micro-session.
- "Rough moment? I can get you to your calm tools or to someone." → routes to grounding / safety.
- "Just here if you want company." → returns to ambient, no follow-up.

What they are **never**: "What did you eat?", "How are you doing on your goals?", "You've been away a while," anything about the body, or anything that demands an answer.

---

## 5. Companion character & art direction (brief; full visual tokens in `06`)

- **Species/form:** a soft, ambiguous, friendly creature (not human, not a body) — avoids any body-image projection. Round, warm, simple. (Final character TBD with user testing.)
- **No gendered/idealised body.** Deliberately a little blob-ish/animal-ish.
- **Palette:** Aspen's calm sage/sand tones (`06`), not bright/urgent.
- **Motion:** slow, breathing, gentle. Nothing twitchy or attention-grabbing.
- **Expressiveness:** warm and present, never sad-to-manipulate ("don't leave me" faces are banned — that's guilt, SR-4).
- **Sprite packs** downloaded on demand and cached (`04` §6) so the base app stays lean and users can later choose a companion they bond with.

---

## 6. Overlay architecture (Android)

### Mechanism
- **`SYSTEM_ALERT_WINDOW`** ("Display over other apps") permission → a **foreground overlay service** that adds a view to the `WindowManager`. This is the same, proven mechanism the Shimeji-class apps use [Shimeji; flutter_floatwing pattern]. Implemented natively per ADR-001.
- The overlay view renders the sprite (Compose on a `SurfaceView`, dropping to Canvas/OpenGL only if profiling demands — `04` ADR-002), handles drag via touch + `WindowManager.updateViewLayout`, and runs animation off the frame callback **only while visible**.

### Fullscreen detection & suspend
- Use **non-invasive** signals (window focus changes, immersive-mode cues, foreground-app category where permitted) to detect fullscreen/immersive foreground and **suspend** (`04` §6). Prefer signals that *don't* require reading content.

### A deliberate stance: **avoid `AccessibilityService` if at all possible**
- Shimeji-style "interact with on-screen things" can be implemented via `AccessibilityService`, and one Shimeji reviewer even *asked* for it. **Aspen should not.** AccessibilityService can read screen content and simulate input — for an app trusted by a vulnerable population with sensitive data, requesting it is a privacy red flag, invites Play Store policy scrutiny, and is a security liability.
- The companion's value (presence, drag, peek-around) is achievable with **overlay-only** capabilities. "Hide behind icons" is faked with z-order/positioning illusions, not by reading the launcher. **If a behaviour requires AccessibilityService, we drop the behaviour, not raise the permission.** (Guardrail §3.7.)

### Permissions UX
- Explain *before* requesting: plain language on what "display over other apps" does and doesn't allow, and that Aspen can't see other apps' content.
- App must **work fully without** the overlay (companion simply won't appear); never gate core support features behind the overlay permission.

### Lifecycle & resilience
- Overlay is a foreground service with a quiet, honest persistent notification (required by Android) — worded calmly, never alarmy.
- Graceful degradation if the OEM kills the service (`04` §6 OEM reality): the companion just disappears; nothing else breaks; re-summon brings it back.

---

## 7. Risks specific to the companion (named honestly)

| Risk | Mitigation |
|---|---|
| Becomes a **dependency** that substitutes for human contact | Never nags; actively routes toward people; presence is ambient not demanding; (consider, post-testing) gentle periodic nudges toward real-world support. |
| Becomes an **intrusion** that adds anxiety | Off by default; one-tap dismiss; suspends on fullscreen; reduced-motion disables; rare/quiet proactive behaviour. |
| **Battery/performance** complaints (a top Shimeji-app gripe) | Render-only-when-visible, suspend-on-fullscreen, frame-throttle, single instance (`04` §6); profile against budgets. |
| **Permission scares** users / trips Play policy | Overlay-only (no AccessibilityService); pre-request explanation; works without it. |
| **Triggering motion/expression** | Calm art direction; no guilt faces; reduced-motion path. |
| Companion engaged **during a crisis** | One-tap to safety flow + human; never attempts to handle it. |

---

## 8. Validation requirement (do not skip)

The companion's safety is an **empirical** question, not a design assertion. Before wide release it must be tested **with people in recovery** (via an ED charity/advisory relationship), specifically probing: does it feel like kind company, or like surveillance/pressure? Does any user feel worse when it appears? Findings gate the feature. If a meaningful subset finds it obstructive, it ships **more conservative** (e.g. summon-only, no proactive behaviour) — or not at all.

---

## 9. Sources

- Shimeji desktop-mascot lineage and Android implementations (overlay via `SYSTEM_ALERT_WINDOW`; drag/tap; pass-through/ceiling modes; hide when fullscreen app in use; on-demand model download; lightweight; common battery/typing-interference complaints; one user requesting AccessibilityService):
  - Shimeji Browser Extension. https://shimejis.xyz/
  - Shimeji: Screen Buddies (Google Play). https://play.google.com/store/apps/details?id=com.digitalcosmos.shimeji
  - Shimeji – desktop pet (Google Play). https://play.google.com/store/apps/details?id=com.anbu.shimeji.desktoppet
  - Shimeji (Softonic/download listings, feature + footprint notes).
- `flutter_floatwing` plugin (illustrates the overlay = `SYSTEM_ALERT_WINDOW` + WindowManager service architecture). https://flutterawesome.com/a-flutter-plugin-that-makes-it-easier-to-make-floating-overlay-window-for-android/
- Recovery Record patient-experience study (supportive↔obstructive continuum; surveillance discomfort) — PMC6035344 (applied here to companion intrusion risk).
