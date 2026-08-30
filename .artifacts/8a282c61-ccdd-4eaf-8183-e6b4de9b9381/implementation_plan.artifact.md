# Đề xuất Khắc phục Lệch Timing Quran cho các Giọng đọc khác nhau

Vấn đề lệch timing xảy ra do sự không đồng nhất giữa nguồn âm thanh (`EveryAyah.com`) và nguồn dữ liệu timing (`Quran.com`). Mỗi phiên bản thu âm có thể có độ dài khoảng lặng khác nhau ở đầu/cuối mỗi câu (Ayah).

## Giải pháp đề xuất

### 1. Đồng bộ hóa nguồn Âm thanh và Timing
Thay vì dùng `EveryAyah.com` để lấy file MP3, chúng ta sẽ sử dụng trực tiếp URL âm thanh được trả về từ API của `Quran.com`. Điều này đảm bảo file âm thanh và dữ liệu timing luôn khớp nhau 100%.

### 2. Cập nhật Cấu trúc Database và Model
Hiện tại, database chỉ lưu timing và dùng `verseKey` làm khóa chính, dẫn đến việc đổi giọng đọc sẽ gây xung đột dữ liệu cũ. Chúng ta cần:
- Thêm `audioUrl` vào dữ liệu lưu trữ.
- Sử dụng khóa chính kết hợp giữa `verseKey` và `reciterId`.

## Các thay đổi chi tiết

### [Domain Layer]

#### [MODIFY] [VerseTiming.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/domain/models/VerseTiming.kt)
- Thêm thuộc tính `val audioUrl: String?` để chứa link âm thanh đi kèm với timing.

### [Data Layer]

#### [MODIFY] [VerseTimingEntity.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/data/local/entities/VerseTimingEntity.kt)
- Cấu hình lại Primary Key: `@Entity(tableName = "verse_timings", primaryKeys = ["verseKey", "reciterId"])`.
- Thêm cột `val audioUrl: String?`.

#### [MODIFY] [QuranRepositoryImpl.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/data/repository/QuranRepositoryImpl.kt)
- Cập nhật hàm `getVerseTiming`:
    - Lấy `audioUrl` từ `response.verse.audio.url`.
    - Nếu URL là tương đối (không bắt đầu bằng `http`), cần prepend base URL của Quran.com (thường là `https://audio.qurancdn.com/`).
    - Lưu `audioUrl` vào database.

### [Presentation Layer]

#### [MODIFY] [SurahDetailViewModel.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/presentation/viewmodels/SurahDetailViewModel.kt)
- Cập nhật hàm `playAyah`:
    - Thay vì tự xây dựng URL qua `QuranAudioUrlBuilder`, hãy ưu tiên lấy `audioUrl` từ `VerseTiming` đã cache.
    - Nếu chưa có cache, thực hiện fetch timing/audio trước khi phát.

## Kế hoạch kiểm chứng

### Kiểm tra Thủ công
- Chọn giọng đọc khác (ví dụ: Abdul Basit).
- Nhấn "Đồng bộ timing" cho một Surah.
- Phát âm thanh và kiểm tra xem highlight từng chữ (word-by-word) có khớp không.
- Chuyển lại giọng Mishary Rashid Alafasy để đảm bảo không bị hỏng.
