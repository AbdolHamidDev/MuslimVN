# Nhiệm vụ triển khai: UI/UX Audit Round 4 — MuslimVN

## Bối cảnh
Round 1–3 đã fix xong: màu/shape/button, accessibility/typography/touch-target, và 7 hiệu ứng "cảm giác Google" (animateItem, pull-to-refresh, scroll app bar, dynamic color, predictive back, shared element). Đã xác nhận bằng cách pull code thật và grep lại.

Round 4 gồm 2 phần: **(A) hoàn thiện nốt 2 việc round 3 làm chưa hết**, và **(B) 5 hạng mục mới**, ưu tiên từ trên xuống.

---

## PHẦN A — Hoàn thiện nốt Round 3

### A1. `animateItem()` mới phủ 10/48 khối `items()` — còn thiếu nhiều

Đếm lại: toàn dự án có 48 khối `items(...)`/`itemsIndexed(...)`, mới có 10 khối gắn `.animateItem()`. Không cần làm hết 48 (nhiều khối là danh sách tĩnh, filter chip, danh sách trong dialog không cần animation), nhưng các màn sau **rõ ràng có thao tác thêm/xoá/lọc item** và nên có:

- `ScholarDetailScreen.kt` — danh sách tập (episode), có thể thêm vào playlist/yêu thích
- `PodcastPlaylistScreen.kt` — danh sách playlist, người dùng xoá/sắp xếp lại
- `ZakatScreen.kt` — nếu có danh sách lịch sử tính toán có thể xoá

Cách làm giống hệt round 3, chỉ cần thêm `.animateItem()` vào đầu `Modifier` của item Composable. Gemini cần tự grep `items(` trong 3 file trên, kiểm tra `key = {...}` đã có chưa (nếu chưa có `key` thì phải thêm trước, nếu không `animateItem()` sẽ không hoạt động đúng).

### A2. Loading state: mới giảm từ 33 → 31 `CircularProgressIndicator`, còn nhiều nơi chưa đổi sang shimmer

Nhiệm vụ 4 ở round 3 gần như chưa được đụng tới. Gemini cần:
1. Grep toàn bộ `CircularProgressIndicator(` còn lại (31 vị trí).
2. Với mỗi vị trí, đọc context xung quanh: nếu nó đang che **toàn bộ nội dung danh sách lúc tải lần đầu** (thường nằm trong 1 `Box` full màn hình, điều kiện `if (isLoading) { CircularProgressIndicator(...) }`) → đổi sang component shimmer đã có sẵn trong `StateComponents.kt`.
3. Giữ nguyên `CircularProgressIndicator` cho: nút bấm đang xử lý, loading cuối danh sách khi kéo thêm trang (pagination), dialog xác nhận.

### A3. 4 file TopAppBar còn thiếu `scrollBehavior` — xác nhận rồi mới bỏ qua

`StateComponents.kt`, `DocumentReaderScreen.kt`, `QiblaScreen.kt`, `PodcastPlayerScreen.kt` chưa có `scrollBehavior`. `PodcastPlayerScreen.kt` đã xác nhận hợp lý để bỏ qua (full-screen player). Với 3 file còn lại, Gemini cần đọc code để xác nhận:
- Nếu màn hình có nội dung cuộn dài (`DocumentReaderScreen.kt` — đọc tài liệu dài) → **cần thêm** `scrollBehavior` giống round 3.
- Nếu màn hình gần như tĩnh, không cuộn (`QiblaScreen.kt` — la bàn Qibla, `StateComponents.kt` — có thể chỉ là empty/error state) → **được phép bỏ qua**, nhưng cần ghi chú lý do vào commit message.

---

## PHẦN B — Hạng mục mới

### B1. Responsive/Adaptive layout cho tablet & màn gập (ưu tiên cao nhất — hiện tại 0% hỗ trợ)

**Hiện trạng:** Dự án chưa dùng `WindowSizeClass` hay `NavigationSuiteScaffold` ở đâu cả. App hiện chỉ có 1 layout cố định cho mọi kích thước màn hình — trên tablet, thanh điều hướng dưới vẫn hiện như điện thoại, danh sách vẫn 1 cột full-width. Đây là điểm khác biệt lớn nhất so với app Google thật (Gmail, Files, Photos đều tự chuyển layout khi mở trên tablet/màn gập).

**Bước 1 — Thêm dependency** (kiểm tra `libs.versions.toml`, nếu chưa có thì thêm):
```toml
androidx-material3-adaptive-navigation-suite = { group = "androidx.compose.material3", name = "material3-adaptive-navigation-suite", version = "..." }
androidx-window-core = { group = "androidx.window", name = "window-core", version = "..." }
```
(Gemini tự tra phiên bản mới nhất tương thích Compose BOM `2026.02.01` khi thêm.)

**Bước 2 — Đổi thanh điều hướng chính trong `MainActivity.kt`** (nơi đang dùng `NavigationBar(`):
```kotlin
// TRƯỚC
NavigationBar {
    items.forEach { item -> NavigationBarItem(...) }
}

// SAU
NavigationSuiteScaffold(
    navigationSuiteItems = {
        items.forEach { item ->
            item(
                selected = ...,
                onClick = { ... },
                icon = { Icon(...) },
                label = { Text(...) }
            )
        }
    }
) {
    // nội dung NavHost hiện tại
}
```
`NavigationSuiteScaffold` tự động chọn `NavigationBar` (điện thoại), `NavigationRail` (tablet/màn ngang), hoặc `NavigationDrawer` (màn rất lớn) tuỳ kích thước — đúng hệt cách Gmail/Files chuyển đổi.

**Bước 3 — Layout 2 cột cho tablet ở các màn danh sách → chi tiết** (ví dụ `PodcastLibraryScreen` → chi tiết podcast): dùng `calculateWindowSizeClass(activity)` lấy `WindowWidthSizeClass`, nếu là `Expanded` thì hiển thị danh sách bên trái + chi tiết bên phải trong cùng 1 màn hình thay vì điều hướng sang màn mới. Đây là việc lớn, có thể làm sau nếu thời gian hạn chế — **Bước 1–2 là bắt buộc, Bước 3 là nên có**.

**Tiêu chí hoàn thành:** Chạy trên tablet/emulator màn lớn → thanh điều hướng chuyển thành `NavigationRail` bên cạnh thay vì bottom bar.

---

### B2. Vuốt để xoá nhanh (`SwipeToDismissBox`) — hiện tại 0% hỗ trợ

**Vấn đề:** Các danh sách `PodcastFavoritesScreen.kt`, `DownloadedPodcastsScreen.kt`, `DownloadedVideosScreen.kt` hiện chỉ xoá được qua nút/menu 3 chấm. Gmail, Google Tasks, Google Keep đều cho vuốt ngang item để xoá/lưu trữ nhanh — thao tác 1 chạm thay vì mở menu.

```kotlin
val dismissState = rememberSwipeToDismissBoxState(
    confirmValueChange = { value ->
        if (value == SwipeToDismissBoxValue.EndToStart) {
            onRemoveClick(item) // gọi hàm xoá/bỏ tải hiện có trong ViewModel
            true
        } else false
    }
)

SwipeToDismissBox(
    state = dismissState,
    backgroundContent = {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Xoá", modifier = Modifier.padding(16.dp))
        }
    }
) {
    PodcastListItem(item = item, modifier = Modifier.animateItem())
}
```

**Áp dụng cho:** `PodcastFavoritesScreen.kt` (bỏ yêu thích), `DownloadedPodcastsScreen.kt` (xoá file đã tải), `DownloadedVideosScreen.kt` (xoá file đã tải). Không áp dụng cho danh sách điều hướng thuần (Podcast Library, Quran, Azkar) vì vuốt để xoá ở đó không có ý nghĩa nghiệp vụ.

---

### B3. Tooltip cho các nút chỉ có icon, không có label

**Vấn đề:** App có nhiều `IconButton` chỉ hiện icon (Cài đặt, Yêu thích, Tua 10s...). Google luôn thêm tooltip hiện khi nhấn giữ, giúp người dùng lần đầu hiểu icon nghĩa là gì mà không cần đoán.

```kotlin
TooltipBox(
    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
    tooltip = { PlainTooltip { Text("Cài đặt") } },
    state = rememberTooltipState()
) {
    IconButton(onClick = onSettingsClick) {
        Icon(Icons.Default.Settings, contentDescription = "Cài đặt")
    }
}
```

**Phạm vi:** Áp dụng cho các `IconButton` đã được thêm `contentDescription` ở round 2 (danh sách 13 vị trí trong file `hardcode-color-checklist.md`/audit round 2 cũ) — vì các vị trí đó đã xác định rõ là "hành động thật cần mô tả", nên cùng logic đó nên có thêm tooltip.

---

### B4. Chuẩn hoá thời lượng animation theo token, không dùng số tuỳ hứng

**Hiện trạng:** `tween(300)`, `tween(400)` (×3), `tween(500)`, `tween(600)` (×2), `tween(1000)` — 5 giá trị khác nhau, không theo quy tắc nào.

**Chuẩn Material 3 Motion** dùng 3 mức: nhanh (100–200ms, cho phản hồi tức thời như bật/tắt), vừa (250–400ms, cho chuyển màn/mở rộng), chậm (400–500ms+, cho hiệu ứng lớn/phức tạp). Việc cần làm:
1. Tạo file `ui/theme/Motion.kt` với các hằng số:
```kotlin
object MuslimVNMotion {
    const val DURATION_SHORT = 150
    const val DURATION_MEDIUM = 300
    const val DURATION_LONG = 500
}
```
2. Thay các `tween(300/400/500/600/1000)` rải rác bằng 1 trong 3 hằng số trên (chọn giá trị gần nhất về mặt ý nghĩa: hiệu ứng nhỏ dùng `DURATION_SHORT`, chuyển cảnh dùng `DURATION_MEDIUM`, hiệu ứng đặc biệt/nổi bật — như `tween(1000)` đang dùng ở đâu đó — dùng `DURATION_LONG` hoặc xem xét rút ngắn lại nếu 1000ms là quá chậm cho UI thông thường, để lại quyết định cho Gemini sau khi đọc context).

**Tiêu chí hoàn thành:** Toàn bộ animation trong app có cảm giác "cùng một nhịp", không có chỗ nhanh chỗ chậm bất thường khi so 2 màn hình cạnh nhau.

---

### B5. FloatingActionButton — cân nhắc, không bắt buộc

**Ghi chú:** Toàn app hiện có **0** `FloatingActionButton`. Đây không hẳn là lỗi — không phải app nào cũng cần FAB. Nhưng nếu có màn hình nào có 1 hành động "thêm mới" rõ ràng (ví dụ: thêm giờ nhắc nhở cầu nguyện tuỳ chỉnh trong `PrayerNotificationsScreen.kt`, thêm masjid yêu thích trong `MasjidListScreen.kt`), Google sẽ dùng FAB thay vì nút trong `TopAppBar` hoặc nút cuối danh sách.

**Yêu cầu cho Gemini:** Tự đọc 2 màn trên, nếu đang có nút "Thêm" nằm trong `TopAppBar actions` hoặc cuối danh sách → cân nhắc chuyển thành FAB (`FloatingActionButton` với icon `Icons.Default.Add`, đặt trong `Scaffold(floatingActionButton = {...})`). Nếu không tìm thấy hành động "thêm mới" nào thực sự phù hợp, **không cần thêm FAB gượng ép** — không phải màn nào cũng cần.

---

## Ghi chú chung
- Vẫn giữ nguyên convention: build lại sau mỗi mục, không gộp nhiều thay đổi lớn.
- B1 (adaptive layout) là việc nặng nhất round này — nên tách branch riêng như đã làm với Shared Element ở round 3.
- Sau round này, phần còn lại chủ yếu là polish nhỏ lẻ theo từng màn hình cụ thể — nên bắt đầu chuyển sang chế độ "review từng màn hình một" thay vì audit toàn app, vì các vấn đề mang tính hệ thống lớn đã được xử lý gần hết.
