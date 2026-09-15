# Triển khai NewPipeExtractor cho học giả Mách Zên (YouTube)

Kế hoạch này sẽ tích hợp thư viện `NewPipeExtractor` để lấy danh sách video từ kênh YouTube của học giả Mách Zên (@islamlavn) và hiển thị trong ứng dụng.

## Proposed Changes

### Infrastructure & Configuration

#### [MODIFY] [settings.gradle.kts](file:///home/hamid/AndroidStudioProjects/MuslimVN/settings.gradle.kts)
- Thêm kho lưu trữ JitPack để có thể tải thư viện NewPipe.

#### [MODIFY] [libs.versions.toml](file:///home/hamid/AndroidStudioProjects/MuslimVN/gradle/libs.versions.toml)
- Thêm định nghĩa thư viện `NewPipeExtractor`.

#### [MODIFY] [app/build.gradle.kts](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/build.gradle.kts)
- Thêm dependency `newpipe-extractor`.

#### [NEW] [NewPipeDownloader.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/core/network/NewPipeDownloader.kt)
- Triển khai interface `Downloader` của NewPipe bằng `OkHttpClient` hiện có để thực hiện các yêu cầu mạng.

#### [MODIFY] [MuslimApplication.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/MuslimApplication.kt)
- Khởi tạo NewPipe trong `onCreate()`.

### Data Layer

#### [NEW] [YoutubeRepository.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/domain/repository/YoutubeRepository.kt)
- Định nghĩa interface để lấy video từ kênh YouTube.

#### [NEW] [YoutubeRepositoryImpl.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/data/repository/YoutubeRepositoryImpl.kt)
- Triển khai logic trích xuất dữ liệu bằng `NewPipeExtractor`.

#### [MODIFY] [RepositoryModule.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/core/di/RepositoryModule.kt)
- Cung cấp `YoutubeRepository` thông qua Hilt.

### UI Layer

#### [NEW] [VietnamScholarViewModel.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/presentation/viewmodels/VietnamScholarViewModel.kt)
- ViewModel xử lý logic lấy danh sách video và trạng thái tải cho học giả Việt Nam.

#### [MODIFY] [VietnamScholarDetailScreen.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/presentation/screens/VietnamScholarDetailScreen.kt)
- Cập nhật giao diện để hiển thị danh sách video (dạng list hoặc grid) thay vì văn bản giữ chỗ.

## Verification Plan

### Automated Tests
- Chạy ứng dụng và điều hướng vào mục "Mách Zên".
- Kiểm tra Logcat để đảm bảo NewPipeExtractor khởi tạo thành công.

### Manual Verification
- Xác nhận danh sách video được tải về từ kênh `@islamlavn`.
- Nhấn vào video để kiểm tra xem có lấy được thông tin chi tiết (optional: phát audio/video).
