package com.jooh.opic.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [WordEntity::class, UserWordEntity::class, DataMetaEntity::class, GrammarReviewEntity::class, SpeakingAnswerEntity::class, ShadowingAttemptEntity::class], version = 4, exportSchema = true)
@TypeConverters(Converters::class)
abstract class OpicDatabase : RoomDatabase() {
    abstract fun dataMetaDao(): DataMetaDao
    abstract fun wordDao(): WordDao
    abstract fun userWordDao(): UserWordDao
    abstract fun grammarReviewDao(): GrammarReviewDao
    abstract fun speakingDao(): SpeakingDao
    abstract fun shadowingAttemptDao(): ShadowingAttemptDao
}
