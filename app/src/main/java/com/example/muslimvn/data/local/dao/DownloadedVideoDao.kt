package com.example.muslimvn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.muslimvn.data.local.entities.DownloadedVideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedVideoDao {

    @Query("SELECT * FROM downloaded_videos ORDER BY downloadDate DESC")
    fun getAllDownloadedVideos(): Flow<List<DownloadedVideoEntity>>

    @Query("SELECT * FROM downloaded_videos WHERE mediaType = :mediaType ORDER BY downloadDate DESC")
    fun getDownloadedMediaByType(mediaType: String): Flow<List<DownloadedVideoEntity>>

    @Query("SELECT * FROM downloaded_videos WHERE videoId = :videoId OR id = :videoId OR id LIKE :videoId || '_%'")
    fun getDownloadedMediaByVideoId(videoId: String): Flow<List<DownloadedVideoEntity>>

    @Query("SELECT * FROM downloaded_videos")
    suspend fun getAllDownloadedVideosOnce(): List<DownloadedVideoEntity>

    @Query("SELECT * FROM downloaded_videos WHERE id = :id")
    suspend fun getDownloadedVideoById(id: String): DownloadedVideoEntity?

    @Query("SELECT * FROM downloaded_videos WHERE (videoId = :videoId OR id = :videoId OR id LIKE :videoId || '_%') AND mediaType = :mediaType LIMIT 1")
    suspend fun getDownloadedMediaByVideoIdAndType(videoId: String, mediaType: String): DownloadedVideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedVideo(video: DownloadedVideoEntity)

    @Query("DELETE FROM downloaded_videos WHERE id = :id")
    suspend fun deleteDownloadedVideo(id: String)
}
