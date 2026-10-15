package com.example.imagevideobatch

import android.content.ContentValues
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Transformer
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class PairRow(
    val root: LinearLayout,
    val imageLabel: TextView,
    val videoLabel: TextView,
    var imageUri: Uri? = null,
    var videoUri: Uri? = null,
    var imageName: String = "",
    var videoName: String = ""
)

class MainActivity : ComponentActivity() {
    private lateinit var listContainer: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private lateinit var logScroll: ScrollView
    private val uiLog = StringBuilder()
    private lateinit var progressBar: ProgressBar
    private lateinit var selectImagesButton: Button
    private lateinit var selectVideosButton: Button
    private lateinit var selectionSummaryText: TextView
    private lateinit var startButton: Button
    private val rows = mutableListOf<PairRow>()
    private var selectedImageUris: List<Uri> = emptyList()
    private var selectedVideoUris: List<Uri> = emptyList()
    private var currentIndex = 0
    private var completedCount = 0
    private var failedCount = 0
    private var isRunning = false
    private var outputDir: File? = null
    private val savedPaths = mutableListOf<String>()
    private val errorDetails = mutableListOf<String>()

    private val imagePicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { persistPermission(it) }
            selectedImageUris = uris.distinctBy { it.toString() }
            appendUiLog("Đã chọn ${selectedImageUris.size} ảnh theo thứ tự picker trả về.")
            rebuildPairRows()
        } else {
            appendUiLog("Đã hủy chọn ảnh; danh sách ảnh trước đó được giữ nguyên.")
        }
    }

    private val videoPicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { persistPermission(it) }
            selectedVideoUris = uris.distinctBy { it.toString() }
            appendUiLog("Đã chọn ${selectedVideoUris.size} video theo thứ tự picker trả về.")
            rebuildPairRows()
        } else {
            appendUiLog("Đã hủy chọn video; danh sách video trước đó được giữ nguyên.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        outputDir = File(getExternalFilesDir(null), "exports")
        outputDir?.mkdirs()
        buildUi()
        rebuildPairRows()
    }

    private fun buildUi() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val title = TextView(this).apply {
            text = "ImageVideo Batch"
            textSize = 24f
            setTextColor(0xFF172033.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        page.addView(title)
        page.addView(TextView(this).apply {
            text = "Mỗi cặp: ảnh 8 giây + video 6 giây → MP4 14 giây"
            textSize = 14f
            setTextColor(0xFF566074.toInt())
            setPadding(0, dp(4), 0, dp(12))
        })

        val settings = TextView(this).apply {
            text = "Đầu ra: 9:16 • 1080×1920 • 30 FPS • không âm thanh\nVideo nguồn lấy 6 giây đầu. Ảnh/video được cắt giữa để vừa khung."
            textSize = 13f
            setTextColor(0xFF30394B.toInt())
            setBackgroundColor(0xFFE9EEF8.toInt())
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        page.addView(settings)

        val rowHeader = TextView(this).apply {
            text = "Chọn nhiều file một lần"
            textSize = 17f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF172033.toInt())
            setPadding(0, dp(14), 0, dp(6))
        }
        page.addView(rowHeader)
        page.addView(TextView(this).apply {
            text = "Bước 1: chọn N ảnh. Bước 2: chọn N video. App tự ghép ảnh số 1 với video số 1, ảnh số 2 với video số 2… theo thứ tự danh sách được trả về từ bộ chọn file."
            textSize = 12f
            setTextColor(0xFF566074.toInt())
            setPadding(0, 0, 0, dp(6))
        })

        val pickButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        selectImagesButton = Button(this).apply {
            text = "Chọn nhiều ảnh"
            setOnClickListener { if (!isRunning) imagePicker.launch(arrayOf("image/*")) }
        }
        selectVideosButton = Button(this).apply {
            text = "Chọn nhiều video"
            setOnClickListener { if (!isRunning) videoPicker.launch(arrayOf("video/*")) }
        }
        pickButtons.addView(selectImagesButton, LinearLayout.LayoutParams(0, -2, 1f).apply {
            marginEnd = dp(4)
        })
        pickButtons.addView(selectVideosButton, LinearLayout.LayoutParams(0, -2, 1f).apply {
            marginStart = dp(4)
        })
        page.addView(pickButtons)

        selectionSummaryText = TextView(this).apply {
            text = "Đã chọn: 0 ảnh • 0 video • 0 cặp"
            textSize = 13f
            setTextColor(0xFF30394B.toInt())
            setPadding(0, dp(4), 0, dp(6))
        }
        page.addView(selectionSummaryText)

        val scroll = ScrollView(this)
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(listContainer)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        startButton = Button(this).apply {
            text = "Tạo tất cả video"
            setOnClickListener { startBatch() }
        }
        page.addView(startButton, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(4)
        })

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
        }
        page.addView(progressBar, LinearLayout.LayoutParams(-1, dp(8)).apply {
            topMargin = dp(10)
        })

        statusText = TextView(this).apply {
            text = "Sẵn sàng. Thêm mapping bằng nút +."
            textSize = 13f
            setTextColor(0xFF30394B.toInt())
            setPadding(0, dp(6), 0, dp(4))
        }
        page.addView(statusText)

        val logHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        logHeader.addView(TextView(this).apply {
            text = "Nhật ký xử lý / lỗi chi tiết"
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF172033.toInt())
        }, LinearLayout.LayoutParams(0, -2, 1f))
        logHeader.addView(Button(this).apply {
            text = "Sao chép log"
            setOnClickListener {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("ImageVideo log", uiLog.toString()))
                toast("Đã sao chép log. Bạn có thể dán gửi lại cho mình.")
            }
        })
        page.addView(logHeader)
        logScroll = ScrollView(this).apply {
            isFillViewport = true
        }
        logText = TextView(this).apply {
            text = "Chưa có log. Khi có lỗi, thông tin và stack trace sẽ hiển thị ở đây."
            textSize = 11f
            setTextColor(0xFF202124.toInt())
            setBackgroundColor(0xFFF1F3F4.toInt())
            setPadding(dp(8), dp(8), dp(8), dp(8))
            typeface = android.graphics.Typeface.MONOSPACE
        }
        logScroll.addView(logText)
        page.addView(logScroll, LinearLayout.LayoutParams(-1, dp(150)).apply {
            topMargin = dp(4)
        })

        setContentView(page)
        appendUiLog("Ứng dụng khởi động. Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT}).")
    }

    private fun rebuildPairRows() {
        if (!::listContainer.isInitialized) return
        rows.clear()
        listContainer.removeAllViews()
        val count = maxOf(selectedImageUris.size, selectedVideoUris.size)
        for (index in 0 until count) addPairRow(index)

        if (::selectionSummaryText.isInitialized) {
            selectionSummaryText.text = "Đã chọn: ${selectedImageUris.size} ảnh • ${selectedVideoUris.size} video • $count cặp"
            selectionSummaryText.setTextColor(
                if (selectedImageUris.size == selectedVideoUris.size) 0xFF166534.toInt() else 0xFFB45309.toInt()
            )
        }
        if (selectedImageUris.size != selectedVideoUris.size) {
            statusText.text = "Số lượng chưa khớp: ${selectedImageUris.size} ảnh và ${selectedVideoUris.size} video. Hãy chọn bổ sung/thay lại để hai số lượng bằng nhau."
        } else if (count > 0) {
            statusText.text = "Đã tạo tự động $count cặp mapping theo thứ tự danh sách."
        } else {
            statusText.text = "Hãy chọn nhiều ảnh, sau đó chọn số lượng video tương ứng."
        }
    }

    private fun addPairRow(index: Int) {
        val imageUri = selectedImageUris.getOrNull(index)
        val videoUri = selectedVideoUris.getOrNull(index)
        val imageName = imageUri?.let { displayName(it) } ?: "chưa có ảnh"
        val videoName = videoUri?.let { displayName(it) } ?: "chưa có video"
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(9), dp(10), dp(9))
            setBackgroundColor(if (index % 2 == 0) 0xFFF4F6FA.toInt() else 0xFFFFFFFF.toInt())
        }
        val title = TextView(this).apply {
            text = String.format(Locale.getDefault(), "Cặp %02d  (Ảnh %d ↔ Video %d)", index + 1, index + 1, index + 1)
            textSize = 15f
            setTextColor(0xFF172033.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        card.addView(title)
        val imageLabel = TextView(this).apply {
            text = "Ảnh ${index + 1}: $imageName"
            textSize = 12f
            setTextColor(if (imageUri != null) 0xFF1D4ED8.toInt() else 0xFFB45309.toInt())
            setPadding(0, dp(5), 0, dp(2))
        }
        card.addView(imageLabel)
        val videoLabel = TextView(this).apply {
            text = "Video ${index + 1}: $videoName"
            textSize = 12f
            setTextColor(if (videoUri != null) 0xFF166534.toInt() else 0xFFB45309.toInt())
            setPadding(0, dp(2), 0, dp(2))
        }
        card.addView(videoLabel)

        val row = PairRow(
            root = card,
            imageLabel = imageLabel,
            videoLabel = videoLabel,
            imageUri = imageUri,
            videoUri = videoUri,
            imageName = imageName,
            videoName = videoName
        )
        rows.add(row)
        listContainer.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(6)
        })
    }

    private fun startBatch() {
        if (isRunning) return
        if (selectedImageUris.isEmpty() || selectedVideoUris.isEmpty()) {
            toast("Hãy chọn nhiều ảnh và nhiều video trước.")
            return
        }
        if (selectedImageUris.size != selectedVideoUris.size) {
            val message = "Số lượng không khớp: ${selectedImageUris.size} ảnh và ${selectedVideoUris.size} video. Cần chọn số lượng bằng nhau."
            statusText.text = message
            appendUiLog(message)
            toast(message)
            return
        }
        val validRows = rows.toList()
        if (validRows.isEmpty() || validRows.any { it.imageUri == null || it.videoUri == null }) {
            toast("Chưa tạo đủ cặp mapping. Hãy kiểm tra danh sách ảnh và video.")
            return
        }
        val duplicateImages = validRows.mapNotNull { it.imageUri }.groupingBy { it.toString() }.eachCount().any { it.value > 1 }
        val duplicateVideos = validRows.mapNotNull { it.videoUri }.groupingBy { it.toString() }.eachCount().any { it.value > 1 }
        if (duplicateImages || duplicateVideos) {
            toast("Mỗi ảnh và mỗi video chỉ được dùng trong một cặp.")
            return
        }

        val dir = outputDir ?: File(getExternalFilesDir(null), "exports")
        dir.mkdirs()
        uiLog.clear()
        logText.text = ""
        appendUiLog("Bắt đầu batch: ${validRows.size} cặp. Đích lưu: Bộ nhớ trong/Movies/.")
        appendUiLog("Android SDK ${android.os.Build.VERSION.SDK_INT}; package=${packageName}")
        isRunning = true
        currentIndex = 0
        completedCount = 0
        failedCount = 0
        savedPaths.clear()
        errorDetails.clear()
        startButton.isEnabled = false
        selectImagesButton.isEnabled = false
        selectVideosButton.isEnabled = false
        progressBar.max = validRows.size
        progressBar.progress = 0
        statusText.text = "Bắt đầu xử lý ${validRows.size} cặp…"
        processNext(validRows)
    }

    private fun processNext(jobs: List<PairRow>) {
        if (currentIndex >= jobs.size) {
            isRunning = false
            startButton.isEnabled = true
            selectImagesButton.isEnabled = true
            selectVideosButton.isEnabled = true
            val summary = StringBuilder()
                .append("Hoàn tất: $completedCount video đã lưu, $failedCount video lỗi.")
                .append("\nThư mục: Bộ nhớ trong/Movies/")
            if (savedPaths.isNotEmpty()) {
                summary.append("\nFile mới nhất: ").append(savedPaths.last())
            }
            if (errorDetails.isNotEmpty()) {
                summary.append("\nChi tiết lỗi: ").append(errorDetails.last())
            }
            statusText.text = summary.toString()
            if (failedCount == 0) {
                toast("Đã lưu $completedCount video vào Bộ nhớ trong/Movies.")
            } else {
                toast("Đã lưu $completedCount video; $failedCount video lỗi. Xem chi tiết trên màn hình.")
            }
            return
        }

        val jobNumber = currentIndex + 1
        val row = jobs[currentIndex]
        val imageUri = row.imageUri!!
        val videoUri = row.videoUri!!
        statusText.text = "Đang xử lý cặp $jobNumber/${jobs.size}…"
        appendUiLog("Cặp $jobNumber/${jobs.size}: bắt đầu. imageUri=$imageUri; videoUri=$videoUri")

        try {
            val imageItem = MediaItem.Builder()
                .setUri(imageUri)
                .setImageDurationMs(8_000)
                .build()

            val videoItem = MediaItem.Builder()
                .setUri(videoUri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(0)
                        .setEndPositionMs(6_000)
                        .build()
                )
                .build()

            val cropEffects = Effects(
                emptyList(),
                listOf(Presentation.createForAspectRatio(9f / 16f, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
            )

            val still = EditedMediaItem.Builder(imageItem)
                // Media3 requires an explicit frame rate for still-image inputs.
                // Without this, ImageAssetLoader throws IllegalStateException before export starts.
                .setFrameRate(30)
                .setRemoveAudio(true)
                .setEffects(cropEffects)
                .build()

            val clip = EditedMediaItem.Builder(videoItem)
                .setRemoveAudio(true)
                .setEffects(cropEffects)
                .build()

            val sequence = EditedMediaItemSequence(listOf(still, clip))
            val composition = Composition.Builder(sequence).build()
            appendUiLog("Cặp $jobNumber: tạo composition thành công (ảnh 8s + video 0-6s).")

            val temp = File(cacheDir, "batch_${System.currentTimeMillis()}_$jobNumber.mp4")
            appendUiLog("Cặp $jobNumber: file tạm=${temp.absolutePath}")
            val transformer = Transformer.Builder(this)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        appendUiLog("Cặp $jobNumber: Media3 báo export hoàn tất. tempExists=${temp.exists()}, bytes=${if (temp.exists()) temp.length() else -1L}")
                        try {
                            val saved = saveToGallery(temp, jobNumber)
                            appendUiLog("Cặp $jobNumber: lưu và xác minh thành công: $saved")
                            savedPaths.add(saved)
                            completedCount++
                            progressBar.progress = jobNumber
                            statusText.text = "Cặp $jobNumber/${jobs.size} đã lưu: $saved"
                        } catch (e: Exception) {
                            failedCount++
                            errorDetails.add("Cặp $jobNumber: ${e.message ?: e.javaClass.simpleName}")
                            appendUiLog("Cặp $jobNumber: NGOẠI LỆ KHI LƯU", e)
                            statusText.text = "Cặp $jobNumber lỗi khi lưu: ${e.javaClass.simpleName}: ${e.message}"
                        } finally {
                            temp.delete()
                            currentIndex++
                            processNext(jobs)
                        }
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        failedCount++
                        errorDetails.add("Cặp $jobNumber: ${exportException.message ?: exportException.javaClass.simpleName}")
                        appendUiLog("Cặp $jobNumber: MEDIA3 EXPORT FAILED (type=${exportException.javaClass.name})", exportException)
                        temp.delete()
                        progressBar.progress = jobNumber
                        statusText.text = "Cặp $jobNumber thất bại: ${exportException.message}"
                        currentIndex++
                        processNext(jobs)
                    }
                })
                .build()

            appendUiLog("Cặp $jobNumber: gọi Transformer.start()")
            transformer.start(composition, temp.absolutePath)
            appendUiLog("Cặp $jobNumber: Transformer.start() đã trả về.")
        } catch (e: Exception) {
            failedCount++
            errorDetails.add("Cặp $jobNumber: ${e.message ?: e.javaClass.simpleName}")
            appendUiLog("Cặp $jobNumber: NGOẠI LỆ KHI KHỞI TẠO/XUẤT", e)
            progressBar.progress = jobNumber
            statusText.text = "Cặp $jobNumber lỗi: ${e.javaClass.simpleName}: ${e.message}"
            currentIndex++
            processNext(jobs)
        }
    }

    private fun saveToGallery(temp: File, pairNumber: Int): String {
        appendUiLog("Cặp $pairNumber: bắt đầu lưu vào MediaStore Movies.")
        if (!temp.exists()) throw IOException("Không tìm thấy video tạm sau khi xuất: ${temp.absolutePath}")
        val expectedBytes = temp.length()
        if (expectedBytes <= 0L) throw IOException("Video tạm có dung lượng 0 byte")

        val fileName = "ImageVideo_${System.currentTimeMillis()}_${pairNumber}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val resolver = contentResolver
        appendUiLog("Cặp $pairNumber: insert MediaStore tên=$fileName, relativePath=Movies/.")
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Android không tạo được file trong thư viện Movies (MediaStore.insert trả null)")
        appendUiLog("Cặp $pairNumber: MediaStore URI=$uri")

        try {
            val copiedBytes = resolver.openOutputStream(uri, "w")?.use { out ->
                FileInputStream(temp).use { input -> input.copyTo(out) }
            } ?: throw IOException("Không mở được file đầu ra trong Movies")

            appendUiLog("Cặp $pairNumber: copy xong $copiedBytes/$expectedBytes bytes.")
            if (copiedBytes <= 0L || copiedBytes != expectedBytes) {
                throw IOException("Ghi file chưa đủ dữ liệu ($copiedBytes/$expectedBytes byte)")
            }

            val done = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
            val updated = resolver.update(uri, done, null, null)
            if (updated <= 0) throw IOException("Đã ghi file nhưng không thể công khai file trong Movies")

            // Verify the saved MediaStore entry is visible and has non-zero size.
            val projection = arrayOf(
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.RELATIVE_PATH,
                MediaStore.Video.Media.IS_PENDING
            )
            resolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) throw IOException("Không tìm thấy file vừa lưu trong thư viện video")
                val sizeIndex = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val pendingIndex = cursor.getColumnIndex(MediaStore.Video.Media.IS_PENDING)
                val savedSize = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else -1L
                val pending = if (pendingIndex >= 0) cursor.getInt(pendingIndex) else 0
                if (savedSize <= 0L) throw IOException("File đã tạo nhưng thư viện báo dung lượng 0 byte")
                if (pending != 0) throw IOException("File vẫn ở trạng thái chờ, chưa hiển thị trong Movies")
                appendUiLog("Cặp $pairNumber: xác minh MediaStore OK; bytes=$savedSize, pending=$pending, uri=$uri")
            } ?: throw IOException("Không xác minh được file trong thư viện video")

            return "Bộ nhớ trong/Movies/$fileName"
        } catch (e: Exception) {
            appendUiLog("Cặp $pairNumber: lỗi trong saveToGallery; xóa entry MediaStore tạm. URI=$uri", e)
            try { resolver.delete(uri, null, null) } catch (deleteError: Exception) {
                appendUiLog("Không xóa được entry MediaStore sau lỗi.", deleteError)
            }
            throw e
        }
    }

    private fun persistPermission(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
            // Some providers do not grant persistable permissions; the current session may still work.
        }
    }

    private fun displayName(uri: Uri): String {
        val projection = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return cursor.getString(index) ?: uri.lastPathSegment ?: "file"
            }
        }
        return uri.lastPathSegment ?: "file"
    }

    private fun appendUiLog(message: String, error: Throwable? = null) {
        val stamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val detail = buildString {
            append("[").append(stamp).append("] ").append(message)
            if (error != null) {
                append("\nType: ").append(error.javaClass.name)
                append("\nMessage: ").append(error.message ?: "(không có message)")
                val sw = StringWriter()
                error.printStackTrace(PrintWriter(sw))
                append("\n").append(sw.toString())
                var cause = error.cause
                var depth = 0
                while (cause != null && cause !== error && depth < 8) {
                    append("\nCaused by [").append(depth + 1).append("]: ")
                        .append(cause.javaClass.name).append(": ").append(cause.message)
                    cause = cause.cause
                    depth++
                }
            }
        }
        Log.e("ImageVideoBatch", detail)
        runOnUiThread {
            uiLog.append(detail).append("\n\n")
            // Keep the panel responsive if a provider returns an extremely long stack trace.
            if (uiLog.length > 40000) uiLog.delete(0, uiLog.length - 30000)
            if (::logText.isInitialized) {
                logText.text = uiLog.toString()
                if (::logScroll.isInitialized) logScroll.post { logScroll.fullScroll(View.FOCUS_DOWN) }
            }
        }
    }

    private fun dp(value: Int): Int = (value * this@MainActivity.getResources().displayMetrics.density).toInt()
    private fun toast(message: String) {
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
    }
}
