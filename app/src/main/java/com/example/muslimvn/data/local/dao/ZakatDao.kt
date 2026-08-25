package com.example.muslimvn.data.local.dao

import androidx.room.*
import com.example.muslimvn.data.local.entities.ZakatHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ZakatDao {
    @Query("SELECT * FROM zakat_history ORDER BY date DESC")
    fun getAllHistory(): Flow<List<ZakatHistoryEntity>>

    @Query("SELECT * FROM zakat_history ORDER BY date DESC LIMIT 1")
    suspend fun getLatestRecord(): ZakatHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ZakatHistoryEntity)

    @Delete
    suspend fun deleteRecord(record: ZakatHistoryEntity)
}
