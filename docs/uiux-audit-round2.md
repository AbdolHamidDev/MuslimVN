# Audit UI/UX vòng 2 — sau khi fix Checklist 1 & 2

## ✅ Đã xác nhận fix tốt
- Màu hardcode: từ 53 dòng/18 file → còn **2 file** (chi tiết bên dưới, có lý do riêng nên xem lại thay vì xoá vội).
- `Shape.kt` đã thêm token mới, nút Follow ở `ChannelSection.kt` đã dùng `OutlinedButton` thật.
- Commit `0c2587b`, `3893d0d` áp dụng đúng như checklist đã đưa.

Phần dưới đây là **những mảng chưa từng kiểm tra** — độ tương phản, khả năng tiếp cận (accessibility), và kiểu chữ (typography).

---

## 1. Accessibility — 13 nút bấm chức năng không có mô tả cho TalkBack (nghiêm trọng nhất)

`contentDescription = null` là **đúng** cho icon trang trí (Google cũng làm vậy). Nhưng 13 vị trí dưới đây là icon **bên trong `IconButton` có hành động thật** — người dùng khiếm thị dùng TalkBack sẽ nghe "Button" trống, không biết nút làm gì. Đây là lỗi accessibility thật sự, không phải chi tiết thẩm mỹ.

| File:Dòng | Hành động | Nên đổi contentDescription thành |
|---|---|---|
| `components/StateComponents.kt:217` | Quay lại | `"Quay lại"` |
| `components/PrayerReminderBottomSheet.kt:248` | Nghe thử / dừng file | `"Nghe thử"` / `"Dừng"` (tuỳ `isPreviewing`) |
| `screens/AzkarScreen.kt:90` | Xoá ô tìm kiếm | `"Xoá tìm kiếm"` |
| `screens/AzkarScreen.kt:244` | Yêu thích / bỏ yêu thích | `"Thêm vào yêu thích"` / `"Bỏ yêu thích"` |
| `screens/HijriCalendarScreen.kt:216` | Tháng trước | `"Tháng trước"` |
| `screens/HijriCalendarScreen.kt:244` | Tháng sau | `"Tháng sau"` |
| `screens/QuranScreen.kt:121` | Xoá ô tìm kiếm | `"Xoá tìm kiếm"` |
| `screens/QuranScreen.kt:125` | Mở cài đặt | `"Cài đặt"` |
| `screens/PodcastPlayerScreen.kt:244` | Menu thêm | `"Thêm tuỳ chọn"` |
| `screens/PodcastPlayerScreen.kt:346` | Bài trước | `"Bài trước"` |
| `screens/PodcastPlayerScreen.kt:349` | Tua lùi 10s | `"Tua lùi 10 giây"` |
| `screens/PodcastPlayerScreen.kt:380` | Tua tới 10s | `"Tua tới 10 giây"` |
| `screens/PodcastPlayerScreen.kt:383` | Bài kế | `"Bài kế tiếp"` |

Đây chính xác là kiểu chi tiết mà app Google luôn làm đúng nhưng gần như vô hình với người dùng thường — bạn sẽ không tự nhận ra thiếu cho đến khi bật TalkBack thử.

---

## 2. Typography — 26 dòng dùng `fontSize` cứng thay vì `MaterialTheme.typography.*`

Bạn đã xây `Typography` đầy đủ 15 style ở `Type.kt` (giống hệt cách làm `Shape.kt`), nhưng nhiều nơi vẫn set `fontSize` tay thay vì gọi `style = MaterialTheme.typography.xxx`. Hậu quả: các cỡ chữ này **không tự nhất quán** giữa các màn, và nếu sau này đổi type scale toàn app thì các dòng này sẽ bị "bỏ quên".

**Cụm nặng nhất — `components/NextPrayerHero.kt`** (9 dòng: 10sp, 11sp×3, 12sp, 13sp, 32sp, 38sp) — 1 component nhưng dùng tới 6 cỡ chữ khác nhau không theo type scale nào, nên đây là nơi ưu tiên sửa trước.

Các vị trí còn lại:
- `screens/AzkarScreen.kt:259` — 28sp
- `screens/SurahDetailScreen.kt:557,642,718` — 24sp, 17sp, 36sp
- `screens/vietnamscholars/MachZenScreen.kt:597` — 8sp *(nhỏ hơn cả `labelSmall` 11sp — khó đọc, nên cân nhắc bỏ hẳn thay vì chỉ đổi token)*
- `screens/NamesOfAllahScreen.kt:301` — 64sp
- `screens/OnboardingScreen.kt:201,256,267,326,376,412,432` — 15sp, 24sp×1, 16sp×4
- `screens/TrackerScreen.kt:359,370,382` — 12sp (label của `NavigationBarItem`, nên map sang `labelMedium`)
- `screens/SplashScreen.kt:79` — 28sp

**Cách sửa chung:** với mỗi giá trị sp, tìm style gần nhất trong `Type.kt` (vd. 28sp → `headlineMedium` gốc 28sp khớp tuyệt đối; 24sp → `headlineSmall`; 16sp → `bodyLarge`; 12sp → `bodyMedium`/`labelMedium`) rồi thay `fontSize = Nsp` bằng `style = MaterialTheme.typography.xxx` (giữ nguyên `fontWeight` riêng nếu cần bằng `.copy(fontWeight = ...)`).

---

## 3. Touch target — 1 nút bấm nhỏ hơn kích thước tối thiểu Google khuyến nghị (48dp)

`screens/DailyReminderViewerScreen.kt:83`:
```kotlin
IconButton(onClick, Modifier.background(...).size(44.dp)) { ... }
```
Material Design quy định vùng chạm tối thiểu **48×48dp** cho mọi phần tử tương tác (để ngón tay bấm chính xác, đặc biệt quan trọng với người dùng lớn tuổi/vận động khó khăn). 44dp là chuẩn của iOS (Apple HIG), không phải Android — dấu hiệu cho thấy có thể code được tham khảo/copy từ pattern iOS. Sửa: đổi `.size(44.dp)` → `.size(48.dp)`.

---

## 4. Hiệu ứng bấm (ripple) bị tắt ở 1 nơi
`components/NextPrayerHero.kt:193` dùng `indication = null` trên một `clickable`. Nếu đây là phần tử có thể bấm (không phải chỉ để chặn double-click), việc tắt ripple khiến người dùng bấm mà không thấy phản hồi thị giác — cảm giác "app bị đứng" dù thực ra vẫn hoạt động. Nên xem lại context, nếu là element tương tác thật thì bỏ `indication = null` đi.

---

## 5. Hai file còn màu hardcode — có thể là chủ đích, cần bạn xác nhận
- `components/MushafView.kt:49` — nền `#FBF8EF` (màu giấy ngà) cho khung đọc Mushaf (Kinh Quran). Nhiều app đọc kinh sách (kể cả Google Play Books) **cố tình** dùng nền giấy riêng, không theo dark/light theme hệ thống, để mô phỏng cảm giác đọc giấy thật.
- `screens/DailyReminderViewerScreen.kt` (nhiều dòng) — toàn bộ màn dùng gradient xanh đậm riêng, không lấy theo `colorScheme`.

**Nếu đây là chủ đích** (chế độ đọc/story độc lập với theme), nên: đặt tên rõ ràng (vd. `object MushafReaderColors { val paper = Color(0xFFFBF8EF) }`) để người đọc code sau này biết đây là ngoại lệ có ý thức, không phải sót lại. Nếu không phải chủ đích, nên đưa vào theme như các phần khác.

---

## Tổng kết ưu tiên

1. **Accessibility (mục 1)** — ưu tiên cao nhất, ảnh hưởng người dùng thật, sửa nhanh (chỉ đổi text).
2. **Touch target 44dp → 48dp (mục 3)** — 1 dòng, không rủi ro.
3. **Typography (mục 2)** — bắt đầu từ `NextPrayerHero.kt` vì tập trung nhiều nhất.
4. **Ripple (mục 4)** — cần xem context trước khi sửa.
5. **2 file màu riêng (mục 5)** — không bắt buộc sửa, chỉ cần đặt tên rõ chủ đích.
