package com.jooh.opic.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

/** 백업용: id 대신 (language, word)로. */
data class UserWordBackupRow(val language: String, val word: String, val correctCount: Int, val wrongCount: Int, val lastStudiedAt: Long?)

@Dao
interface UserWordDao {
    @Query("SELECT w.language AS language, w.word AS word, u.correctCount AS correctCount, u.wrongCount AS wrongCount, u.lastStudiedAt AS lastStudiedAt FROM user_words u JOIN words w ON w.id = u.wordId WHERE w.language = :language")
    suspend fun backupRows(language: String): List<UserWordBackupRow>

    @Query("SELECT * FROM user_words WHERE wordId = :wordId LIMIT 1")
    suspend fun get(wordId: Long): UserWordEntity?

    @Insert
    suspend fun insert(word: UserWordEntity)

    @Update
    suspend fun update(word: UserWordEntity)

    @Transaction
    suspend fun recordResult(wordId: Long, correct: Boolean, now: Long) {
        val current = get(wordId)
        if (current == null) {
            insert(UserWordEntity(wordId, if (correct) 1 else 0, if (correct) 0 else 1, now))
        } else {
            update(current.copy(
                correctCount = current.correctCount + if (correct) 1 else 0,
                wrongCount = current.wrongCount + if (correct) 0 else 1,
                lastStudiedAt = now,
            ))
        }
    }

    /** "맞았어요": 방금 오답으로 기록한 것을 정답으로 바꾼다. 기록이 없거나 오답이 0이면 아무것도 하지 않는다. */
    @Transaction
    suspend fun correctLastWrong(wordId: Long, now: Long): Boolean {
        val current = get(wordId) ?: return false
        if (current.wrongCount <= 0) return false
        update(current.copy(correctCount = current.correctCount + 1, wrongCount = current.wrongCount - 1, lastStudiedAt = now))
        return true
    }

    // ---- 통계 (TASK 39): 필요한 열만 읽는다 ----
    @Query("SELECT u.lastStudiedAt FROM user_words u JOIN words w ON w.id = u.wordId WHERE w.language = :language AND u.lastStudiedAt IS NOT NULL")
    suspend fun studiedAt(language: String): List<Long>

    @Query("SELECT COUNT(*) FROM user_words u JOIN words w ON w.id = u.wordId WHERE w.language = :language AND u.correctCount >= 3")
    suspend fun masteredCount(language: String): Int

    @Query("SELECT w.word AS name, u.wrongCount AS count FROM user_words u JOIN words w ON w.id = u.wordId WHERE w.language = :language AND u.wrongCount > 0 ORDER BY u.wrongCount DESC, w.word LIMIT :limit")
    suspend fun mostWrong(language: String, limit: Int = 5): List<KeyCount>
}
