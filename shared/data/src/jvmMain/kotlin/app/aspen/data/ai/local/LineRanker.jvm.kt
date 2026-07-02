package app.aspen.data.ai.local

import app.aspen.domain.ai.LineRanker

/** JVM: no on-device ranker — deterministic selection (the JVM target is tests/dev only). */
actual fun platformLineRanker(): LineRanker? = null
