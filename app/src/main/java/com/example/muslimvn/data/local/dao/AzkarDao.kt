package com.example.muslimvn.data.local.dao

import androidx.room.*
import com.example.muslimvn.data.local.entities.AzkarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AzkarDao {
    @Query("SELECT * FROM azkar")
    fun getAllAzkar(): Flow<List<AzkarEntity>>

    @Query("SELECT * FROM azkar WHERE category = :category")
    fun getAzkarByCategory(category: String): Flow<List<AzkarEntity>>

    @Query("SELECT * FROM azkar WHERE isFavorite = 1")
    fun getFavoriteAzkar(): Flow<List<AzkarEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAzkarList(azkarList: List<AzkarEntity>)

    @Update
    suspend fun updateAzkar(azkar: AzkarEntity)

    @Query("UPDATE azkar SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Int, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM azkar")
    suspend fun getAzkarCount(): Int

    @Query("SELECT DISTINCT category FROM azkar ORDER BY category ASC")
    fun getCategories(): Flow<List<String>>
}
