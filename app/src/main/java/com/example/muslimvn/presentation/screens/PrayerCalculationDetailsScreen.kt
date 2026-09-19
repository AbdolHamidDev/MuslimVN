package com.example.muslimvn.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.muslimvn.R
import com.example.muslimvn.domain.models.AsrMethod
import com.example.muslimvn.domain.models.PrayerAdjustments
import com.example.muslimvn.domain.models.PrayerTimes
import com.example.muslimvn.presentation.components.PreferenceHeader
import com.example.muslimvn.presentation.viewmodels.PrayerCalculationDetailsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerCalculationDetailsScreen(
    onBackClick: () -> Unit,
    viewModel: PrayerCalculationDetailsViewModel = hiltViewModel()
) {
    val calculationMethod by viewModel.calculationMethod.collectAsState()
    val asrMethod by viewModel.asrMethod.collectAsState()
    val prayerAdjustments by viewModel.prayerAdjustments.collectAsState()
    val prayerTimes by viewModel.prayerTimes.collectAsState()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết cách tính giờ cầu nguyện") },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Intro
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CHI TIẾT CÁCH TÍNH",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "MuslimVN sử dụng tính toán thiên văn để xác định các mốc thời gian trong ngày, sau đó áp dụng cấu hình giờ cầu nguyện của ứng dụng.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // 1. PHƯƠNG PHÁP TÍNH
            item {
                SectionCard(title = "Phương pháp tính") {
                    val profile = prayerTimes?.calculationProfile
                    val fajrAngle = profile?.fajrAngle ?: 18.0
                    val ishaAngle = profile?.ishaAngle ?: 18.0

                    InfoRow(label = "Phương pháp", value = formatMethodName(calculationMethod))
                    InfoRow(label = "Góc Fajr", value = "${fajrAngle.toInt()}°")
                    InfoRow(label = "Góc Isha", value = "${ishaAngle.toInt()}°")
                    InfoRow(label = "Múi giờ", value = "Asia/Ho_Chi_Minh (UTC+7)")

                    val loc = prayerTimes?.location
                    val locLabel = if (loc != null) "Vị trí hiện tại" else "Vị trí mặc định"
                    InfoRow(label = "Địa điểm", value = locLabel)

                    if (loc != null) {
                        val latStr = String.format(Locale.US, "%.4f°", loc.latitude)
                        val lngStr = String.format(Locale.US, "%.4f°", loc.longitude)
                        Text(
                            text = "Tọa độ: $latStr N, $lngStr E",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // 2. CÁC MỐC GIỜ ĐƯỢC TÍNH NHƯ THẾ NÀO?
            item {
                SectionCard(title = "Các mốc giờ được tính như thế nào?") {
                    PrayerDescriptionRow(
                        name = "Fajr (Bình minh)",
                        desc = "Được xác định khi Mặt Trời ở dưới đường chân trời 18° theo cấu hình MuslimVN."
                    )
                    PrayerDescriptionRow(
                        name = "Sunrise (Mặt trời mọc)",
                        desc = "Thời điểm Mặt Trời xuất hiện tại đường chân trời theo mô hình tính toán thiên văn."
                    )
                    PrayerDescriptionRow(
                        name = "Dhuhr (Trưa)",
                        desc = "Bắt đầu sau khi Mặt Trời đi qua điểm thiên đỉnh (zawal/solar transit)."
                    )
                    val asrMethodStr = if (asrMethod == AsrMethod.STANDARD) "Tiêu chuẩn / Shafi'i (Bóng = 1)" else "Hanafi (Bóng = 2)"
                    PrayerDescriptionRow(
                        name = "Asr (Chiều)",
                        desc = "Được tính dựa trên độ dài bóng của vật thể. Phương pháp hiện tại: $asrMethodStr."
                    )
                    PrayerDescriptionRow(
                        name = "Maghrib (Hoàng hôn)",
                        desc = "Được xác định theo thời điểm Mặt Trời lặn về mặt thiên văn."
                    )
                    PrayerDescriptionRow(
                        name = "Isha (Tối)",
                        desc = "Được xác định khi Mặt Trời ở dưới đường chân trời 18° theo cấu hình MuslimVN."
                    )
                }
            }

            // 3. THỜI GIAN CẦU NGUYỆN (WINDOWS)
            item {
                SectionCard(title = "Khoảng thời gian cầu nguyện") {
                    Text(
                        text = "Trong MuslimVN, mỗi mốc cầu nguyện có thời điểm bắt đầu và giới hạn kết thúc rõ ràng:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    WindowBoundaryRow(name = "Fajr", start = "Fajr", end = "Sunrise (Mặt trời mọc)")
                    WindowBoundaryRow(name = "Dhuhr", start = "Dhuhr", end = "Asr")
                    WindowBoundaryRow(name = "Asr", start = "Asr", preferredEnd = "Bóng gấp đôi / Late Asr", finalEnd = "Sunset (Mặt trời lặn)")
                    WindowBoundaryRow(name = "Maghrib", start = "Maghrib", end = "Isha")
                    WindowBoundaryRow(
                        name = "Isha",
                        start = "Isha",
                        preferredEnd = "Nửa đêm Islam",
                        finalEnd = "Fajr tiếp theo (theo cấu hình MuslimVN)"
                    )
                }
            }

            // 4. NỬA ĐÊM ISLAM
            item {
                SectionCard(title = "Nửa đêm Islam (Islamic Midnight)") {
                    Text(
                        text = "Nửa đêm Islam không phải là 00:00 theo giờ dân sự.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "MuslimVN tính nửa đêm Islam bằng điểm giữa khoảng thời gian từ Mặt Trời lặn đến Fajr tiếp theo.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Nửa đêm Islam = Hoàng hôn + (Fajr tiếp theo - Hoàng hôn) / 2",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // 5. THỜI ĐIỂM HẠN CHẾ
            item {
                SectionCard(title = "Thời điểm hạn chế") {
                    Text(
                        text = "MuslimVN theo dõi riêng các khoảng thời điểm thiên văn xung quanh Mặt Trời mọc, Zawal (thiên đỉnh) và Mặt Trời lặn.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Khoảng đệm an toàn của ứng dụng:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Đây là khoảng đệm vận hành do ứng dụng sử dụng để cảnh báo/thể hiện trạng thái, không phải một con số phút cố định được MuslimVN tuyên bố là quy định fiqh phổ quát.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 6. ASR / MADHHAB
            item {
                SectionCard(title = "Phương pháp Asr (Madhhab)") {
                    if (asrMethod == AsrMethod.STANDARD) {
                        Text(
                            text = "Tiêu chuẩn / Shafi'i (Bóng = 1)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Giờ Asr được tính khi bóng của vật thể đạt tỷ lệ theo phương pháp tiêu chuẩn (bóng = 1).",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = "Hanafi (Bóng = 2)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Giờ Asr được tính theo phương pháp Hanafi với hệ số bóng = 2.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Các trường phái và học giả có thể có cách diễn giải khác nhau về một số giới hạn thời gian. MuslimVN cho phép lựa chọn phương pháp tính Asr để phù hợp với cách thực hành của người dùng.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 7. ĐIỀU CHỈNH THỦ CÔNG
            item {
                SectionCard(title = "Điều chỉnh thủ công") {
                    Text(
                        text = formatAdjustmentsDetailed(prayerAdjustments),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Điều chỉnh thủ công chỉ thay đổi thời gian hiển thị/tính toán cuối cùng theo số phút bạn đặt. Nó không thay đổi các góc thiên văn hoặc phương pháp tính cơ bản.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 8. ĐỊA ĐIỂM VÀ MÚI GIỜ
            item {
                SectionCard(title = "Địa điểm và múi giờ") {
                    Text(
                        text = "Giờ cầu nguyện phụ thuộc vào vị trí địa lý và ngày hiện tại.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "MuslimVN sử dụng tọa độ vị trí để tính các sự kiện thiên văn và sử dụng múi giờ địa phương Asia/Ho_Chi_Minh cho Việt Nam.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Việt Nam hiện sử dụng UTC+7 và không áp dụng giờ mùa hè.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // 9. MINH BẠCH VỀ NGUỒN TÍNH
            item {
                SectionCard(title = "Về cách tính") {
                    Text(
                        text = "MuslimVN sử dụng thư viện Adhan làm nền tảng cho các phép tính thiên văn về giờ cầu nguyện. Ứng dụng áp dụng cấu hình MuslimVN và lớp xử lý thời gian riêng để xác định các khoảng giờ cầu nguyện, trạng thái hiện tại và các mốc liên quan.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(label = "Thư viện tính toán", value = "Adhan")
                    InfoRow(label = "Thiên văn", value = "Tính theo vị trí và ngày")
                    InfoRow(label = "Cấu hình mặc định", value = "Fajr 18° / Isha 18°")
                }
            }

            // 10. FIQH DISCLAIMER
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Lưu ý: Giờ cầu nguyện có thể có khác biệt nhỏ giữa các phương pháp tính, vị trí địa lý và cách diễn giải fiqh. MuslimVN cung cấp cấu hình minh bạch để người dùng biết ứng dụng đang sử dụng phương pháp nào.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PrayerDescriptionRow(name: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WindowBoundaryRow(
    name: String,
    start: String,
    preferredEnd: String? = null,
    finalEnd: String? = null,
    end: String? = null
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text("• Bắt đầu: $start", style = MaterialTheme.typography.bodySmall)
        if (preferredEnd != null) {
            Text("• Giới hạn ưu tiên: $preferredEnd", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (finalEnd != null) {
            Text("• Giới hạn cuối: $finalEnd", style = MaterialTheme.typography.bodySmall)
        }
        if (end != null) {
            Text("• Kết thúc: $end", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatMethodName(id: String): String {
    return when (id) {
        "MUSLIMVN_DEFAULT" -> "MuslimVN mặc định (18° / 18°)"
        "MUSLIM_WORLD_LEAGUE" -> "Liên đoàn Thế giới Hồi giáo (MWL)"
        "EGYPTIAN" -> "Ai Cập"
        "KARACHI" -> "Karachi"
        "UMM_AL_QURA" -> "Umm al-Qura"
        "DUBAI" -> "Dubai"
        "MOON_SIGHTING_COMMITTEE" -> "Ủy ban Quan sát Trăng"
        "NORTH_AMERICA" -> "Bắc Mỹ (ISNA)"
        "KUWAIT" -> "Kuwait"
        "QATAR" -> "Qatar"
        "SINGAPORE" -> "Singapore"
        "TURKEY" -> "Thổ Nhĩ Kỳ"
        else -> id
    }
}

private fun formatAdjustmentsDetailed(adjustments: PrayerAdjustments): String {
    val fajr = if (adjustments.fajr >= 0) "+${adjustments.fajr}m" else "${adjustments.fajr}m"
    val dhuhr = if (adjustments.dhuhr >= 0) "+${adjustments.dhuhr}m" else "${adjustments.dhuhr}m"
    val asr = if (adjustments.asr >= 0) "+${adjustments.asr}m" else "${adjustments.asr}m"
    val maghrib = if (adjustments.maghrib >= 0) "+${adjustments.maghrib}m" else "${adjustments.maghrib}m"
    val isha = if (adjustments.isha >= 0) "+${adjustments.isha}m" else "${adjustments.isha}m"
    return "Fajr: $fajr | Dhuhr: $dhuhr | Asr: $asr | Maghrib: $maghrib | Isha: $isha"
}
