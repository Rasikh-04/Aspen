package app.aspen.core.log

/**
 * Privacy-safe logging seam (docs/04 §4). Placeholder for Phase 1: a no-op sink so feature code
 * can log without ever emitting sensitive user content. Real sinks are wired in a later phase;
 * by contract, user reflection/log text must never be passed here.
 */
object AspenLog {
    fun d(tag: String, message: String) { /* no-op in Phase 1 */ }
    fun w(tag: String, message: String) { /* no-op in Phase 1 */ }
    fun e(tag: String, message: String, cause: Throwable? = null) { /* no-op in Phase 1 */ }
}
