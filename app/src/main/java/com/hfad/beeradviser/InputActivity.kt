package com.hfad.beeradviser

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.hfad.beeradviser.data.Note
import com.hfad.beeradviser.data.NoteDatabaseHelper
import java.io.File
import java.io.IOException
import java.util.Date


class InputActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "InputActivity"

        // 問題文字改為圖片後，儲存時仍保留固定題目文字，方便詳情頁排版。
        private const val QUESTION_1 = "1. 你覺得今天值得珍藏的事情是什麼？"
        private const val QUESTION_2 = "2. 你現在感受到自己有什麼情緒呢？"
        private const val QUESTION_3 = "3. 觀察一下，你有發現自己的小改變嗎？"
        private const val QUESTION_4 = "4. 明天你想對自己說什麼？"
    }

    private lateinit var dbHelper: NoteDatabaseHelper
    private lateinit var dateTextView: TextView
    private lateinit var titleEditText: EditText
    private lateinit var photoUploadArea: ImageView
    private var emotionPromptTextView: TextView? = null
    private var emotionContainer: View? = null
    private var niceEmojiImageView: ImageView? = null
    private var okEmojiImageView: ImageView? = null
    private var sadEmojiImageView: ImageView? = null
    private var guideWord1ImageView: ImageView? = null
    private var guideWord2ImageView: ImageView? = null

    private var question1ImageView: ImageView? = null
    private var question2ImageView: ImageView? = null
    private var question3ImageView: ImageView? = null
    private var question4ImageView: ImageView? = null

    private lateinit var content1EditText: EditText
    private lateinit var content2EditText: EditText
    private lateinit var content3EditText: EditText
    private var content4EditText: EditText? = null
    private lateinit var completeButton: ImageButton

    private var latestTmpUri: Uri? = null // 用於保存相機拍照後的臨時 Uri (給 TakePictureLauncher 使用)
    private var currentPhotoUri: Uri? = null // 用於保存當前顯示和準備儲存的照片 Uri


    // --- 【Activity Result Launcher 宣告】 ---
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                showImageSourceDialog()
            } else {
                Toast.makeText(this, "需要相機權限才能拍照。", Toast.LENGTH_SHORT).show()
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                latestTmpUri?.let { uri ->
                    photoUploadArea.setImageURI(uri)
                    currentPhotoUri = uri // 更新當前照片 Uri
                }
            } else {
                Toast.makeText(this, "拍照失敗或取消。", Toast.LENGTH_SHORT).show()
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                // 如果是從相簿選圖，必須保留 Uri 的讀取權限 (這是 ContentResolver 的要求)
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(it, flag)

                photoUploadArea.setImageURI(it)
                currentPhotoUri = it // 更新當前照片 Uri
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input)

        dateTextView = findViewById(R.id.dateTextView)
        titleEditText = findViewById(R.id.titleEditText)
        photoUploadArea = findViewById(R.id.photoUploadArea)
        emotionPromptTextView = findViewById(R.id.emotionPromptTextView)
        emotionContainer = findViewById(R.id.emotionSelectorContainer)
        niceEmojiImageView = findViewById(R.id.niceEmojiImageView)
        okEmojiImageView = findViewById(R.id.okEmojiImageView)
        sadEmojiImageView = findViewById(R.id.sadEmojiImageView)
        guideWord1ImageView = findViewById(R.id.guideWord1ImageView)
        guideWord2ImageView = findViewById(R.id.guideWord2ImageView)

        question1ImageView = findViewById(R.id.question1TextView)
        question2ImageView = findViewById(R.id.question2TextView)
        question3ImageView = findViewById(R.id.question3TextView)
        question4ImageView = findViewById(R.id.question4ImageView)

        content1EditText = findViewById(R.id.content1EditText)
        content2EditText = findViewById(R.id.content2EditText)
        content3EditText = findViewById(R.id.content3EditText)
        content4EditText = findViewById(R.id.content4EditText)
        completeButton = findViewById(R.id.completeButton)
        dbHelper = NoteDatabaseHelper(this)


        setupUI()
    }

    // --- 相機/相簿/權限 相關邏輯 ---

    private fun checkCameraPermission() {
        // 針對 Android 13+ 檢查 READ_MEDIA_IMAGES (相簿) 權限，這裡我們專注在 CAMERA
        when {
            // A. 權限已授予 -> 顯示選擇對話框
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                showImageSourceDialog()
            }
            // B. 權限尚未授予 -> 請求權限
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    // 顯示選擇「相機」或「相簿」的對話框
    private fun showImageSourceDialog() {
        val options = arrayOf("拍照", "從相簿選擇")
        AlertDialog.Builder(this)
            .setTitle("選擇圖片來源")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera() // 拍照
                    1 -> openGallery() // 從相簿選擇
                }
            }
            .show()
    }

    private fun openCamera() {
        try {
            // 1. 創建一個臨時檔案的 Uri
            latestTmpUri = getTmpFileUri()
            // 2. 啟動相機
            latestTmpUri?.let { uri ->
                takePictureLauncher.launch(uri)
            } ?: run {
                Toast.makeText(this, "無法準備檔案，請重試。", Toast.LENGTH_LONG).show()
            }
        } catch (e: IOException) {
            Log.e("InputActivity", "Error creating temporary file for camera: ${e.message}")
            Toast.makeText(this, "無法開啟相機，請檢查儲存空間。", Toast.LENGTH_LONG).show()
        }
    }

    private fun openGallery() {
        // 使用 GetContent 方便地開啟相簿並過濾圖片類型
        pickImageLauncher.launch("image/*")
    }

    // 創建一個臨時檔案的 Uri，並使用 FileProvider
    @Throws(IOException::class)
    private fun getTmpFileUri(): Uri {
        val picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        val tmpFile = File.createTempFile(
            "IMG_${System.currentTimeMillis()}_",
            ".jpg",
            picturesDir
        ).apply {
            createNewFile()
        }

        return FileProvider.getUriForFile(
            applicationContext,
            "${applicationContext.packageName}.fileprovider",
            tmpFile
        )
    }

    private fun setupUI() {
        // 1. 顯示當前日期
        val date = Date()
        val formattedDate = DateFormat.format("yyyy年MM月dd日 EEEE", date)
        dateTextView.text = formattedDate

        bindInputDecorations()
        setupEmotionSelector()

        // 2. 設定照片上傳區點擊事件
        photoUploadArea.setOnClickListener {
            checkCameraPermission() // 檢查權限並彈出選擇對話框
        }

        // 3. 設定完成按鈕的點擊事件
        completeButton.setOnClickListener {
            saveDiaryEntry()
        }
    }

    private fun bindInputDecorations() {
        bindDrawableByName(guideWord1ImageView, "guideword1")
        bindDrawableByName(guideWord2ImageView, "guideword2")
        bindDrawableByName(question1ImageView, "inputq1")
        bindDrawableByName(question2ImageView, "inputq2")
        bindDrawableByName(question3ImageView, "inputq3")
        bindDrawableByName(question4ImageView, "inputq4")

        bindDrawableByName(niceEmojiImageView, "niceemoji")
        bindDrawableByName(okEmojiImageView, "okemoji")
        bindDrawableByName(sadEmojiImageView, "sademoji")
    }

    private fun bindDrawableByName(target: ImageView?, drawableName: String) {
        target ?: return
        val resId = resources.getIdentifier(drawableName, "drawable", packageName)
        if (resId != 0) {
            target.setImageResource(resId)
            target.visibility = View.VISIBLE
        } else {
            target.visibility = View.GONE
            Log.w(TAG, "找不到圖片資源：$drawableName")
        }
    }

    private fun setupEmotionSelector() {
        val prompt = emotionPromptTextView ?: return
        val container = emotionContainer ?: return
        val emojis = listOfNotNull(niceEmojiImageView, okEmojiImageView, sadEmojiImageView)
        if (emojis.size < 3) {
            Log.w(TAG, "情緒選擇器資源缺失，略過互動初始化。")
            return
        }

        // 進入頁面預設三顆都縮小，並顯示引導語。
        prompt.visibility = View.VISIBLE
        emojis.forEach {
            it.visibility = View.VISIBLE
            it.alpha = 1f
            it.scaleX = 0.72f
            it.scaleY = 0.72f
            it.translationX = 0f
        }

        emojis.forEach { clicked ->
            clicked.setOnClickListener {
                prompt.visibility = View.GONE

                emojis.filter { it != clicked }.forEach { other ->
                    other.animate()
                        .alpha(0f)
                        .setDuration(260L)
                        .withEndAction { other.visibility = View.INVISIBLE }
                        .start()
                }

                // 將選中的 emoji 回到容器中間，並放大為正常大小。
                container.post {
                    val parentCenterX = container.width / 2f
                    val clickedCenterX = clicked.x + clicked.width / 2f
                    val offsetToCenter = parentCenterX - clickedCenterX

                    clicked.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .translationXBy(offsetToCenter)
                        .alpha(1f)
                        .setDuration(320L)
                        .start()
                }
            }
        }
    }

    private fun saveDiaryEntry() {
        val title = titleEditText.text.toString().trim()
        val photoUriString = currentPhotoUri?.toString()

        val c1 = content1EditText.text.toString().trim()
        val c2 = content2EditText.text.toString().trim()
        val c3 = content3EditText.text.toString().trim()
        val c4 = content4EditText?.text?.toString()?.trim().orEmpty()

        // 合併所有問題和回答，使用換行符 (\n) 分隔
        val fullContent = buildString {
            if (c1.isNotEmpty()) {
                append("$QUESTION_1\n$c1\n")
            }
            if (c2.isNotEmpty()) {
                append("$QUESTION_2\n$c2\n")
            }
            if (c3.isNotEmpty()) {
                append("$QUESTION_3\n$c3\n")
            }
            if (c4.isNotEmpty()) {
                append("$QUESTION_4\n$c4\n")
            }
        }.trim() // 移除字串尾部的多餘換行和空格

        if (title.isBlank() && fullContent.isBlank() && photoUriString == null) {
            Toast.makeText(this, "日記內容不可為空。", Toast.LENGTH_SHORT).show()
            return
        }

        if (title.isNotEmpty() || fullContent.isNotEmpty() || photoUriString != null) {

            // 創建包含 photoUri 的 Note 物件
            val note = Note(
                title = title,
                content = fullContent, // 🌟 使用合併後的內容 🌟
                timestamp = System.currentTimeMillis(),
                photoUri = photoUriString
            )

            // 實際呼叫資料庫的 insertNote 方法
            val result = dbHelper.insertNote(note)

            if (result > 0) {
                Toast.makeText(this, "日記已儲存", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "儲存失敗", Toast.LENGTH_SHORT).show()
            }

        } else {
            Toast.makeText(this, "請至少輸入標題、內容或上傳照片。", Toast.LENGTH_SHORT).show()
        }
    }
}
