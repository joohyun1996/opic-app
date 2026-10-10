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

    // 실전 영문법 (TASK 26)
    private val coreRaw by lazy { File("../../exports/grammar-core.json").readText() }

    @Test fun coreGrammarParsesWithNewKinds() {
        val core = (GrammarCatalog.parse(coreRaw) as GrammarLoadResult.Loaded).book
        assertEquals(true, core.units.map { it.id }.containsAll(listOf("c0", "c1", "c2", "c3")))
        assertEquals(0, core.units.minOf { it.order }) // 0장 문법 용어 풀이
        assertEquals(true, core.units.first { it.id == "c0" }.explanation.details.size >= 5)
        assertEquals(setOf("core"), core.units.map { it.track }.toSet())
        val kinds = core.units.flatMap { u -> u.exercises.map { it.kind } }.toSet()
        assertEquals(true, kinds.containsAll(listOf("spot", "structure")))
        core.units.filter { it.order > 0 }.forEach { assertEquals(true, it.explanation.breakdowns.isNotEmpty() && it.explanation.table != null && it.writingTask == null) }
        // spot 정답 위치가 문장 단어 범위 밖이면 실패
        assertEquals(GrammarLoadResult.Failed, GrammarCatalog.parse(coreRaw.replaceFirst("\"answer\": 4", "\"answer\": 40")))
    }

    @Test fun mergeKeepsBothTracksAndRejectsDuplicateIds() {
        val merged = mergeBooks(GrammarCatalog.parse(raw), GrammarCatalog.parse(coreRaw)) as GrammarLoadResult.Loaded
        assertEquals(setOf("opic", "core"), merged.book.units.map { it.track }.toSet())
        assertEquals(true, merged.book.units.size >= 14)
        assertEquals(GrammarLoadResult.Failed, mergeBooks(GrammarCatalog.parse(raw), GrammarCatalog.parse(raw)))
        assertEquals(GrammarLoadResult.Failed, mergeBooks(GrammarCatalog.parse(raw), GrammarLoadResult.Failed))
    }
}
