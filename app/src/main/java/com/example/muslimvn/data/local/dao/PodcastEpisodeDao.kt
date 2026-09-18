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

    @Query("UPDATE podcast_episodes SET isDownloaded = :isDownloaded, localFilePath = :localFilePath, downloadStatus = :downloadStatus WHERE id = :episodeId")
    suspend fun updateDownloadStatus(episodeId: String, isDownloaded: Boolean, localFilePath: String?, downloadStatus: String)

    @Query("SELECT * FROM podcast_episodes WHERE isDownloaded = 1")
    fun getDownloadedEpisodes(): Flow<List<PodcastEpisodeEntity>>

    @Query("SELECT * FROM podcast_episodes WHERE isDownloaded = 1")
    suspend fun getDownloadedEpisodesOnce(): List<PodcastEpisodeEntity>

    @Query("SELECT * FROM podcast_episodes WHERE downloadStatus != 'IDLE'")
    suspend fun getNonIdleEpisodesOnce(): List<PodcastEpisodeEntity>

    @Query("UPDATE podcast_episodes SET isFavorite = :isFavorite, favoritedAt = :favoritedAt WHERE id = :episodeId")
    suspend fun updateFavoriteStatus(episodeId: String, isFavorite: Boolean, favoritedAt: Long)

    @Query("SELECT * FROM podcast_episodes WHERE isFavorite = 1 ORDER BY favoritedAt DESC")
    fun getFavoriteEpisodes(): Flow<List<PodcastEpisodeEntity>>

    @Query("SELECT id FROM podcast_episodes WHERE isFavorite = 1")
    fun getFavoriteEpisodeIds(): Flow<List<String>>

    @Query("SELECT isFavorite FROM podcast_episodes WHERE id = :episodeId")
    fun isEpisodeFavorite(episodeId: String): Flow<Boolean?>

    @Query("UPDATE podcast_episodes SET isInPlaylist = :isInPlaylist, addedToPlaylistAt = :addedToPlaylistAt WHERE id = :episodeId")
    suspend fun updatePlaylistStatus(episodeId: String, isInPlaylist: Boolean, addedToPlaylistAt: Long)

    @Query("SELECT * FROM podcast_episodes WHERE isInPlaylist = 1 ORDER BY addedToPlaylistAt DESC")
    fun getPlaylistEpisodes(): Flow<List<PodcastEpisodeEntity>>

    @Query("SELECT id FROM podcast_episodes WHERE isInPlaylist = 1")
    fun getPlaylistEpisodeIds(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM podcast_episodes WHERE scholarId = :scholarId")
    suspend fun countByScholar(scholarId: String): Int
}
