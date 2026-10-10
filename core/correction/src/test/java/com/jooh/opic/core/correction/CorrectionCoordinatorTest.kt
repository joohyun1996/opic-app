package com.jooh.opic.core.correction

import com.jooh.opic.core.llm.LlmEngineState
import com.jooh.opic.core.llm.OnDeviceLlmEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CorrectionCoordinatorTest {
    @Test fun emitsThreeResultsInSentenceOrder() = runBlocking {
        val fake = FakeEngine(listOf(
            """{"correct":true,"corrected":"I went home.","errors":[]}""",
            """{"correct":false,"corrected":"I went to school.","errors":[{"type":"tense","original":"go","fix":"went","explanationKo":"과거형입니다."}]}""",
            "broken",
        ))
        val seen = mutableListOf<CorrectionOutcome>()
        CorrectionCoordinator(fake).correct(listOf("I went home.", "I go to school.", "Third.")) { seen += it }
        assertEquals(listOf("I went home.", "I go to school.", "Third."), seen.map { it.sentence })
        assertTrue(seen[0] is CorrectionOutcome.Correct)
        assertTrue(seen[1] is CorrectionOutcome.Incorrect)
        assertEquals(FailureReason.INVALID_JSON, (seen[2] as CorrectionOutcome.Failed).reason)
        assertEquals(3, fake.calls)
    }

    @Test fun cancellationPreventsLaterGenerateCalls() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val blocked = CompletableDeferred<String>()
        var calls = 0
        val fake = object : OnDeviceLlmEngine {
            override val state = MutableStateFlow<LlmEngineState>(LlmEngineState.Ready("fake"))
            override suspend fun ensureModelReady(): Result<Unit> = Result.success(Unit)
            override suspend fun generate(prompt: String, timeoutMs: Long): Result<String> {
                calls++
                started.complete(Unit)
                return Result.success(blocked.await())
            }
            override fun close() = Unit
        }
        val job = launch { CorrectionCoordinator(fake).correct(listOf("One.", "Two.", "Three.")) {} }
        started.await()
        job.cancelAndJoin()
        assertEquals(1, calls)
    }

    private class FakeEngine(private val responses: List<String>) : OnDeviceLlmEngine {
        override val state = MutableStateFlow<LlmEngineState>(LlmEngineState.Ready("fake"))
        var calls = 0
        override suspend fun ensureModelReady(): Result<Unit> = Result.success(Unit)
        override suspend fun generate(prompt: String, timeoutMs: Long): Result<String> = Result.success(responses[calls++])
        override fun close() = Unit
    }
}
