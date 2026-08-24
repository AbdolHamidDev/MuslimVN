package com.example.muslimvn.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants

/**
 * Hiệu ứng nhấn "bouncy" + phản hồi haptic nhẹ — ngôn ngữ chuyển động chuẩn
 * Material 3 giống các app Google: phần tử lún xuống nhẹ (scale ~0.96) bằng
 * spring tự nhiên và rung tick rất khẽ khi ngón tay chạm xuống.
 *
 * Dùng cho các ô/nút quan trọng (tile Tiện ích, hàng danh sách…);
 * nút bấm thường của M3 đã có ripple riêng thì không cần áp thêm.
 */
@Composable
fun Modifier.bouncyClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "bouncyClickScale"
    )

    // Haptic lực nhẹ ngay lúc chạm xuống (CLOCK_TICK là tick khẽ nhất phổ biến).
    // API 27+ có CLOCK_TICK; máy cũ hơn fallback sang KEYBOARD_TAP.
    val view = LocalView.current
    androidx.compose.runtime.LaunchedEffect(pressed) {
        if (pressed) {
            val constant = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                HapticFeedbackConstants.CLOCK_TICK
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
            view.performHapticFeedback(constant)
        }
    }

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled,
            onClick = onClick
        )
}