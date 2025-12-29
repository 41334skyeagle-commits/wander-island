package com.hfad.beeradviser.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.hfad.beeradviser.R
import com.hfad.beeradviser.data.Island

private const val TAG = "IslandAdapter"

class IslandAdapter(
    private var islandList: List<Island>
) : RecyclerView.Adapter<IslandAdapter.IslandViewHolder>() {

    class IslandViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val islandImageView: ImageView = itemView.findViewById(R.id.islandImageView)

        fun bind(island: Island) {

            // 核心邏輯：根據解鎖狀態生成圖片檔名
            val statusString = if (island.isUnlocked) "unlocked" else "locked"

            // 組合出完整的圖片檔案名稱字串 (例如 "island_1_unlocked" 或 "island_2_locked")
            val imageResourceName = "island_${island.id}_$statusString"

            Log.d(TAG, "嘗試載入關卡圖片: $imageResourceName (Unlocked: ${island.isUnlocked})")

            // 使用 context.resources.getIdentifier() 將檔名轉換為資源 ID
            val imageResourceId = itemView.context.resources.getIdentifier(
                imageResourceName, // 組合出的檔名
                "drawable",        // 資源類型
                itemView.context.packageName // 應用程式的包名
            )

            // 載入圖片資源
            if (imageResourceId != 0) {
                islandImageView.setImageResource(imageResourceId)
            } else {
                Log.e(TAG, "錯誤：找不到整合圖片資源名為 '$imageResourceName' 的圖片！")
                // 使用一個通用的錯誤佔位圖
                islandImageView.setImageResource(android.R.drawable.ic_menu_help)
            }

            if (!island.isUnlocked) {
                // 視覺上顯示為不可互動的狀態
                itemView.alpha = 0.5f
            } else {
                itemView.alpha = 1.0f
            }

            itemView.setOnClickListener(null)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IslandViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_island, parent, false)
        return IslandViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: IslandViewHolder, position: Int) {
        val currentIsland = islandList[position]
        holder.bind(currentIsland)
    }

    override fun getItemCount(): Int {
        return islandList.size
    }

    fun updateData(newIslandList: List<Island>) {
        islandList = newIslandList
        notifyDataSetChanged()
    }
}