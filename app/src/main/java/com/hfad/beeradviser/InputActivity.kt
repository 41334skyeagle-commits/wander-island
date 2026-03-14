package com.hfad.beeradviser

import android.os.Bundle
import android.widget.ImageButton
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.hfad.beeradviser.data.Note
import com.hfad.beeradviser.data.NoteDatabaseHelper

class InputActivity : AppCompatActivity(), InputIntroFragment.Callback, InputQuestionFragment.Callback {

    companion object {
        private const val QUESTION_1 = "1. 你覺得今天值得珍藏的事情是什麼？"
        private const val QUESTION_2 = "2. 你現在感受到自己有什麼情緒呢？"
        private const val QUESTION_3 = "3. 觀察一下，你有發現自己的小改變嗎？"
        private const val QUESTION_4 = "4. 明天你想對自己說什麼？"
    }

    private val draftViewModel: InputDraftViewModel by viewModels()

    private lateinit var dbHelper: NoteDatabaseHelper
    private lateinit var completeButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input)

        dbHelper = NoteDatabaseHelper(this)
        completeButton = findViewById(R.id.completeButton)

        completeButton.setOnClickListener {
            saveDiaryEntry()
        }

        onBackPressedDispatcher.addCallback(this) {
            when (draftViewModel.currentPage) {
                InputDraftViewModel.PAGE_QUESTIONS -> switchToIntroPage(animated = true)
                else -> finish()
            }
        }

        if (savedInstanceState == null) {
            when (draftViewModel.currentPage) {
                InputDraftViewModel.PAGE_QUESTIONS -> switchToQuestionPage(animated = false)
                else -> switchToIntroPage(animated = false)
            }
        } else {
            updateCompleteButtonVisibility()
        }
    }

    override fun onSwipeUpToQuestionsRequested() {
        if (!draftViewModel.canMoveToQuestions()) {
            Toast.makeText(this, "請至少輸入標題、上傳照片或選擇情緒後再前往下一頁。", Toast.LENGTH_SHORT).show()
            return
        }
        switchToQuestionPage(animated = true)
    }

    override fun onSwipeDownToIntroRequested() {
        switchToIntroPage(animated = true)
    }

    private fun switchToIntroPage(animated: Boolean) {
        draftViewModel.currentPage = InputDraftViewModel.PAGE_INTRO
        supportFragmentManager.commit {
            if (animated) {
                setCustomAnimations(
                    R.anim.slide_in_top,
                    R.anim.slide_out_bottom,
                    R.anim.slide_in_bottom,
                    R.anim.slide_out_top
                )
            }
            replace(R.id.inputFragmentContainer, InputIntroFragment())
        }
        updateCompleteButtonVisibility()
    }

    private fun switchToQuestionPage(animated: Boolean) {
        draftViewModel.currentPage = InputDraftViewModel.PAGE_QUESTIONS
        supportFragmentManager.commit {
            if (animated) {
                setCustomAnimations(
                    R.anim.slide_in_bottom,
                    R.anim.slide_out_top,
                    R.anim.slide_in_top,
                    R.anim.slide_out_bottom
                )
            }
            replace(R.id.inputFragmentContainer, InputQuestionFragment())
        }
        updateCompleteButtonVisibility()
    }

    private fun updateCompleteButtonVisibility() {
        completeButton.visibility =
            if (draftViewModel.currentPage == InputDraftViewModel.PAGE_QUESTIONS) View.VISIBLE else View.GONE
    }

    private fun saveDiaryEntry() {
        val title = draftViewModel.title.trim()
        val photoUriString = draftViewModel.photoUri

        val c1 = draftViewModel.content1.trim()
        val c2 = draftViewModel.content2.trim()
        val c3 = draftViewModel.content3.trim()
        val c4 = draftViewModel.content4.trim()

        val fullContent = buildString {
            if (c1.isNotEmpty()) append("$QUESTION_1\n$c1\n")
            if (c2.isNotEmpty()) append("$QUESTION_2\n$c2\n")
            if (c3.isNotEmpty()) append("$QUESTION_3\n$c3\n")
            if (c4.isNotEmpty()) append("$QUESTION_4\n$c4\n")
        }.trim()

        if (title.isBlank() && fullContent.isBlank() && photoUriString.isNullOrBlank()) {
            Toast.makeText(this, "日記內容不可為空。", Toast.LENGTH_SHORT).show()
            return
        }

        val note = Note(
            title = title,
            content = fullContent,
            timestamp = System.currentTimeMillis(),
            photoUri = photoUriString,
            emotion = draftViewModel.emotion?.name
        )

        val result = dbHelper.insertNote(note)
        if (result > 0) {
            Toast.makeText(this, "日記已儲存", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "儲存失敗", Toast.LENGTH_SHORT).show()
        }
    }
}
