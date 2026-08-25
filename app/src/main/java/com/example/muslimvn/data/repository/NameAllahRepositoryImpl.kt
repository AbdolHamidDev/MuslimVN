package com.example.muslimvn.data.repository

import android.content.Context
import android.util.Log
import com.example.muslimvn.domain.models.AllahName
import com.example.muslimvn.domain.repository.NameAllahRepository
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Repository đọc 99 Danh Xưng của Allah từ `assets/NameAllah.json` bằng Gson.
 *
 * Toàn bộ file nhỏ (~99 mục) nên chiến lược đơn giản nhất là:
 * parse đúng MỘT lần rồi cache trong bộ nhớ ([cachedNames]) — các lần truy cập
 * sau trả kết quả ngay không cần I/O. Việc parse được bảo vệ bằng [Mutex] để
 * tránh hai coroutine cùng parse đồng thời khi màn hình được mở nhanh liên tiếp.
 */
class NameAllahRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NameAllahRepository {

    private val gson = Gson()

    @Volatile
    private var cachedNames: List<AllahName>? = null

    private val cacheMutex = Mutex()

    override suspend fun getAllNames(): List<AllahName> {
        // Fast-path không khoá cho lần truy cập sau khi đã cache
        cachedNames?.let { return it }

        return cacheMutex.withLock {
            cachedNames ?: parseFromAssets().also { cachedNames = it }
        }
    }

    /** Đọc + parse file JSON trong assets trên luồng IO; lỗi thì trả danh sách rỗng. */
    private suspend fun parseFromAssets(): List<AllahName> = withContext(Dispatchers.IO) {
        runCatching {
            context.assets.open(JSON_FILE).bufferedReader().use { reader ->
                gson.fromJson(reader, NameAllahFileDto::class.java)
                    .names
                    .map { dto ->
                        AllahName(
                            number = dto.number,
                            nameArabic = dto.arabic,
                            transliteration = dto.transliteration,
                            meaningVietnamese = dto.vietnamese,
                            description = dto.description
                        )
                    }
            }
        }.getOrElse { error ->
            Log.e(TAG, "Không thể đọc $JSON_FILE từ assets", error)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "NameAllahRepository"
        private const val JSON_FILE = "NameAllah.json"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DTO phản ánh cấu trúc JSON gốc (chỉ dùng nội bộ cho việc parse bằng Gson).
// Các giá trị mặc định giúp parse không nổ khi thiếu trường phụ.
// ─────────────────────────────────────────────────────────────────────────────

/** Root object: { "title": ..., "description": ..., "names": [...] } */
private data class NameAllahFileDto(
    val title: String? = null,
    val description: String? = null,
    val names: List<NameAllahEntryDto> = emptyList()
)

/** Một mục danh xưng: { "number", "arabic", "transliteration", "vietnamese", "description" } */
private data class NameAllahEntryDto(
    val number: Int = 0,
    @SerializedName("arabic") val arabic: String = "",
    val transliteration: String = "",
    @SerializedName("vietnamese") val vietnamese: String = "",
    val description: String = ""
)
