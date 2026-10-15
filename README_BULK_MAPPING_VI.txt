IMAGEVIDEO BATCH — ANDROID (BẢN CHỌN NHIỀU ẢNH / VIDEO)
========================================================

Ứng dụng Android xử lý trực tiếp trên điện thoại; không tải ảnh/video lên máy chủ và không dùng API trả phí.

CÁCH DÙNG MAPPING MỚI
1. Nhấn “Chọn nhiều ảnh” và chọn N ảnh trong một lần.
2. Nhấn “Chọn nhiều video” và chọn N video trong một lần.
3. Ứng dụng tự sinh N cặp: Ảnh 1 ↔ Video 1, Ảnh 2 ↔ Video 2, ... theo thứ tự danh sách URI được Android Document Picker trả về.
4. Kiểm tra tên ảnh/video trong danh sách mapping trước khi nhấn “Tạo tất cả video”.
5. Số lượng ảnh và video phải bằng nhau. Nếu lệch, ứng dụng hiển thị số lượng đang chọn và không cho chạy batch cho đến khi hai số bằng nhau.
6. Nếu chọn lại ảnh hoặc video, danh sách tương ứng được thay bằng lựa chọn mới và mapping tự cập nhật. Bấm hủy picker sẽ giữ lựa chọn trước đó.

ĐỊNH DẠNG VIDEO
- Mỗi cặp: ảnh tĩnh 8 giây + 6 giây đầu của video nguồn = MP4 14 giây.
- Khung dọc 9:16, 1080x1920, 30 FPS, H.264, không âm thanh.
- Lưu vào thư viện video Android: Bộ nhớ trong/Movies.
- Có mục nhật ký xử lý/lỗi chi tiết trên giao diện; nút “Sao chép log” để gửi thông tin lỗi.
- Mỗi batch được xử lý tuần tự để giảm tải thiết bị.

BUILD APK QUA GITHUB ACTIONS
1. Giải nén ZIP.
2. Đưa nội dung dự án vào repository hoặc thay các file đã sửa trong repository hiện có.
3. Giữ workflow `.github/workflows/build-apk.yml` trong thư mục `.github/workflows/` ở cấp gốc repository.
4. Commit thay đổi và chạy workflow trong tab Actions.
5. Khi build thành công, tải artifact `ImageVideoBatch-debug-apk`.

Dự án dùng AndroidX Media3 Transformer và AndroidX Activity. Mã nguồn được cập nhật dựa trên log người dùng gửi; chưa được build/kiểm thử thực tế trên Samsung S23 Ultra trong môi trường tạo ZIP này. Nếu build hoặc chạy gặp lỗi, mở mục log trên app và gửi log để chẩn đoán.
