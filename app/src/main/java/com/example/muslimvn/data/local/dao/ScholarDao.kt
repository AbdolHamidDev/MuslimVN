package com.example.muslimvn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.muslimvn.data.local.entities.ScholarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScholarDao {

    /** Danh sách học giả: nổi bật lên đầu, trong mỗi nhóm sắp theo tên. */
    @Query("SELECT * FROM scholars ORDER BY featured DESC, name COLLATE NOCASE ASC")
    fun getAllScholars(): Flow<List<ScholarEntity>>

    @Query("SELECT * FROM scholars WHERE id = :id")
    suspend fun getScholarById(id: String): ScholarEntity?

    @Query("SELECT COUNT(*) FROM scholars")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScholars(scholars: List<ScholarEntity>)
}
