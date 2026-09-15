package com.example.muslimvn.presentation.components

import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Bộ hàm định dạng dùng chung cho tính năng Podcast. */

/** Đường dẫn asset / file / URL -> URI hợp lệ mà Coil và ExoPlayer hiểu. */
fun String.toAndroidAssetUri(): String = when {
    startsWith("file://") || startsWith("http://") || startsWith("https://") || startsWith("content://") -> this
    startsWith("/") -> "file://$this"
    else -> "file:///android_asset/${trimStart('/')}"
}

/** ms -> "M:SS" hoặc "H:MM:SS"; trả "--:--" nếu chưa biết thời lượng. */
fun formatDurationMs(durationMs: Long): String {
    if (durationMs <= 0L) return "--:--"
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

/** Epoch ms -> ngày theo locale hệ thống (vd "21 thg 8, 2024"); chuỗi rỗng nếu 0. */
fun formatPubDate(epochMs: Long): String {
    if (epochMs <= 0L) return ""
    return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date(epochMs))
}

/** Tốc độ phát -> nhãn nút, ví dụ "1.0x", "1.25x". */
fun formatSpeedLabel(speed: Float): String {
    // 1.0 -> "1.0x", 1.5 -> "1.5x", 1.25 -> "1.25x"
    return String.format(Locale.US, "%.2f", speed).removeSuffix("0") + "x"
}
