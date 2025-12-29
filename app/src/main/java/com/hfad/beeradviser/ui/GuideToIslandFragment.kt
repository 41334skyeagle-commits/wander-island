package com.hfad.beeradviser.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.graphics.Outline
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.hfad.beeradviser.R
import com.hfad.beeradviser.SettingsFragment

class GuideToIslandFragment : Fragment(R.layout.fragment_guide_to_island) {

    private val handler = Handler(Looper.getMainLooper())
    private var shimmerAnimator: ObjectAnimator? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val descriptionContainer = view.findViewById<View>(R.id.descriptionContainer)
        val shimmerRay = view.findViewById<View>(R.id.shimmerRay)
        val confirmBtn = view.findViewById<ImageView>(R.id.guideConfirm)

        // 應用 PS 提供的 36px 圓角裁切
        descriptionContainer.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                // 使用 PS 提供的圓角數據: 36px
                outline.setRoundRect(0, 0, view.width, view.height, 36f)
            }
        }
        descriptionContainer.clipToOutline = true

        // 當 Layout 完成後啟動流光動畫
        descriptionContainer.post {
            startLoopingShimmer(shimmerRay, descriptionContainer.width)
        }

        confirmBtn.setOnClickListener {
            // 引導用戶回到島嶼選擇頁面（關閉 Activity 並跳轉）
            activity?.finish()
        }
    }

    private fun startLoopingShimmer(shimmerView: View, containerWidth: Int) {
        // 設定動畫路徑：從左側邊界外，掃到右側邊界外
        val startX = -shimmerView.width.toFloat() - 100f
        val endX = containerWidth.toFloat() + 100f

        shimmerAnimator = ObjectAnimator.ofFloat(shimmerView, "translationX", startX, endX).apply {
            duration = 1500 // 掃過圖片的時間 (1.5秒)
            interpolator = LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // 掃完一次後，延遲 3 秒再進行下一次
                    handler.postDelayed({
                        if (isAdded) startLoopingShimmer(shimmerView, containerWidth)
                    }, 3000)
                }
            })
            start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        shimmerAnimator?.cancel()

        // 當字卡消失時，通知 Activity 移除模糊效果
        (activity as? SettingsFragment.SettingsChangeListener)?.onApplyBlurEffect(false)
    }
}