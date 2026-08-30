package com.example.muslimvn.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Hiệu ứng nhấn chuẩn Material 3 (Ripple) — ngôn ngữ giao diện thống nhất
 * của Google. Loại bỏ hiệu ứng co giãn (bouncy) để tiệm cận nhất với
 * các ứng dụng như Gmail, Files.
 */
@Composable
fun Modifier.bouncyClick(
    enabled: Boolean = true,
    @Suppress("UNUSED_PARAMETER") pressedScale: Float = 1f, // Giữ param để không gãy các chỗ gọi cũ, nhưng không dùng để giống Google
    onClick: () -> Unit
): Modifier {
    return this.clickable(
        enabled = enabled,
        onClick = onClick
    )
}