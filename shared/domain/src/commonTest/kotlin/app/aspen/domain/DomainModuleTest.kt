package app.aspen.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class DomainModuleTest {
    @Test
    fun domainModuleCompilesAndReportsPhase() {
        assertEquals(3, DomainModule.PHASE)
    }
}
