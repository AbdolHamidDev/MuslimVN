package com.example.muslimvn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.muslimvn.data.local.entities.PodcastEpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastEpisodeDao {

    /** Tập phát của một học giả, mới nhất lên đầu — phát liên tục tới UI qua Room Flow. */
    @Query("SELECT * FROM podcast_episodes WHERE scholarId = :scholarId ORDER BY pubDate DESC")
    fun getEpisodesByScholar(scholarId: String): Flow<List<PodcastEpisodeEntity>>

    @Query("SELECT * FROM podcast_episodes WHERE scholarId = :scholarId ORDER BY pubDate DESC")
    fun getEpisodesByScholarPaging(scholarId: String): androidx.paging.PagingSource<Int, PodcastEpisodeEntity>

    /** Bản chụp tức thời (không Flow) — dùng để merge dữ liệu mới từ RSS giữ lại vị trí nghe. */
    @Query("SELECT * FROM podcast_episodes WHERE scholarId = :scholarId")
    suspend fun getEpisodesByScholarOnce(scholarId: String): List<PodcastEpisodeEntity>

    @Query("SELECT * FROM podcast_episodes WHERE id = :episodeId")
    suspend fun getEpisodeById(episodeId: String): PodcastEpisodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<PodcastEpisodeEntity>)

    /** Chỉ cập nhật cột vị trí nghe — không đụng các trường metadata khác. */
    @Query("UPDATE podcast_episodes SET lastPositionMs = :positionMs WHERE id = :episodeId")
    suspend fun updateLastPosition(episodeId: String, positionMs: Long)

    @Query("SELECT COUNT(*) FROM podcast_episodes WHERE scholarId = :scholarId")
    suspend fun countByScholar(scholarId: String): Int
}
