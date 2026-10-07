package com.jooh.opic.feature.grammar

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class GrammarCatalogTest {
    private val raw by lazy { File("../../exports/grammar.json").readText() }

    @Test fun catalogHasHundredValidExercises() {
        val result = GrammarCatalog.parse(raw) as GrammarLoadResult.Loaded
        assertEquals(2, result.book.dataVersion)
        assertEquals((1..10).toList(), result.book.units.map { it.order })
        assertEquals(10, result.book.units.size)
        assertEquals(100, result.book.units.sumOf { it.exercises.size })
        assertEquals(110, (result.book.units.map { it.id } + result.book.units.flatMap { unit -> unit.exercises.map { it.id } }).toSet().size)
        result.book.units.flatMap { it.exercises }.filter { it.kind == "choice" }.forEach {
            assertTrue(it.answer!! in it.choices!!.indices)
        }
    }

    @Test fun invalidDataReturnsFailure() {
        assertEquals(GrammarLoadResult.Failed, GrammarCatalog.parse("{"))
        assertEquals(GrammarLoadResult.Failed, GrammarCatalog.parse(raw.replace("\"u1-02\"", "\"u1-01\"")))
        assertEquals(GrammarLoadResult.Failed, GrammarCatalog.parse(raw.replace("\"answer\": 1", "\"answer\": 99")))
    }
}
