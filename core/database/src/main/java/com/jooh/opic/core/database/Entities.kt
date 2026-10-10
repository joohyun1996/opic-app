package com.jooh.opic.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "words", indices = [Index(value = ["language", "word"], unique = true), Index(value = ["language", "seq"], unique = true)])
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val language: String,
    val word: String,
    val seq: Int,
    val phonetic: String,
    val meaningKo: String,
    val meaningEn: String,
    val example: String,
    val exampleKo: String,
    val level: Int,
    val category: String,
    val partOfSpeech: String,
    val collocations: List<String> = emptyList(),
    val deleted: Boolean = false,
)

@Entity(tableName = "user_words", foreignKeys = [ForeignKey(entity = WordEntity::class, parentColumns = ["id"], childColumns = ["wordId"], onDelete = ForeignKey.NO_ACTION)])
data class UserWordEntity(
    @PrimaryKey val wordId: Long,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val lastStudiedAt: Long? = null,
)

@Entity(tableName = "data_meta")
data class DataMetaEntity(@PrimaryKey val key: String, val value: String)

/** 문법 문제 간격 복습 (DB v2, v4에서 language 추가). 키는 (language, exerciseId). 단어 테이블과 관계 없음. */
@Entity(tableName = "grammar_reviews", primaryKeys = ["language", "exerciseId"])
data class GrammarReviewEntity(
    val language: String,
    val exerciseId: String,
    val unitId: String,
    val stage: Int,
    val dueEpochDay: Long,
    val wrongCount: Int,
    val lastStudiedAt: Long,
)

/** 스피킹 답변 기록 (DB v3, TASK 22). 녹음 파일은 저장하지 않는다. mockId는 같은 모의고사의 답변끼리 같은 값. */
@Entity(tableName = "speaking_answers", indices = [Index(value = ["language", "questionId"]), Index(value = ["language", "mockId"])])
data class SpeakingAnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val language: String,
    val questionId: String,
    val topicId: String,
    val createdAt: Long,
    val durationMs: Long,
    val transcript: String,
    val editedText: String,
    val wordCount: Int,
    val wordsPerMinute: Int,
    val fillerCount: Int,
    val sentenceCount: Int,
    val mockId: Long? = null,
)

/** 섀도잉 한 번 따라 말한 기록 (DB v3, TASK 22). */
@Entity(tableName = "shadowing_attempts", indices = [Index(value = ["language", "videoId"])])
data class ShadowingAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val language: String,
    val videoId: String,
    val sentence: String,
    val heard: String,
    val matchRate: Double,
    val createdAt: Long,
)
