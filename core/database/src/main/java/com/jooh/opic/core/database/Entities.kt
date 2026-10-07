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

/** 문법 문제 간격 복습 (DB v2). exerciseId는 grammar.json의 문제 id. 단어 테이블과 관계 없음. */
@Entity(tableName = "grammar_reviews")
data class GrammarReviewEntity(
    @PrimaryKey val exerciseId: String,
    val unitId: String,
    val stage: Int,
    val dueEpochDay: Long,
    val wrongCount: Int,
    val lastStudiedAt: Long,
)
