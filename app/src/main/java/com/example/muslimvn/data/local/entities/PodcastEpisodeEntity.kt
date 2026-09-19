package com.example.muslimvn.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Một tập phát podcast của một học giả, cache từ RSS feed về Room.
 *
 * @param id Hash MD5 của [audioUrl] — ổn định qua các lần refresh nên không sinh trùng.
 * @param duration Thời lượng tập (millisecond); 0 nếu feed không khai báo itunes:duration.
 * @param pubDate Ngày phát hành (epoch millisecond); 0 nếu parse thất bại.
 * @param isDownloaded Tập đã được tải offline chưa (đặt chỗ cho tính năng tải về).
 * @param lastPositionMs Vị trí dừng gần nhất (ms) để phát tiếp tục — 0 nếu nghe mới.
 */
@Entity(
    tableName = "podcast_episodes",
    // Cố ý KHÔNG dùng ForeignKey: REPLACE lên bảng cha khi reseed sẽ xoá dây chuyền tập đã cache.
    indices = [Index("scholarId")]
)
data class PodcastEpisodeEntity(
    @PrimaryKey val id: String,
    val scholarId: String,
    val title: String,
    val audioUrl: String,
    val artworkUrl: String?,
    val duration: Long,
    val pubDate: Long,
    val description: String,
    val isDownloaded: Boolean,
    val lastPositionMs: Long,
    val localFilePath: String? = null,
    val downloadStatus: String = "IDLE",
    val isFavorite: Boolean = false,
    val favoritedAt: Long = 0L,
    val isInPlaylist: Boolean = false,
    val addedToPlaylistAt: Long = 0L,
    val lastPlayedAt: Long = 0L,
    val playCount: Int = 0
)
