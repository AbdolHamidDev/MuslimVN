package com.example.muslimvn.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = md_light_primary,
    onPrimary = md_light_on_primary,
    primaryContainer = md_light_primary_container,
    onPrimaryContainer = md_light_on_primary_container,
    secondary = md_light_secondary,
    onSecondary = md_light_on_secondary,
    secondaryContainer = md_light_secondary_container,
    onSecondaryContainer = md_light_on_secondary_container,
    tertiary = md_light_tertiary,
    onTertiary = md_light_on_tertiary,
    tertiaryContainer = md_light_tertiary_container,
    onTertiaryContainer = md_light_on_tertiary_container,
    error = md_light_error,
    onError = md_light_on_error,
    errorContainer = md_light_error_container,
    onErrorContainer = md_light_on_error_container,
    background = md_light_background,
    onBackground = md_light_on_background,
    surface = md_light_surface,
    onSurface = md_light_on_surface,
    surfaceVariant = md_light_surface_variant,
    onSurfaceVariant = md_light_on_surface_variant,
    outline = md_light_outline,
    outlineVariant = md_light_outline_variant,
    inverseSurface = md_light_inverse_surface,
    inverseOnSurface = md_light_inverse_on_surface,
    inversePrimary = md_light_inverse_primary,
    surfaceDim = md_light_surface_dim,
    surfaceBright = md_light_surface_bright,
    surfaceContainerLowest = md_light_surface_container_lowest,
    surfaceContainerLow = md_light_surface_container_low,
    surfaceContainer = md_light_surface_container,
    surfaceContainerHigh = md_light_surface_container_high,
    surfaceContainerHighest = md_light_surface_container_highest
)

private val DarkColorScheme = darkColorScheme(
    primary = md_dark_primary,
    onPrimary = md_dark_on_primary,
    primaryContainer = md_dark_primary_container,
    onPrimaryContainer = md_dark_on_primary_container,
    secondary = md_dark_secondary,
    onSecondary = md_dark_on_secondary,
    secondaryContainer = md_dark_secondary_container,
    onSecondaryContainer = md_dark_on_secondary_container,
    tertiary = md_dark_tertiary,
    onTertiary = md_dark_on_tertiary,
    tertiaryContainer = md_dark_tertiary_container,
    onTertiaryContainer = md_dark_on_tertiary_container,
    error = md_dark_error,
    onError = md_dark_on_error,
    errorContainer = md_dark_error_container,
    onErrorContainer = md_dark_on_error_container,
    background = md_dark_background,
    onBackground = md_dark_on_background,
    surface = md_dark_surface,
    onSurface = md_dark_on_surface,
    surfaceVariant = md_dark_surface_variant,
    onSurfaceVariant = md_dark_on_surface_variant,
    outline = md_dark_outline,
    outlineVariant = md_dark_outline_variant,
    inverseSurface = md_dark_inverse_surface,
    inverseOnSurface = md_dark_inverse_on_surface,
    inversePrimary = md_dark_inverse_primary,
    surfaceDim = md_dark_surface_dim,
    surfaceBright = md_dark_surface_bright,
    surfaceContainerLowest = md_dark_surface_container_lowest,
    surfaceContainerLow = md_dark_surface_container_low,
    surfaceContainer = md_dark_surface_container,
    surfaceContainerHigh = md_dark_surface_container_high,
    surfaceContainerHighest = md_dark_surface_container_highest
)

/**
 * Màu ngữ nghĩa mở rộng ngoài bộ Material 3 color scheme chuẩn.
 * Truy cập: `MaterialTheme.extendedColors.success`
 */
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color
)

private fun lightExtendedColors() = ExtendedColors(
    success = md_light_success,
    onSuccess = md_light_on_success,
    successContainer = md_light_success_container,
    onSuccessContainer = md_light_on_success_container
)

private fun darkExtendedColors() = ExtendedColors(
    success = md_dark_success,
    onSuccess = md_dark_on_success,
    successContainer = md_dark_success_container,
    onSuccessContainer = md_dark_on_success_container
)

internal val LocalExtendedColors = staticCompositionLocalOf { lightExtendedColors() }

val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current

/**
 * Theme chính của MuslimVN.
 *
 * - Android 12+ (S) và [dynamicColor] bật: dùng Dynamic Color (Material You)
 *   sinh bảng màu từ wallpaper người dùng — đúng hành vi các app Google
 *   (Files/Gmail); nền luôn là tone surfaceContainerLowest rất nhạt/sạch.
 * - Thiết bị cũ hơn: dùng bảng màu thương hiệu "Teal & Brass" tĩnh.
 *
 * @param dynamicColor mặc định BẬT theo yêu cầu thiết kế M3 giống Google apps.
 */
@Composable
fun MuslimVNTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val extendedColors = if (darkTheme) darkExtendedColors() else lightExtendedColors()

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = MuslimVNShapes,
            typography = Typography,
            content = content
        )
    }
}
