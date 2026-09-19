package com.example.muslimvn.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Hệ bo góc thống nhất toàn app — các thẻ/canh gián tiếp nhau nhẹ nhàng
 * tạo cảm giác mềm mại, thân thiện theo ngôn ngữ Material 3.
 */
val MuslimVNShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val BadgeShape: CornerBasedShape = RoundedCornerShape(4.dp)
val ExtraLarge2Shape: CornerBasedShape = RoundedCornerShape(24.dp)

/**
 * Token bo góc bổ sung cho badge/nhãn nhỏ (4dp)
 */
@Suppress("UnusedReceiverParameter")
val Shapes.badge: CornerBasedShape
    get() = BadgeShape

/**
 * Token bo góc bổ sung cho card/banner cỡ trung-lớn (24dp)
 */
@Suppress("UnusedReceiverParameter")
val Shapes.extraLarge2: CornerBasedShape
    get() = ExtraLarge2Shape


