package com.jooh.opic.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DataMetaDao {
    @Query("SELECT value FROM data_meta WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(meta: DataMetaEntity)

    @Query("UPDATE data_meta SET value = :value WHERE `key` = :key")
    suspend fun update(key: String, value: String)
}
