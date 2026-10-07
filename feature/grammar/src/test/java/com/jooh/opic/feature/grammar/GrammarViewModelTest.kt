package com.jooh.opic.feature.grammar

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class GrammarViewModelTest {
    private val book by lazy {
        (GrammarCatalog.parse(File("../../exports/grammar.json").readText()) as GrammarLoadResult.Loaded).book
    }

    @Test fun tenQuestionsReachResultAndRetryResetsScore() {
        val model = GrammarViewModel(GrammarLoadResult.Loaded(book))
        model.openUnit(book.units.first())
        model.start()
        assertEquals(GrammarPage.EXERCISE, model.state.value.page)
        repeat(10) { index ->
            val question = model.state.value.current!!
            if (question.kind == "choice") model.select(question.answer!!)
            else model.setInput(question.answers!!.first())
            model.submit()
            assertTrue(model.state.value.attempt.isFinished())
            model.next()
            if (index < 9) assertEquals(index + 1, model.state.value.index)
        }
        assertEquals(GrammarPage.RESULT, model.state.value.page)
        assertEquals(10, model.state.value.score.firstTry)
        model.start()
        assertEquals(GrammarPage.EXERCISE, model.state.value.page)
        assertEquals(0, model.state.value.score.firstTry)
    }

    @Test fun failedCatalogRemainsSafeState() {
        val model = GrammarViewModel(GrammarLoadResult.Failed)
        assertEquals(GrammarLoadResult.Failed, model.state.value.catalog)
        model.start()
        model.submit()
        model.next()
        assertEquals(GrammarPage.LIST, model.state.value.page)
    }

    @Test fun missedQuestionAppearsInResult() {
        val model = GrammarViewModel(GrammarLoadResult.Loaded(book))
        model.openUnit(book.units.first())
        model.start()
        val missed = model.state.value.current!!
        if (missed.kind == "choice") model.select((missed.answer!! + 1) % missed.choices!!.size)
        else model.setInput("not a valid answer")
        model.submit()
        model.submit()
        assertEquals(1, model.state.value.score.wrong)
        model.next()
        while (model.state.value.page == GrammarPage.EXERCISE) {
            val question = model.state.value.current!!
            if (question.kind == "choice") model.select(question.answer!!)
            else model.setInput(question.answers!!.first())
            model.submit()
            model.next()
        }
        assertEquals(listOf(missed), model.state.value.missed)
        assertEquals(9, model.state.value.score.firstTry)
    }
}
