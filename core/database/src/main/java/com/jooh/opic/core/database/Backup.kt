package com.jooh.opic.core.database

import androidx.room.withTransaction
import com.jooh.opic.core.common.StudyLanguages
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 학습 기록 백업 파일 (TASK 22). 단어는 id가 아니라 (language, word)로 저장해 재설치 뒤에도 맞게 복원한다. */
@Serializable
data class BackupFile(
    val backupVersion: Int = 1,
    val exportedAt: Long,
    val userWords: List<BackupUserWord> = emptyList(),
    val grammarReviews: List<BackupGrammarReview> = emptyList(),
    val speakingAnswers: List<BackupSpeakingAnswer> = emptyList(),
    val shadowingAttempts: List<BackupShadowingAttempt> = emptyList(),
)
@Serializable data class BackupUserWord(val language: String, val word: String, val correctCount: Int, val wrongCount: Int, val lastStudiedAt: Long? = null)
@Serializable data class BackupGrammarReview(val language: String = "en", val exerciseId: String, val unitId: String, val stage: Int, val dueEpochDay: Long, val wrongCount: Int, val lastStudiedAt: Long)
@Serializable data class BackupSpeakingAnswer(
    val language: String, val questionId: String, val topicId: String, val createdAt: Long, val durationMs: Long, val transcript: String,
    val editedText: String, val wordCount: Int, val wordsPerMinute: Int, val fillerCount: Int, val sentenceCount: Int, val mockId: Long? = null,
)
@Serializable data class BackupShadowingAttempt(val language: String, val videoId: String, val sentence: String, val heard: String, val matchRate: Double, val createdAt: Long)

data class RestoreResult(val userWords: Int, val skippedWords: Int, val grammarReviews: Int, val speakingAnswers: Int, val shadowingAttempts: Int)

class BackupManager(private val db: OpicDatabase, private val languages: List<String> = StudyLanguages.codes) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    suspend fun export(now: Long): String = json.encodeToString(BackupFile.serializer(), BackupFile(
        exportedAt = now,
        userWords = languages.flatMap { lang -> db.userWordDao().backupRows(lang).map { BackupUserWord(it.language, it.word, it.correctCount, it.wrongCount, it.lastStudiedAt) } },
        grammarReviews = languages.flatMap { lang -> db.grammarReviewDao().all(lang).map { BackupGrammarReview(it.language, it.exerciseId, it.unitId, it.stage, it.dueEpochDay, it.wrongCount, it.lastStudiedAt) } },
        speakingAnswers = languages.flatMap { lang -> db.speakingDao().all(lang).map {
            BackupSpeakingAnswer(it.language, it.questionId, it.topicId, it.createdAt, it.durationMs, it.transcript, it.editedText,
                it.wordCount, it.wordsPerMinute, it.fillerCount, it.sentenceCount, it.mockId)
        } },
        shadowingAttempts = languages.flatMap { lang -> db.shadowingAttemptDao().all(lang).map {
            BackupShadowingAttempt(it.language, it.videoId, it.sentence, it.heard, it.matchRate, it.createdAt)
        } },
    ))

    /** 형식이 틀리면 null을 돌려주고 아무것도 바꾸지 않는다. 같은 파일을 두 번 복원해도 늘어나지 않는다. */
    suspend fun restore(raw: String): RestoreResult? {
        val file = runCatching { json.decodeFromString(BackupFile.serializer(), raw) }.getOrNull() ?: return null
        if (file.backupVersion != 1) return null
        return db.withTransaction {
            var words = 0; var skipped = 0; var reviews = 0; var answers = 0; var attempts = 0
            for (w in file.userWords) {
                val word = db.wordDao().find(w.language, w.word.lowercase())
                if (word == null) { skipped++; continue }
                val current = db.userWordDao().get(word.id)
                val incoming = UserWordEntity(word.id, w.correctCount, w.wrongCount, w.lastStudiedAt)
                when {
                    current == null -> { db.userWordDao().insert(incoming); words++ }
                    (w.lastStudiedAt ?: 0) > (current.lastStudiedAt ?: 0) -> { db.userWordDao().update(incoming); words++ }
                }
            }
            for (r in file.grammarReviews) {
                val current = db.grammarReviewDao().get(r.language, r.exerciseId)
                val incoming = GrammarReviewEntity(r.language, r.exerciseId, r.unitId, r.stage, r.dueEpochDay, r.wrongCount, r.lastStudiedAt)
                when {
                    current == null -> { db.grammarReviewDao().insert(incoming); reviews++ }
                    r.lastStudiedAt > current.lastStudiedAt -> { db.grammarReviewDao().update(incoming); reviews++ }
                }
            }
            for (a in file.speakingAnswers) {
                if (db.speakingDao().countSame(a.language, a.createdAt, a.questionId) > 0) continue
                db.speakingDao().insert(SpeakingAnswerEntity(0, a.language, a.questionId, a.topicId, a.createdAt, a.durationMs, a.transcript,
                    a.editedText, a.wordCount, a.wordsPerMinute, a.fillerCount, a.sentenceCount, a.mockId))
                answers++
            }
            for (s in file.shadowingAttempts) {
                if (db.shadowingAttemptDao().countSame(s.language, s.createdAt, s.videoId) > 0) continue
                db.shadowingAttemptDao().insert(ShadowingAttemptEntity(0, s.language, s.videoId, s.sentence, s.heard, s.matchRate, s.createdAt))
                attempts++
            }
            RestoreResult(words, skipped, reviews, answers, attempts)
        }
    }
}
