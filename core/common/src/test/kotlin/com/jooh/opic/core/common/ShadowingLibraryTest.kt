package com.jooh.opic.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ShadowingLibraryTest {
    private fun one(id: String = "aircAruvnKk", category: String = "ted", level: String = "AL", id2: String = "jNQXAC9IVRw") = """
        {"dataVersion":1,"checkedAt":"2026-10-10","videos":[
          {"id":"$id","title":"A","channel":"C","category":"$category","level":"$level","minutes":5},
          {"id":"$id2","title":"B","channel":"C","category":"learner","level":"IM","minutes":3}]}
    """.trimIndent()

    @Test fun bundledFileHasHundredVideos() {
        val library = parseShadowingLibrary(File("../../exports/shadowing.json").readText())!!
        assertEquals(100, library.videos.size)
        assertEquals(SHADOWING_CATEGORIES.map { it.first }.toSet(), library.videos.map { it.category }.toSet())
        assertTrue(library.videos.all { youtubeVideoId("https://youtu.be/${it.id}") == it.id })
    }

    @Test fun rejectsInvalid() {
        assertEquals(2, parseShadowingLibrary(one())!!.videos.size)
        assertNull(parseShadowingLibrary(one(id = "short")))
        assertNull(parseShadowingLibrary(one(id2 = "aircAruvnKk")))
        assertNull(parseShadowingLibrary(one(category = "movie")))
        assertNull(parseShadowingLibrary(one(level = "B2")))
    }
}
