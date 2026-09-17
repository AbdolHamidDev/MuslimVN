package com.example.muslimvn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.example.muslimvn.domain.models.AppTheme

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
 * Truy cập: `MaterialTheme.extendedColors.brand` hoặc `MaterialTheme.extendedColors.success`
 */
@Immutable
data class ExtendedColors(
    val brand: Color,
    val onBrand: Color,
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color
)

private fun lightExtendedColors() = ExtendedColors(
    brand = BrandGreen,
    onBrand = Color.White,
    success = md_light_success,
    onSuccess = md_light_on_success,
    successContainer = md_light_success_container,
    onSuccessContainer = md_light_on_success_container
)

private fun darkExtendedColors() = ExtendedColors(
    brand = BrandGreen,
    onBrand = Color.White,
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
 * @param themeMode Chế độ theme được chọn (Hệ thống, Sáng, Tối).
 */
@Composable
fun MuslimVNTheme(
    themeMode: AppTheme = AppTheme.FOLLOW_SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppTheme.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
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
