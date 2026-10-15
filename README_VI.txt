IMAGEVIDEO BATCH — ANDROID (MÃ NGUỒN)
======================================

Ứng dụng Android xử lý trực tiếp trên điện thoại, không tải media lên máy chủ và không dùng API trả phí.

TÍNH NĂNG
- Thêm nhiều cặp mapping 1–1.
- Mỗi cặp có 1 ảnh và 1 video.
- Xóa từng cặp.
- Chặn dùng lặp cùng URI ảnh/video giữa nhiều cặp.
- Xử lý tuần tự để giảm tải thiết bị.
- 8 giây đầu là ảnh tĩnh.
- 6 giây cuối lấy 6 giây đầu của video nguồn.
- Xuất MP4 9:16, 1080x1920, 30 FPS, H.264, không âm thanh.
- Lưu kết quả trong thư viện video: Movies.

MỞ VÀ BUILD APK
1. Trên máy tính Windows, cài Android Studio:
   https://developer.android.com/studio

2. Cài Android SDK Platform 35 và JDK 17 trong Android Studio SDK Manager.

3. Giải nén ZIP này.
4. Android Studio → Open → chọn thư mục ImageVideoAndroid.
5. Chờ Gradle Sync tải AndroidX Media3 dependencies.
6. Chọn Build > Build Bundle(s) / APK(s) > Build APK(s).
7. APK debug thường nằm ở:
   app/build/outputs/apk/debug/app-debug.apk
8. Chép APK sang điện thoại Android và cài đặt. Có thể cần cho phép cài ứng dụng từ nguồn này.

GHI CHÚ KỸ THUẬT
- Dự án dùng AndroidX Media3 Transformer, xử lý media ngay trên thiết bị.
- Bản nguồn này được tạo nhưng chưa được build/kiểm thử trong Android Studio ở môi trường tạo file.
- API Media3 có thể thay đổi theo phiên bản; nếu Gradle/compile báo lỗi, kiểm tra phiên bản thư viện và API trong tài liệu:
  https://developer.android.com/media/media3/transformer
- Xuất video 1080x1920 có thể nóng máy, tốn pin và dung lượng; xử lý lần lượt.
- Hình ảnh/video được cắt giữa để lấp khung dọc; phần rìa có thể bị cắt.
- Video ngắn hơn 6 giây có thể không đáp ứng đủ độ dài với cách cắt này; nên dùng video ít nhất 6 giây.
- Không yêu cầu quyền truy cập toàn bộ bộ nhớ; người dùng chọn file qua Android document picker.


BẢN CHẨN ĐOÁN LỖI: Màn hình có mục “Nhật ký xử lý / lỗi chi tiết”. Khi lỗi xảy ra, xem loại ngoại lệ, message và stack trace; nhấn “Sao chép log” để copy gửi hỗ trợ.
