package com.hfad.beeradviser

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.RenderEffect
import android.graphics.Shader
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.hfad.beeradviser.data.Plant
import com.hfad.beeradviser.data.PlantStatus
import com.hfad.beeradviser.ui.GuideToIslandFragment
import com.hfad.beeradviser.ui.ManualGuideFragment
import com.hfad.beeradviser.ui.PlantCompletionCardFragment
import com.hfad.beeradviser.ui.PlantPokedexViewModel
import com.hfad.beeradviser.ui.PlantPokedexViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class LevelActivity : AppCompatActivity(), SettingsFragment.SettingsChangeListener {

    companion object {
        private const val TAG = "LevelActivity"
    }

    private lateinit var timerTextView: TextView
    private lateinit var startStopButton: ImageButton
    private lateinit var resetEnergyButton: Button
    private lateinit var energyTextView: TextView
    private lateinit var noteButton: ImageButton
    private lateinit var imageButton4: ImageButton
    private lateinit var gifBadgeImageView: ImageView
    private lateinit var settingsButton: ImageButton
    private lateinit var quizButton: ImageButton
    private lateinit var imageButton8: ImageButton
    private lateinit var plantProgressBar: ProgressBar
    private lateinit var viewModel: PlantPokedexViewModel
    private lateinit var plantImageView: ImageView
    private lateinit var imageButton6: ImageButton
    private lateinit var floatingEnergyAnimation: ImageView
    private lateinit var timerWaveOverlay: ImageView

    // ic_button3 的逐格動畫控制器（播放中/暫停中各一組）
    private var startStopLoopAnimation: android.graphics.drawable.AnimationDrawable? = null
    private var playLoopAnimation: android.graphics.drawable.AnimationDrawable? = null
    private var pauseLoopAnimation: android.graphics.drawable.AnimationDrawable? = null

    private var startTime: Long = 0L
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private var isTimerRunning = false
    private var islandId: Int = -1
    private var showExclamationMark = false
    private var plantBase: Plant? = null
    private var currentActivePlantId: String? = null

    private var totalSecondsElapsed: Long = 0L
    private var currentEnergy: Int = 0
    private val eNERGYRATESECONDS = 1

    private var currentSessionEnergyGenerated: Int = 0
    private val ANIMATION_DURATION_MS = 2000L // 總播放時間 2 秒
    private val FRAME_COUNT = 184
    private val FRAME_DURATION = (ANIMATION_DURATION_MS / FRAME_COUNT).toInt() // 約 10.87 ms/幀
    private val animationHandler = Handler(Looper.getMainLooper())
    private var isStage3AnimationRunning = false
    private var stage3ImageResIds: List<Int> = emptyList()
    private val sTAGE3ANIMATIONDELAYMS = 1000 // 每張圖片顯示 1 秒

    private var islandCompletionJob: Job? = null

    private lateinit var sharedPreferences: SharedPreferences

    private var soundPlayer: MediaPlayer? = null
    private var lastStartStopClickAt = 0L
    private val startStopClickThrottleMs = 250L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level)
        // ************ 【ViewModel 初始化區塊】 ************
        // 1. 取得 Application 實例
        val app = application as PlantPokedexApplication

        // 2. 建立 Factory
        val factory = PlantPokedexViewModelFactory(app.plantRepository)

        // 3. 初始化 ViewModel
        viewModel = ViewModelProvider(this, factory)[PlantPokedexViewModel::class.java]

        timerTextView = findViewById(R.id.timerTextView)
        energyTextView = findViewById(R.id.energyTextView)
        startStopButton = findViewById(R.id.imageButton3)
        noteButton = findViewById(R.id.imageButton2)
        imageButton4 = findViewById(R.id.imageButton4)
        gifBadgeImageView = findViewById(R.id.gif_badge_image)
        resetEnergyButton = findViewById(R.id.resetEnergyButton)
        settingsButton = findViewById(R.id.imageButton1)
        quizButton = findViewById(R.id.imageButton5)
        imageButton8 = findViewById(R.id.imageButton8)
        plantProgressBar = findViewById(R.id.plantProgressBar)
        plantImageView = findViewById(R.id.plant_image_view)
        imageButton6 = findViewById(R.id.imageButton6)
        loadGifIntoImageButton6(imageButton6)
        floatingEnergyAnimation = findViewById(R.id.floatingEnergyAnimation)
        timerWaveOverlay = findViewById(R.id.timerWaveOverlay)


        sharedPreferences = getSharedPreferences(SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE)

        soundPlayer = MediaPlayer.create(this, R.raw.click_sound)


        setButtonState(true)
        // 初始狀態：尚未開始計時，顯示「播放中」迴圈逐格動畫
        updateStartStopButtonLoopAnimation(isTimerRunning = false)

        noteButton.setOnClickListener {
            val intent = Intent(this, ActivityB::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top)
        }

        imageButton4.setOnClickListener {
            // 導航到 PokedexActivity
            if (islandId != -1) {
                val intent = Intent(this, PokedexActivity::class.java).apply {
                    // 將當前的 Level/島嶼 ID 傳遞給 PokedexActivity
                    putExtra("ISLAND_ID", islandId)
                }
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top)
            } else {
                Toast.makeText(this, "錯誤：無法識別當前島嶼，跳轉失敗。", Toast.LENGTH_SHORT).show()
                // 作為備用，如果 levelNumber 為 -1，仍然可以嘗試跳轉，但不傳 ID，PokedexActivity 將使用預設值 1。
                startActivity(Intent(this, PokedexActivity::class.java))
                overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top)
            }
        }

        settingsButton.setOnClickListener {
            val settingsFragment = SettingsFragment()
            settingsFragment.show(supportFragmentManager, "settings_dialog_level")
        }

        quizButton.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java).apply {
                putExtra(QuizActivity.EXTRA_ENTRY_SOURCE, QuizActivity.ENTRY_LEVEL)
                putExtra(QuizActivity.EXTRA_LEVEL_ID, islandId)
            }
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top)
        }

        imageButton6.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag("MANUAL_GUIDE_DIALOG") == null) {
                supportFragmentManager.beginTransaction()
                    .add(android.R.id.content, ManualGuideFragment(), "MANUAL_GUIDE_DIALOG")
                    .commit()
            }
        }

        // 為 imageButton8 設定點擊監聽器，點擊後導航回 MainActivity
        imageButton8.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            finish()
        }

        islandId = intent.getIntExtra("level", -1)

        if (islandId != -1) {

            observeActivePlantStatus()
            observeCompletionEvent() // 觀察完成事件

            timerTextView.text = "00:00"

            loadLevelData(islandId)
            setupLevelUI(islandId)

            val backgroundResId = getBackgroundResourceForLevel(islandId)
            val backgroundFragment = LevelBackgroundFragment.newInstance(backgroundResId)
            supportFragmentManager.commit {
                replace(R.id.levelBackgroundContainer, backgroundFragment)
            }
            observeActivePlantStatus()

        } else {
            timerTextView.text = "錯誤：無法載入關卡"
            energyTextView.text = "N/A"
        }

        resetEnergyButton.setOnClickListener {
            resetEnergyForTest()
        }

        startStopButton.setOnClickListener {
            val now = SystemClock.elapsedRealtime()
            if (now - lastStartStopClickAt < startStopClickThrottleMs) {
                return@setOnClickListener
            }
            lastStartStopClickAt = now

            if (!isTimerRunning) {
                startTimer()
                playTimerWaveEffect(isStartAction = true)
                // 切換成「暫停」逐格動畫（持續循環直到再次被按）
                updateStartStopButtonLoopAnimation(isTimerRunning = true)
            } else {
                stopTimer()
                playTimerWaveEffect(isStartAction = false)
                // 切換成「播放」逐格動畫（持續循環直到再次被按）
                updateStartStopButtonLoopAnimation(isTimerRunning = false)
            }
            playClickSound()
        }

        runnable = Runnable {
            val currentSessionMilliseconds = SystemClock.uptimeMillis() - startTime
            val currentSessionSeconds = (currentSessionMilliseconds / 1000).toInt()

            val minutes = (currentSessionSeconds % 3600) / 60
            val seconds = currentSessionSeconds % 60

            val time = String.format(
                Locale.getDefault(),
                "%02d:%02d",
                minutes,
                seconds
            )
            timerTextView.text = time
            handler.postDelayed(runnable, 100)
        }

        setupRedDotObserver()

    }

    override fun onSoundSettingChanged(enabled: Boolean) {
        if (!enabled) {
            soundPlayer?.stop()
            soundPlayer?.prepareAsync()
        }
        println("LevelActivity：音效設定變更為 $enabled")
    }

    override fun onMusicSettingChanged(enabled: Boolean) {
        val musicServiceIntent = Intent(this, BackgroundMusicService::class.java)
        if (enabled) {
            musicServiceIntent.action = "PLAY"
            startService(musicServiceIntent)
        } else {
            musicServiceIntent.action = "PAUSE"
            startService(musicServiceIntent)
        }
        println("LevelActivity：音樂設定變更為 $enabled (透過服務控制)")
    }

    override fun onApplyBlurEffect(apply: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val rootLayout = findViewById<View>(R.id.level_root_layout)
            rootLayout?.setRenderEffect(
                if (apply) RenderEffect.createBlurEffect(
                    15f,
                    15f,
                    Shader.TileMode.CLAMP
                ) else null
            )
        } else {
            println("Android 版本低於 API 31，無法應用 RenderEffect 模糊。")
        }
    }

    /**
     * 依據計時狀態，切換 ic_button3 的逐格動畫。
     * - 計時中：ic_mediapause_000 ~ ic_mediapause_038
     * - 暫停中：ic_mediaplay_000 ~ ic_mediaplay_037
     * 目標播放速率：15 FPS（每幀約 66ms）
     */
    private fun updateStartStopButtonLoopAnimation(isTimerRunning: Boolean) {
        startStopLoopAnimation?.stop()
        ensureStartStopAnimationsCached()

        val animation = if (isTimerRunning) pauseLoopAnimation else playLoopAnimation

        if (animation != null) {
            startStopLoopAnimation = animation
            startStopButton.setImageDrawable(animation)
            animation.start()
        } else {
            // 若尚未匯入逐格圖，保留可執行性（fallback 到既有靜態圖）
            val fallbackRes = if (isTimerRunning) R.drawable.ic_media_pause else R.drawable.ic_media_play
            startStopButton.setImageResource(fallbackRes)
        }
    }

    private fun ensureStartStopAnimationsCached() {
        if (playLoopAnimation == null) {
            playLoopAnimation = buildFrameAnimation(prefix = "ic_mediaplay_", start = 0, end = 37, fps = 15)
        }
        if (pauseLoopAnimation == null) {
            pauseLoopAnimation = buildFrameAnimation(prefix = "ic_mediapause_", start = 0, end = 38, fps = 15)
        }
    }

    /**
     * 動態組裝逐格動畫，避免在 XML animation-list 內綁死資源，
     * 讓你只要把對應檔名的 PNG 匯入即可直接生效。
     */
    private fun buildFrameAnimation(
        prefix: String,
        start: Int,
        end: Int,
        fps: Int,
        scaleInset: Int = 250
    ): android.graphics.drawable.AnimationDrawable? {
        val frameDurationMs = (1000f / fps).toInt().coerceAtLeast(1)
        val animation = android.graphics.drawable.AnimationDrawable().apply {
            isOneShot = false
        }

        for (index in start..end) {
            val frameName = String.format(Locale.US, "%s%03d", prefix, index)
            val frameResId = resources.getIdentifier(frameName, "drawable", packageName)

            if (frameResId == 0) {
                Log.w(TAG, "逐格動畫缺少資源：$frameName，改用 fallback 靜態圖。")
                return null
            }

            val frameDrawable = resources.getDrawable(frameResId, theme)

            // --- 修改部分：套用負向 Inset ---
            val finalDrawable = if (scaleInset != 0) {
                // 傳入負值會使 Drawable 向外擴張，達成放大效果
                // 四個參數分別為：left, top, right, bottom
                android.graphics.drawable.InsetDrawable(frameDrawable, -scaleInset)
            } else {
                frameDrawable
            }
            animation.addFrame(finalDrawable, frameDurationMs)
        }

        return animation
    }

    /**
     * 播放開始/暫停時的擴散補間效果。
     * - 開始：tweenani_play
     * - 暫停：tweenani_pause
     * 效果：從按鈕中心勻速放大到覆蓋全畫面，並同步淡出。
     */
    private fun playTimerWaveEffect(isStartAction: Boolean) {
        val drawableName = if (isStartAction) "tweenani_play" else "tweenani_pause"
        val resId = resources.getIdentifier(drawableName, "drawable", packageName)
        if (resId == 0) {
            Log.w(TAG, "找不到補間動畫資源：$drawableName，略過擴散效果。")
            return
        }

        timerWaveOverlay.animate().cancel()
        timerWaveOverlay.setImageResource(resId)
        timerWaveOverlay.visibility = View.VISIBLE
        timerWaveOverlay.alpha = 0.9f
        timerWaveOverlay.scaleX = 4f
        timerWaveOverlay.scaleY = 4f

        // 以螢幕對角線計算需要的放大倍率，確保覆蓋全畫面。
        val root = findViewById<View>(R.id.level_root_layout)
        val baseSize = maxOf(timerWaveOverlay.width, timerWaveOverlay.height, 1)
        val diagonal = kotlin.math.hypot(root.width.toDouble(), root.height.toDouble()).toFloat()
        val targetScale = ((diagonal / baseSize) * 1.2f).coerceAtLeast(1.5f)

        timerWaveOverlay.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .alpha(0f)
            .setDuration(900L)
            .setInterpolator(android.view.animation.LinearInterpolator())
            .withEndAction {
                timerWaveOverlay.visibility = View.GONE
                timerWaveOverlay.alpha = 0f
                timerWaveOverlay.scaleX = 1f
                timerWaveOverlay.scaleY = 1f
            }
            .start()
    }

    private fun playClickSound() {
        val isSoundEnabled = sharedPreferences.getBoolean(SettingsFragment.KEY_SOUND_ENABLED, true)
        if (isSoundEnabled) {
            soundPlayer?.seekTo(0)
            soundPlayer?.start()
        }
    }

    private fun getBackgroundResourceForLevel(level: Int): Int {
        return when (level) {
            1 -> R.drawable.level_1_background
            2 -> R.drawable.level_2_background
            3 -> R.drawable.level_3_background
            4 -> R.drawable.level_4_background
            5 -> R.drawable.level_5_background
            else -> R.drawable.default_level_background
        }
    }

    private fun startTimer() {
        // 開始計時時，禁用按鈕
        Log.d("LevelActivity", "startTimer: 當前植物 ID 為 ${currentActivePlantId ?: "NULL"}")
        setButtonState(false)
        startFloatingEnergyAnimation()
        startTime = SystemClock.uptimeMillis()
        timerTextView.text = "00:00"
        currentSessionEnergyGenerated = 0

        handler.postDelayed(runnable, 0)
        isTimerRunning = true
    }

    private fun stopTimer() {
        if (isTimerRunning) {
            handler.removeCallbacks(runnable)
            isTimerRunning = false
            setButtonState(true) // 啟用按鈕
            stopFloatingEnergyAnimation()

            // 1. 取得本次專注的總秒數
            val currentSessionSeconds = (SystemClock.uptimeMillis() - startTime) / 1000
            // 2. 根據您的能量規則計算獲得的能量
            val energyGained = (currentSessionSeconds.toInt() / eNERGYRATESECONDS)

            Log.d(
                "LevelActivity",
                "stopTimer: 準備寫入數據，當前植物 ID 為 ${currentActivePlantId ?: "NULL"}"
            )
            Log.d(
                "LevelActivity",
                "本次專注時長: ${currentSessionSeconds} 秒，獲得能量: ${energyGained}"
            )

            // 3. 將 DataStore 寫入和 UI 刷新邏輯移至協程中處理
            if (currentActivePlantId != null && energyGained > 0) {
                val plantId = currentActivePlantId!!

                // 啟動一個協程來執行 suspend 函式
                lifecycleScope.launch {
                    try {
                        // a. 呼叫 ViewModel 執行數據更新 (包含 Stage 升級邏輯)
                        // 這個 suspend 函式會在 IO 線程中完成 DataStore 寫入
                        viewModel.updatePlantEnergyAndCheckStageUp(plantId, energyGained)

                        // d. 顯示 Toast 提示
                        Toast.makeText(
                            this@LevelActivity,
                            "專注結束！獲得 ${energyGained} 能量。",
                            Toast.LENGTH_LONG
                        ).show()

                    } catch (e: Exception) {
                        Log.e(TAG, "stopTimer: 數據寫入或手動更新 UI 失敗: ${e.message}", e)
                        Toast.makeText(this@LevelActivity, "數據更新失敗。", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } else if (energyGained == 0) {
                Toast.makeText(this, "專注時間太短，未獲得能量。", Toast.LENGTH_SHORT).show()
            }

            // 4. 重設計時器顯示 (放在 if/else 外部)
            timerTextView.text = "00:00"
        }
    }


    // 一個輔助函數，用來統一控制按鈕的啟用/禁用狀態和視覺效果
    private fun setButtonState(enabled: Boolean) {
        val alpha = if (enabled) 1.0f else 0.5f // 啟用時完全不透明，禁用時半透明
        imageButton4.isClickable = enabled
        imageButton4.alpha = alpha
        noteButton.isClickable = enabled
        noteButton.alpha = alpha
        imageButton8.isClickable = enabled
        imageButton8.alpha = alpha
    }

    private fun updatePlantUI(status: PlantStatus, base: Plant) {

        // 1. 處理圖像切換
        val imageResId = when (status.stage) {
            1 -> base.stageOneImageRes
            2 -> base.stageTwoImageRes
            3 -> base.stageThreeImageRes
            else -> R.drawable.plant_default_default_sdefault
        }
        plantImageView.setImageResource(imageResId)

        // 2. 處理進度條和能量文字
        val totalRequired = base.totalEnergyRequired

        // 計算當前階段的目標、起點和長度
        val energyToNextStage = when (status.stage) {
            1 -> totalRequired / 2
            2 -> totalRequired
            else -> 0
        }
        val startEnergy = if (status.stage == 2) totalRequired / 2 else 0

        val currentProgressEnergy = status.currentEnergy - startEnergy
        val totalLength = energyToNextStage - startEnergy

        if (totalLength > 0 && status.stage < 3) {
            // 計算當前階段的百分比
            val progressPercent = (currentProgressEnergy.toDouble() / totalLength * 100).toInt()

            plantProgressBar.max = 100
            plantProgressBar.progress = progressPercent

            // 更新 TextView 顯示即時進度
            energyTextView.text =
                " ${currentProgressEnergy} / ${totalLength} "

        } else {
            // Stage 3 (已完成) 或 Stage 0/錯誤
            plantProgressBar.progress = if (status.stage == 3) 100 else 0
            energyTextView.text = if (status.stage == 3) "🌱" else "準備中..."
        }
    }

    // 觀察植物完成事件，並在條件符合時顯示字卡
    private fun observeCompletionEvent() {
        viewModel.plantCompletedEvent.observe(this) { completedPlant ->
            // 確保事件非空，且 ID 有效
            if (completedPlant != null && completedPlant.id.isNotEmpty()) {

                // 檢查 plantIndex：只有索引為 1 或 2 的植物才顯示字卡
                if (completedPlant.plantIndex == 1 || completedPlant.plantIndex == 2) {

                    // 1. 顯示字卡 (DialogFragment)，並傳遞植物 ID
                    val cardFragment = PlantCompletionCardFragment.newInstance(completedPlant.id)
                    cardFragment.show(supportFragmentManager, "PLANT_COMPLETED_CARD")

                    Log.i(TAG, "植物 ${completedPlant.id} (Index ${completedPlant.plantIndex}) 完成，顯示字卡。")

                } else if (completedPlant.plantIndex == 3) {
                    // 索引 3 的植物（最後一株）不顯示字卡，讓程式碼進入 Stage 3 邏輯
                    Log.i(TAG, "植物 ${completedPlant.id} (Index 3) 完成，跳過字卡，等待島嶼完成引導。")
                    // 這裡不需要額外操作，Stage 3 狀態的推送會觸發 updatePlantUIAndControls 內的島嶼完成檢查
                }
            }
        }
    }

    private fun loadLevelData(levelNumber: Int) {
        Log.d("LevelActivity", "loadLevelData: 處理關卡 $levelNumber 的靜態 UI 設置。")
    }

    private fun setupLevelUI(level: Int) {
        println("設置第 $level 關的 UI...")
    }

    private fun resetEnergyForTest() {
        if (isTimerRunning) {
            stopTimer()
        }

        // 呼叫 ViewModel 執行重設邏輯
        currentActivePlantId?.let {
            viewModel.resetPlantEnergy(it)
            Toast.makeText(this@LevelActivity, "能量已重設為零！", Toast.LENGTH_SHORT).show()
        }

        totalSecondsElapsed = 0L
        currentEnergy = 0

        energyTextView.text = "0"
        timerTextView.text = "00:00"
    }


    private fun observeActivePlantStatus() {
        // 取得當前 LevelActivity 的目標島嶼 ID
        val currentIslandId = this.islandId

        if (currentIslandId == -1) {
            Log.e(TAG, "FATAL: LevelActivity 的 islandId 尚未設置，無法觀察狀態。")
            return
        }

        viewModel.currentActivePlantStatus.observe(this) { status: PlantStatus ->
            val plantId = status.id

            // 1. 忽略初始/空狀態
            if (plantId == null || plantId == "0_0") {
                Log.d(TAG, "忽略初始/空狀態 (ID: ${status.id})")
                return@observe
            }

            // 收到來自不同島嶼的 LiveData 更新，直接忽略，解決 UI 閃爍問題。
            if (status.islandId != currentIslandId) {
                Log.w(TAG, "LVA: 忽略跨島狀態。當前 Level ID ${currentIslandId}，收到 Plant ID ${plantId} (島嶼 ${status.islandId})。")
                return@observe
            }

            // 2. 活躍 ID 追蹤與切換 (保持原邏輯)
            if (currentActivePlantId != plantId) {
                Log.i(TAG, "LVA: 活躍植物 ID 已從 ${currentActivePlantId} 切換到 ${plantId}")
                currentActivePlantId = plantId
            }

            // 3. 確保 currentActivePlantId 被設置（若尚未設置）
            // 在新邏輯中，由於 LiveData 是異步的，我們假設 LiveData 提供的 plantId 就是我們要用的。
            // 如果 currentActivePlantId 是 null (只有在 Activity 剛啟動時可能)，則設置它。
            if (currentActivePlantId == null) {
                currentActivePlantId = plantId
            }

            // 4. 【核心檢查】：確保 plantBase 存在且 ID 匹配
            var base = plantBase
            if (base == null || base.id != plantId) {
                // 如果 plantBase 缺失或 ID 不匹配，我們**必須**在這裡異步載入它。
                lifecycleScope.launch {
                    Log.d(TAG, "LVA: PlantBase 缺失或 ID 不匹配，啟動載入 (${plantId})")
                    base = viewModel.getPlantById(plantId)
                    plantBase = base

                    if (base != null) {
                        // 載入完成後，執行 UI 更新
                        updatePlantUIAndControls(status)
                        Log.d(TAG, "LVA: PlantBase 載入完成，UI 刷新成功。")
                    }
                }
                return@observe // 暫時跳過本次，等待載入完成後的重新觸發
            }

            // 5. 如果 plantBase 已經載入且 ID 匹配，直接執行 UI 更新
            Log.d(TAG, "LVA: 觀察到植物狀態更新：Stage=${status.stage}, Energy=${status.currentEnergy}")
            updatePlantUIAndControls(status)
        }
    }

    private fun updatePlantUIAndControls(status: PlantStatus) {
        val base = plantBase ?: return

        // 1. 如果已經是 Stage 3 且島嶼已完成，我們不希望 updatePlantUI 覆蓋掉動畫
        // 所以只有在非 Stage 3 時才更新基本 UI
        if (status.stage < 3) {
            updatePlantUI(status, base)
            stopStage3Animation() // 確保非完成狀態下動畫是停止的
        }

        when (status.stage) {
            1, 2 -> {
                if (!isTimerRunning) {
                    setTimerControlsDisabled(false)
                    timerTextView.text = "00:00"
                    timerTextView.visibility = View.VISIBLE
                }
            }

            3 -> {
                setTimerControlsDisabled(true, isFullyCompleted = true)
                timerTextView.visibility = View.VISIBLE
                timerTextView.text = ""

                // 2. 處理島嶼完成邏輯
                if (islandCompletionJob?.isActive == true) {
                    // 如果 Job 已經在確認完成了，我們不需要重複啟動
                    return
                }

                islandCompletionJob = lifecycleScope.launch {
                    val islandIsFullyCompleted = viewModel.checkIslandCompletionStatus(base.islandId)
                    if (!isActive) return@launch

                    if (islandIsFullyCompleted) {
                        Log.i(TAG, "島嶼 ${base.islandId} 確認完成。")

                        // 3. 準備資源並啟動動畫
                        val resources = getStage3AnimationResources(base.islandId)
                        if (resources.isNotEmpty()) {
                            stage3ImageResIds = resources
                            // 強制啟動，即使當前正在運行也重新校正
                            startStage3Animation()
                        }

                        // 4. 顯示引導字卡 (內部有 SharedPreferences 判斷)
                        showReturnToIslandGuide()
                    } else {
                        // 如果島嶼未全完，只顯示單張 Stage 3 靜態圖
                        updatePlantUI(status, base)
                    }
                }
            }

            else -> {
                setTimerControlsDisabled(true, isFullyCompleted = false)
                stopStage3Animation()
            }
        }
    }


    private fun setTimerControlsDisabled(disabled: Boolean, isFullyCompleted: Boolean = false) {
        if (disabled) {
            startStopButton.isEnabled = false
            startStopButton.alpha = 0.5f
            if (isFullyCompleted) {
                // 如果是完全完成，確保計時器不顯示
                timerTextView.text = ""
            } else {
                // 可能是 Stage 0 或其他禁用狀態
                timerTextView.text = "00:00"
            }
        } else {
            // 啟用計時器 (用於 Stage 1 或 2)
            startStopButton.isEnabled = true
            startStopButton.alpha = 1.0f
        }
    }

    private fun updatePokedexButtonUI() {
        val context = this

        if (showExclamationMark) {
            // 1. 顯示 GIF 動畫作為徽章

            // 設定主按鈕為無紅點的靜態圖 (GIF 會覆蓋紅點區域)
            imageButton4.setImageResource(R.drawable.ic_button4)

            // 載入 GIF 到徽章 View
            Glide.with(context)
                .load(R.drawable.ic_button4_reddot) // 假設這是您的 GIF 資源 ID
                .into(gifBadgeImageView)

            // 讓 GIF 徽章可見
            gifBadgeImageView.visibility = View.VISIBLE

        } else {
            // 2. 停止 GIF 動畫並切換回靜態圖片

            // 停止 Glide 播放的 GIF 動畫，釋放資源
            Glide.with(context).clear(gifBadgeImageView)

            // 讓 GIF 徽章隱藏
            gifBadgeImageView.visibility = View.GONE

            // 顯示無紅點/驚嘆號的靜態圖片
            imageButton4.setImageResource(R.drawable.ic_button4)
        }
    }

    private fun setupRedDotObserver() {
        // 觀察 ViewModel 公開的 LiveData
        viewModel.shouldShowPokedexExclamation.observe(this) { isAvailable ->
            Log.d(TAG, "紅點狀態觀察者：圖鑑按鈕紅點狀態更新為 ${isAvailable}")

            // 1. 更新 LevelActivity 內的 showExclamationMark 變數
            showExclamationMark = isAvailable

            // 2. 呼叫您已定義的 UI 刷新函式
            updatePokedexButtonUI()
        }
    }

    private fun loadGifIntoImageButton6(button: ImageButton) {
        val gifResId = R.drawable.animated_button6
        Glide.with(this)
            .asGif()
            .load(gifResId)
            .fitCenter()
            .into(button)

        Log.d(TAG, "ImageButton6 GIF Level 載入完成。")
    }

    private fun startFloatingEnergyAnimation() {
        // 1. 設置 AnimationDrawable
        val animation = android.graphics.drawable.AnimationDrawable()
        for (i in 0 until FRAME_COUNT) {
            val resourceName = String.format(Locale.US, "energy_%05d", i)
            val frameId = resources.getIdentifier(resourceName, "drawable", packageName)

            if (frameId != 0) {
                animation.addFrame(resources.getDrawable(frameId, theme), FRAME_DURATION)
            } else {
                Log.e(TAG, "找不到資源幀: $resourceName")
                return
            }
        }

        floatingEnergyAnimation.setImageDrawable(animation)
        floatingEnergyAnimation.visibility = View.VISIBLE

        // 2. 開始播放動畫
        animation.isOneShot = false // 循環播放
        animation.start()
    }

    private fun stopFloatingEnergyAnimation() {
        val drawable = floatingEnergyAnimation.drawable
        if (drawable is android.graphics.drawable.AnimationDrawable) {
            if (drawable.isRunning) {
                drawable.stop()
            }
        }
        floatingEnergyAnimation.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()

        // 停止計時器 (如果還在運行)
        if (isTimerRunning) {
            stopTimer()
        }

        // 停止 Stage 3 輪播動畫
        stopStage3Animation()

        startStopLoopAnimation?.stop()
        timerWaveOverlay.animate().cancel()
        timerWaveOverlay.visibility = View.GONE

        soundPlayer?.release()
        soundPlayer = null
    }

    override fun onResume() {
        super.onResume()
        val isSoundEnabled = sharedPreferences.getBoolean(SettingsFragment.KEY_SOUND_ENABLED, true)
        onSoundSettingChanged(isSoundEnabled)

        if (soundPlayer == null) {
            soundPlayer = MediaPlayer.create(this, R.raw.click_sound)
        }

        // 在 onResume 中確保按鈕處於正確的初始狀態
        setButtonState(true)
        // 初始狀態：尚未開始計時，顯示「播放中」迴圈逐格動畫
        updateStartStopButtonLoopAnimation(isTimerRunning = false)
        // 在 Activity 重新啟動/返回時更新驚嘆號狀態
        updatePokedexButtonUI()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun getStage3AnimationResources(level: Int): List<Int> {
        return when (level) {
            1 -> listOf(
                R.drawable.plant_1_1_s3,
                R.drawable.plant_1_2_s3,
                R.drawable.plant_1_3_s3
            )

            2 -> listOf(
                R.drawable.plant_2_1_s3,
                R.drawable.plant_2_2_s3,
                R.drawable.plant_2_3_s3
            )

            3 -> listOf(
                R.drawable.plant_3_1_s3,
                R.drawable.plant_3_2_s3,
                R.drawable.plant_3_3_s3
            )
            4 -> listOf(
             R.drawable.plant_4_1_s3,
             R.drawable.plant_4_2_s3,
             R.drawable.plant_4_3_s3
            )
            5 -> listOf(
             R.drawable.plant_5_1_s3,
             R.drawable.plant_5_2_s3,
             R.drawable.plant_5_3_s3
            )
            else -> emptyList()
        }
    }

    private fun startStage3Animation() {
        if (isStage3AnimationRunning) return

        Log.d(TAG, "startStage3Animation: 開始播放島嶼完成動畫")
        isStage3AnimationRunning = true
        var currentIndex = 0

        val animationRunnable = object : Runnable {
            override fun run() {
                if (isStage3AnimationRunning && stage3ImageResIds.isNotEmpty()) {
                    // 這裡使用 post 確保在 UI 執行緒執行
                    plantImageView.setImageResource(stage3ImageResIds[currentIndex])
                    currentIndex = (currentIndex + 1) % stage3ImageResIds.size
                    animationHandler.postDelayed(this, sTAGE3ANIMATIONDELAYMS.toLong())
                }
            }
        }
        animationHandler.removeCallbacksAndMessages(null)
        animationHandler.post(animationRunnable)
    }


    private fun stopStage3Animation() {
        if (isStage3AnimationRunning) {
            Log.d(TAG, "stopStage3Animation: 停止動畫")
            isStage3AnimationRunning = false
            animationHandler.removeCallbacksAndMessages(null)
        }
    }


    /**
     * 顯示島嶼完成引導字卡 (僅在第一次完成時觸發)
     */
    private fun showReturnToIslandGuide() {
        val guideKey = "ISLAND_GUIDE_SHOWN_${islandId}"
        val isGuideShown = sharedPreferences.getBoolean(guideKey, false)

        if (!isGuideShown) {
            Log.i(TAG, "第一次完成：顯示引導字卡。")
            onApplyBlurEffect(true)
            val guideFragment = GuideToIslandFragment()
            supportFragmentManager.commit {
                add(android.R.id.content, guideFragment, "GUIDE_TO_ISLAND")
                addToBackStack(null)
            }
            sharedPreferences.edit().putBoolean(guideKey, true).apply()
        } else {
            Log.i(TAG, "非第一次完成：僅執行 Stage 3 動畫，不彈出字卡。")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
        soundPlayer?.release()
        soundPlayer = null
        islandCompletionJob?.cancel()
    }
}
