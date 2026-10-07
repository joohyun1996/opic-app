package com.jooh.opic.feature.grammar

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class GrammarBook(val dataVersion: Int, val units: List<GrammarUnit>)
@Serializable data class GrammarUnit(
    val id: String,
    val order: Int,
    val title: String,
    val opicUse: String,
    val errorType: String,
    val explanation: GrammarExplanation,
    val exercises: List<GrammarExercise>,
    val writingTask: GrammarWritingTask,
)
@Serializable data class GrammarExplanation(
    val summary: String,
    val points: List<String>,
    val examples: List<GrammarExample>,
    val commonMistakes: List<GrammarMistake>,
)
@Serializable data class GrammarExample(val en: String, val ko: String)
@Serializable data class GrammarMistake(val wrong: String, val right: String, val note: String)
@Serializable data class GrammarWritingTask(val promptKo: String, val promptEn: String, val minSentences: Int)
@Serializable data class GrammarExercise(
    val id: String,
    val kind: String,
    val prompt: String,
    val sentence: String,
    val hint: String,
    val explanation: String,
    val answers: List<String>? = null,
    val choices: List<String>? = null,
    val answer: Int? = null,
) {
    fun displayAnswer(): String = if (kind == "choice") choices!![answer!!] else answers!!.first()
}

sealed interface GrammarLoadResult {
    data class Loaded(val book: GrammarBook) : GrammarLoadResult
    data object Failed : GrammarLoadResult
}

object GrammarCatalog {
    private val json = Json { ignoreUnknownKeys = false }

    fun parse(raw: String): GrammarLoadResult = try {
        val book = json.decodeFromString<GrammarBook>(raw)
        val ids = mutableSetOf<String>()
        require(book.dataVersion > 0 && book.units.isNotEmpty())
        book.units.forEach { unit ->
            require(unit.id.isNotBlank() && ids.add(unit.id))
            require(unit.exercises.isNotEmpty())
            unit.exercises.forEach { exercise ->
                require(exercise.id.isNotBlank() && ids.add(exercise.id))
                when (exercise.kind) {
                    "fix", "blank" -> require(exercise.answers?.isNotEmpty() == true &&
                        exercise.answers.all { it.isNotBlank() } && exercise.choices == null && exercise.answer == null)
                    "choice" -> require(exercise.choices?.isNotEmpty() == true &&
                        exercise.answer != null && exercise.answer in exercise.choices.indices && exercise.answers == null)
                    else -> error("Unknown exercise kind")
                }
            }
        }
        GrammarLoadResult.Loaded(book)
    } catch (_: Exception) {
        GrammarLoadResult.Failed
    }
}
