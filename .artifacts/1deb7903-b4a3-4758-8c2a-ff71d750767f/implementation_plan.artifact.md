# Phân trang cho danh sách Podcast

Thay đổi này sẽ áp dụng Paging 3 để hiển thị danh sách các tập Podcast cho từng học giả, giúp ứng dụng mượt mà hơn khi xử lý các feed RSS có hàng trăm tập.

## User Review Required

> [!IMPORTANT]
> Việc áp dụng Paging 3 sẽ thay đổi cách dữ liệu được tải từ Room. Thay vì tải toàn bộ danh sách `List<PodcastEpisode>`, chúng ta sẽ tải theo từng trang (ví dụ 20 tập/lần).

## Proposed Changes

### [Data Layer]

Cập nhật DAO và Repository để hỗ trợ PagingSource.

#### [MODIFY] [PodcastEpisodeDao.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/data/local/dao/PodcastEpisodeDao.kt)
- Thêm phương thức trả về `PagingSource<Int, PodcastEpisodeEntity>`.

#### [MODIFY] [PodcastRepository.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/domain/repository/PodcastRepository.kt)
- Thêm phương thức `getEpisodesByScholarPaging` trả về `Flow<PagingData<PodcastEpisode>>`.

#### [MODIFY] [PodcastRepositoryImpl.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/data/repository/PodcastRepositoryImpl.kt)
- Triển khai phương thức Paging bằng cách sử dụng `Pager`.

---

### [Presentation Layer]

Cập nhật ViewModel và UI để sử dụng PagingData.

#### [MODIFY] [ScholarDetailViewModel.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/presentation/viewmodels/ScholarDetailViewModel.kt)
- Thay đổi `uiState` để bỏ danh sách `episodes` cố định.
- Thêm `val episodesPagingData: Flow<PagingData<PodcastEpisode>>`.

#### [MODIFY] [ScholarDetailScreen.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/presentation/screens/ScholarDetailScreen.kt)
- Sử dụng `collectAsLazyPagingItems()` để hiển thị danh sách.
- Cập nhật `items` trong `LazyColumn` để làm việc với `LazyPagingItems`.

## Verification Plan

### Automated Tests
- Kiểm tra build thành công sau khi thêm thư viện Paging.
- Kiểm tra logcat xem có lỗi khi cuộn danh sách không.

### Manual Verification
- Mở màn hình chi tiết của một học giả có nhiều tập (ví dụ Hamza Yusuf).
- Cuộn xuống dưới cùng để đảm bảo các tập cũ hơn được tải thêm.
- Kiểm tra tính năng Phát/Tạm dừng vẫn hoạt động đúng với các tập trong danh sách phân trang.
