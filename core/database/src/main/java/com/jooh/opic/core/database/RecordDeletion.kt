package com.jooh.opic.core.database

import androidx.room.withTransaction

/** 학습 기록 종류 (학습 기록 관리 화면의 탭). */
enum class RecordKind(val label: String) { WORDS("단어"), GRAMMAR("문법"), SPEAKING("스피킹"), SHADOWING("섀도잉") }

/**
 * 사용자가 고른 기록만 한 트랜잭션에서 지운다 (2026-10-10). 항상 그 언어 행만 지우고, 단어 데이터(words)는 지우지 않는다.
 * SQLite 변수 개수 제한 때문에 500개씩 나눠 지운다. 지운 행 수를 돌려준다.
 */
suspend fun deleteRecords(db: OpicDatabase, language: String, kind: RecordKind, keys: List<String>): Int = db.withTransaction {
    keys.distinct().chunked(500).sumOf { chunk ->
        when (kind) {
            RecordKind.WORDS -> db.userWordDao().deleteRecords(language, chunk.map(String::toLong))
            RecordKind.GRAMMAR -> db.grammarReviewDao().deleteRecords(language, chunk)
            RecordKind.SPEAKING -> db.speakingDao().deleteRecords(language, chunk.map(String::toLong))
            RecordKind.SHADOWING -> db.shadowingAttemptDao().deleteRecords(language, chunk.map(String::toLong))
        }
    }
}
