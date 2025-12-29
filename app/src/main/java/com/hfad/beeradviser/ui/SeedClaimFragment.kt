package com.hfad.beeradviser.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.hfad.beeradviser.R
import com.hfad.beeradviser.SettingsFragment


/**
 * 領取種子成功的提示字卡 Fragment
 */
class SeedClaimFragment : Fragment(R.layout.fragment_seed_claim) {

    private var plantId: String = ""
    private var settingsChangeListener: SettingsFragment.SettingsChangeListener? = null

    companion object {
        private const val ARG_PLANT_ID = "plant_id"

        fun newInstance(plantId: String): SeedClaimFragment {
            return SeedClaimFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PLANT_ID, plantId)
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // 確保 Activity 有實作 SettingsChangeListener
        if (context is SettingsFragment.SettingsChangeListener) {
            settingsChangeListener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        plantId = arguments?.getString(ARG_PLANT_ID) ?: ""
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 啟動時通知 Activity 進行背景模糊 (API 31+)
        settingsChangeListener?.onApplyBlurEffect(true)

        // 2. 處理確認按鈕邏輯
        val confirmBtn = view.findViewById<ImageView>(R.id.confirmButton)
        confirmBtn.setOnClickListener {
            notifyClaimConfirmed()
            // 關閉自己
            parentFragmentManager.popBackStack()
        }
    }

    private fun notifyClaimConfirmed() {
        (activity as? ClaimSeedListener)?.onSeedClaimConfirmed(plantId)
    }

    interface ClaimSeedListener {
        fun onSeedClaimConfirmed(plantId: String)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 3. 關閉時通知 Activity 移除模糊效果
        settingsChangeListener?.onApplyBlurEffect(false)
    }
}