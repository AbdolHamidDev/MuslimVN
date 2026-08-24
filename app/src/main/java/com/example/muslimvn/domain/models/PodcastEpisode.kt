package com.example.muslimvn.domain.models

/**
 * Một tập podcast đã cache (model tầng domain).
 *
 * @param duration Thời lượng (millisecond); 0 nếu feed không khai báo.
 * @param pubDate Ngày phát hành (epoch millisecond); 0 nếu parse thất bại.
 * @param isDownloaded Đặt chỗ cho tính năng tải offline.
 * @param lastPositionMs Vị trí dừng gần nhất để phát tiếp tục; 0 nếu chưa nghe.
 */
data class PodcastEpisode(
    val id: String,
    val scholarId: String,
    val title: String,
    val audioUrl: String,
    val duration: Long,
    val pubDate: Long,
    val description: String,
    val isDownloaded: Boolean,
    val lastPositionMs: Long
)
