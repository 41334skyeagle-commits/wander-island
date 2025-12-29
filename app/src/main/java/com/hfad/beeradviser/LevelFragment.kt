package com.hfad.beeradviser

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
class LevelFragment : Fragment() {

    private var isUnlocked: Boolean = false
    private var onIslandClicked: ((Int) -> Unit)? = null
    private var levelNumber: Int = -1

    companion object {
        private const val ARG_IMAGE_RES_ID = "imageResId"
        private const val ARG_LEVEL_NUMBER = "levelNumber"
        private const val ARG_IS_UNLOCKED = "isUnlocked"

        fun newInstance(
            imageResId: Int,
            levelNumber: Int,
            isUnlocked: Boolean,
            onIslandClicked: (islandId: Int) -> Unit
        ): LevelFragment {
            return LevelFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_IMAGE_RES_ID, imageResId)
                    putInt(ARG_LEVEL_NUMBER, levelNumber)
                    putBoolean(ARG_IS_UNLOCKED, isUnlocked)
                }
                this.onIslandClicked = onIslandClicked
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            levelNumber = it.getInt(ARG_LEVEL_NUMBER)
            isUnlocked = it.getBoolean(ARG_IS_UNLOCKED)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_level, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val levelImageView: ImageView = view.findViewById(R.id.levelImageView)
        val staticResId = arguments?.getInt(ARG_IMAGE_RES_ID) ?: R.drawable.default_level_icon

        // 統一縮放模式
        levelImageView.scaleType = ImageView.ScaleType.FIT_CENTER

        if (isUnlocked) {
            val gifResName = "level_${levelNumber}_icon_gif"
            val gifResId = resources.getIdentifier(gifResName, "drawable", requireContext().packageName)

            if (gifResId != 0) {
                // GIF 內容通常比例較小，設定較小的 Padding
                levelImageView.setPadding(0, 0, 0, 0)

                Glide.with(this)
                    .asGif()
                    .load(gifResId)
                    // 使用 Target.SIZE_ORIGINAL 告訴 Glide 使用圖片原始大小，避免被過度壓縮
                    .override(Target.SIZE_ORIGINAL)
                    .placeholder(staticResId)
                    .into(levelImageView)
            } else {
                // 靜態圖內容通常較滿，設定較大的 Padding 來縮小視覺效果
                levelImageView.setPadding(220, 200, 220, 650)

                Glide.with(this)
                    .load(staticResId)
                    .into(levelImageView)
            }

            levelImageView.alpha = 1.0f
            levelImageView.isClickable = true
            levelImageView.colorFilter = null
            levelImageView.setOnClickListener {
                if (levelNumber != -1) onIslandClicked?.invoke(levelNumber)
            }

        } else {
            // 未解鎖狀態：套用灰階與較大 Padding
            levelImageView.setPadding(320, 300, 320, 700)
            Glide.with(this).load(staticResId).into(levelImageView)
            levelImageView.alpha = 0.5f
            levelImageView.isClickable = false

            val matrix = ColorMatrix().apply { setSaturation(0f) }
            levelImageView.colorFilter = ColorMatrixColorFilter(matrix)
        }
    }
}