CÁCH TẠO APK KHÔNG CẦN CÀI ANDROID STUDIO
=========================================

Mục tiêu: dùng GitHub Actions để biên dịch APK trên máy chủ build của GitHub. Máy tính của bạn không cần cài Android Studio, Android SDK hay Gradle.

LƯU Ý QUAN TRỌNG
- Đây là mã nguồn + quy trình build tự động, KHÔNG phải APK đã được build sẵn.
- Dự án chưa được xác nhận build thành công. Lần chạy workflow đầu tiên sẽ kiểm tra việc biên dịch; nếu có lỗi, cần sửa mã nguồn rồi chạy lại.
- Mã nguồn xử lý ảnh/video trên điện thoại; bước build trên GitHub chỉ biên dịch ứng dụng, không nhận ảnh/video cá nhân của bạn.

CÁC BƯỚC
1. Tải ZIP và giải nén trên máy tính.
2. Đăng nhập hoặc tạo tài khoản GitHub.
3. Tạo repository mới. Có thể đặt Private để giữ repository riêng tư.
4. Tải các file trong thư mục ImageVideoAndroid lên repository, giữ nguyên cấu trúc thư mục, bao gồm thư mục ẩn .github/workflows.
   - Nếu dùng giao diện web GitHub, có thể cần bật hiển thị file ẩn trên máy tính trước khi tải lên.
   - Cách dễ hơn: dùng GitHub Desktop để thêm thư mục đã giải nén và Publish repository.
5. Vào repository trên GitHub → tab Actions → chọn “Build Android APK” → Run workflow.
6. Chờ workflow chạy xong. Nếu màu xanh, mở lần chạy đó và tải artifact “ImageVideoBatch-debug-apk”.
7. Giải nén artifact, lấy app-debug.apk, gửi sang điện thoại Android và mở file để cài.
8. Nếu điện thoại chặn cài đặt, vào phần cài đặt được yêu cầu trên màn hình để cho phép cài ứng dụng từ ứng dụng quản lý tệp/trình duyệt bạn vừa dùng.

TÍNH NĂNG DỰ KIẾN
- Ghép từng cặp ảnh + video theo mapping 1–1, thêm nhiều cặp bằng nút +.
- Mỗi cặp: ảnh tĩnh 8 giây + 6 giây đầu video = 14 giây.
- MP4 dọc 9:16, 1080x1920, 30 FPS, không âm thanh.
- Xử lý cục bộ trên điện thoại; không tải media lên máy chủ.

Nếu workflow báo lỗi màu đỏ, mở bước bị lỗi, sao chép phần log lỗi và gửi lại để sửa.
