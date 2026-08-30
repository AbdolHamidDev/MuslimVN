package com.example.muslimvn.domain.repository

import com.example.muslimvn.domain.models.PodcastCategory
import com.example.muslimvn.domain.models.PodcastEpisode
import com.example.muslimvn.domain.models.Scholar
import kotlinx.coroutines.flow.Flow

/**
 * Kho dữ liệu Podcast theo chiến lược OFFLINE-FIRST:
 * - Học giả được nạp một lần từ assets/scholars.json vào Room lúc khởi động app.
 * - Tập phát được cache vào Room sau khi fetch RSS; UI luôn đọc từ Room (Flow)
 *   nên mở màn hình là thấy dữ liệu cũ ngay, mạng chỉ để làm mới nền.
 */
interface PodcastRepository {

    /** Nạp seed học giả từ assets vào Room nếu bảng còn trống. Gọi 1 lần ở Application. */
    suspend fun initializeData()

    fun getScholars(): Flow<List<Scholar>>

    suspend fun getScholarById(id: String): Scholar?

    /** Danh sách phân loại (Tafsir, Fiqh…) dùng cho FilterChips — đọc từ asset, cache bộ nhớ. */
    suspend fun getCategories(): List<PodcastCategory>

    /** Flow tập phát của một học giả — phát lại tức thì từ cache Room. */
    fun getEpisodesByScholar(scholarId: String): Flow<List<PodcastEpisode>>

    /** Paging: Tải danh sách tập theo từng trang để tối ưu hiệu năng. */
    fun getEpisodesByScholarPaging(scholarId: String): Flow<androidx.paging.PagingData<PodcastEpisode>>

    /**
     * Fetch + parse RSS feed của học giả rồi ghi đè cache trong Room.
     * @return Result số lượng tập sau khi merge; failure khi offline/feed lỗi
     * (cache cũ trong Room vẫn nguyên vẹn).
     */
    suspend fun refreshEpisodes(scholarId: String): Result<Int>

    suspend fun getEpisodeById(episodeId: String): PodcastEpisode?

    /** Lưu vị trí nghe để lần sau phát tiếp tục. */
    suspend fun savePlaybackPosition(episodeId: String, positionMs: Long)
}
