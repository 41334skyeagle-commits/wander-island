package com.hfad.beeradviser

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hfad.beeradviser.data.Island

class LevelPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val levelStaticImageResIds: List<Int>,
    private val onIslandClicked: (islandId: Int) -> Unit
) : FragmentStateAdapter(fragmentActivity) {

    // 儲存最新的島嶼狀態列表
    private var islands: List<Island> = emptyList()

    override fun getItemCount(): Int = islands.size

    override fun getItemId(position: Int): Long {
        val island = islands[position]
        // 基礎 ID + (如果解鎖了就加一個很大的基數)
        return if (island.isUnlocked) {
            island.id.toLong() + 10000
        } else {
            island.id.toLong()
        }
    }

    override fun containsItem(itemId: Long): Boolean {
        // 檢查 ID 是否在目前的列表中（考慮到加了 10000 的情況）
        val actualId = if (itemId >= 10000) itemId - 10000 else itemId
        return islands.any { it.id.toLong() == actualId }
    }

    override fun createFragment(position: Int): Fragment {
        val island = islands[position]
        return LevelFragment.newInstance(
            imageResId = levelStaticImageResIds[position],
            levelNumber = island.id,
            isUnlocked = island.isUnlocked,
            onIslandClicked = onIslandClicked
        )
    }
    fun updateIslands(newIslands: List<Island>) {
        this.islands = newIslands.sortedBy { it.id }
        notifyDataSetChanged()
    }
}