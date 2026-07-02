package app.aspen.data.ai.local

import android.content.Context
import app.aspen.data.local.AspenLocalStorage
import app.aspen.domain.ai.CompanionLine
import app.aspen.domain.ai.LineRanker
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder

/**
 * Android Tier-1 ranker (docs/04 ADR-003): a small LiteRT/MediaPipe **text embedder** — NOT a
 * generative model — that scores which approved library line best fits the moment by cosine
 * similarity between the ranking context and each line's non-user-facing hint. It can only reorder
 * the candidates it is given; the words themselves always come from the curated library.
 *
 * Model: `companion_ranker.tflite` in the app's assets (see docs/DEV_SETUP: MediaPipe Text Embedder,
 * Average Word Embedding variant, ~4 MB, Apache-2.0). The asset is OPTIONAL by design — missing or
 * unloadable model → null ranker → deterministic selection; the app must never depend on it.
 */
private class MediaPipeLineRanker(private val embedder: TextEmbedder) : LineRanker {

    override fun pickBest(context: String, candidates: List<CompanionLine>): CompanionLine? {
        if (candidates.size <= 1) return candidates.firstOrNull()
        val contextEmbedding = embed(context) ?: return null
        return candidates.maxByOrNull { candidate ->
            embed(candidate.rankingHint)?.let { cosineSimilarity(contextEmbedding, it) } ?: Double.NEGATIVE_INFINITY
        }
    }

    private fun embed(text: String): FloatArray? =
        runCatching {
            embedder.embed(text).embeddingResult().embeddings().firstOrNull()?.floatEmbedding()
        }.getOrNull()

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        if (a.size != b.size || a.isEmpty()) return Double.NEGATIVE_INFINITY
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        if (normA == 0.0 || normB == 0.0) return Double.NEGATIVE_INFINITY
        return dot / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))
    }
}

private const val RANKER_MODEL_ASSET = "companion_ranker.tflite"

private fun loadEmbedder(context: Context): TextEmbedder? = runCatching {
    // Probe the asset first: TextEmbedder throws (and logs noisily) on a missing file, and an
    // absent model is the NORMAL dev state, not an error.
    context.assets.open(RANKER_MODEL_ASSET).use { }
    TextEmbedder.createFromOptions(
        context,
        TextEmbedder.TextEmbedderOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(RANKER_MODEL_ASSET).build())
            .build(),
    )
}.getOrNull()

actual fun platformLineRanker(): LineRanker? =
    runCatching { AspenLocalStorage.requireContext() }.getOrNull()
        ?.let(::loadEmbedder)
        ?.let(::MediaPipeLineRanker)
