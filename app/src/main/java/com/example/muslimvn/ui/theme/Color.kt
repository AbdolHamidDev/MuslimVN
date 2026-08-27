package com.example.muslimvn.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// MuslimVN brand palette — "Teal & Brass"
// Xanh ngọc (teal) lấy trực tiếp từ icon app (#107065, khớp launcher icon)
// kết hợp cùng vàng đồng (brass) lấy cảm hứng từ nghệ thuật trang trí Hồi giáo.
// Các role trung tính được sinh theo thuật toán Material 3 (TONAL_SPOT,
// material-color-utilities, seed #107065) đảm bảo tương phản WCAG AA.
// ─────────────────────────────────────────────────────────────────────────────

// Brand anchors
val TealDeep = Color(0xFF107065)        // primary thương hiệu (light) = màu icon
val TealBright = Color(0xFF82D5C8)      // primary thương hiệu (dark)
val BrassGold = Color(0xFFEEC148)       // accent vàng đồng (dark/decorative)
val BrassDeep = Color(0xFF755A00)       // accent vàng đồng (light/text)

// ── Light scheme ──
val md_light_primary = TealDeep
val md_light_on_primary = Color(0xFFFFFFFF)
val md_light_primary_container = Color(0xFF9EF2E3)
val md_light_on_primary_container = Color(0xFF005048)
val md_light_secondary = Color(0xFF4A635E)
val md_light_on_secondary = Color(0xFFFFFFFF)
val md_light_secondary_container = Color(0xFFCCE8E2)
val md_light_on_secondary_container = Color(0xFF334B47)
val md_light_tertiary = BrassDeep
val md_light_on_tertiary = Color(0xFFFFFFFF)
val md_light_tertiary_container = Color(0xFFFFE08D)
val md_light_on_tertiary_container = Color(0xFF241A00)
val md_light_error = Color(0xFFBA1A1A)
val md_light_on_error = Color(0xFFFFFFFF)
val md_light_error_container = Color(0xFFFFDAD6)
val md_light_on_error_container = Color(0xFF410002)

// ⚡ GOOGLE 2026 DESIGN SYSTEM:
// Nền màn hình (background) mặc định dùng surface_container để tạo độ sâu.
// Thẻ (surface) mặc định dùng trắng tinh để nổi bật trên nền container.
val md_light_background = Color(0xFFE9EFED)          // Đồng bộ với surface_container
val md_light_on_background = Color(0xFF161D1B)
val md_light_surface = Color(0xFFFFFFFF)             // Surface luôn trắng tinh
val md_light_on_surface = Color(0xFF161D1B)
val md_light_surface_variant = Color(0xFFDAE5E1)
val md_light_on_surface_variant = Color(0xFF3F4946)
val md_light_outline = Color(0xFF6F7977)
val md_light_outline_variant = Color(0xFFBEC9C5)
val md_light_inverse_surface = Color(0xFF2B3230)
val md_light_inverse_on_surface = Color(0xFFECF2EF)
val md_light_inverse_primary = TealBright

// Surface container roles (Material 3)
val md_light_surface_dim = Color(0xFFD5DBD9)
val md_light_surface_bright = Color(0xFFF4FBF8)
val md_light_surface_container_lowest = Color(0xFFFFFFFF)
val md_light_surface_container_low = Color(0xFFEFF5F2)
val md_light_surface_container = Color(0xFFE9EFED)
val md_light_surface_container_high = Color(0xFFE3EAE7)
val md_light_surface_container_highest = Color(0xFFDDE4E1)

// ── Dark scheme ──
val md_dark_primary = TealBright
val md_dark_on_primary = Color(0xFF003731)
val md_dark_primary_container = Color(0xFF005048)
val md_dark_on_primary_container = Color(0xFF9EF2E3)
val md_dark_secondary = Color(0xFFB1CCC6)
val md_dark_on_secondary = Color(0xFF1C3531)
val md_dark_secondary_container = Color(0xFF334B47)
val md_dark_on_secondary_container = Color(0xFFCCE8E2)
val md_dark_tertiary = BrassGold
val md_dark_on_tertiary = Color(0xFF3F2E00)
val md_dark_tertiary_container = Color(0xFF5B4400)
val md_dark_on_tertiary_container = Color(0xFFFFE08D)
val md_dark_error = Color(0xFFFFB4AB)
val md_dark_on_error = Color(0xFF690005)
val md_dark_error_container = Color(0xFF93000A)
val md_dark_on_error_container = Color(0xFFFFDAD6)

// Dark mode background: dùng container để giảm chói mắt
val md_dark_background = Color(0xFF1A211F)           // Đồng bộ với surface_container
val md_dark_on_background = Color(0xFFDDE4E1)
val md_dark_surface = Color(0xFF0E1513)              // Surface tối hẳn
val md_dark_on_surface = Color(0xFFDDE4E1)
val md_dark_surface_variant = Color(0xFF3F4946)
val md_dark_on_surface_variant = Color(0xFFBEC9C5)
val md_dark_outline = Color(0xFF899390)
val md_dark_outline_variant = Color(0xFF3F4946)
val md_dark_inverse_surface = Color(0xFFDDE4E1)
val md_dark_inverse_on_surface = Color(0xFF2B3230)
val md_dark_inverse_primary = Color(0xFF006B60)

// Surface container roles (Material 3)
val md_dark_surface_dim = Color(0xFF0E1513)
val md_dark_surface_bright = Color(0xFF343B39)
val md_dark_surface_container_lowest = Color(0xFF090F0E)
val md_dark_surface_container_low = Color(0xFF161D1B)
val md_dark_surface_container = Color(0xFF1A211F)
val md_dark_surface_container_high = Color(0xFF252B2A)
val md_dark_surface_container_highest = Color(0xFF303634)

// ── Semantic (extended) colors ──
val md_light_success = Color(0xFF206A33)
val md_light_on_success = Color(0xFFFFFFFF)
val md_light_success_container = Color(0xFFA9F2AF)
val md_light_on_success_container = Color(0xFF00210A)

val md_dark_success = Color(0xFF8BD896)
val md_dark_on_success = Color(0xFF00390F)
val md_dark_success_container = Color(0xFF005320)
val md_dark_on_success_container = Color(0xFFA9F2AF)
