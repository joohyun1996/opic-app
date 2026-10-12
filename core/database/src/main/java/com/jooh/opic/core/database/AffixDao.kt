package com.jooh.opic.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

data class AffixPartRow(
    val position: Int,
    val glossKo: String?,
    val affixId: Long?,
    val baseWordId: Long?,
    val label: String,
    val meaningKo: String,
)

@Dao
interface AffixDao {
    @Query("SELECT * FROM affixes WHERE language = :language AND form = :form AND type = :type LIMIT 1")
    suspend fun findAffix(language: String, form: String, type: String): AffixEntity?

    @Insert
    suspend fun insertAffix(affix: AffixEntity): Long

    @Update
    suspend fun updateAffix(affix: AffixEntity)

    @Query("SELECT * FROM word_parts WHERE language = :language")
    suspend fun allParts(language: String): List<WordPartEntity>

    @Insert
    suspend fun insertPart(part: WordPartEntity): Long

    @Update
    suspend fun updatePart(part: WordPartEntity)

    @Query("DELETE FROM word_parts WHERE language = :language AND id = :id")
    suspend fun deletePart(language: String, id: Long)

    @Query("DELETE FROM affixes WHERE language = :language AND id NOT IN (SELECT affixId FROM word_parts WHERE language = :language AND affixId IS NOT NULL)")
    suspend fun deleteUnusedAffixes(language: String)

    @Query("""
        SELECT p.position, p.glossKo, p.affixId, p.baseWordId,
               COALESCE(a.form, b.word, '') AS label,
               COALESCE(a.meaningKo, b.meaningKo, '') AS meaningKo
        FROM word_parts p
        LEFT JOIN affixes a ON a.id = p.affixId AND a.language = p.language
        LEFT JOIN words b ON b.id = p.baseWordId AND b.language = p.language AND b.deleted = 0
        WHERE p.language = :language AND p.wordId = :wordId
        ORDER BY p.position
    """)
    suspend fun partsForWord(language: String, wordId: Long): List<AffixPartRow>

    @Query("""
        SELECT DISTINCT w.* FROM word_parts p
        JOIN words w ON w.id = p.wordId AND w.language = p.language
        WHERE p.language = :language AND p.affixId = :affixId AND w.deleted = 0
        ORDER BY w.seq
    """)
    suspend fun wordsWithAffix(language: String, affixId: Long): List<WordEntity>

    @Query("""
        SELECT DISTINCT w.* FROM word_parts p
        JOIN words w ON w.id = p.wordId AND w.language = p.language
        WHERE p.language = :language AND p.baseWordId = :baseWordId AND w.deleted = 0
        ORDER BY w.seq
    """)
    suspend fun wordsWithBase(language: String, baseWordId: Long): List<WordEntity>

    @Query("SELECT * FROM affixes WHERE language = :language AND id = :id LIMIT 1")
    suspend fun affixById(language: String, id: Long): AffixEntity?

    @Query("SELECT * FROM words WHERE language = :language AND id = :id AND deleted = 0 LIMIT 1")
    suspend fun baseById(language: String, id: Long): WordEntity?
}
