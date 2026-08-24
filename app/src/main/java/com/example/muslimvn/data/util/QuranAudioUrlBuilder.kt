package com.example.muslimvn.data.util

object QuranAudioUrlBuilder {
    private const val BASE_URL = "https://everyayah.com/data/Alafasy_128kbps"

    fun buildAyahUrl(surahNumber: Int, ayahNumber: Int): String {
        val surahStr = surahNumber.toString().padStart(3, '0')
        val ayahStr = ayahNumber.toString().padStart(3, '0')
        return "$BASE_URL/$surahStr$ayahStr.mp3"
    }
}
