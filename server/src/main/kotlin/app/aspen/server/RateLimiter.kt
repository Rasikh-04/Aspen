package app.aspen.server

/**
 * Minimal sliding-window rate limiter (per key, e.g. account id or route+ip). Hand-rolled on
 * purpose: a handful of lines, no extra plugin dependency, and an injectable clock for tests.
 * Guards the credential endpoints (brute force) and the AI relay (cost) — never content.
 */
class RateLimiter(
    private val maxPerWindow: Int,
    private val windowMs: Long,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val hits = HashMap<String, MutableList<Long>>()
    private val lock = Any()

    fun allow(key: String): Boolean = synchronized(lock) {
        val cutoff = now() - windowMs
        val list = hits.getOrPut(key) { mutableListOf() }
        list.removeAll { it < cutoff }
        if (list.size >= maxPerWindow) return false
        list.add(now())
        true
    }
}
