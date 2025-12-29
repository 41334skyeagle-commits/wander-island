package com.hfad.beeradviser.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.hfad.beeradviser.data.Island
import com.hfad.beeradviser.data.PlantRepository
import kotlinx.coroutines.launch


class MainViewModelFactory(
    private val repository: PlantRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
class MainViewModel(
    private val plantRepository: PlantRepository
) : ViewModel() {

    // 供 MainActivity 觀察所有島嶼列表 (包含解鎖狀態)
    val islands: LiveData<List<Island>> = plantRepository.getAllIslandsFlow().asLiveData()


    /**
     * 在 MainActivity 點擊 Island 時呼叫。
     * 作用：透過 Repository 智慧地找出該島嶼上最適合作為活躍植物的 ID (正在養成/可領取)。
     */
    fun selectAndSetNextActivePlant(islandId: Int) {
        viewModelScope.launch {
            // 1. 呼叫 Repository 的新函式，取得最適合的植物 ID
            val nextPlantId = plantRepository.findNextActivePlantIdForIsland(islandId)

            // 2. 獲取該植物的狀態
            val status = plantRepository.getPlantStatus(nextPlantId)

            // 3. 執行領取或單純設定活躍
            if (status.stage == 0 && status.isAvailableToClaim == true) {
                // 如果是 Stage 0 (且已解鎖/可領取)，執行完整的領取流程 (Stage 0 -> 1，設定活躍)
                plantRepository.claimNewSeed(nextPlantId)
            } else {
                // 如果是 Stage 1, 2, 或 3，只需要確保它被設為當前活躍植物 ID
                plantRepository.setCurrentActivePlantId(nextPlantId)
            }

            // Log 用於追蹤
            Log.d("MainViewModel", "島嶼 $islandId 點擊，設定活躍植物為 $nextPlantId。")
        }
    }


}