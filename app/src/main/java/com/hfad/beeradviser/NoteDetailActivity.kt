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
import com.hfad.beeradviser.data.NoteDatabaseHelper
import java.util.Date

class NoteDetailActivity : AppCompatActivity() {

    companion object {
        private const val QUESTION_1 = "1. 你覺得今天值得珍藏的事情是什麼？"
        private const val QUESTION_2 = "2. 你現在感受到自己有什麼情緒呢？"
        private const val QUESTION_3 = "3. 觀察一下，你有發現自己的小改變嗎？"
        private const val QUESTION_4 = "4. 明天你想對自己說什麼？"
        private const val ANSWER_DEFAULT = "未填寫"
    }

    private lateinit var dbHelper: NoteDatabaseHelper
    private lateinit var detailDateTextView: TextView
    private lateinit var detailTitleTextView: TextView
    private lateinit var detailPhotoImageView: ImageView
    private lateinit var detailEmotionImageView: ImageView
    private lateinit var detailAnswer1TextView: TextView
    private lateinit var detailAnswer2TextView: TextView
    private lateinit var detailAnswer3TextView: TextView
    private lateinit var detailAnswer4TextView: TextView
    private lateinit var backButton: ImageButton
    private val tag = "NoteDetailActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        dbHelper = NoteDatabaseHelper(this)
        detailDateTextView = findViewById(R.id.detailDateTextView)
        detailTitleTextView = findViewById(R.id.detailTitleTextView)
        detailPhotoImageView = findViewById(R.id.detailPhotoImageView)
        detailEmotionImageView = findViewById(R.id.detailEmotionImageView)
        detailAnswer1TextView = findViewById(R.id.detailAnswer1TextView)
        detailAnswer2TextView = findViewById(R.id.detailAnswer2TextView)
        detailAnswer3TextView = findViewById(R.id.detailAnswer3TextView)
        detailAnswer4TextView = findViewById(R.id.detailAnswer4TextView)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener { finish() }

        val noteId = intent.getLongExtra("NOTE_ID", -1L)
        Log.d(tag, "Received NOTE_ID: $noteId")

        if (noteId != -1L && noteId != 0L) {
            loadNoteDetails(noteId)
        } else {
            Toast.makeText(this, "無法載入日記詳情：無效的 ID ($noteId)。", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadNoteDetails(id: Long) {
        val note = dbHelper.getNoteById(id)

        if (note == null) {
            Toast.makeText(this, "找不到該筆日記。", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val date = Date(note.timestamp)
        detailDateTextView.text = DateFormat.format("yyyy年MM月dd日 EEEE", date)

        val title = note.title.trim()
        if (title.isBlank()) {
            detailTitleTextView.visibility = View.GONE
        } else {
            detailTitleTextView.text = title
            detailTitleTextView.visibility = View.VISIBLE
        }

        bindEmotion(note.emotion)
        bindAnswers(note.content)
        bindPhoto(note.photoUri)
    }

    private fun bindEmotion(rawEmotion: String?) {
        val drawableName = when (rawEmotion?.trim()) {
            InputDraftViewModel.Emotion.NICE.name -> "niceemoji"
            InputDraftViewModel.Emotion.OK.name -> "okemoji"
            InputDraftViewModel.Emotion.SAD.name -> "sademoji"
            else -> null
        }

        if (drawableName == null) {
            detailEmotionImageView.visibility = View.GONE
            return
        }

        val resId = resources.getIdentifier(drawableName, "drawable", packageName)
        if (resId == 0) {
            detailEmotionImageView.visibility = View.GONE
            Log.w(tag, "找不到情緒圖片資源：$drawableName")
            return
        }

        detailEmotionImageView.setImageResource(resId)
        detailEmotionImageView.visibility = View.VISIBLE
    }

    private fun bindAnswers(rawContent: String?) {
        val answers = parseAnswers(rawContent.orEmpty())
        detailAnswer1TextView.text = answers[0].ifBlank { ANSWER_DEFAULT }
        detailAnswer2TextView.text = answers[1].ifBlank { ANSWER_DEFAULT }
        detailAnswer3TextView.text = answers[2].ifBlank { ANSWER_DEFAULT }
        detailAnswer4TextView.text = answers[3].ifBlank { ANSWER_DEFAULT }
    }

    private fun parseAnswers(content: String): List<String> {
        val lines = content
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val questionOrder = listOf(QUESTION_1, QUESTION_2, QUESTION_3, QUESTION_4)
        val answerMap = mutableMapOf<String, String>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line in questionOrder) {
                answerMap[line] = lines.getOrNull(i + 1).orEmpty()
                i += 2
            } else {
                i += 1
            }
        }

        return questionOrder.map { answerMap[it].orEmpty() }
    }

    private fun bindPhoto(photoUriRaw: String?) {
        if (photoUriRaw.isNullOrBlank()) {
            detailPhotoImageView.visibility = View.GONE
            return
        }

        try {
            detailPhotoImageView.setImageURI(Uri.parse(photoUriRaw))
            detailPhotoImageView.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e(tag, "Error loading photo URI: $photoUriRaw", e)
            detailPhotoImageView.visibility = View.GONE
            Toast.makeText(this, "照片載入失敗。", Toast.LENGTH_SHORT).show()
        }
    }
}
