package com.hfad.beeradviser.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.hfad.beeradviser.R
import com.hfad.beeradviser.SettingsFragment

/**
 * 顯示植物完成的自定義對話框字卡。
 * 專門用於處理 plantIndex 1 和 2 的完成事件。
 */
class PlantCompletionCardFragment : DialogFragment() {

    companion object {
        const val ARG_PLANT_ID = "plant_id"
        private const val TAG = "PlantCompletionCardFrag"

        fun newInstance(plantId: String): PlantCompletionCardFragment {
            val fragment = PlantCompletionCardFragment()
            val args = Bundle()
            args.putString(ARG_PLANT_ID, plantId)
            fragment.arguments = args
            return fragment
        }
    }

    private var settingsChangeListener: SettingsFragment.SettingsChangeListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is SettingsFragment.SettingsChangeListener) {
            settingsChangeListener = context
        } else {
            throw RuntimeException("$context must implement SettingsChangeListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        settingsChangeListener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return inflater.inflate(R.layout.fragment_completion_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val plantId = arguments?.getString(ARG_PLANT_ID)

        val rootContainer =view.findViewById <LinearLayout>(R.id.completion_root_container)
        val cardContainer = view.findViewById<View>(R.id.card_content_container)
        val plantAnimationGif1: ImageView = view.findViewById(R.id.plant_animation_gif_1)
        val plantNamePng2: ImageView = view.findViewById(R.id.plant_name_png_2)
        val unlockedEffectGif3: ImageView = view.findViewById(R.id.unlocked_effect_gif_3)
        val guideTextGif4: ImageView = view.findViewById(R.id.guide_text_gif_4)
        val tvClose = view.findViewById<TextView>(R.id.click_to_close)

        if (plantId != null) {
            // --- 處理動態圖片載入 (GIF 1) ---
            val plantGifResId = getPlantCompletionGifResId(plantId)
            Glide.with(this)
                .asGif()
                .load(plantGifResId)
                .into(plantAnimationGif1)

            // --- 處理靜態圖片載入 (PNG 2) ---
            val plantNameResId = getPlantNamePngResId(plantId)
            plantNamePng2.setImageResource(plantNameResId)

            Log.d(TAG, "已載入植物 $plantId 的完成動畫和名稱。")
        }

        // --- 解鎖特效動態文字 (GIF 3) - 【設定只播放一次】 ---
        Glide.with(this)
            .asGif()
            .load(R.drawable.unlocked_effecttext)
            .listener(createSingleLoopListener("GIF 3 (Unlocked Effect)"))
            .into(unlockedEffectGif3)

        // --- 指引動態文字 (GIF 4) - 【設定只播放一次】 ---
        Glide.with(this)
            .asGif()
            .load(R.drawable.guidetext_to_pokedex)
            .listener(createSingleLoopListener("GIF 4 (Guide Text)"))
            .into(guideTextGif4)

        // 文字呼吸效果
        val breathingAnim = AlphaAnimation(0.2f, 1.0f).apply {
            duration = 1000
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
            interpolator = LinearInterpolator()
        }
        tvClose.startAnimation(breathingAnim)

        //  點擊空白處關閉
        rootContainer.setOnClickListener { dismiss() }

        // 防止點擊卡片本身觸發關閉
        cardContainer.setOnClickListener{ /* 消耗事件 */ }
    }


     // 建立一個 Glide 監聽器，確保 GIF 資源加載後僅播放一次。 //
    private fun createSingleLoopListener(label: String): RequestListener<GifDrawable> {
        return object : RequestListener<GifDrawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any,
                target: Target<GifDrawable>,
                isFirstResource: Boolean
            ): Boolean {
                Log.e(TAG, "$label 載入失敗: ${e?.message}")
                return false
            }

            override fun onResourceReady(
                resource: GifDrawable,
                model: Any,
                target: Target<GifDrawable>,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                resource.setLoopCount(1) // 播放次數設定為 1
                resource.start()
                Log.d(TAG, "$label 已設定為播放一次。")
                return false
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        settingsChangeListener?.onApplyBlurEffect(true)
    }

    override fun onStop() {
        super.onStop()
        settingsChangeListener?.onApplyBlurEffect(false)
    }

    private fun getPlantCompletionGifResId(plantId: String): Int {
        return when (plantId) {
            "1_1" -> R.drawable.plant_1_1_completion
            "1_2" -> R.drawable.plant_1_2_completion
            "2_1" -> R.drawable.plant_2_1_completion
            "2_2" -> R.drawable.plant_2_2_completion
            "3_1" -> R.drawable.plant_3_1_completion
            "3_2" -> R.drawable.plant_3_2_completion
            "4_1" -> R.drawable.plant_4_1_completion
            "4_2" -> R.drawable.plant_4_2_completion
            "5_1" -> R.drawable.plant_5_1_completion
            "5_2" -> R.drawable.plant_5_2_completion
            else -> R.drawable.default_completion
        }
    }

    private fun getPlantNamePngResId(plantId: String): Int {
        return when (plantId) {
            "1_1" -> R.drawable.plant_1_1_name
            "1_2" -> R.drawable.plant_1_2_name
            "2_1" -> R.drawable.plant_2_1_name
            "2_2" -> R.drawable.plant_2_2_name
            "3_1" -> R.drawable.plant_3_1_name
            "3_2" -> R.drawable.plant_3_2_name
            "4_1" -> R.drawable.plant_4_1_name
            "4_2" -> R.drawable.plant_4_2_name
            "5_1" -> R.drawable.plant_5_1_name
            "5_2" -> R.drawable.plant_5_2_name
            else -> R.drawable.default_name
        }
    }
}