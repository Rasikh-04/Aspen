# Aspen — advisor-review & translation master catalog

> **Generated** by `l10n/tools/l10n_review.py generate` — do not hand-edit; edit the source strings or the worksheets and re-run. Last generated 2026-07-05.


Every user-facing string in Aspen that needs advisor sign-off and/or translation. See [README](README.md) for the workflow.


## Sensitivity tiers

| Tier | Meaning |
|---|---|
| **SENSITIVE** | ED-informed **native-speaker** review mandatory; machine translation not acceptable; cannot ship in a language until `APPROVED` (docs/12 §3/§5). |
| STANDARD | Native translation + standard review. |
| DEV_ONLY | Debug builds only, never shipped; translation not required. |
| DO_NOT_TRANSLATE | Brand / proper noun; identical every language. |

## Per-language UI coverage

| Language | Approved | Pending | Other | Translatable total |
|---|--:|--:|--:|--:|
| English (`en`) | 0 | 244 | 0 | 244 |
| Urdu (`ur`) | 0 | 242 | 2 | 244 |
| German (`de`) | 0 | 244 | 0 | 244 |
| Mandarin Chinese (`zh`) | 0 | 244 | 0 | 244 |
| Hindi (`hi`) | 0 | 244 | 0 | 244 |
| Arabic (`ar`) | 0 | 244 | 0 | 244 |
| Spanish (`es`) | 0 | 244 | 0 | 244 |

## UI strings by surface


### Home  ·  STANDARD  ·  4 keys

| key | English source |
|---|---|
| `home_greeting` | A quiet place to be. |
| `home_subtitle` | Whatever today holds, you can be here for a moment. |
| `home_hard_moment` | I am having a hard moment |
| `home_reach_person` | Reach a person |

### Navigation  ·  STANDARD  ·  4 keys

| key | English source |
|---|---|
| `nav_home` | Home |
| `nav_reflect` | Reflect |
| `nav_calm` | Calm |
| `nav_settings` | Settings |

### Common  ·  STANDARD  ·  1 keys

| key | English source |
|---|---|
| `back` | Back |

### Onboarding questionnaire  ·  🔴 SENSITIVE  ·  51 keys

| key | English source |
|---|---|
| `onb_intro_title` | A few gentle questions |
| `onb_intro_body` | So Aspen can fit you a little better. There are no right answers, nothing here is a test or a diagnosis, and you can skip anything. Take your time. |
| `onb_begin` | Begin |
| `onb_skip_all` | Skip these for now |
| `onb_skip_question` | Skip this one |
| `onb_next` | Continue |
| `onb_back` | Back |
| `onb_prefer_not` | I'd rather not say |
| `onb_q1_title` | What would feel most helpful from Aspen right now? |
| `onb_q1_calmer` | A calmer moment in hard times |
| `onb_q1_private` | Somewhere private to put my thoughts |
| `onb_q1_company` | Company that doesn't ask much of me |
| `onb_q1_reach_help` | A way to reach real help |
| `onb_q1_not_sure` | I'm not sure yet |
| `onb_q2_title` | How would you describe your relationship with eating lately? |
| `onb_q2_tense_rules` | Tense, or full of rules |
| `onb_q2_out_of_control` | Out of my control at times |
| `onb_q2_undo_after` | Something I try to undo afterwards |
| `onb_q2_sensory_hard` | Hard because of how foods feel, taste, or seem |
| `onb_q2_body_focused` | Mostly about how I feel in my body |
| `onb_q2_varies` | It varies |
| `onb_likert_not_really` | Not really |
| `onb_likert_sometimes` | Sometimes |
| `onb_likert_often` | Often |
| `onb_q3_title` | Do you find yourself holding back from eating, even when part of you wants or needs to? |
| `onb_q4_title` | Do you ever feel that once you start eating, it's hard to feel in control? |
| `onb_q5_title` | After eating, do you often feel a strong urge to make up for it somehow? |
| `onb_q6_title` | Is eating hard mainly because of how foods feel, taste, or smell, or worry about what eating might do, rather than worry about your body or shape? |
| `onb_q6_yes` | Yes |
| `onb_q6_somewhat` | Somewhat |
| `onb_q6_no` | No |
| `onb_q7_title` | How much do thoughts about your body or shape affect your day? |
| `onb_q7_not_much` | Not much |
| `onb_q7_sometimes` | Sometimes |
| `onb_q7_a_lot` | A lot |
| `onb_q8_title` | How much is this getting in the way of your everyday life right now? |
| `onb_q8_a_little` | A little |
| `onb_q8_some` | Some |
| `onb_q8_a_lot` | A lot |
| `onb_q9_title` | Do you have anyone, a professional or someone you trust, supporting you with this? |
| `onb_q9_professional` | Yes, a professional |
| `onb_q9_trusted` | Yes, someone I trust |
| `onb_q9_none` | Not right now |
| `onb_q10_title` | On the hardest days, would it help to have a quick way to reach a person? |
| `onb_q10_set_up` | Yes, set that up |
| `onb_q10_maybe_later` | Maybe later |
| `onb_q10_no` | No |
| `onb_closing_title` | Thank you for sharing |
| `onb_closing_body` | Aspen will gently shape itself around what you've said, and you can change any of this anytime. One thing worth saying clearly: this isn't a diagnosis or an assessment. If you'd like to find real, specialist support, here's where to start. |
| `onb_closing_find_help` | Find real support |
| `onb_closing_continue` | Go to Aspen |

### Grounding tools  ·  🔴 SENSITIVE  ·  25 keys

| key | English source |
|---|---|
| `grounding_title` | A hard moment |
| `grounding_subtitle` | Here are a few quiet things that can help. There's no wrong choice. |
| `grounding_breathe` | Breathe |
| `grounding_54321` | Ground (5-4-3-2-1) |
| `grounding_ride_urge` | Ride the urge |
| `grounding_talk` | Write it down |
| `grounding_reach_someone` | Reach someone |
| `grounding_close` | Close |
| `grounding_gentle_close` | Glad you took a moment. |
| `grounding_done` | I'm okay for now |
| `breathe_title` | Let's breathe together |
| `breathe_in` | Breathe in |
| `breathe_hold` | Hold |
| `breathe_out` | Breathe out |
| `breathe_reduced_note` | Follow the words at your own pace. |
| `ground_54321_title` | Notice where you are |
| `ground_54321_intro` | Slowly, gently, take each one in. There's no rush. |
| `ground_54321_see` | Five things you can see |
| `ground_54321_hear` | Four things you can hear |
| `ground_54321_touch` | Three things you can touch |
| `ground_54321_smell` | Two things you can smell |
| `ground_54321_taste` | One thing you can taste |
| `ground_54321_next` | Next |
| `ride_urge_title` | Ride the urge |
| `ride_urge_body` | Urges rise, crest, and pass, like a wave. You don't have to act on it. Stay here a moment and let it move through. |

### Crisis / safety screen  ·  🔴 SENSITIVE  ·  14 keys

| key | English source |
|---|---|
| `safety_title` | Reaching a person |
| `safety_body` | A calm route to real human support, a trusted contact or a region-correct line, will live here. |
| `safety_intro` | You deserve real support. These are calm ways to reach a person. |
| `safety_region_label` | Show help for |
| `safety_region_pk` | Pakistan |
| `safety_region_de` | Germany |
| `safety_region_uk` | United Kingdom |
| `safety_region_intl` | International |
| `safety_trusted_person` | Reach your trusted person |
| `safety_heading_acute` | If you need someone right now |
| `safety_heading_support` | Eating-disorder support |
| `safety_heading_finder` | Find ongoing treatment |
| `safety_fallback_note` | We don't have verified local services for this region yet. These trusted options can help you find support near you. |
| `safety_unverified` | Details are being verified |

### Reflection & logging  ·  STANDARD  ·  15 keys

| key | English source |
|---|---|
| `reflect_title` | Your space |
| `reflect_subtitle` | A private place for your thoughts. Only you can see this. |
| `reflect_empty` | Nothing here yet, and that's completely okay. This space is here whenever you'd like it. |
| `reflect_new_reflection` | Write something down |
| `reflect_new_feeling_log` | Note how you're feeling |
| `reflect_new_food_log` | Note a moment around eating |
| `reflect_section_reflections` | Reflections |
| `reflect_section_feelings` | Feelings |
| `reflect_section_food` | Around eating |
| `reflect_save` | Save |
| `reflect_cancel` | Cancel |
| `reflect_delete` | Delete |
| `reflect_text_hint` | Whatever's on your mind… |
| `reflect_note_hint` | A few words, if you'd like… |
| `reflect_feelings_label` | If it helps, what are you feeling? |

### Feeling tags  ·  STANDARD  ·  12 keys

| key | English source |
|---|---|
| `feeling_calm` | Calm |
| `feeling_content` | Content |
| `feeling_relieved` | Relieved |
| `feeling_hopeful` | Hopeful |
| `feeling_tired` | Tired |
| `feeling_numb` | Numb |
| `feeling_anxious` | Anxious |
| `feeling_overwhelmed` | Overwhelmed |
| `feeling_sad` | Sad |
| `feeling_frustrated` | Frustrated |
| `feeling_guilty` | Guilty |
| `feeling_alone` | Alone |

### Companion messages  ·  🔴 SENSITIVE  ·  19 keys

| key | English source |
|---|---|
| `companion_greeting_here` | Hi. Just here with you. |
| `companion_greeting_company` | Want some company for a bit? |
| `companion_greeting_soft` | It's good that you're here. |
| `companion_hard_sit_together` | Want to sit together for a minute? |
| `companion_hard_no_fixing` | I'm not going anywhere. We can just be here. |
| `companion_hard_reach_someone` | Rough moment? I can get you to your calm tools, or to someone. |
| `companion_hard_passing` | This feeling is like weather. It moves. I'll stay while it does. |
| `companion_ground_breathe` | Want to breathe with me for a minute? |
| `companion_ground_senses` | Let's notice the room together, slowly. |
| `companion_ground_write` | Want to put it into words? I'll hold onto them. |
| `companion_ambient_here_if_needed` | Just here if you want company. |
| `companion_ambient_no_followup` | I'll be nearby. |
| `companion_notify_gentle_checkin` | Just a hello. Nothing needed. |
| `companion_notify_tools_nearby` | Your quiet tools are right here if today needs them. |
| `companion_species_aspen` | Aspen sprite |
| `companion_species_cat` | Cat |
| `companion_species_bunny` | Bunny |
| `companion_a11y_label` | Your companion. Tap to play together. |
| `companion_rest_action` | Rest now |

### AI reflection companion  ·  🔴 SENSITIVE  ·  7 keys

| key | English source |
|---|---|
| `reflect_companion_title` | Reflect together |
| `reflect_companion_hint` | If you want, tell me how it felt… |
| `reflect_companion_send` | Share |
| `reflect_companion_thinking` | … |
| `reflect_companion_unavailable` | Not available right now. Your notebook still works, always. |
| `reflect_companion_handoff` | That sounds like a lot to carry. You deserve real support right now, from a real person. |
| `reflect_companion_handoff_button` | Reach someone now |

### AI safety fallback  ·  🔴 SENSITIVE  ·  1 keys

| key | English source |
|---|---|
| `safety_ai_fallback` | I can't respond the way I'd want to here. What you're feeling matters, and a real person can be there for it. |

### AI consent (deeper reflection)  ·  🔴 SENSITIVE  ·  8 keys

| key | English source |
|---|---|
| `settings_ai_title` | Deeper reflection |
| `settings_ai_subtitle_off` | Off. Your words never leave this device. |
| `settings_ai_subtitle_on` | On. You can turn this off anytime. |
| `settings_ai_dialog_title` | Turn on deeper reflection? |
| `settings_ai_dialog_body` | Turning this on means what you write in the reflection space is sent securely to an AI service to give better responses. You can turn it off anytime, and everything it holds can be deleted. |
| `settings_ai_dialog_confirm` | Turn it on |
| `settings_ai_dialog_cancel` | Not now |
| `settings_ai_local_note` | Everything else in Aspen works fully on this device, with no AI service involved. |

### Companion presence settings  ·  STANDARD  ·  4 keys

| key | English source |
|---|---|
| `settings_companion_title` | A small companion |
| `settings_companion_subtitle_off` | Off. A tiny pixel friend can sit quietly in the corner, only if you want. |
| `settings_companion_subtitle_on` | Keeping you company. Tap to let it rest. |
| `settings_companion_species_label` | Who keeps you company |

### Overlay settings  ·  STANDARD  ·  7 keys

| key | English source |
|---|---|
| `settings_overlay_title` | Across your screen |
| `settings_overlay_subtitle_off` | The companion stays inside Aspen. |
| `settings_overlay_subtitle_on` | The companion can sit at the edge of your screen, outside Aspen too. |
| `settings_overlay_dialog_title` | Let it sit outside Aspen? |
| `settings_overlay_dialog_body` | Android will ask you to allow Aspen to “display over other apps”. That only lets the companion sit on your screen — Aspen cannot see, read, or record anything you do there. It hides on its own during videos and games, and you can let it rest anytime. |
| `settings_overlay_dialog_confirm` | Okay, continue |
| `settings_overlay_dialog_cancel` | Not now |

### Overlay notification  ·  STANDARD  ·  4 keys

| key | English source |
|---|---|
| `overlay_channel_name` | Companion |
| `overlay_channel_description` | Keeps the little companion on your screen. Quiet by design. |
| `overlay_notification_title` | Your companion is nearby |
| `overlay_notification_body` | It sits quietly at the edge of the screen. You can let it rest anytime from Aspen. |

### Notifications  ·  STANDARD  ·  5 keys

| key | English source |
|---|---|
| `settings_notify_title` | A rare hello |
| `settings_notify_subtitle_off` | Off. Aspen stays completely silent. |
| `settings_notify_subtitle_on` | Once in a while, a gentle hello. Nothing is ever asked of you. |
| `notify_channel_name` | Gentle check-ins |
| `notify_channel_description` | A quiet hello from your companion, once in a while. |

### Account  ·  STANDARD  ·  24 keys

| key | English source |
|---|---|
| `settings_account_title` | An account (optional) |
| `settings_account_subtitle_off` | Everything works without one. An account only adds the choice to back up later. |
| `settings_account_subtitle_on` | Signed in. Your writing still lives on this device. |
| `account_mode_create` | Create |
| `account_mode_signin` | Sign in |
| `account_email_optional` | Email (optional, helps you recover sign-in) |
| `account_identifier` | Email or account ID |
| `account_password` | Password |
| `account_submit_create` | Create account |
| `account_submit_signin` | Sign in |
| `account_note_local` | Your writing stays on this device either way. |
| `account_id_caption` | Account ID (your way back in if no email is attached): %1$s |
| `account_error_denied` | That didn't match. Take your time and try again whenever. |
| `account_error_email_taken` | That email already has an account. You can sign in instead. |
| `account_error_weak_password` | A longer password is needed here. |
| `account_error_unavailable` | Aspen couldn't reach the server just now. Everything on this device is unaffected. |
| `account_signout` | Sign out |
| `account_signout_subtitle` | Ends the session on this device. Your writing stays. |
| `account_delete` | Delete this account |
| `account_delete_subtitle` | Removes the account and anything backed up. Writing on this device stays. |
| `account_delete_dialog_title` | Delete this account? |
| `account_delete_dialog_body` | The account and anything backed up to it will be permanently removed. Everything written on this device stays here, untouched. |
| `account_delete_confirm` | Delete the account |
| `account_delete_cancel` | Keep it |

### Backup  ·  STANDARD  ·  26 keys

| key | English source |
|---|---|
| `backup_title` | Back up (optional) |
| `backup_subtitle_off` | Keep an encrypted copy with your account. It's locked before it leaves your phone. |
| `backup_subtitle_on` | On. Backed up only when you choose. |
| `backup_passphrase` | A passphrase for your backup |
| `backup_enable` | Turn on and back up |
| `backup_note_keymodel` | Your backup is locked with this passphrase before it leaves your phone. We store only what we cannot read, so we can never unlock it for you. |
| `backup_error_weak` | A longer passphrase is needed here. |
| `backup_error_unavailable` | Aspen couldn't reach the server just now. Nothing has changed. |
| `backup_code_title` | Your recovery code |
| `backup_code_body` | If the passphrase is ever forgotten, this code is the only other way into your backup. Keep it somewhere safe outside this phone. We cannot recover it for you — an email reset brings back your sign-in, never your backup. |
| `backup_code_confirm` | I wrote it down |
| `backup_now` | Back up now |
| `backup_now_subtitle` | Replaces your last backup with what's written here now. |
| `backup_done` | Backed up. |
| `backup_restore_title` | Restore from a backup |
| `backup_restore_hint` | Passphrase or recovery code |
| `backup_restore_action` | Restore |
| `backup_restore_done` | Restored. Your writing from the backup is here now. |
| `backup_error_wrong_secret` | That didn't open the backup. The passphrase or the recovery code — either works here. |
| `backup_error_no_backup` | There's no backup on this account yet. |
| `backup_off` | Turn off backup |
| `backup_off_subtitle` | Removes the backup from the server. Writing on this device stays. |
| `backup_off_dialog_title` | Remove the backup? |
| `backup_off_dialog_body` | The encrypted copy on the server will be deleted. Everything on this device stays as it is. |
| `backup_off_confirm` | Remove it |
| `backup_off_cancel` | Keep it |

### Language settings  ·  STANDARD  ·  4 keys

| key | English source |
|---|---|
| `settings_language_label` | Language |
| `language_system` | Match my device |
| `language_en` | English |
| `language_ur` | اردو |

### Settings  ·  STANDARD  ·  9 keys

| key | English source |
|---|---|
| `settings_title` | Settings |
| `settings_revisit_questions` | Revisit the questions |
| `settings_revisit_subtitle` | Things change. You can answer again anytime. |
| `settings_delete_all` | Delete everything I've written |
| `settings_delete_all_subtitle` | Permanently removes your reflections and notes from this device. |
| `settings_delete_confirm` | Delete permanently |
| `settings_delete_cancel` | Keep my writing |
| `settings_delete_dialog_title` | Delete everything? |
| `settings_delete_dialog_body` | This permanently removes everything you've written on this device. It can't be undone. |

### Brand  ·  DO_NOT_TRANSLATE  ·  1 keys

| key | English source |
|---|---|
| `app_name` | Aspen |

### Debug (not shipped)  ·  DEV_ONLY  ·  13 keys

| key | English source |
|---|---|
| `settings_debug_companion` | Companion preview (debug) |
| `settings_debug_companion_subtitle` | Developer-only view of the companion voice and safety guard. |
| `debug_companion_title` | Companion preview |
| `debug_companion_moment` | Moment |
| `debug_companion_tone` | Tone |
| `debug_companion_line` | Chosen line |
| `debug_guard_title` | Guard playground |
| `debug_guard_hint` | Text to run through the guard… |
| `debug_guard_run` | Check |
| `debug_guard_pass` | Guard: pass |
| `debug_guard_rewrite` | Guard: withheld and replaced |
| `debug_guard_crisis` | Crisis signals: would hand off |
| `debug_guard_no_crisis` | Crisis signals: no hand-off |

## Crisis registry (verify data + translate copy, per country)

Real phone/URL values are `TODO-VERIFY` until advisors verify them — that is a release gate (`crisisGateStrict`, docs/10 §7). Worksheets: `l10n/worksheets/crisis.<country>.csv`.

| Country | Resources | Contacts verified |
|---|--:|--:|
| DE | 5 | 0/6 |
| INTL | 2 | 0/2 |
| PK | 4 | 0/4 |
| UK | 4 | 0/6 |
| US | 4 | 0/4 |

## Safety lexicons (advisor-supplied per language)

Forbidden-token lists (numberless/anti-shame copy-lint) and crisis-sign phrases (hand-off trigger). These are **equivalents supplied per language**, not translations. Worksheets: `l10n/worksheets/safety-lexicon.<lang>.csv`.

| Language | Forbidden tokens | Crisis-sign phrases |
|---|--:|--:|
| English (`en`) | 37 | 18 |
| Urdu (`ur`) | 3 | 4 |
| German (`de`) | 14 | 6 |
| Mandarin Chinese (`zh`) | 0 | 0 |
| Hindi (`hi`) | 0 | 0 |
| Arabic (`ar`) | 0 | 0 |
| Spanish (`es`) | 0 | 0 |

## AI reflection system prompt

Revision `draft-2026-07-02` — English source at `l10n/worksheets/ai-prompt/en.txt`. SENSITIVE; per-language versions (`<lang>.txt`) need native ED-informed review before the server can route them.

