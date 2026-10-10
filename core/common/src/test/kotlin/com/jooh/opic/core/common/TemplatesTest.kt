package com.jooh.opic.core.common

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TemplatesTest {
    private val raw by lazy { File("../../exports/templates.json").readText() }

    @Test fun bundledTemplatesCoverEveryQuestionType() {
        val data = parseAnswerTemplates(raw)!!
        assertEquals(SPEAKING_TYPES, data.templates.map { it.type }.toSet())
        data.templates.forEach { t ->
            assertTrue(t.type, t.steps.size >= 3)
            assertTrue(t.type, t.sample.split(Regex("\\s+")).size in 100..150)
        }
    }

    @Test fun chapterIdsExistInCoreGrammar() {
        val ids = Json.parseToJsonElement(File("../../exports/grammar-core.json").readText()).jsonObject["units"]!!.jsonArray
            .map { it.jsonObject["id"]!!.jsonPrimitive.content }.toSet()
        assertEquals(emptySet<String>(), parseAnswerTemplates(raw)!!.templates.flatMap { it.chapters }.toSet() - ids)
    }

    @Test fun invalidTemplatesReturnNull() {
        assertNull(parseAnswerTemplates("{"))
        assertNull(parseAnswerTemplates(raw.replace("\"type\": \"routine\"", "\"type\": \"describe\"")))
        assertNull(parseAnswerTemplates(raw.replace("\"type\": \"routine\"", "\"type\": \"dance\"")))
    }
}
