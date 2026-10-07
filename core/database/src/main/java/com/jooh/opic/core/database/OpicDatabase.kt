package com.jooh.opic.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [WordEntity::class, UserWordEntity::class, DataMetaEntity::class, GrammarReviewEntity::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class OpicDatabase : RoomDatabase() {
    abstract fun dataMetaDao(): DataMetaDao
    abstract fun wordDao(): WordDao
    abstract fun userWordDao(): UserWordDao
    abstract fun grammarReviewDao(): GrammarReviewDao
}
