package com.hfad.beeradviser

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import java.io.File
import java.io.IOException
import java.util.Date
import kotlin.math.abs

class InputIntroFragment : Fragment() {

    interface Callback {
        fun onSwipeUpToQuestionsRequested()
    }

    companion object {
        private const val TAG = "InputIntroFragment"
        private const val SWIPE_THRESHOLD_DP = 80
    }

    private val draftViewModel: InputDraftViewModel by activityViewModels()

    private lateinit var dateTextView: TextView
    private lateinit var titleEditText: EditText
    private lateinit var photoUploadArea: ImageView
    private lateinit var upSwipeHintImageView: ImageView

    private var emotionPromptTextView: TextView? = null
    private var emotionContainer: View? = null
    private var niceEmojiImageView: ImageView? = null
    private var okEmojiImageView: ImageView? = null
    private var sadEmojiImageView: ImageView? = null
    private var guideWord1ImageView: ImageView? = null
    private var guideWord2ImageView: ImageView? = null

    private var latestTmpUri: Uri? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                showImageSourceDialog()
            } else {
                Toast.makeText(requireContext(), "需要相機權限才能拍照。", Toast.LENGTH_SHORT).show()
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                latestTmpUri?.let { uri ->
                    photoUploadArea.setImageURI(uri)
                    draftViewModel.photoUri = uri.toString()
                }
            } else {
                Toast.makeText(requireContext(), "拍照失敗或取消。", Toast.LENGTH_SHORT).show()
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                runCatching {
                    requireContext().contentResolver.takePersistableUriPermission(it, flag)
                }
                photoUploadArea.setImageURI(it)
                draftViewModel.photoUri = it.toString()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_input_intro, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dateTextView = view.findViewById(R.id.dateTextView)
        titleEditText = view.findViewById(R.id.titleEditText)
        photoUploadArea = view.findViewById(R.id.photoUploadArea)
        upSwipeHintImageView = view.findViewById(R.id.upSwipeHintImageView)

        emotionPromptTextView = view.findViewById(R.id.emotionPromptTextView)
        emotionContainer = view.findViewById(R.id.emotionSelectorContainer)
        niceEmojiImageView = view.findViewById(R.id.niceEmojiImageView)
        okEmojiImageView = view.findViewById(R.id.okEmojiImageView)
        sadEmojiImageView = view.findViewById(R.id.sadEmojiImageView)
        guideWord1ImageView = view.findViewById(R.id.guideWord1ImageView)
        guideWord2ImageView = view.findViewById(R.id.guideWord2ImageView)

        setupUI()
    }

    private fun setupUI() {
        val date = Date()
        dateTextView.text = DateFormat.format("yyyy年MM月dd日 EEEE", date)

        bindInputDecorations()
        setupEmotionSelector()

        titleEditText.setText(draftViewModel.title)
        titleEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                draftViewModel.title = s?.toString().orEmpty()
            }
        })

        draftViewModel.photoUri?.let { uri ->
            photoUploadArea.setImageURI(Uri.parse(uri))
        }

        photoUploadArea.setOnClickListener {
            checkCameraPermission()
        }

        setupSwipeHintTouch()
        startFloatingAnimation(upSwipeHintImageView)
        refreshEmotionVisualState()
    }

    private fun setupSwipeHintTouch() {
        val thresholdPx = SWIPE_THRESHOLD_DP * resources.displayMetrics.density
        var startY = 0f

        upSwipeHintImageView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val dy = event.rawY - startY
                    if (dy < 0 && abs(dy) >= thresholdPx) {
                        (activity as? Callback)?.onSwipeUpToQuestionsRequested()
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun startFloatingAnimation(view: View) {
        ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, -12f, 0f).apply {
            duration = 1400L
            repeatCount = Animation.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun bindInputDecorations() {
        bindDrawableByName(guideWord1ImageView, "guideword1")
        bindDrawableByName(guideWord2ImageView, "guideword2")

        bindDrawableByName(niceEmojiImageView, "niceemoji")
        bindDrawableByName(okEmojiImageView, "okemoji")
        bindDrawableByName(sadEmojiImageView, "sademoji")
    }

    private fun bindDrawableByName(target: ImageView?, drawableName: String) {
        target ?: return
        val resId = resources.getIdentifier(drawableName, "drawable", requireContext().packageName)
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

        prompt.visibility = View.VISIBLE
        emojis.forEach {
            it.visibility = View.VISIBLE
            it.alpha = 1f
            it.scaleX = 0.72f
            it.scaleY = 0.72f
            it.translationX = 0f
        }

        val mapping = mapOf(
            niceEmojiImageView to InputDraftViewModel.Emotion.NICE,
            okEmojiImageView to InputDraftViewModel.Emotion.OK,
            sadEmojiImageView to InputDraftViewModel.Emotion.SAD
        )

        emojis.forEach { clicked ->
            clicked.setOnClickListener {
                prompt.visibility = View.GONE
                draftViewModel.emotion = mapping[clicked]

                emojis.filter { it != clicked }.forEach { other ->
                    other.animate().alpha(0.35f).scaleX(0.72f).scaleY(0.72f).setDuration(220L).start()
                }

                container.post {
                    val parentCenterX = container.width / 2f
                    val clickedCenterX = clicked.x + clicked.width / 2f
                    val offsetToCenter = parentCenterX - clickedCenterX

                    clicked.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .translationXBy(offsetToCenter)
                        .alpha(1f)
                        .setDuration(260L)
                        .start()
                }
            }
        }
    }

    private fun refreshEmotionVisualState() {
        val selected = draftViewModel.emotion ?: return
        val selectedView = when (selected) {
            InputDraftViewModel.Emotion.NICE -> niceEmojiImageView
            InputDraftViewModel.Emotion.OK -> okEmojiImageView
            InputDraftViewModel.Emotion.SAD -> sadEmojiImageView
        } ?: return

        emotionPromptTextView?.visibility = View.GONE

        val all = listOfNotNull(niceEmojiImageView, okEmojiImageView, sadEmojiImageView)
        all.filter { it != selectedView }.forEach {
            it.alpha = 0.35f
            it.scaleX = 0.72f
            it.scaleY = 0.72f
            it.translationX = 0f
        }

        val container = emotionContainer as? LinearLayout ?: return
        container.post {
            val parentCenterX = container.width / 2f
            val clickedCenterX = selectedView.x + selectedView.width / 2f
            val offsetToCenter = parentCenterX - clickedCenterX
            selectedView.translationX = offsetToCenter
            selectedView.scaleX = 1f
            selectedView.scaleY = 1f
            selectedView.alpha = 1f
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                showImageSourceDialog()
            }
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("拍照", "從相簿選擇")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("選擇圖片來源")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openCamera() {
        try {
            latestTmpUri = getTmpFileUri()
            latestTmpUri?.let { uri ->
                takePictureLauncher.launch(uri)
            } ?: run {
                Toast.makeText(requireContext(), "無法準備檔案，請重試。", Toast.LENGTH_LONG).show()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error creating temporary file for camera: ${e.message}")
            Toast.makeText(requireContext(), "無法開啟相機，請檢查儲存空間。", Toast.LENGTH_LONG).show()
        }
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    @Throws(IOException::class)
    private fun getTmpFileUri(): Uri {
        val picturesDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val tmpFile = File.createTempFile(
            "IMG_${System.currentTimeMillis()}_",
            ".jpg",
            picturesDir
        ).apply {
            createNewFile()
        }

        return FileProvider.getUriForFile(
            requireContext().applicationContext,
            "${requireContext().applicationContext.packageName}.fileprovider",
            tmpFile
        )
    }
}
