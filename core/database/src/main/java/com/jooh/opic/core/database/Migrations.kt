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

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
