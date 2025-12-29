package com.hfad.beeradviser

import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.hfad.beeradviser.data.NoteDatabaseHelper
import java.util.Date

class NoteDetailActivity : AppCompatActivity() {

    private lateinit var dbHelper: NoteDatabaseHelper
    private lateinit var detailDateTextView: TextView
    private lateinit var detailTitleTextView: TextView
    private lateinit var detailPhotoImageView: ImageView
    private lateinit var detailContentTextView: TextView
    private lateinit var backButton: ImageButton
    private val TAG = "NoteDetailActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        // 1. 元件初始化
        dbHelper = NoteDatabaseHelper(this)
        detailDateTextView = findViewById(R.id.detailDateTextView)
        detailTitleTextView = findViewById(R.id.detailTitleTextView)
        detailPhotoImageView = findViewById(R.id.detailPhotoImageView)
        detailContentTextView = findViewById(R.id.detailContentTextView)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener {
            // 呼叫 finish() 即可返回上一個 Activity
            finish()
        }

        // 2. 獲取傳遞過來的日記 ID
        val noteId = intent.getLongExtra("NOTE_ID", -1L)

        Log.d(TAG, "Received NOTE_ID: $noteId")

        if (noteId != -1L && noteId != 0L) { // 🌟 檢查 ID 是否為 0L，以防萬一 🌟
            loadNoteDetails(noteId)
        } else {
            Toast.makeText(this, "無法載入日記詳情：無效的 ID ($noteId)。", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadNoteDetails(id: Long) {
        val note = dbHelper.getNoteById(id)

        if (note != null) {
            // 綁定日期
            val date = Date(note.timestamp)
            val formattedDate = DateFormat.format("yyyy年MM月dd日 EEEE", date)
            detailDateTextView.text = formattedDate

            // 綁定標題
            detailTitleTextView.text = note.title

            val rawContent = note.content ?: ""

            val lines = rawContent.split("\n")
            val htmlBuilder = StringBuilder()

            // 由於內容儲存格式為 Q1\nA1\nQ2\nA2\nQ3\nA3
            lines.forEachIndexed { index, line ->
                val trimmedLine = line.trim()
                if (trimmedLine.isNotBlank()) {
                    if (index % 2 == 0) {
                        // 這是問題 (Q) - 加粗，並用 <p> 包起來以確保段落間距
                        htmlBuilder.append("<b>$trimmedLine</b><br>")
                    } else {
                        // 這是答案 (A) - 答案後加上 <br><br> 來產生間隔
                        // 注意：使用 <br><br> 依然是標準作法，但為了強化效果，我們在下一個問題前會多加一個 <br>
                        htmlBuilder.append("$trimmedLine")

                        // 檢查後面是否還有內容 (也就是還有下一個問題)
                        if (index < lines.size - 1) {
                            // 在每個 Q&A 組合結束後，使用兩個明確的換行符號 <br> 來強制間距
                            // <br> 確保換行，而後續的 <br> 則確保了空行
                            htmlBuilder.append("<br><br>")
                        }
                    }
                }
            }

            val finalFormattedHtml = htmlBuilder.toString()

            // 再次檢查 Log 輸出，確認 HTML 結構是否正確
            Log.d(TAG, "Generated HTML Content: $finalFormattedHtml")

            // 使用 HtmlCompat 渲染 HTML 格式的內容
            detailContentTextView.text = HtmlCompat.fromHtml(
                finalFormattedHtml,
                HtmlCompat.FROM_HTML_MODE_COMPACT // 使用 COMPACT 模式有時對換行更友善
            )
            // 處理照片 URI
            if (note.photoUri != null && note.photoUri.isNotEmpty()) {
                try {
                    val photoUri = Uri.parse(note.photoUri)
                    // 為了確保權限，這裡可能需要添加 FLAG_GRANT_READ_URI_PERMISSION
                    // 但由於在 InputActivity 中使用了 takePersistableUriPermission，通常在這裡可以直接使用
                    detailPhotoImageView.setImageURI(photoUri)
                    detailPhotoImageView.visibility = View.VISIBLE
                } catch (e: Exception) {
                    Log.e("DetailActivity", "Error loading photo URI: ${note.photoUri}", e)
                    detailPhotoImageView.visibility = View.GONE
                    Toast.makeText(this, "照片載入失敗。", Toast.LENGTH_SHORT).show()
                }
            } else {
                detailPhotoImageView.visibility = View.GONE
            }

        } else {
            Toast.makeText(this, "找不到該筆日記。", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}