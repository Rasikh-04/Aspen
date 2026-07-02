package app.aspen.ui.companion

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.aspen.domain.companion.CompanionBehaviour
import app.aspen.domain.companion.CompanionContext
import app.aspen.domain.companion.CompanionEvent
import app.aspen.domain.companion.CompanionPrefsStore
import app.aspen.domain.companion.CompanionState
import app.aspen.domain.companion.model.CompanionPrefs
import app.aspen.domain.companion.model.CompanionSpecies
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Plain Compose state holder for the in-app companion (same no-ViewModel-lib pattern as
 * `OnboardingController`). All rules live in the domain [CompanionBehaviour]; this only feeds it
 * events and persists preference changes. A null [store] (unwired platform) means default prefs —
 * i.e. everything off, companion hidden (docs/05 §3.1).
 */
@Stable
class CompanionController(
    private val store: CompanionPrefsStore?,
    private val now: () -> Instant = { Clock.System.now() },
) {
    var prefs: CompanionPrefs by mutableStateOf(store?.current() ?: CompanionPrefs())
        private set

    /** Kept current by the host from [app.aspen.design.LocalReducedMotion] (SR-6). */
    var reducedMotion: Boolean by mutableStateOf(false)

    private val context: CompanionContext
        get() = CompanionContext(enabled = prefs.enabled, reducedMotion = reducedMotion)

    var state: CompanionState by mutableStateOf(CompanionBehaviour.initial(context))
        private set

    fun on(event: CompanionEvent) {
        state = CompanionBehaviour.next(state, event, context)
    }

    fun tap() = on(CompanionEvent.Tap(at = now()))

    fun tick() = on(CompanionEvent.Tick(now = now()))

    fun rest() = on(CompanionEvent.FlingDismiss)

    fun setEnabled(enabled: Boolean) {
        updatePrefs(prefs.copy(enabled = enabled))
        // Turning the toggle IS the summon/banish act (docs/05 §4 "in-app toggle").
        state = CompanionBehaviour.initial(context)
    }

    fun setSpecies(species: CompanionSpecies) = updatePrefs(prefs.copy(species = species))

    fun setOverlayEnabled(enabled: Boolean) = updatePrefs(prefs.copy(overlayEnabled = enabled))

    fun setNotificationsEnabled(enabled: Boolean) = updatePrefs(prefs.copy(notificationsEnabled = enabled))

    private fun updatePrefs(newPrefs: CompanionPrefs) {
        prefs = newPrefs
        store?.save(newPrefs)
    }
}
