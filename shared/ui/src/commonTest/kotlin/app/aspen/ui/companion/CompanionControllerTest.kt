package app.aspen.ui.companion

import app.aspen.domain.companion.CompanionEvent
import app.aspen.domain.companion.CompanionPrefsStore
import app.aspen.domain.companion.CompanionState
import app.aspen.domain.companion.model.CompanionPrefs
import app.aspen.domain.companion.model.CompanionSpecies
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Instant

private class FakePrefsStore(var stored: CompanionPrefs? = null) : CompanionPrefsStore {
    override fun save(prefs: CompanionPrefs) { stored = prefs }
    override fun current(): CompanionPrefs? = stored
    override fun clear() { stored = null }
}

class CompanionControllerTest {

    private val t0 = Instant.fromEpochMilliseconds(0)

    @Test
    fun unwiredPlatformMeansHiddenAndDisabled() {
        val controller = CompanionController(store = null)
        assertEquals(false, controller.prefs.enabled)
        assertEquals(CompanionState.Hidden, controller.state)
    }

    @Test
    fun enablingSummonsAndPersists() {
        val store = FakePrefsStore()
        val controller = CompanionController(store, now = { t0 })

        controller.setEnabled(true)

        assertEquals(CompanionState.Ambient, controller.state)
        assertEquals(true, store.stored?.enabled)
    }

    @Test
    fun disablingHidesImmediatelyAndPersists() {
        val store = FakePrefsStore(CompanionPrefs(enabled = true))
        val controller = CompanionController(store, now = { t0 })
        assertEquals(CompanionState.Ambient, controller.state)

        controller.setEnabled(false)

        assertEquals(CompanionState.Hidden, controller.state)
        assertEquals(false, store.stored?.enabled)
    }

    @Test
    fun tapPlaysAndRestDismissesUntilResummoned() {
        val controller = CompanionController(FakePrefsStore(CompanionPrefs(enabled = true)), now = { t0 })

        controller.tap()
        assertIs<CompanionState.Playful>(controller.state)

        controller.rest()
        assertEquals(CompanionState.Hidden, controller.state)

        // No event may bring it back on its own (SR-4) — only an explicit summon.
        controller.tick()
        controller.on(CompanionEvent.HardMomentOpened)
        assertEquals(CompanionState.Hidden, controller.state)
        controller.on(CompanionEvent.Summon)
        assertEquals(CompanionState.Ambient, controller.state)
    }

    @Test
    fun reducedMotionRefusesPlay() {
        val controller = CompanionController(FakePrefsStore(CompanionPrefs(enabled = true)), now = { t0 })
        controller.reducedMotion = true

        controller.tap()

        assertEquals(CompanionState.Ambient, controller.state)
    }

    @Test
    fun speciesChoicePersistsWithoutChangingPresence() {
        val store = FakePrefsStore(CompanionPrefs(enabled = true))
        val controller = CompanionController(store, now = { t0 })

        controller.setSpecies(CompanionSpecies.CAT)

        assertEquals(CompanionSpecies.CAT, store.stored?.species)
        assertEquals(CompanionState.Ambient, controller.state)
    }

    @Test
    fun corruptStoreMeansDefaultsAllOff() {
        val controller = CompanionController(FakePrefsStore(stored = null))
        assertNull(FakePrefsStore().current())
        assertEquals(CompanionPrefs(), controller.prefs)
        assertEquals(CompanionState.Hidden, controller.state)
    }
}
