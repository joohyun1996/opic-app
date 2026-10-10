package com.jooh.opic.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: 문법 간격 복습 테이블만 추가한다. 기존 words / user_words / data_meta는 건드리지 않는다.
 * SQL은 schemas/…/2.json의 createSql과 같아야 한다 (Room이 열 때 스키마를 검증한다).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `grammar_reviews` (`exerciseId` TEXT NOT NULL, `unitId` TEXT NOT NULL, " +
                "`stage` INTEGER NOT NULL, `dueEpochDay` INTEGER NOT NULL, `wrongCount` INTEGER NOT NULL, " +
                "`lastStudiedAt` INTEGER NOT NULL, PRIMARY KEY(`exerciseId`))",
        )
    }
}

/**
 * v2 → v3 (TASK 22): 스피킹 답변·섀도잉 연습 기록 테이블과 인덱스만 추가한다. 기존 테이블은 건드리지 않는다.
 * SQL은 schemas/…/3.json의 createSql 그대로.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf(
            "CREATE TABLE IF NOT EXISTS `speaking_answers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `language` TEXT NOT NULL, `questionId` TEXT NOT NULL, `topicId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `durationMs` INTEGER NOT NULL, `transcript` TEXT NOT NULL, `editedText` TEXT NOT NULL, `wordCount` INTEGER NOT NULL, `wordsPerMinute` INTEGER NOT NULL, `fillerCount` INTEGER NOT NULL, `sentenceCount` INTEGER NOT NULL, `mockId` INTEGER)",
            "CREATE INDEX IF NOT EXISTS `index_speaking_answers_language_questionId` ON `speaking_answers` (`language`, `questionId`)",
            "CREATE INDEX IF NOT EXISTS `index_speaking_answers_language_mockId` ON `speaking_answers` (`language`, `mockId`)",
            "CREATE TABLE IF NOT EXISTS `shadowing_attempts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `language` TEXT NOT NULL, `videoId` TEXT NOT NULL, `sentence` TEXT NOT NULL, `heard` TEXT NOT NULL, `matchRate` REAL NOT NULL, `createdAt` INTEGER NOT NULL)",
            "CREATE INDEX IF NOT EXISTS `index_shadowing_attempts_language_videoId` ON `shadowing_attempts` (`language`, `videoId`)",
        ).forEach(db::execSQL)
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
