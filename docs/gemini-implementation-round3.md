# Nhiệm vụ triển khai: UI/UX Audit Round 3 — MuslimVN

## Bối cảnh dự án (đọc trước khi làm)

- Package gốc: `com.example.muslimvn`
- Ngôn ngữ: Kotlin + Jetpack Compose, kiến trúc MVVM (ViewModel + StateFlow), Hilt DI
- Compose BOM: `2026.02.01` (rất mới — mọi API dùng trong file này đều đã ổn định, KHÔNG cần thêm dependency ngoài nào)
- `compileSdk = 37`, `minSdk = 26`, `targetSdk = 35`
- Theme: `app/src/main/java/com/example/muslimvn/ui/theme/` (`Theme.kt`, `Color.kt`, `Type.kt`, `Shape.kt`, `CategoryColors.kt`)
- 2 round audit trước đã fix xong: màu hardcode, shape lệch chuẩn, phân cấp Button, accessibility contentDescription, typography, touch target. **Không động lại các phần đó.**

Nhiệm vụ round này là 7 mục dưới đây, đã sắp theo thứ tự ưu tiên (làm từ trên xuống). Mỗi mục ghi rõ: file cần sửa, code mẫu, và tiêu chí hoàn thành để tự kiểm tra.

---

## Nhiệm vụ 1 — Thêm `Modifier.animateItem()` cho mọi danh sách (ưu tiên cao nhất, rủi ro thấp nhất)

**Vấn đề:** Khi thêm/xoá/lọc item trong `LazyColumn`/`LazyRow`, các item còn lại nhảy vị trí đột ngột thay vì trượt mượt.

**Tin tốt:** Hầu hết các `items(...)` trong dự án đã có tham số `key = { ... }` — đây là điều kiện bắt buộc để `animateItem()` hoạt động đúng, nên không cần sửa thêm gì ở phần `key`.

**Cách làm:** Với mỗi lambda item bên trong `items(...)`/`itemsIndexed(...)`, thêm `.animateItem()` vào đầu chuỗi `Modifier` của Composable gốc (Card/Row/Column bọc ngoài item).

```kotlin
// TRƯỚC
items(filteredPodcasts, key = { it.episode.id }) { item ->
    PodcastListItem(
        item = item,
        modifier = Modifier.fillMaxWidth()
    )
}

// SAU
items(filteredPodcasts, key = { it.episode.id }) { item ->
    PodcastListItem(
        item = item,
        modifier = Modifier
            .animateItem()
            .fillMaxWidth()
    )
}
```

**Danh sách file cần sửa (đã xác nhận có `LazyColumn`/`LazyRow` với danh sách nội dung động — ưu tiên các danh sách người dùng có thể thêm/xoá/lọc):**
- `presentation/screens/PodcastLibraryScreen.kt` (dòng ~258, danh sách lịch sử nghe)
- `presentation/screens/DownloadedPodcastsScreen.kt` (dòng ~252)
- `presentation/screens/PodcastFavoritesScreen.kt` (dòng ~180)
- `presentation/screens/DownloadedVideosScreen.kt` (dòng ~100, ~130 — 2 danh sách video/audio)
- `presentation/screens/AzkarScreen.kt` (dòng ~107, ~202 — danh sách azkar có filter theo category)
- `presentation/screens/QuranScreen.kt` (dòng ~158, có ô tìm kiếm lọc danh sách surah)
- `presentation/screens/MasjidListScreen.kt` (dòng ~276)
- `presentation/screens/HomeScreen.kt` (dòng ~296)

Các `LazyColumn`/`LazyRow` còn lại trong dự án (component `PdfViewer.kt`, `ReciterSelectionDialog.kt`, các danh sách tĩnh không đổi trong lúc dùng) không bắt buộc, có thể bỏ qua để tiết kiệm thời gian.

**Tiêu chí hoàn thành:** Bấm nút yêu thích/xoá 1 item trong danh sách đã sửa → item biến mất kèm animation trượt mượt, không giật.

---

## Nhiệm vụ 2 — Pull-to-refresh cho các màn danh sách chính

**Vấn đề:** 0 màn hình nào hỗ trợ "kéo xuống để làm mới".

**API dùng:** `androidx.compose.material3.pulltorefresh.PullToRefreshBox` (đã có sẵn trong `androidx.compose.material3`, không cần thêm dependency).

**Yêu cầu trước khi code:** Kiểm tra ViewModel tương ứng đã có hàm `refresh()`/`reload()` và 1 `StateFlow<Boolean>` tên `isRefreshing` (hoặc tương đương) chưa. Nếu chưa có, cần thêm vào ViewModel trước (gọi lại use case fetch dữ liệu, set `isRefreshing = true` lúc bắt đầu, `false` khi xong).

```kotlin
@Composable
fun PodcastLibraryScreen(viewModel: PodcastLibraryViewModel = hiltViewModel()) {
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn {
            // nội dung danh sách hiện tại giữ nguyên
        }
    }
}
```

**Áp dụng cho các màn (theo thứ tự ưu tiên):**
1. `PodcastLibraryScreen.kt` — màn danh sách podcast chính
2. `HomeScreen.kt` — trang chủ
3. `AzkarScreen.kt`
4. `QuranScreen.kt` (nếu danh sách surah có load từ mạng/DB, không cần nếu là dữ liệu tĩnh đóng gói sẵn trong app)

**Không cần áp dụng:** `DownloadedPodcastsScreen.kt`, `DownloadedVideosScreen.kt`, `PodcastFavoritesScreen.kt` — đây là dữ liệu local (đã tải máy/đã lưu), kéo refresh không có ý nghĩa vì không có gì để "làm mới" từ server.

**Tiêu chí hoàn thành:** Kéo xuống ở đầu danh sách → hiện vòng xoay loading chuẩn Material 3, dữ liệu load lại, vòng xoay biến mất khi xong.

---

## Nhiệm vụ 3 — TopAppBar co giãn khi cuộn (`scrollBehavior`)

**Hiện trạng:** 19 file dùng `TopAppBar`, nhưng chỉ 2 file (`SurahDetailScreen.kt`, `SettingsScreen.kt`) đã gắn `scrollBehavior`. 17 file còn lại thanh tiêu đề đứng yên khi cuộn.

**Cách làm chuẩn (áp dụng cho từng file):**

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastLibraryScreen(...) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Thư viện Podcast") },
                scrollBehavior = scrollBehavior
                // giữ nguyên các tham số actions/navigationIcon hiện có
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) { ... }
    }
}
```

**Chọn kiểu `scrollBehavior` theo loại màn hình:**
- Màn danh sách chính (cuộn nhiều) → `TopAppBarDefaults.enterAlwaysScrollBehavior()` (bar ẩn khi cuộn xuống, hiện lại khi cuộn lên — kiểu Gmail)
- Màn chi tiết dài (đọc nội dung) → `TopAppBarDefaults.exitUntilCollapsedScrollBehavior()` (dùng kèm `LargeTopAppBar`)
- Màn ngắn, ít nội dung cuộn (Settings, dialog-like screens) → `TopAppBarDefaults.pinnedScrollBehavior()` (bar đứng yên nhưng đổi màu nền khi cuộn, không cần đổi từ `TopAppBar` thường)

**Danh sách 17 file cần thêm `scrollBehavior`:**
`StateComponents.kt`, `DocumentReaderScreen.kt`, `zakat/ZakatScreen.kt`, `HijriCalendarScreen.kt`, `QiblaScreen.kt`, `QuranSettingsScreen.kt`, `vietnamscholars/MachZenScreen.kt`, `vietnamscholars/GosalyAhmadScreen.kt`, `RoadmapCategoryScreen.kt`, `MasjidListScreen.kt`, `UtilitiesScreen.kt`, `NamesOfAllahScreen.kt`, `DownloadedVideosScreen.kt`, `PodcastPlayerScreen.kt`, `TrackerScreen.kt`, `PrayerCalculationDetailsScreen.kt`, `PrayerNotificationsScreen.kt`

**Lưu ý riêng:** `PodcastPlayerScreen.kt` là màn phát nhạc full-screen, thường KHÔNG cần bar co giãn (giữ pinned) — nếu Gemini thấy layout đặc thù (không có nội dung cuộn dài), có thể bỏ qua file này và ghi chú lại.

**Tiêu chí hoàn thành:** Cuộn danh sách xuống → thanh tiêu đề ẩn/co lại mượt mà; cuộn lên → hiện lại. Không có giật/nhảy layout.

---

## Nhiệm vụ 4 — Đồng nhất Loading State: ưu tiên Shimmer hơn Spinner cho tải nội dung danh sách

**Hiện trạng:** Dự án đã có shimmer (trong `StateComponents.kt`, dùng ở `MachZenScreen.kt`, `GosalyAhmadScreen.kt`, `QuranScreen.kt`, `HomeScreen.kt`, `TrackerScreen.kt`) — đây là component đã có sẵn, không cần viết lại từ đầu. Nhưng còn 33 vị trí dùng `CircularProgressIndicator`.

**Việc cần làm:**
1. Tìm hàm/component shimmer đã có sẵn trong `StateComponents.kt` (ví dụ tên dạng `ShimmerListPlaceholder`, `LoadingShimmer` — Gemini cần tự đọc file này để biết tên chính xác và cách gọi).
2. Quét toàn bộ 33 vị trí `CircularProgressIndicator( )`, phân loại:
   - **Giữ nguyên spinner** nếu dùng cho: nút bấm đang xử lý (loading trong Button/Dialog), tải trang tiếp theo cuối danh sách (pagination), hành động ngắn hạn khác.
   - **Đổi sang shimmer đã có** nếu dùng để che toàn bộ màn hình/toàn bộ danh sách khi tải nội dung lần đầu.

**Tiêu chí hoàn thành:** Mở màn hình danh sách lần đầu (khi cache trống) → thấy khung xương mờ (shimmer) đúng hình dạng item thật, không phải vòng xoay ở giữa màn hình trống trơn.

---

## Nhiệm vụ 5 — Dynamic Color (Material You) — có tuỳ chọn bật/tắt

**File cần sửa:** `ui/theme/Theme.kt`, và màn `SettingsScreen.kt` (thêm 1 switch mới).

**Bước 1 — Thêm state lưu lựa chọn người dùng** (dùng chung cơ chế `AppTheme`/DataStore đã có sẵn cho theme sáng/tối trong dự án — Gemini cần đọc `SettingsViewModel.kt` để biết cách lưu preference hiện tại và làm tương tự cho biến `useDynamicColor: Boolean`).

**Bước 2 — Sửa `MuslimVNTheme` trong `Theme.kt`:**
```kotlin
@Composable
fun MuslimVNTheme(
    themeMode: AppTheme = AppTheme.FOLLOW_SYSTEM,
    useDynamicColor: Boolean = false, // mặc định TẮT để giữ brand green
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = when (themeMode) {
        AppTheme.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S // Android 12+

    val colorScheme = when {
        useDynamicColor && supportsDynamicColor && darkTheme -> dynamicDarkColorScheme(context)
        useDynamicColor && supportsDynamicColor && !darkTheme -> dynamicLightColorScheme(context)
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
```

**Bước 3 — Thêm switch trong `SettingsScreen.kt`:**
- Label: "Dùng màu theo hình nền máy (Material You)"
- Mô tả phụ: "Chỉ khả dụng trên Android 12 trở lên"
- Ẩn/disable switch nếu `Build.VERSION.SDK_INT < Build.VERSION_CODES.S`
- Mặc định: **tắt** (giữ brand green #055136 làm trải nghiệm gốc)

**Lưu ý quan trọng cho Gemini:** Khi `useDynamicColor = true`, các màu ngữ nghĩa mở rộng (`extendedColors.success`, `extendedColors.warning`...) từ `CategoryColors.kt` VẪN giữ nguyên brand — không bị dynamic color ghi đè, vì chúng nằm ngoài `colorScheme` chuẩn.

**Tiêu chí hoàn thành:** Bật switch trên máy Android 12+ → đổi hình nền → mở lại app → màu primary/surface đổi theo hình nền. Tắt switch → quay lại brand green như cũ.

---

## Nhiệm vụ 6 — Predictive Back Gesture (Android 14+)

**Bước 1 — Kiểm tra/thêm vào `AndroidManifest.xml`:**
```xml
<application
    android:enableOnBackInvokedCallback="true"
    ... >
```
(Gemini cần đọc file `AndroidManifest.xml` hiện tại trước — hiện chưa có thuộc tính này, cần thêm vào thẻ `<application>` đã tồn tại, không tạo thẻ mới.)

**Bước 2 — Với các nơi đang dùng `BackHandler` tuỳ chỉnh** (ví dụ đóng bottom sheet, thoát chế độ xem toàn màn hình video/PDF):
```kotlin
// TRƯỚC
BackHandler(enabled = isSheetOpen) {
    isSheetOpen = false
}

// SAU — dùng PredictiveBackHandler để lấy được animation progress
PredictiveBackHandler(enabled = isSheetOpen) { progress ->
    try {
        progress.collect { backEvent ->
            // (tuỳ chọn nâng cao) dùng backEvent.progress để animate sheet theo cử chỉ vuốt
        }
        isSheetOpen = false // hoàn tất khi vuốt xong
    } catch (e: CancellationException) {
        // người dùng huỷ vuốt giữa chừng — không làm gì, giữ nguyên trạng thái
    }
}
```

**Phạm vi tối thiểu cần làm:** Chỉ cần Bước 1 (bật cờ trong Manifest) là toàn bộ navigation mặc định của `Navigation Compose` trong app đã tự động có hiệu ứng xem trước khi vuốt lùi — vì thư viện Navigation đã hỗ trợ sẵn predictive back, không cần code thêm gì cho phần điều hướng giữa các màn hình chính.

Bước 2 (dùng `PredictiveBackHandler`) chỉ cần thiết cho các `BackHandler` tùy chỉnh đang chặn nút back ở cấp component (bottom sheet, fullscreen player) — Gemini cần tự tìm bằng cách grep `BackHandler(` trong dự án và xử lý từng chỗ.

**Tiêu chí hoàn thành:** Trên máy/emulator Android 14+, vuốt từ cạnh màn hình để quay lại → thấy màn hình hiện tại thu nhỏ/mờ dần lộ ra màn phía sau, thay vì biến mất đột ngột.

---

## Nhiệm vụ 7 — Shared Element Transition (danh sách → chi tiết)

**Đây là việc nặng nhất, làm sau cùng.** Cần bọc navigation graph trong `SharedTransitionLayout` và đánh dấu các Composable ảnh bìa bằng `Modifier.sharedElement()` + `rememberSharedContentState(key = ...)` — key phải khớp giữa Composable ở màn danh sách và Composable tương ứng ở màn chi tiết (thường dùng ID của item làm key).

**Phạm vi đề xuất (chỉ làm 2 luồng có giá trị nhất, không cần làm hết toàn app):**
1. `PodcastLibraryScreen.kt` (ảnh bìa podcast trong danh sách) → `ScholarDetailScreen.kt`/`PodcastPlayerScreen.kt` (ảnh bìa lớn ở màn chi tiết/player)
2. `HomeScreen.kt` (ảnh học giả) → `ScholarDetailScreen.kt` (ảnh học giả ở header chi tiết)

**Yêu cầu kỹ thuật:** Cần biết cấu trúc `NavHost`/`NavGraph` hiện tại của dự án trước khi code (tìm file `NavGraph.kt`/`AppNavigation.kt` hoặc tương đương) — vì `SharedTransitionLayout` phải bọc ở cấp `NavHost`, không phải ở từng màn hình riêng lẻ.

**Gợi ý cho Gemini:** Đây là thay đổi có rủi ro layout cao nhất trong 7 nhiệm vụ — nên tạo thành 1 commit/branch riêng, và giữ nguyên khả năng rollback dễ dàng nếu animation bị lỗi ở 1 số kích thước màn hình.

**Tiêu chí hoàn thành:** Bấm vào ảnh bìa ở danh sách → ảnh đó phóng to mượt mà thành ảnh ở màn chi tiết (không phải màn chi tiết hiện ra độc lập rồi ảnh mới load riêng).

---

## Ghi chú chung cho Gemini khi triển khai

- Sau mỗi nhiệm vụ, build lại project (`./gradlew assembleDebug`) trước khi sang nhiệm vụ tiếp theo — không gộp nhiều thay đổi lớn cùng lúc rồi mới build.
- Giữ nguyên toàn bộ theme tokens (`colorScheme`, `extendedColors`, `MuslimVNShapes`, `Typography`) đã có — 7 nhiệm vụ này KHÔNG đụng đến hệ màu/shape/typography đã chuẩn hoá ở 2 round trước.
- Nếu 1 file không đủ thông tin để quyết định (ví dụ không rõ ViewModel có sẵn hàm refresh chưa), Gemini nên đọc file ViewModel tương ứng trước khi sửa Composable, thay vì đoán tên hàm.
- Làm theo đúng thứ tự 1 → 7. Nhiệm vụ 1–4 an toàn, nên làm hết trước. Nhiệm vụ 5–7 ảnh hưởng kiến trúc rộng hơn, nên làm riêng từng cái, test kỹ trước khi merge.
