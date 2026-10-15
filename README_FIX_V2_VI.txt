BẢN SỬA v2 – Android app ghép ảnh + video

Đã sửa theo log build mới:
- Thêm dependency androidx.activity:activity-ktx để nhận diện ComponentActivity và ActivityResultContracts.
- Thêm dependency androidx.media3:media3-effect và sửa import Presentation sang androidx.media3.effect.Presentation.
- Giữ workflow GitHub Actions không dùng android-actions/setup-android@v3 (tránh lỗi sdkmanager package 'tools').

Cập nhật trên GitHub:
1. Giải nén ZIP.
2. Trong repository video, thay hai file:
   app/build.gradle.kts
   app/src/main/java/com/example/imagevideobatch/MainActivity.kt
3. Kiểm tra workflow ở đường dẫn cấp gốc repository:
   .github/workflows/build-apk.yml
   Nếu đang dùng workflow đã sửa ở bản trước, giữ nguyên file này.
4. Commit changes.
5. Mở Actions và chạy workflow lại.
6. Khi thành công, tải artifact ImageVideoBatch-debug-apk.

Lưu ý: đây là sửa dựa trên lỗi biên dịch được gửi, chưa có xác nhận build thành công trên GitHub. Nếu còn lỗi, gửi log mới nhất.
