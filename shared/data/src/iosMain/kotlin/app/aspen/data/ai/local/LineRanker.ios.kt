package app.aspen.data.ai.local

import app.aspen.domain.ai.LineRanker

/**
 * iOS: no on-device ranker yet — deterministic selection over the same curated library, so iOS
 * behaviour differs only in line *variety*, never in safety. A Core ML / LiteRT-C ranker actual is a
 * tracked later task (docs/STATUS.md), same pattern as the iOS cipher.
 */
actual fun platformLineRanker(): LineRanker? = null
