package com.example.muslimvn.presentation.screens

import androidx.compose.runtime.Composable
import com.example.muslimvn.presentation.screens.vietnamscholars.GosalyAhmadScreen
import com.example.muslimvn.presentation.screens.vietnamscholars.MachZenScreen

/**
 * Màn hình cha (Parent Router Screen) điều phối nội dung chi tiết cho từng học giả Việt Nam.
 * Mỗi học giả sẽ có file và logic xử lý dữ liệu riêng biệt (YouTube, Facebook, Tài liệu, v.v.).
 */
@androidx.media3.common.util.UnstableApi
@Composable
fun VietnamScholarDetailScreen(
    scholarId: String,
    onBackClick: () -> Unit,
    onVideoClick: (String, String, String, String) -> Unit,
    onDocumentClick: (String, String) -> Unit,
    onOpenFullPlayer: () -> Unit
) {
    when (scholarId) {
        "mach_zen" -> {
            MachZenScreen(
                onBackClick = onBackClick,
                onVideoClick = onVideoClick,
                onDocumentClick = onDocumentClick,
                onOpenFullPlayer = onOpenFullPlayer
            )
        }
        "gosaly_ahmad" -> {
            GosalyAhmadScreen(
                onBackClick = onBackClick,
                onVideoClick = onVideoClick,
                onOpenFullPlayer = onOpenFullPlayer
            )
        }
        else -> {
            GosalyAhmadScreen(
                onBackClick = onBackClick,
                onVideoClick = onVideoClick,
                onOpenFullPlayer = onOpenFullPlayer
            )
        }
    }
}
