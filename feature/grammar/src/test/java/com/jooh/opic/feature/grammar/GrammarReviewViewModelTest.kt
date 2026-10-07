package com.jooh.opic.feature.grammar

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class GrammarReviewViewModelTest {
    private val book by lazy {
        (GrammarCatalog.parse(File("../../exports/grammar.json").readText()) as GrammarLoadResult.Loaded).book
    }

    private class FakeStore(var due: List<String> = emptyList()) : GrammarReviewStore {
        val practiced = mutableListOf<Pair<String, String>>()
        val reviewed = mutableListOf<Pair<String, Boolean>>()
        override suspend fun dueIds() = due
        override suspend fun recordPractice(exerciseId: String, unitId: String) { practiced += exerciseId to unitId }
        override suspend fun recordReview(exerciseId: String, firstTryCorrect: Boolean) { reviewed += exerciseId to firstTryCorrect }
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun answer(model: GrammarViewModel, correct: Boolean) {
        val question = model.state.value.current!!
        if (question.kind == "choice") model.select(if (correct) question.answer!! else (question.answer!! + 1) % question.choices!!.size)
        else model.setInput(if (correct) question.answers!!.first() else "zzz wrong")
        model.submit()
    }

    @Test fun practiceRecordsOnlySecondTryAndWrong() {
        val store = FakeStore()
        val model = GrammarViewModel(GrammarLoadResult.Loaded(book), store)
        val unit = book.units.first()
        model.openUnit(unit)
        model.start()
        val first = model.state.value.current!!
        answer(model, true) // 1차 정답 → 기록 안 함
        model.next()
        val second = model.state.value.current!!
        answer(model, false); answer(model, true) // 2차 정답 → 복습
        model.next()
        val third = model.state.value.current!!
        answer(model, false); answer(model, false) // 끝내 오답 → 복습
        assertEquals(listOf(second.id to unit.id, third.id to unit.id), store.practiced)
        assertTrue(store.practiced.none { it.first == first.id })
        assertTrue(store.reviewed.isEmpty())
    }

    @Test fun reviewSessionTakesAtMostTenKnownExercisesAndRecordsFirstTryFlag() {
        val all = book.units.flatMap { it.exercises }.map { it.id }
        val store = FakeStore(listOf("removed-from-json") + all.take(12))
        val model = GrammarViewModel(GrammarLoadResult.Loaded(book), store)
        model.refreshDue()
        assertEquals(12, model.state.value.dueCount) // 모르는 id는 건너뜀
        model.startReview()
        val state = model.state.value
        assertTrue(state.reviewMode)
        assertEquals(GrammarPage.EXERCISE, state.page)
        assertEquals(10, state.exercises.size)
        assertTrue(state.exercises.all { it.id in all.take(10) }) // 오래된 10개
        val firstId = state.current!!.id
        answer(model, true)
        model.next()
        val secondId = model.state.value.current!!.id
        answer(model, false); answer(model, true)
        assertEquals(listOf(firstId to true, secondId to false), store.reviewed)
        assertTrue(store.practiced.isEmpty())
        model.back()
        assertEquals(GrammarPage.LIST, model.state.value.page)
    }

    @Test fun noDueMeansNoReviewSession() {
        val model = GrammarViewModel(GrammarLoadResult.Loaded(book), FakeStore())
        model.refreshDue()
        assertEquals(0, model.state.value.dueCount)
        model.startReview()
        assertEquals(GrammarPage.LIST, model.state.value.page)
    }
}
