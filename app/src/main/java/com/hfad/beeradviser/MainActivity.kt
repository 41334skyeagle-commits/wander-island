package com.hfad.beeradviser

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.hfad.beeradviser.ui.MainViewModel
import com.hfad.beeradviser.ui.MainViewModelFactory

sealed class IslandBackground {
    data class SingleImage(val drawableRes: Int) : IslandBackground()
    data class CompositeLayout(val layoutRes: Int) : IslandBackground()
}

class MainActivity : AppCompatActivity(), SettingsFragment.SettingsChangeListener {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var backgroundContainer: FrameLayout
    private lateinit var viewModel: MainViewModel
    private lateinit var sharedPreferences: SharedPreferences
    private var previousUnlockedIslandCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViewModel()
        initViews()
        setupViewPager()

        sharedPreferences = getSharedPreferences(SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE)
        applyMusicSetting(sharedPreferences.getBoolean(SettingsFragment.KEY_MUSIC_ENABLED, true))
    }

    private fun initViewModel() {
        val app = application as PlantPokedexApplication
        val factory = MainViewModelFactory(app.plantRepository)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
    }

    private fun initViews() {
        viewPager = findViewById(R.id.levelViewPager)
        backgroundContainer = findViewById(R.id.backgroundContainer)

        val animatedButton6 = findViewById<ImageButton>(R.id.imageButton6)
        loadGifIntoButton(animatedButton6, R.drawable.animated_button6)

        findViewById<ImageButton>(R.id.imageButton2).setOnClickListener {
            startActivity(Intent(this, ActivityB::class.java))
        }
        findViewById<ImageButton>(R.id.imageButton4).setOnClickListener {
            startActivity(Intent(this, PokedexActivity::class.java))
        }
        findViewById<ImageButton>(R.id.imageButton1).setOnClickListener {
            SettingsFragment().show(supportFragmentManager, "settings_dialog_main")
        }
    }

    private fun setupViewPager() {
        val levelStaticImageResIds = listOf(
            R.drawable.level_1_icon, R.drawable.level_2_icon,
            R.drawable.level_3_icon, R.drawable.level_4_icon, R.drawable.level_5_icon
        )

        val levelAdapter = LevelPagerAdapter(this, levelStaticImageResIds) { islandId ->
            onIslandClicked(islandId)
        }
        viewPager.adapter = levelAdapter

        viewModel.islands.observe(this) { islands ->
            // 更新數據並觸發 getItemId 變化
            levelAdapter.updateIslands(islands)

            val currentUnlockedCount = islands.count { it.isUnlocked }

            if (previousUnlockedIslandCount == 0 && currentUnlockedCount > 0) {
                val initialPos = currentUnlockedCount - 1
                viewPager.setCurrentItem(initialPos, false)
                showBackground(islandBackgroundFor(initialPos))
            } else if (currentUnlockedCount > previousUnlockedIslandCount) {
                // 使用 post 確保跳轉是在 Adapter 完成刷新後才執行
                viewPager.post {
                    viewPager.setCurrentItem(currentUnlockedCount - 1, true)
                }
            }
            previousUnlockedIslandCount = currentUnlockedCount
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val bg = islandBackgroundFor(position)
                showBackground(bg)
            }
        })
    }

    private fun islandBackgroundFor(position: Int): IslandBackground {
        return when (position) {
            0 -> IslandBackground.SingleImage(R.drawable.island_1_bg)
            1 -> IslandBackground.CompositeLayout(R.layout.bg_island_2)
            2 -> IslandBackground.CompositeLayout(R.layout.bg_island_3)
            3 -> IslandBackground.SingleImage(R.drawable.island_4_bg)
            4 -> IslandBackground.SingleImage(R.drawable.island_5_bg)
            else -> IslandBackground.SingleImage(R.drawable.island_1_bg)
        }
    }

    private fun showBackground(background: IslandBackground) {
        backgroundContainer.removeAllViews()

        when (background) {
            is IslandBackground.SingleImage -> {
                val imageView = ImageView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
                backgroundContainer.addView(imageView)
                Glide.with(this).load(background.drawableRes).centerCrop().into(imageView)
            }
            is IslandBackground.CompositeLayout -> {
                val layoutView = layoutInflater.inflate(background.layoutRes, backgroundContainer, false)
                backgroundContainer.addView(layoutView)
                // 掃描佈局內需要播放 GIF 的元件
                startGifsInLayout(layoutView)
            }
        }
    }

    private fun startGifsInLayout(view: View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                startGifsInLayout(view.getChildAt(i))
            }
        } else if (view is ImageView) {
            try {
                val idName = resources.getResourceEntryName(view.id)
                // 只要 ID 裡面有 "gif"，就抓取它在 XML 設定的資源並啟動播放
                if (idName.contains("gif", ignoreCase = true)) {
                    val resId = when (view.id) {
                        R.id.island2_meteor_gif -> R.drawable.island2_meteor_gif
                        R.id.island3_cloud_bottom_gif -> R.drawable.island3_cloud_bottom
                        R.id.island3_cloud_top_gif -> R.drawable.island3_cloud_top
                        else -> 0
                    }

                    if (resId != 0) {
                        Glide.with(this)
                            .asGif()
                            .load(resId)
                            .into(view)
                        Log.d(TAG, "啟動佈局 GIF: $idName")
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun onIslandClicked(islandId: Int) {
        viewModel.selectAndSetNextActivePlant(islandId)
        val intent = Intent(this, LevelActivity::class.java).apply {
            putExtra("level", islandId)
        }
        startActivity(intent)
    }

    private fun loadGifIntoButton(button: ImageButton, resId: Int) {
        Glide.with(this).asGif().load(resId).override(Target.SIZE_ORIGINAL).into(button)
    }

    private fun applyMusicSetting(enabled: Boolean) {
        val intent = Intent(this, BackgroundMusicService::class.java)
        intent.action = if (enabled) "PLAY" else "PAUSE"
        if (enabled) ContextCompat.startForegroundService(this, intent) else startService(intent)
    }

    override fun onMusicSettingChanged(enabled: Boolean) = applyMusicSetting(enabled)
    override fun onSoundSettingChanged(enabled: Boolean) {}

    override fun onApplyBlurEffect(apply: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blur = if (apply) RenderEffect.createBlurEffect(15f, 15f, Shader.TileMode.CLAMP) else null
            findViewById<View>(R.id.main).setRenderEffect(blur)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::sharedPreferences.isInitialized) {
            applyMusicSetting(sharedPreferences.getBoolean(SettingsFragment.KEY_MUSIC_ENABLED, true))
        }
    }
}