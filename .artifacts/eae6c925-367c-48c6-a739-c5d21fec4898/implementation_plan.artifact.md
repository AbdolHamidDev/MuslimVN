# Cập nhật cơ chế xin quyền Thông báo và Vị trí

Tối ưu hóa trải nghiệm người dùng bằng cách xin quyền thông báo ngay khi mở ứng dụng và chỉ xin quyền vị trí khi vào màn hình Qibla. Nếu không có quyền vị trí, ứng dụng sẽ mặc định sử dụng tọa độ của An Giang/TP.HCM.

## Proposed Changes

### [MainActivity](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/MainActivity.kt)
- Thêm logic xin quyền `POST_NOTIFICATIONS` ngay trong `onCreate` (dành cho Android 13+).
- Đảm bảo quyền này được yêu cầu ngay khi người dùng vừa vào app lần đầu.

### [HomeScreen](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/presentation/screens/HomeScreen.kt)
- Loại bỏ `LocationPermissionCard` và toàn bộ logic liên quan đến việc xin quyền vị trí/thông báo tại đây.
- Xóa `permissionLauncher` và các biến trạng thái `deniedAttempts`.

### [QiblaViewModel](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/presentation/viewmodels/QiblaViewModel.kt)
- Cập nhật hàm `loadLocationAndCalculateQibla` để sử dụng tọa độ mặc định (An Giang: 10.7005, 105.1147) nếu không lấy được vị trí thực tế hoặc không có quyền.
- Thêm trạng thái để biết đang dùng vị trí thực hay vị trí mặc định.

### [QiblaScreen](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/presentation/screens/QiblaScreen.kt)
- Điều chỉnh logic hiển thị: Vẫn yêu cầu quyền khi vào màn hình, nhưng cho phép xem Qibla với vị trí mặc định nếu người dùng từ chối hoặc chưa cấp quyền (thay vì chặn ở màn hình xin quyền).

## Verification Plan

### Automated Tests
- Kiểm tra logic fallback tọa độ trong `QiblaViewModel`.

### Manual Verification
1. Cài đặt lại app:
   - Ngay khi mở app, phải có dialog xin quyền thông báo của hệ thống.
   - Trang chủ không được hiện card xin quyền vị trí.
2. Vào màn hình Qibla:
   - Lần đầu vào phải hiện dialog xin quyền vị trí.
   - Nếu từ chối: Màn hình Qibla vẫn hiển thị và tính toán dựa trên An Giang/TP.HCM.
   - Nếu đồng ý: Màn hình Qibla hiển thị theo vị trí thực tế.
