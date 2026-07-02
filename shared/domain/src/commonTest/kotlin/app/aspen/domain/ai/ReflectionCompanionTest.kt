package app.aspen.domain.ai

import app.aspen.domain.consent.ConsentStore
import app.aspen.domain.consent.DefaultConsentManager
import app.aspen.domain.consent.model.ConsentEvent
import app.aspen.domain.consent.model.ConsentGrant
import app.aspen.domain.consent.model.DataCategory
import app.aspen.domain.consent.model.Recipient
import app.aspen.domain.consent.model.RecipientType
import app.aspen.domain.safety.CrisisResolver
import app.aspen.domain.safety.CrisisSignals
import app.aspen.domain.safety.DefaultCrisisSignalLexicon
import app.aspen.domain.safety.DefaultForbiddenLexicon
import app.aspen.domain.safety.DefaultSafetyEngine
import app.aspen.domain.safety.SafetyRules
import app.aspen.domain.safety.model.CrisisResourceSet
import app.aspen.domain.safety.model.LocaleKey
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * The highest-scrutiny suite of Phase 4 (CLAUDE.md #8): the ONE pipeline user text and AI text pass
 * through. Uses the REAL consent manager, safety engine, and crisis signals (fakes only at the
 * client/store edges) so the gate order — consent → crisis → client → guard → persist — is proven
 * against production logic, not mocks of it.
 */
class ReflectionCompanionTest {

    private class InMemoryConsentStore : ConsentStore {
        private val grants = mutableMapOf<String, ConsentGrant>()
        private val log = mutableListOf<ConsentEvent>()
        override fun allGrants() = grants.values.toList()
        override fun putGrant(grant: ConsentGrant) { grants[grant.id] = grant }
        override fun events() = log.toList()
        override fun appendEvent(event: ConsentEvent) { log += event }
    }

    private class FixedClock : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(1_000_000L)
    }

    private class RecordingClient(var result: AiClientResult) : AiClient {
        var calls = 0
        override suspend fun reply(userText: String, history: List<AiMessage>): AiClientResult {
            calls++
            return result
        }
    }

    private class ThrowingClient : AiClient {
        override suspend fun reply(userText: String, history: List<AiMessage>): AiClientResult =
            error("network exploded")
    }

    private class InMemoryAiMessageStore : AiMessageStore {
        val messages = mutableListOf<AiMessage>()
        override fun append(message: AiMessage) { messages += message }
        override fun history() = messages.toList()
        override fun clearAll() = messages.clear()
    }

    private val fixedClock = FixedClock()
    private var idSeq = 0
    private val consent = DefaultConsentManager(InMemoryConsentStore(), fixedClock) { "id-${idSeq++}" }
    private val store = InMemoryAiMessageStore()
    private val safetyEngine = DefaultSafetyEngine(
        crisisResolver = object : CrisisResolver {
            override fun resolve(locale: LocaleKey) = CrisisResourceSet(
                locale = locale,
                edSupport = emptyList(),
                acuteCrisis = emptyList(),
                treatmentFinder = emptyList(),
                isFallback = true,
            )
        },
        safetyRules = SafetyRules(DefaultForbiddenLexicon.lexicon),
        safeFallbackText = SAFE_FALLBACK,
    )

    private fun companion(client: AiClient) = ReflectionCompanion(
        consent = consent,
        client = client,
        safetyEngine = safetyEngine,
        crisisSignals = CrisisSignals(DefaultCrisisSignalLexicon.lexicon),
        store = store,
        newId = { "msg-${idSeq++}" },
        clock = fixedClock,
    )

    private fun grantCloudConsent() {
        consent.grant(
            Recipient(ReflectionCompanion.AI_RECIPIENT_ID, RecipientType.AI_SERVICE, "Cloud reflection"),
            setOf(DataCategory.AI_MESSAGES),
            purpose = "cloud reflection",
        )
    }

    // ---- consent gate: default deny, revoke immediate, client untouched ----

    @Test
    fun withoutConsentTheClientIsNeverTouched() = runTest {
        val client = RecordingClient(AiClientResult.Reply("hello"))

        val outcome = companion(client).reflect("today was hard")

        assertIs<ReflectionOutcome.Disabled>(outcome)
        assertEquals(0, client.calls, "cloud client must never be invoked without an active grant")
        assertTrue(store.messages.isEmpty(), "nothing may be persisted without consent")
    }

    @Test
    fun revokingConsentDisablesImmediately() = runTest {
        val client = RecordingClient(AiClientResult.Reply("that sounds heavy"))
        val subject = companion(client)
        grantCloudConsent()
        assertIs<ReflectionOutcome.Reply>(subject.reflect("today was hard"))

        consent.activeGrants().forEach { consent.revoke(it.id) }

        assertIs<ReflectionOutcome.Disabled>(subject.reflect("still hard"))
        assertEquals(1, client.calls)
        assertTrue(subject.history().isEmpty(), "history is not readable after revoke")
    }

    // ---- crisis gate: hand-off BEFORE anything leaves the device ----

    @Test
    fun crisisInputHandsOffWithoutClientCallOrPersistence() = runTest {
        val client = RecordingClient(AiClientResult.Reply("reply"))
        grantCloudConsent()

        val outcome = companion(client).reflect("i want to end my life")

        assertIs<ReflectionOutcome.CrisisHandOff>(outcome)
        assertEquals(0, client.calls, "crisis text must never leave the device")
        assertTrue(store.messages.isEmpty(), "crisis exchanges are not stored")
    }

    @Test
    fun ordinaryDistressIsNotTreatedAsCrisis() = runTest {
        val client = RecordingClient(AiClientResult.Reply("that sounds like a heavy evening"))
        grantCloudConsent()

        val outcome = companion(client).reflect("i feel awful about dinner and today was hard")

        assertIs<ReflectionOutcome.Reply>(outcome)
    }

    // ---- output guard: withhold + replace, never echo, never persist unsafe text ----

    @Test
    fun cleanReplyPassesAndPersistsBothTurns() = runTest {
        grantCloudConsent()
        val subject = companion(RecordingClient(AiClientResult.Reply("that sounds really heavy")))

        val outcome = subject.reflect("today was a lot")

        val reply = assertIs<ReflectionOutcome.Reply>(outcome)
        assertEquals("that sounds really heavy", reply.text)
        assertFalse(reply.wasGuarded)
        assertEquals(listOf(AiRole.USER, AiRole.COMPANION), store.messages.map { it.role })
    }

    @Test
    fun forbiddenReplyIsWithheldReplacedAndNeverPersisted() = runTest {
        grantCloudConsent()
        val unsafe = "you should eat less and count calories"
        val subject = companion(RecordingClient(AiClientResult.Reply(unsafe)))

        val outcome = subject.reflect("what do you think i should do")

        val reply = assertIs<ReflectionOutcome.Reply>(outcome)
        assertEquals(SAFE_FALLBACK, reply.text, "guard must substitute the safe fallback")
        assertTrue(reply.wasGuarded)
        assertTrue(store.messages.none { it.text == unsafe }, "unsafe text must never be persisted")
        assertTrue(store.messages.any { it.text == SAFE_FALLBACK })
    }

    // ---- degradation: calm, never an error state ----

    @Test
    fun throwingClientDegradesToUnavailableWithoutPersisting() = runTest {
        grantCloudConsent()

        val outcome = companion(ThrowingClient()).reflect("today was hard")

        assertIs<ReflectionOutcome.Unavailable>(outcome)
        assertTrue(store.messages.isEmpty())
    }

    @Test
    fun disabledClientReportsDisabled() = runTest {
        grantCloudConsent()
        assertIs<ReflectionOutcome.Disabled>(
            companion(RecordingClient(AiClientResult.Disabled)).reflect("hello"),
        )
    }

    @Test
    fun offlineClientReportsUnavailable() = runTest {
        grantCloudConsent()
        assertIs<ReflectionOutcome.Unavailable>(
            companion(RecordingClient(AiClientResult.Unavailable)).reflect("hello"),
        )
    }

    @Test
    fun blankInputNeverReachesTheClient() = runTest {
        val client = RecordingClient(AiClientResult.Reply("x"))
        grantCloudConsent()

        companion(client).reflect("   ")

        assertEquals(0, client.calls)
    }

    // ---- FR-11 ----

    @Test
    fun deleteEverythingClearsTheHistory() = runTest {
        grantCloudConsent()
        val subject = companion(RecordingClient(AiClientResult.Reply("that sounds heavy")))
        subject.reflect("today was hard")
        assertTrue(store.messages.isNotEmpty())

        subject.deleteEverything()

        assertTrue(store.messages.isEmpty())
    }

    private companion object {
        const val SAFE_FALLBACK = "What you're feeling matters. A real person can be there for this."
    }
}
