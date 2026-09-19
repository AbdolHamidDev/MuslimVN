package com.example.muslimvn.domain.repository

import com.example.muslimvn.domain.models.PodcastCategory
import com.example.muslimvn.domain.models.PodcastEpisode
import com.example.muslimvn.domain.models.Scholar
import kotlinx.coroutines.flow.Flow

/**
 * Kho dữ liệu Podcast theo chiến lược OFFLINE-FIRST:
 * - Học giả được nạp một lần từ assets/scholars.json vào Room lúc khởi động app.
 * - Tự động đồng bộ các học giả mới từ xa (Muslim Central) vào Room mà không cần sửa file JSON.
 * - Tập phát được cache vào Room sau khi fetch RSS; UI luôn đọc từ Room (Flow).
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
     * @return Result số lượng tập sau khi merge; failure khi offline/feed lỗi.
     */
    suspend fun refreshEpisodes(scholarId: String): Result<Int>

    suspend fun getEpisodeById(episodeId: String): PodcastEpisode?

    /** Lưu vị trí nghe để lần sau phát tiếp tục. */
    suspend fun savePlaybackPosition(episodeId: String, positionMs: Long)

    /** Cập nhật thông tin chi tiết (tên, bio, ảnh đại diện) của 1 học giả từ RSS channel metadata. */
    suspend fun syncScholarMetadata(scholarId: String): Result<Unit>

    /** Tự động đồng bộ danh sách học giả từ nguồn Muslim Central vào Room DB. */
    suspend fun syncMuslimCentralDirectory(): Result<Int>

    /** Đảo trạng thái yêu thích (Favorite) của 1 tập podcast. */
    suspend fun toggleFavorite(episodeId: String)

    /** Danh sách tất cả các tập podcast đã yêu thích. */
    fun getFavoriteEpisodes(): Flow<List<PodcastEpisode>>

    /** Flow danh sách các episodeId đã yêu thích. */
    fun getFavoriteEpisodeIds(): Flow<List<String>>

    /** Theo dõi trạng thái yêu thích của 1 tập podcast cụ thể. */
    fun isEpisodeFavorite(episodeId: String): Flow<Boolean>

    /** Đảo trạng thái thêm vào danh sách phát (Playlist chờ) của 1 tập podcast. */
    suspend fun togglePlaylist(episodeId: String)

    /** Flow danh sách các tập podcast trong danh sách phát cá nhân. */
    fun getPlaylistEpisodes(): Flow<List<PodcastEpisode>>

    /** Flow danh sách các episodeId nằm trong danh sách phát. */
    fun getPlaylistEpisodeIds(): Flow<List<String>>

    /** Ghi nhận tập podcast bắt đầu phát thành công. */
    suspend fun recordPlayStarted(episodeId: String)

    /** Flow danh sách các tập podcast nghe gần đây (Recently Played), mới nhất xếp trước. */
    fun getRecentlyPlayedEpisodes(limit: Int = 20): Flow<List<PodcastEpisode>>

    /** Xóa 1 mục khỏi lịch sử nghe. */
    suspend fun clearHistoryItem(episodeId: String)

    /** Xóa toàn bộ lịch sử nghe. */
    suspend fun clearAllHistory()
}
