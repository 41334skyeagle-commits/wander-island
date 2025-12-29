package com.hfad.beeradviser

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.hfad.beeradviser.data.Plant
import com.hfad.beeradviser.ui.IslandAdapter
import com.hfad.beeradviser.ui.PlantAdapter
import com.hfad.beeradviser.ui.PlantDetailCardFragment
import com.hfad.beeradviser.ui.PlantPokedexViewModel
import com.hfad.beeradviser.ui.PlantPokedexViewModelFactory
import com.hfad.beeradviser.ui.SeedClaimFragment
import kotlin.jvm.java

class PokedexActivity : AppCompatActivity(),
    SeedClaimFragment.ClaimSeedListener,
    SettingsFragment.SettingsChangeListener {

    private lateinit var viewModel: PlantPokedexViewModel
    private lateinit var backButton: ImageButton
    private lateinit var islandViewPager: ViewPager2
    private lateinit var islandAdapter: IslandAdapter
    private lateinit var plantRecyclerView: RecyclerView
    private lateinit var plantAdapter: PlantAdapter
    private lateinit var dimOverlay: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pokedex)

        // 1. 初始化 ViewModel
        val plantRepository = (application as PlantPokedexApplication).plantRepository
        viewModel = ViewModelProvider(this, PlantPokedexViewModelFactory(plantRepository))
            .get(PlantPokedexViewModel::class.java)

        val initialIslandId = intent.getIntExtra("ISLAND_ID", 1)

        // 2. 元件初始化
        plantRecyclerView = findViewById(R.id.plantRecyclerView)
        islandViewPager = findViewById(R.id.islandViewPager)
        backButton = findViewById(R.id.backButton)
        dimOverlay = findViewById(R.id.dimOverlay)

        // 3. 初始化清單
        plantRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        plantAdapter = PlantAdapter(emptyList()) { clickedPlant: Plant ->
            handlePlantClick(clickedPlant)
        }
        plantRecyclerView.adapter = plantAdapter

        // 4. 初始化分頁器
        islandAdapter = IslandAdapter(emptyList())
        islandViewPager.adapter = islandAdapter
        setupIslandChangeListener()

        // 5. 數據觀察
        observeData(initialIslandId)

        backButton.setOnClickListener { finish() }
    }

    private fun handlePlantClick(clickedPlant: Plant) {
        when {
            clickedPlant.isAvailableToClaim -> showClaimSeedFragment(clickedPlant.id)
            clickedPlant.stage == 3 -> {
                // 1. 開啟背景模糊
                onApplyBlurEffect(true)

                // 2. 顯示 Fragment
                val detailFragment = PlantDetailCardFragment.newInstance(clickedPlant.id)
                detailFragment.show(supportFragmentManager, "PLANT_DETAIL_CARD")
            }
            clickedPlant.stage >= 1 -> Toast.makeText(this, "該植物正在成長中！", Toast.LENGTH_SHORT).show()
            else -> Toast.makeText(this, "該植物尚未達到解鎖條件。", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeData(initialIslandId: Int) {
        viewModel.islands.observe(this) { islandList ->
            islandAdapter.updateData(islandList)
            if (islandList.isNotEmpty()) {
                val targetIndex = (initialIslandId - 1).coerceIn(islandList.indices)
                islandViewPager.setCurrentItem(targetIndex, false)
                viewModel.setSelectedIsland(islandList[targetIndex].id)
            }
        }
        viewModel.currentIslandPlants.observe(this) { plantList ->
            plantAdapter.updateData(plantList)
        }
    }

    private fun setupIslandChangeListener() {
        islandViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.setSelectedIsland(position + 1)
            }
        })
    }

    private fun showClaimSeedFragment(plantId: String) {
        val fragment = SeedClaimFragment.newInstance(plantId)
        supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment, "SeedClaimFragment")
            .addToBackStack(null)
            .commit()
    }

    // --- SeedClaimFragment.ClaimSeedListener 實作 ---

    override fun onSeedClaimConfirmed(plantId: String) {
        viewModel.claimSeed(plantId)
    }

    // --- SettingsFragment.SettingsChangeListener 實作 ---

    /**
     * 處理背景模糊效果
     * 當彈出領取種子字卡時，SeedClaimFragment 會呼叫此方法
     */
    override fun onApplyBlurEffect(apply: Boolean) {
        val rootLayout = findViewById<View>(R.id.pokedex_root_layout)

        if (apply) {
            // 1. 套用模糊效果 (API 31+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                rootLayout?.setRenderEffect(
                    RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP)
                )
            }

            // 2. 讓遮罩層顯現 (漸變動畫)
            dimOverlay.visibility = View.VISIBLE
            ObjectAnimator.ofFloat(dimOverlay, "alpha", 0f, 0.7f).apply {
                duration = 300
                start()
            }
        } else {
            // 1. 移除模糊
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                rootLayout?.setRenderEffect(null)
            }

            // 2. 隱藏遮罩層 (漸變動畫)
            ObjectAnimator.ofFloat(dimOverlay, "alpha", 0.7f, 0f).apply {
                duration = 300
                setOnEndListener { dimOverlay.visibility = View.GONE }
                start()
            }
        }
    }

    // 擴展 ObjectAnimator 的小工具，方便處理動畫結束
    private fun ObjectAnimator.setOnEndListener(onEnd: () -> Unit) {
        this.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                onEnd()
            }
        })
    }

    override fun onSoundSettingChanged(enabled: Boolean) {
    }

    override fun onMusicSettingChanged(enabled: Boolean) {
    }
}