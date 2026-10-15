package com.example.imagevideobatch

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
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
    private lateinit var progressBar: ProgressBar
    private lateinit var addButton: Button
    private lateinit var startButton: Button
    private val rows = mutableListOf<PairRow>()
    private var nextPickTarget: PairRow? = null
    private var pickingImage = true
    private var currentIndex = 0
    private var completedCount = 0
    private var failedCount = 0
    private var isRunning = false
    private var outputDir: File? = null
    private val savedPaths = mutableListOf<String>()
    private val errorDetails = mutableListOf<String>()

    private val imagePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            persistPermission(uri)
            nextPickTarget?.let {
                it.imageUri = uri
                it.imageName = displayName(uri)
                it.imageLabel.text = "Ảnh: ${it.imageName}"
            }
        }
        nextPickTarget = null
    }

    private val videoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            persistPermission(uri)
            nextPickTarget?.let {
                it.videoUri = uri
                it.videoName = displayName(uri)
                it.videoLabel.text = "Video: ${it.videoName}"
            }
        }
        nextPickTarget = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        outputDir = File(getExternalFilesDir(null), "exports")
        outputDir?.mkdirs()
        buildUi()
        addPairRow()
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
            text = "Danh sách mapping 1–1"
            textSize = 17f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF172033.toInt())
            setPadding(0, dp(14), 0, dp(6))
        }
        page.addView(rowHeader)

        val scroll = ScrollView(this)
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(listContainer)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        addButton = Button(this).apply {
            text = "+ Thêm cặp ảnh + video"
            setOnClickListener { if (!isRunning) addPairRow() }
        }
        page.addView(addButton, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(8)
        })

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

        setContentView(page)
    }

    private fun addPairRow() {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        val index = rows.size + 1
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = String.format(Locale.getDefault(), "Cặp %02d", index)
            textSize = 16f
            setTextColor(0xFF172033.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val remove = Button(this).apply {
            text = "Xóa"
            setOnClickListener {
                if (!isRunning) {
                    val row = rows.firstOrNull { it.root === card }
                    if (row != null) {
                        rows.remove(row)
                        listContainer.removeView(card)
                        renumberRows()
                    }
                }
            }
        }
        top.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(remove)
        card.addView(top)

        val imageLabel = TextView(this).apply {
            text = "Ảnh: chưa chọn"
            textSize = 13f
            setTextColor(0xFF566074.toInt())
            setPadding(0, dp(5), 0, dp(2))
        }
        card.addView(imageLabel)
        val imageButton = Button(this).apply {
            text = "Chọn ảnh"
            setOnClickListener {
                if (!isRunning) {
                    nextPickTarget = rows.firstOrNull { it.root === card }
                    pickingImage = true
                    imagePicker.launch(arrayOf("image/*"))
                }
            }
        }
        card.addView(imageButton)

        val videoLabel = TextView(this).apply {
            text = "Video: chưa chọn"
            textSize = 13f
            setTextColor(0xFF566074.toInt())
            setPadding(0, dp(5), 0, dp(2))
        }
        card.addView(videoLabel)
        val videoButton = Button(this).apply {
            text = "Chọn video"
            setOnClickListener {
                if (!isRunning) {
                    nextPickTarget = rows.firstOrNull { it.root === card }
                    videoPicker.launch(arrayOf("video/*"))
                }
            }
        }
        card.addView(videoButton)

        val row = PairRow(card, imageLabel, videoLabel)
        rows.add(row)
        listContainer.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(10)
        })
        renumberRows()
    }

    private fun renumberRows() {
        rows.forEachIndexed { index, row ->
            val top = row.root.getChildAt(0) as? LinearLayout
            val label = top?.getChildAt(0) as? TextView
            label?.text = String.format(Locale.getDefault(), "Cặp %02d", index + 1)
        }
    }

    private fun startBatch() {
        if (isRunning) return
        val validRows = rows.filter { it.imageUri != null || it.videoUri != null }
        if (validRows.isEmpty()) {
            toast("Hãy thêm ít nhất một cặp ảnh + video.")
            return
        }
        val incomplete = validRows.indexOfFirst { it.imageUri == null || it.videoUri == null }
        if (incomplete >= 0) {
            toast("Cặp ${incomplete + 1} chưa có đủ ảnh và video.")
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
        isRunning = true
        currentIndex = 0
        completedCount = 0
        failedCount = 0
        savedPaths.clear()
        errorDetails.clear()
        startButton.isEnabled = false
        addButton.isEnabled = false
        progressBar.max = validRows.size
        progressBar.progress = 0
        statusText.text = "Bắt đầu xử lý ${validRows.size} cặp…"
        processNext(validRows)
    }

    private fun processNext(jobs: List<PairRow>) {
        if (currentIndex >= jobs.size) {
            isRunning = false
            startButton.isEnabled = true
            addButton.isEnabled = true
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
                .setRemoveAudio(true)
                .setEffects(cropEffects)
                .build()

            val clip = EditedMediaItem.Builder(videoItem)
                .setRemoveAudio(true)
                .setEffects(cropEffects)
                .build()

            val sequence = EditedMediaItemSequence(listOf(still, clip))
            val composition = Composition.Builder(sequence).build()

            val temp = File(cacheDir, "batch_${System.currentTimeMillis()}_$jobNumber.mp4")
            val transformer = Transformer.Builder(this)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        try {
                            val saved = saveToGallery(temp, jobNumber)
                            savedPaths.add(saved)
                            completedCount++
                            progressBar.progress = jobNumber
                            statusText.text = "Cặp $jobNumber/${jobs.size} đã lưu: $saved"
                        } catch (e: Exception) {
                            failedCount++
                            errorDetails.add("Cặp $jobNumber: ${e.message ?: e.javaClass.simpleName}")
                            statusText.text = "Cặp $jobNumber lỗi khi lưu: ${e.message}"
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
                        temp.delete()
                        progressBar.progress = jobNumber
                        statusText.text = "Cặp $jobNumber thất bại: ${exportException.message}"
                        currentIndex++
                        processNext(jobs)
                    }
                })
                .build()

            transformer.start(composition, temp.absolutePath)
        } catch (e: Exception) {
            failedCount++
            errorDetails.add("Cặp $jobNumber: ${e.message ?: e.javaClass.simpleName}")
            progressBar.progress = jobNumber
            statusText.text = "Cặp $jobNumber lỗi: ${e.message}"
            currentIndex++
            processNext(jobs)
        }
    }

    private fun saveToGallery(temp: File, pairNumber: Int): String {
        if (!temp.exists()) throw IOException("Không tìm thấy video tạm sau khi xuất")
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
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Android không tạo được file trong thư viện Movies")

        try {
            val copiedBytes = resolver.openOutputStream(uri, "w")?.use { out ->
                FileInputStream(temp).use { input -> input.copyTo(out) }
            } ?: throw IOException("Không mở được file đầu ra trong Movies")

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
            } ?: throw IOException("Không xác minh được file trong thư viện video")

            return "Bộ nhớ trong/Movies/$fileName"
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
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

    private fun dp(value: Int): Int = (value * this@MainActivity.getResources().displayMetrics.density).toInt()
    private fun toast(message: String) {
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
    }
}
