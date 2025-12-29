package com.hfad.beeradviser.ui

import android.graphics.drawable.TransitionDrawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hfad.beeradviser.R
import com.hfad.beeradviser.data.Plant

private const val TAG = "PlantAdapter"

class PlantAdapter(
    private var plantList: List<Plant>,
    private val onItemClick: (Plant) -> Unit
) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    class PlantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val plantImageView: ImageView = itemView.findViewById(R.id.plantImageView)
        private val handler = Handler(Looper.getMainLooper())
        private var blinkRunnable: Runnable? = null

        fun bind(plant: Plant, onItemClick: (Plant) -> Unit) {
            // 停止之前的動畫，避免回收使用時衝突
            stopBlinkAnimation()

            val context = itemView.context

            when {
                // 情況 A：Stage 3 - 已完成植物
                plant.stage == 3 -> {
                    val imageResourceName = "plant_${plant.islandId}_${plant.plantIndex}_stage_3"
                    val loadedId = context.resources.getIdentifier(
                        imageResourceName, "drawable", context.packageName
                    )
                    plantImageView.setImageResource(if (loadedId != 0) loadedId else R.drawable.ic_default_plant_placeholder)
                }

                // 情況 B：可領取的新種子 - 啟動兩圖交替閃爍
                plant.isAvailableToClaim -> {
                    startImageExchangeAnimation()
                }

                // 情況 C：一般未知植物
                else -> {
                    plantImageView.setImageResource(R.drawable.ic_default_plant_placeholder)
                }
            }

            itemView.setOnClickListener { onItemClick(plant) }
        }

        /**
         * 啟動圖片交替切換動畫 (使用 TransitionDrawable)
         */
        private fun startImageExchangeAnimation() {
            val context = itemView.context

            // 準備兩張要交替的圖片
            val drawables = arrayOf(
                ContextCompat.getDrawable(context, R.drawable.ic_default_plant_placeholder)!!,
                ContextCompat.getDrawable(context, R.drawable.ic_default_plant_placeholder_claimable)!!
            )

            val transitionDrawable = TransitionDrawable(drawables)
            plantImageView.setImageDrawable(transitionDrawable)

            var isForward = true
            val duration = 800 // 切換時間 (毫秒)

            blinkRunnable = object : Runnable {
                override fun run() {
                    if (isForward) {
                        transitionDrawable.startTransition(duration)
                    } else {
                        transitionDrawable.reverseTransition(duration)
                    }
                    isForward = !isForward
                    // 循環排程
                    handler.postDelayed(this, duration.toLong() + 100)
                }
            }
            handler.post(blinkRunnable!!)
        }

        /**
         * 停止動畫並清理資源
         */
        private fun stopBlinkAnimation() {
            blinkRunnable?.let { handler.removeCallbacks(it) }
            blinkRunnable = null
            plantImageView.clearAnimation()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plant, parent, false)
        return PlantViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        holder.bind(plantList[position], onItemClick)
    }

    override fun getItemCount(): Int = plantList.size

    fun updateData(newPlantList: List<Plant>) {
        plantList = newPlantList
        notifyDataSetChanged()
    }
}