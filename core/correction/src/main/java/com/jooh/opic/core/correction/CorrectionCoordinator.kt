package com.jooh.opic.core.correction

import com.jooh.opic.core.common.CorrectionParse
import com.jooh.opic.core.common.CorrectionResult
import com.jooh.opic.core.common.buildCorrectionPrompt
import com.jooh.opic.core.common.parseCorrection
import com.jooh.opic.core.llm.LlmException
import com.jooh.opic.core.llm.LlmFailureReason
import com.jooh.opic.core.llm.OnDeviceLlmEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

sealed interface CorrectionOutcome {
    val sentence: String
    val elapsedMs: Long
    data class Correct(override val sentence: String, val result: CorrectionResult, override val elapsedMs: Long) : CorrectionOutcome
    data class Incorrect(override val sentence: String, val result: CorrectionResult, override val elapsedMs: Long) : CorrectionOutcome
    data class Failed(override val sentence: String, val reason: FailureReason, override val elapsedMs: Long) : CorrectionOutcome
}

enum class FailureReason { INVALID_JSON, CONTRADICTION, TIMEOUT, ENGINE }

class CorrectionCoordinator(private val engine: OnDeviceLlmEngine) {
    suspend fun correct(sentences: List<String>, max: Int = 5, onResult: suspend (CorrectionOutcome) -> Unit) {
        for (sentence in sentences.take(max)) {
            currentCoroutineContext().ensureActive()
            onResult(correctOne(sentence))
        }
    }

    suspend fun correctOne(sentence: String): CorrectionOutcome {
        currentCoroutineContext().ensureActive()
        val start = System.nanoTime()
        fun elapsed() = (System.nanoTime() - start) / 1_000_000
        return try {
            val raw = engine.generate(buildCorrectionPrompt(sentence), timeoutMs = 120_000).getOrThrow()
            when (val parsed = parseCorrection(raw, sentence)) {
                is CorrectionParse.Ok -> if (parsed.result.correct) CorrectionOutcome.Correct(sentence, parsed.result, elapsed())
                    else CorrectionOutcome.Incorrect(sentence, parsed.result, elapsed())
                CorrectionParse.InvalidJson -> CorrectionOutcome.Failed(sentence, FailureReason.INVALID_JSON, elapsed())
                CorrectionParse.Contradiction -> CorrectionOutcome.Failed(sentence, FailureReason.CONTRADICTION, elapsed())
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val reason = if ((error as? LlmException)?.reason == LlmFailureReason.TIMEOUT) FailureReason.TIMEOUT else FailureReason.ENGINE
            CorrectionOutcome.Failed(sentence, reason, elapsed())
        }
    }
}
