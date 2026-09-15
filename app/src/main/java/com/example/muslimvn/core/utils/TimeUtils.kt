package com.example.muslimvn.core.utils

import java.util.concurrent.TimeUnit

object TimeUtils {
    fun getRelativeTimeSpanString(timeMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timeMillis

        if (diff < 0) return "vừa xong"

        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val months = days / 30
        val years = days / 365

        return when {
            seconds < 60 -> "vừa xong"
            minutes < 60 -> "$minutes phút trước"
            hours < 24 -> "$hours giờ trước"
            days < 30 -> "$days ngày trước"
            months < 12 -> "$months tháng trước"
            else -> "$years năm trước"
        }
    }
    
    fun formatViewCount(viewCount: Long): String {
        val locale = java.util.Locale("vi", "VN")
        return when {
            viewCount >= 1_000_000 -> String.format(locale, "%.1f Tr lượt xem", viewCount / 1_000_000.0)
            viewCount >= 1_000 -> String.format(locale, "%.1f N lượt xem", viewCount / 1_000.0)
            else -> "$viewCount lượt xem"
        }
    }
}
