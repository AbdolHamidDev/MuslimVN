@file:OptIn(ExperimentalTextApi::class)

package com.example.muslimvn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.muslimvn.R

/**
 * Amiri — kiểu chữ Naskh cổ điển được thiết kế chuyên cho việc dàn trang Quran,
 * hiển thị đẹp dấu chấm và harakat Ả Rập.
 * Giấy phép SIL Open Font License 1.1 (được phép phân phối kèm app).
 */
val ArabicFontFamily = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold)
)

/**
 * Inter — font chữ hiện đại, bo tròn nhẹ chuẩn giao diện Google (Files/Gmail),
 * hỗ trợ đầy đủ dấu tiếng Việt. Dùng 1 file variable font duy nhất và map
 * các trọng lượng qua trục wght (FontVariation) thay vì nhiều file tĩnh.
 * Giấy phép SIL Open Font License 1.1.
 */
val AppFontFamily = FontFamily(
    Font(
        resId = R.font.inter_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        resId = R.font.inter_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        resId = R.font.inter_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        resId = R.font.inter_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    ),
    Font(
        resId = R.font.inter_variable,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    ),
    Font(
        resId = R.font.inter_variable,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(FontVariation.weight(900))
    )
)

// ─────────────────────────────────────────────────────────────────────────────
// Material 3 type scale đầy đủ trên font Inter.
// Text Ả Rập KHÔNG dùng các style này mà dùng [ExtendedTypography] bên dưới,
// vì font Latin hiển thị harakat Ả Rập kém đẹp hơn font chuyên dụng.
// ─────────────────────────────────────────────────────────────────────────────
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.25).sp
    ),
    displaySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Các style text Ả Rập dùng font Amiri — dành riêng cho nội dung Quran
 * (tên surah, bismillah, câu ayah). Không dùng cho text tiếng Việt.
 */
@Immutable
data class ExtendedTypography(
    val arabicDisplay: TextStyle,
    val arabicHeading: TextStyle,
    val arabicAyah: TextStyle,
    val arabicInline: TextStyle
)

private fun defaultExtendedTypography() = ExtendedTypography(
    // Tên surah Ả Rập lớn ở header màn chi tiết
    arabicDisplay = TextStyle(
        fontFamily = ArabicFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 50.sp
    ),
    // Basmalah và các tiêu đề Ả Rập nổi bật
    arabicHeading = TextStyle(
        fontFamily = ArabicFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 46.sp
    ),
    // Thân câu ayah — line height rộng giúp harakat không bị dính nhau
    arabicAyah = TextStyle(
        fontFamily = ArabicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 46.sp
    ),
    // Tên Ả Rập trong list item / inline
    arabicInline = TextStyle(
        fontFamily = ArabicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 34.sp
    )
)

internal val LocalExtendedTypography = staticCompositionLocalOf { defaultExtendedTypography() }

/** Truy cập: `MaterialTheme.extendedTypography.arabicAyah` */
val MaterialTheme.extendedTypography: ExtendedTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedTypography.current
