package com.hfad.beeradviser.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.hfad.beeradviser.PokedexActivity
import com.hfad.beeradviser.R

class PlantDetailCardFragment : DialogFragment() {

    private var plantId: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var shimmerAnimator: ObjectAnimator? = null

    companion object {
        private const val ARG_PLANT_ID = "arg_plant_id"

        fun newInstance(plantId: String): PlantDetailCardFragment {
            return PlantDetailCardFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PLANT_ID, plantId)
                }
            }
        }

        // 資源映射列表：根據植物 ID 指定詳解大圖 //
        private fun getDetailImageRes(plantId: String?): Int {
            return when (plantId) {
                "1_1" -> R.drawable.plant1_1_detail
                "1_2" -> R.drawable.plant1_2_detail
                "1_3" -> R.drawable.plant1_3_detail
                "2_1" -> R.drawable.plant2_1_detail
                "2_2" -> R.drawable.plant2_2_detail
                "2_3" -> R.drawable.plant2_3_detail
                "3_1" -> R.drawable.plant3_1_detail
                "3_2" -> R.drawable.plant3_2_detail
                "3_3" -> R.drawable.plant3_3_detail
                "4_1" -> R.drawable.plant4_1_detail
                "4_2" -> R.drawable.plant4_2_detail
                "4_3" -> R.drawable.plant4_3_detail
                "5_1" -> R.drawable.plant5_1_detail
                "5_2" -> R.drawable.plant5_2_detail
                "5_3" -> R.drawable.plant5_3_detail
                // ... 依此類推
                else -> R.drawable.plant_default_default_sdefault // 預設圖
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
        plantId = arguments?.getString(ARG_PLANT_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_plant_detail_card, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setWindowAnimations(android.R.style.Animation_Dialog)
        setupUI(view)
        return view
    }

    private fun setupUI(view: View) {
        val rootContainer = view.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.detail_root_container)
        val cardContainer = view.findViewById<View>(R.id.cardContainer)
        val imgMainCard = view.findViewById<ImageView>(R.id.img_main_card)
        val shimmerRay = view.findViewById<View>(R.id.shimmerRay)
        val tvClose = view.findViewById<TextView>(R.id.tv_click_to_close)

        // 1. 設置圓角裁切
        cardContainer.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, 85f)
            }
        }
        cardContainer.clipToOutline = true

        // 2. 綁定大圖資源
        val detailRes = getDetailImageRes(plantId)
        imgMainCard.setImageResource(detailRes)

        // 3. 啟動流光動畫
        cardContainer.post {
            startLoopingShimmer(shimmerRay, cardContainer.width)
        }

        // 4. 文字呼吸效果
        val breathingAnim = AlphaAnimation(0.2f, 1.0f).apply {
            duration = 1000
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
            interpolator = LinearInterpolator()
        }
        tvClose.startAnimation(breathingAnim)

        // 5. 點擊空白處關閉
        rootContainer.setOnClickListener { dismiss() }

        // 防止點擊卡片本身觸發關閉
        cardContainer.setOnClickListener { /* 消耗事件 */ }
    }

    private fun startLoopingShimmer(shimmerView: View, containerWidth: Int) {
        // 從左側掃到右側
        val startX = -shimmerView.width.toFloat() - 150f
        val endX = containerWidth.toFloat() + 150f

        shimmerAnimator = ObjectAnimator.ofFloat(shimmerView, "translationX", startX, endX).apply {
            duration = 1500 // 掃過時間
            interpolator = LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    handler.postDelayed({
                        if (isAdded) startLoopingShimmer(shimmerView, containerWidth)
                    }, 1500) // 延遲 1.5 秒再掃一次
                }
            })
            start()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // 當字卡消失時，通知 Activity 移除模糊效果
        (activity as? PokedexActivity)?.onApplyBlurEffect(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        shimmerAnimator?.cancel()
    }
}