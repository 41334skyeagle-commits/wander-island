package com.hfad.beeradviser.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.hfad.beeradviser.data.Island
import com.hfad.beeradviser.data.Plant
import com.hfad.beeradviser.data.PlantRepository
import com.hfad.beeradviser.data.PlantStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlantPokedexViewModel(private val plantRepository: PlantRepository) : ViewModel() {

    companion object {
        private const val TAG = "PlantPokedexViewModel"
    }

    // 植物完成事件 (用於 Stage 3 完工彈出字卡)
    private val _plantCompletedEvent = MutableLiveData<Plant?>()
    val plantCompletedEvent: LiveData<Plant?> = _plantCompletedEvent

    // --- 活躍植物狀態追蹤 ---
    private val _currentActivePlantIdLiveData: LiveData<String?> =
        plantRepository.getCurrentActivePlantId().asLiveData(viewModelScope.coroutineContext)

    val currentActivePlantStatus: LiveData<PlantStatus> =
        _currentActivePlantIdLiveData.switchMap { plantId ->
            if (plantId.isNullOrEmpty()) {
                MutableLiveData(PlantStatus("0_0", 0, false, 0, false, 0))
            } else {
                plantRepository.getPlantStatusFlow(plantId).asLiveData(viewModelScope.coroutineContext)
            }
        }

    // --- 關卡與圖鑑數據 ---
    private val _islands = MutableLiveData<List<Island>>()
    val islands: LiveData<List<Island>> = _islands

    private val _selectedIslandId = MutableStateFlow<Int?>(null)
    val selectedIslandId: StateFlow<Int?> = _selectedIslandId.asStateFlow()

    private val _allPlants = MutableLiveData<List<Plant>>()

    // 當前選中島嶼的植物列表 (供 PokedexActivity 的 PlantAdapter 使用)
    val currentIslandPlants: LiveData<List<Plant>> =
        _selectedIslandId.asLiveData().switchMap { islandId ->
            if (islandId == null) {
                MutableLiveData(emptyList())
            } else {
                _allPlants.map { allPlants ->
                    allPlants.filter { it.islandId == islandId }.sortedBy { it.plantIndex }
                }
            }
        }

    // 決定是否顯示紅點 (觀察是否有任何植物處於 isAvailableToClaim = true)
    val shouldShowPokedexExclamation: LiveData<Boolean> =
        plantRepository.observeAnyPlantAvailable()
            .asLiveData(viewModelScope.coroutineContext)

    init {
        loadInitialData()
        viewModelScope.launch {
            plantRepository.ensureActivePlantExists()
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            plantRepository.loadAllDataIntoCache()
            loadAllPlants()
            loadIslands()
        }
    }

    private fun loadIslands() {
        viewModelScope.launch(Dispatchers.IO) {
            val allIslands = plantRepository.getAllIslands()
            withContext(Dispatchers.Main) {
                _islands.value = allIslands
                if (_selectedIslandId.value == null) {
                    _selectedIslandId.value = allIslands.firstOrNull { it.isUnlocked }?.id ?: allIslands.firstOrNull()?.id
                }
            }
        }
    }

    private fun loadAllPlants() {
        viewModelScope.launch(Dispatchers.IO) {
            val allPlants = plantRepository.getAllPlants()
            withContext(Dispatchers.Main) {
                _allPlants.value = allPlants
            }
        }
    }

    /**
     * 檢查特定島嶼是否已經全部完成（所有植物皆達到 Stage 3）
     * 用於 LevelActivity 判斷是否播放全島完成動畫
     */
    suspend fun checkIslandCompletionStatus(islandId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val allPlants = plantRepository.getAllPlants()
            // 過濾出屬於該島嶼的植物
            val islandPlants = allPlants.filter { it.islandId == islandId }

            if (islandPlants.isEmpty()) return@withContext false

            // 檢查是否所有植物的 stage 都是 3
            val isAllCompleted = islandPlants.all { it.stage == 3 }

            Log.d(TAG, "檢查島嶼 $islandId 完成狀態: $isAllCompleted")
            isAllCompleted
        }
    }
    fun setSelectedIsland(islandId: Int) {
        _selectedIslandId.value = islandId
    }

    /**
     * ⭐ 第一個目標：領取新種子
     * 執行後會將植物的 isAvailableToClaim 設為 false，並將其 Stage 設為 1
     */
    fun claimSeed(plantId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. 在 Repository 執行資料庫更新
                plantRepository.claimNewSeed(plantId)

                // 2. 重新讀取所有植物資料以更新 Pokedex 列表 (LiveData 會自動推送更新)
                val updatedPlants = plantRepository.getAllPlants()
                val updatedIslands = plantRepository.getAllIslands()

                withContext(Dispatchers.Main) {
                    _allPlants.value = updatedPlants
                    _islands.value = updatedIslands
                    Log.i(TAG, "成功領取種子 $plantId，UI 已同步更新。")
                }
            } catch (e: Exception) {
                Log.e(TAG, "領取種子失敗: ${e.message}")
            }
        }
    }

    /**
     * 更新能量與階段 (用於專注結束時)
     */
    fun updatePlantEnergyAndCheckStageUp(plantId: String, energyGained: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val plantBase = plantRepository.getPlantById(plantId) ?: return@launch
            val currentStatus = plantRepository.getPlantStatus(plantId)

            var accumulatedEnergy = currentStatus.currentEnergy + energyGained
            var currentStage = currentStatus.stage

            val energyToStage2 = plantBase.totalEnergyRequired / 2
            val energyToStage3 = plantBase.totalEnergyRequired

            // 檢查升級到 Stage 2
            if (currentStage == 1 && accumulatedEnergy >= energyToStage2) {
                currentStage = 2
                plantRepository.updatePlantStatus(plantId, currentStage, accumulatedEnergy)
                delay(500L) // 給予 UI 反應時間
            }

            // 檢查完成 Stage 3
            if (currentStage == 2 && accumulatedEnergy >= energyToStage3) {
                currentStage = 3
                accumulatedEnergy = energyToStage3
                // 解鎖下一顆種子 (會將下一顆的 isAvailableToClaim 設為 true)
                plantRepository.checkAndUnlockNextSeed(plantId)

                withContext(Dispatchers.Main) {
                    _plantCompletedEvent.value = plantBase
                }
            }

            // 最終狀態更新
            plantRepository.updatePlantStatus(plantId, currentStage, accumulatedEnergy)

            // 刷新數據流
            loadAllPlants()
            loadIslands()
        }
    }

    // --- 其他輔助方法 ---
    suspend fun setCurrentActivePlantId(plantId: String?) = plantRepository.setCurrentActivePlantId(plantId)
    fun getSelectedIsland(): Island? = _islands.value?.find { it.id == _selectedIslandId.value }
    suspend fun getPlantById(plantId: String): Plant? = plantRepository.getPlantById(plantId)
    fun resetPlantEnergy(plantId: String) {
        viewModelScope.launch {
            plantRepository.updatePlantStatus(plantId, newStage = 1, newEnergy = 0)
            loadAllPlants()
        }
    }
}