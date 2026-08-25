package com.example.muslimvn.data.util

object QuranAudioUrlBuilder {
    private const val BASE_URL = "https://everyayah.com/data"

    fun buildAyahUrl(surahNumber: Int, ayahNumber: Int, reciterIdentifier: String): String {
        val surahStr = surahNumber.toString().padStart(3, '0')
        val ayahStr = ayahNumber.toString().padStart(3, '0')
        return "$BASE_URL/$reciterIdentifier/$surahStr$ayahStr.mp3"
    }
    
    // Giữ lại hàm cũ để tránh lỗi build trong lúc migration
    fun buildAyahUrl(surahNumber: Int, ayahNumber: Int): String {
        return buildAyahUrl(surahNumber, ayahNumber, "Alafasy_128kbps")
    }
}
