# Bản sửa lưu video + kiểm tra kết quả (Samsung)

Bản này cải thiện phần lưu vào thư viện MediaStore của Android và không còn báo chung chung rằng đã lưu khi một cặp bị lỗi.

- Xuất video vào `Bộ nhớ trong/Movies/`.
- Kiểm tra file tạm có dung lượng lớn hơn 0.
- Kiểm tra số byte đã sao chép, trạng thái công khai (`IS_PENDING = 0`) và mục video trong MediaStore.
- Nếu lưu thất bại, màn hình ứng dụng sẽ hiện số video lỗi và chi tiết lỗi cuối cùng thay vì luôn báo thành công.

## Cập nhật trên GitHub

Trong repository `voquyen090401/video`, thay file:
`app/src/main/java/com/example/imagevideobatch/MainActivity.kt`

bằng file cùng đường dẫn trong ZIP này. Giữ nguyên workflow build đã sửa. Commit rồi chạy lại Actions và tải artifact `ImageVideoBatch-debug-apk`.

Lưu ý: APK debug build trên GitHub có thể được ký bằng debug key khác mỗi lần chạy. Nếu Android báo không thể cập nhật ứng dụng do xung đột chữ ký, hãy gỡ bản cũ rồi cài APK mới (việc gỡ ứng dụng có thể xóa dữ liệu ứng dụng, nhưng không nên xóa video đã xuất trong Movies).
