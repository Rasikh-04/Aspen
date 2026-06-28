package app.aspen.data.consent

import app.aspen.domain.consent.ConsentStore
import app.aspen.domain.consent.model.ConsentEvent
import app.aspen.domain.consent.model.ConsentGrant
import kotlinx.serialization.json.Json

/**
 * The encrypted, fail-safe [ConsentStore] (docs/09 §3.2). Consent state is serialized to JSON,
 * encrypted via [ConsentCipher], and held as one opaque blob in a [ConsentBlobStore].
 *
 * **Fail-safe is the whole point.** A missing, unreadable, undecryptable, or malformed blob yields an
 * EMPTY state — never an exception — so the [app.aspen.domain.consent.ConsentManager]'s default-deny
 * turns any corruption into "deny all", never "fail open". The broad catch here is therefore the
 * intended security behaviour, not a swallowed error: a store we cannot trust grants nothing. Per-entry
 * mapping is likewise lenient ([toDomainOrNull]) so one bad record can't take the rest down with it.
 */
class PersistentConsentStore(
    private val cipher: ConsentCipher,
    private val blob: ConsentBlobStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : ConsentStore {

    override fun allGrants(): List<ConsentGrant> = load().grants.mapNotNull { it.toDomainOrNull() }

    override fun putGrant(grant: ConsentGrant) {
        val state = load()
        save(state.copy(grants = state.grants.filterNot { it.id == grant.id } + grant.toDto()))
    }

    override fun events(): List<ConsentEvent> = load().events.mapNotNull { it.toDomainOrNull() }

    override fun appendEvent(event: ConsentEvent) {
        val state = load()
        save(state.copy(events = state.events + event.toDto()))
    }

    private fun load(): ConsentStateDto = try {
        val bytes = blob.load() ?: return ConsentStateDto()
        val plaintext = cipher.decrypt(bytes).decodeToString()
        json.decodeFromString(ConsentStateDto.serializer(), plaintext)
    } catch (t: Throwable) {
        // Fail SAFE: anything we cannot read becomes empty → default-deny (see class doc).
        ConsentStateDto()
    }

    private fun save(state: ConsentStateDto) {
        val plaintext = json.encodeToString(ConsentStateDto.serializer(), state)
        blob.save(cipher.encrypt(plaintext.encodeToByteArray()))
    }
}
