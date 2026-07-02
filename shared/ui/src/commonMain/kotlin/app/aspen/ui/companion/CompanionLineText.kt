package app.aspen.ui.companion

import androidx.compose.runtime.Composable
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.companion_ambient_here_if_needed
import app.aspen.ui.generated.resources.companion_ambient_no_followup
import app.aspen.ui.generated.resources.companion_greeting_company
import app.aspen.ui.generated.resources.companion_greeting_here
import app.aspen.ui.generated.resources.companion_greeting_soft
import app.aspen.ui.generated.resources.companion_ground_breathe
import app.aspen.ui.generated.resources.companion_ground_senses
import app.aspen.ui.generated.resources.companion_ground_write
import app.aspen.ui.generated.resources.companion_hard_no_fixing
import app.aspen.ui.generated.resources.companion_hard_passing
import app.aspen.ui.generated.resources.companion_hard_reach_someone
import app.aspen.ui.generated.resources.companion_hard_sit_together
import app.aspen.ui.generated.resources.companion_notify_gentle_checkin
import app.aspen.ui.generated.resources.companion_notify_tools_nearby
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Resolves a curated companion line KEY (docs/04 ADR-003; `config/companion/library.json`) to its
 * localized copy. The library carries keys only — this is the single place keys become words, so the
 * per-language copy-lint and the sensitive-surface review (docs/12 §3) cover every companion line.
 *
 * Total by design: an unknown key falls back to the quietest ambient line rather than ever showing
 * a raw key or an empty companion (the library invariants tests keep this branch theoretical).
 */
fun companionLineResource(key: String): StringResource = when (key) {
    "companion_greeting_here" -> Res.string.companion_greeting_here
    "companion_greeting_company" -> Res.string.companion_greeting_company
    "companion_greeting_soft" -> Res.string.companion_greeting_soft
    "companion_hard_sit_together" -> Res.string.companion_hard_sit_together
    "companion_hard_no_fixing" -> Res.string.companion_hard_no_fixing
    "companion_hard_reach_someone" -> Res.string.companion_hard_reach_someone
    "companion_hard_passing" -> Res.string.companion_hard_passing
    "companion_ground_breathe" -> Res.string.companion_ground_breathe
    "companion_ground_senses" -> Res.string.companion_ground_senses
    "companion_ground_write" -> Res.string.companion_ground_write
    "companion_ambient_here_if_needed" -> Res.string.companion_ambient_here_if_needed
    "companion_ambient_no_followup" -> Res.string.companion_ambient_no_followup
    "companion_notify_gentle_checkin" -> Res.string.companion_notify_gentle_checkin
    "companion_notify_tools_nearby" -> Res.string.companion_notify_tools_nearby
    else -> Res.string.companion_ambient_here_if_needed
}

/** Convenience: the localized text for a companion line key. */
@Composable
fun companionLineText(key: String): String = stringResource(companionLineResource(key))
