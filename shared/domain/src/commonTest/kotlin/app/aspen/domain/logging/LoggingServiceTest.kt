package app.aspen.domain.logging

import app.aspen.domain.logging.model.BehaviourLog
import app.aspen.domain.logging.model.FeelingTag
import app.aspen.domain.logging.model.FoodLog
import app.aspen.domain.logging.model.Reflection
import app.aspen.domain.onboarding.AppConfigProvider
import app.aspen.domain.onboarding.ProfileStore
import app.aspen.domain.onboarding.model.OnboardingResult
import app.aspen.domain.onboarding.model.ProtectiveFlag
import app.aspen.domain.onboarding.model.RoutingHints
import app.aspen.domain.onboarding.model.SupportProfile
import app.aspen.domain.onboarding.model.SupportRoutingStrength
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** In-memory fake of the persistence port — the service's policy is what's under test here. */
private class FakeLoggingStore : LoggingStore {
    val reflections = mutableListOf<Reflection>()
    val food = mutableListOf<FoodLog>()
    val behaviour = mutableListOf<BehaviourLog>()
    override fun addReflection(reflection: Reflection) { reflections += reflection }
    override fun reflections() = reflections.toList()
    override fun deleteReflection(id: String) { reflections.removeAll { it.id == id } }
    override fun addFoodLog(log: FoodLog) { food += log }
    override fun foodLogs() = food.toList()
    override fun deleteFoodLog(id: String) { food.removeAll { it.id == id } }
    override fun addBehaviourLog(log: BehaviourLog) { behaviour += log }
    override fun behaviourLogs() = behaviour.toList()
    override fun deleteBehaviourLog(id: String) { behaviour.removeAll { it.id == id } }
    override fun clearAll() { reflections.clear(); food.clear(); behaviour.clear() }
}

/** A ProfileStore that returns a fixed result (or null = no profile → safest config). */
private class FixedProfileStore(private val result: OnboardingResult?) : ProfileStore {
    override fun save(result: OnboardingResult) = Unit
    override fun current(): OnboardingResult? = result
    override fun clear() = Unit
}

private fun profileResult(
    profile: SupportProfile,
    flags: Set<ProtectiveFlag> = emptySet(),
) = OnboardingResult(
    profiles = mapOf(profile to 1),
    dominantProfile = profile,
    protectiveFlags = flags,
    routingHints = RoutingHints(SupportRoutingStrength.STANDARD, false, false),
)

class LoggingServiceTest {

    private val epoch = Instant.fromEpochSeconds(0)
    private val fixedClock = object : Clock { override fun now() = epoch }
    private var counter = 0

    private fun service(store: LoggingStore, profile: ProfileStore) =
        LoggingService(store, AppConfigProvider(profile), newId = { "id-${counter++}" }, clock = fixedClock)

    @Test
    fun `food log is suppressed for a restriction profile`() {
        // Arrange — restriction → SUPPRESS_FOOD_LOGGING → FoodLoggingMode.OFF.
        val store = FakeLoggingStore()
        val svc = service(
            store,
            FixedProfileStore(profileResult(SupportProfile.RESTRICTION_LEANING, setOf(ProtectiveFlag.SUPPRESS_FOOD_LOGGING))),
        )

        // Act
        val outcome = svc.logFood("a hard lunch", setOf(FeelingTag.ANXIOUS))

        // Assert — refused, nothing written.
        assertIs<LogOutcome.SuppressedFoodLogging>(outcome)
        assertTrue(store.foodLogs().isEmpty())
        assertFalse(svc.isFoodLoggingOffered())
    }

    @Test
    fun `food log is suppressed when no profile is stored - safest default`() {
        // Arrange — null profile → AppConfigProvider safest → MIXED_OR_UNSURE → food logging OFF.
        val store = FakeLoggingStore()
        val svc = service(store, FixedProfileStore(null))

        // Act
        val outcome = svc.logFood("note", emptySet())

        // Assert
        assertIs<LogOutcome.SuppressedFoodLogging>(outcome)
        assertTrue(store.foodLogs().isEmpty())
    }

    @Test
    fun `food log is stored for a binge profile - available and shame-free`() {
        // Arrange — binge, no protective flag → AVAILABLE.
        val store = FakeLoggingStore()
        val svc = service(store, FixedProfileStore(profileResult(SupportProfile.BINGE_LEANING)))

        // Act
        val outcome = svc.logFood("dinner", setOf(FeelingTag.OVERWHELMED))

        // Assert
        assertIs<LogOutcome.Saved>(outcome)
        assertEquals(1, store.foodLogs().size)
        assertTrue(svc.isFoodLoggingOffered())
    }

    @Test
    fun `reflections and behaviour logs are always available regardless of profile`() {
        // Arrange — most protective profile.
        val store = FakeLoggingStore()
        val svc = service(
            store,
            FixedProfileStore(profileResult(SupportProfile.RESTRICTION_LEANING, setOf(ProtectiveFlag.SUPPRESS_FOOD_LOGGING))),
        )

        // Act
        svc.logReflection("just needed to write this down")
        svc.logBehaviour("went for a walk", setOf(FeelingTag.CALM))

        // Assert — lower-risk logging is never suppressed (docs/03 FR-3b).
        assertEquals(1, store.reflections().size)
        assertEquals(1, store.behaviour.size)
    }

    @Test
    fun `delete everything wipes all logs - FR-11`() {
        // Arrange
        val store = FakeLoggingStore()
        val svc = service(store, FixedProfileStore(profileResult(SupportProfile.BINGE_LEANING)))
        svc.logFood("a", emptySet())
        svc.logReflection("b")
        svc.logBehaviour("c", emptySet())

        // Act
        svc.deleteEverything()

        // Assert
        assertTrue(store.foodLogs().isEmpty() && store.reflections().isEmpty() && store.behaviourLogs().isEmpty())
    }
}
