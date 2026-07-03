package app.aspen.server

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RateLimiterTest {

    @Test
    fun `allows up to the limit then blocks`() {
        val limiter = RateLimiter(maxPerWindow = 3, windowMs = 1_000) { 0L }
        repeat(3) { assertTrue(limiter.allow("k")) }
        assertFalse(limiter.allow("k"))
    }

    @Test
    fun `keys are independent`() {
        val limiter = RateLimiter(maxPerWindow = 1, windowMs = 1_000) { 0L }
        assertTrue(limiter.allow("a"))
        assertFalse(limiter.allow("a"))
        assertTrue(limiter.allow("b"))
    }

    @Test
    fun `window slides - old hits stop counting`() {
        var now = 0L
        val limiter = RateLimiter(maxPerWindow = 2, windowMs = 100) { now }
        assertTrue(limiter.allow("k"))
        assertTrue(limiter.allow("k"))
        assertFalse(limiter.allow("k"))
        now = 150
        assertTrue(limiter.allow("k"))
    }
}
