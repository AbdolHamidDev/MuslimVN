package com.example.muslimvn.domain.models

/**
 * Một Danh Xưng của Allah trong 99 Danh Xưng Đẹp Nhất (Asmaul Husna).
 *
 * @param number Số thứ tự từ 1 đến 99.
 * @param nameArabic Chữ Ả Rập gốc, ví dụ: "الرحمن".
 * @param transliteration Cách đọc phiên âm Latinh, ví dụ: "Ar-Rahman".
 * @param meaningVietnamese Ý nghĩa tiếng Việt, ví dụ: "Đấng Từ Bi Vô Hạn".
 */
data class AllahName(
    val number: Int,
    val nameArabic: String,
    val transliteration: String,
    val meaningVietnamese: String,
    val description: String = ""
)
