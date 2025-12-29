package com.hfad.beeradviser.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hfad.beeradviser.plantDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext


// 定義 DataStore Keys
private object PreferencesKeys {
    // 儲存當前正在種植的植物 ID
    val CURRENT_ACTIVE_PLANT_ID = stringPreferencesKey("current_active_plant_id")
    // 儲存植物階段的前綴 (Key 格式: "plant_stage_1_1")
    private const val PLANT_STAGE_PREFIX = "plant_stage_"
    private const val PLANT_ENERGY_PREFIX = "plant_energy_"

    // 取得植物階段的 DataStore Key
    fun plantStageKey(plantId: String) = intPreferencesKey(PLANT_STAGE_PREFIX + plantId)

    // 取得植物能量的 DataStore Key
    fun plantEnergyKey(plantId: String) = intPreferencesKey(PLANT_ENERGY_PREFIX + plantId)

    // 追蹤植物是否可領取 (領取是手動操作)
    private const val PLANT_AVAILABLE_PREFIX = "plant_available_"
    fun plantAvailableKey(plantId: String) = booleanPreferencesKey(PLANT_AVAILABLE_PREFIX + plantId)

    // 儲存關卡解鎖狀態的前綴 (Key 格式: "island_unlocked_1")
    private const val ISLAND_UNLOCKED_PREFIX = "island_unlocked_"

    // 取得關卡解鎖的 DataStore Key
    fun islandUnlockedKey(islandId: Int) = booleanPreferencesKey(ISLAND_UNLOCKED_PREFIX + islandId)
}


class PlantRepositoryImpl(
    private val plantDataSource: PlantDataSource,
    private val context: Context
) : PlantRepository {
    companion object {
        private const val TAG = "PlantRepositoryImpl"
    }
    // 內部快取：儲存植物的基本資料 + 階段狀態
    private val plantsCache = mutableMapOf<String, Plant>()
    // 內部快取：儲存關卡的基本資料 + 解鎖狀態
    private val islandsCache = mutableMapOf<Int, Island>()

    // --- 初始化和載入 ---

    // 替換 loadAllPlantsIntoCache
    override suspend fun loadAllDataIntoCache() = withContext(Dispatchers.IO) {
        Log.d("PlantRepoImpl", "loadAllDataIntoCache: 開始載入所有植物及狀態...")

        // 1. 載入靜態植物資料
        val plantsFromDataSource = plantDataSource.loadPlants()
        plantsCache.clear()
        plantsFromDataSource.forEach { plant ->
            plantsCache[plant.id] = plant
        }

        // 2. 載入關卡靜態資訊 (假設有 10 個關卡，從 1 到 10)
        // 注意: 這裡需要根據您的實際遊戲設計來確定關卡數量
        for (i in 1..5) {
            islandsCache[i] = Island(id = i, name = "Island $i", isUnlocked = (i == 1)) // 預設第 1 關解鎖
        }

        // 3. 載入 DataStore 狀態 (Stage & Unlocked)
        loadStatusesFromDataStore()

        Log.d("PlantRepoImpl", "loadAllDataIntoCache: 數據載入完成，植物數: ${plantsCache.size}，關卡數: ${islandsCache.size}")
        Unit
    }

    private suspend fun loadStatusesFromDataStore() = withContext(Dispatchers.IO) {
        Log.d("PlantRepoImpl", "loadStatusesFromDataStore: 從 DataStore 載入所有動態狀態...")
        try {
            val preferences = context.plantDataStore.data.first()

            // a. 載入植物 Stage 狀態、Energy 和 Available 狀態
            plantsCache.values.forEach { plant ->
                val stageKey = PreferencesKeys.plantStageKey(plant.id)
                val energyKey = PreferencesKeys.plantEnergyKey(plant.id)
                val availableKey = PreferencesKeys.plantAvailableKey(plant.id)
                val savedStage = preferences[stageKey] ?: plant.stage
                val savedEnergy = preferences[energyKey] ?: 0
                val savedAvailable = preferences[availableKey] ?: when (plant.id) {
                    "1_1" -> true // 如果 DataStore 中沒有此 Key，預設 1_1 為可領取
                    else -> false
                }

                plantsCache[plant.id] = plant.copy(
                    stage = savedStage,
                    currentEnergy = savedEnergy,
                    isAvailableToClaim = savedAvailable
                )
            }

            // b. 載入關卡 Unlocked 狀態
            islandsCache.values.forEach { island ->
                val unlockedKey = PreferencesKeys.islandUnlockedKey(island.id)
                // 從 DataStore 讀取解鎖狀態，如果沒有，則預設為 Island 初始值 (通常是第 1 關為 true)
                val savedUnlocked = preferences[unlockedKey] ?: island.isUnlocked
                // 更新快取
                islandsCache[island.id] = island.copy(isUnlocked = savedUnlocked)
            }

            Log.d("PlantRepoImpl", "loadStatusesFromDataStore: 狀態載入完成。")
        } catch (e: Exception) {
            Log.e("PlantRepoImpl", "載入 DataStore 狀態失敗: ${e.message}", e)
        }
    }

    override fun getAllIslandsFlow(): Flow<List<Island>> = context.plantDataStore.data
        // 將 DataStore preferences 轉換為 Island 列表
        .map { preferences ->
            islandsCache.values.map { island ->
                // 從 DataStore 讀取解鎖狀態
                val unlockedKey = PreferencesKeys.islandUnlockedKey(island.id)
                val savedUnlocked = preferences[unlockedKey] ?: island.isUnlocked
                // 返回帶有最新狀態的 Island 物件
                island.copy(isUnlocked = savedUnlocked)
            }.sortedBy { it.id } // 確保按 ID 順序返回
        }
        .distinctUntilChanged() // 只有當島嶼狀態真正改變時才發出


    // --- Plant 相關方法 ---

    override suspend fun getAllPlants(): List<Plant> = withContext(Dispatchers.IO) {
        // 返回包含最新 stage 狀態的植物列表
        plantsCache.values.toList()
    }

    override suspend fun getPlantById(plantId: String): Plant? = withContext(Dispatchers.IO) {
        plantsCache[plantId]
    }

    override suspend fun getPlantStatus(plantId: String): PlantStatus = withContext(Dispatchers.IO) {
        Log.i(TAG, "getPlantStatus READ: Plant $plantId")
        try {
            val preferences = context.plantDataStore.data.first()

            val stageKey = PreferencesKeys.plantStageKey(plantId)
            val energyKey = PreferencesKeys.plantEnergyKey(plantId)
            val availableKey = PreferencesKeys.plantAvailableKey(plantId)

            val stage = preferences[stageKey] ?: 0
            val isAvailable = preferences[availableKey] ?: (plantId == "1_1")
            val isUnlocked = stage >= 1
            val currentEnergy = preferences[energyKey] ?: 0

            // *** 修正點 1：從快取獲取靜態的 islandId ***
            val plantBase = plantsCache[plantId]
            val actualIslandId = plantBase?.islandId ?: 0

            val status = PlantStatus(
                id = plantId,
                stage = stage,
                isUnlocked = isUnlocked,
                currentEnergy = currentEnergy,
                isAvailableToClaim = isAvailable,
                islandId = actualIslandId
            )

            Log.i(TAG, "getPlantStatus READ: Plant $plantId -> Stage: $stage, Available: $isAvailable, Island: $actualIslandId")

            return@withContext status

        } catch (e: Exception) {
            Log.e(TAG, "獲取植物 ${plantId} 狀態失敗: ${e.message}", e)

            // *** 修正點 2：錯誤處理時，必須傳入 islandId ***
            return@withContext PlantStatus(
                id = plantId,
                stage = 0,
                isUnlocked = false,
                currentEnergy = 0,
                isAvailableToClaim = false,
                islandId = 0
            )
        }
    }

    override suspend fun updatePlantStatus(
        plantId: String,
        newStage: Int,
        newEnergy: Int
    ) = withContext(Dispatchers.IO) {
        Log.d("PlantRepoImpl", "updatePlantStatus: 植物 ${plantId} -> Stage: ${newStage}, Energy: ${newEnergy}")

        // 1. 更新 DataStore
        context.plantDataStore.edit { preferences ->
            preferences[PreferencesKeys.plantStageKey(plantId)] = newStage
            preferences[PreferencesKeys.plantEnergyKey(plantId)] = newEnergy
        }

        // 2. 更新快取中的 Stage 和 Energy (修正這裡)
        plantsCache[plantId]?.let { currentPlant ->
            // 使用 currentPlant.isAvailableToClaim 確保其他狀態不丟失
            plantsCache[plantId] = currentPlant.copy(
                stage = newStage,
                currentEnergy = newEnergy,
                isAvailableToClaim = currentPlant.isAvailableToClaim // 保持原有的 Available 狀態
            )
        }

        Log.d("PlantRepoImpl", "updatePlantStatus: 快取與 DataStore 已更新。")
        Unit
    }
    // 透過 Flow 響應式地提供植物階段
    override fun getPlantStageFlow(plantId: String): Flow<Int> = context.plantDataStore.data
        .map { preferences ->
            // 如果 DataStore 中找不到，返回預設值 0 (未種植)
            preferences[PreferencesKeys.plantStageKey(plantId)] ?: 0
        }.distinctUntilChanged() // 只有當數值真正改變時才發出

    override fun getPlantStatusFlow(plantId: String): Flow<PlantStatus> {
        // *** 修正點：在 Flow 外部同步獲取靜態的 islandId ***
        val plantBase = plantsCache[plantId]
        val actualIslandId = plantBase?.islandId ?: 0

        return context.plantDataStore.data
            // 使用 map 將 Preference 數據轉換為 PlantStatus 物件
            .map { preferences ->
                val stageKey = PreferencesKeys.plantStageKey(plantId)
                val energyKey = PreferencesKeys.plantEnergyKey(plantId)
                val availableKey = PreferencesKeys.plantAvailableKey(plantId)

                val stage = preferences[stageKey] ?: 0
                val currentEnergy = preferences[energyKey] ?: 0
                val isAvailable = preferences[availableKey] ?: false

                // isUnlocked 判斷：Stage >= 1 即為已獲得種子
                val isUnlocked = stage >= 1

                PlantStatus(
                    id = plantId,
                    stage = stage,
                    isUnlocked = isUnlocked,
                    currentEnergy = currentEnergy,
                    isAvailableToClaim = isAvailable,
                    islandId = actualIslandId // <--- 使用修正後的 islandId
                )
            }
            .distinctUntilChanged() // 只有當 PlantStatus 真正改變時才發出新值
    }

    // --- Island 相關方法 ---

    override suspend fun getAllIslands(): List<Island> = withContext(Dispatchers.IO) {
        // 返回包含最新 isUnlocked 狀態的關卡列表
        islandsCache.values.toList().sortedBy { it.id } // 確保按 ID 順序返回
    }

    override suspend fun updateIslandUnlockedStatus(islandId: Int, isUnlocked: Boolean) = withContext(Dispatchers.IO) {
        Log.d("PlantRepoImpl", "updateIslandUnlockedStatus: 更新關卡 ${islandId} 解鎖狀態為 ${isUnlocked}")
        val currentIsland = islandsCache[islandId]
        currentIsland?.let {
            val updatedIsland = it.copy(isUnlocked = isUnlocked)
            islandsCache[islandId] = updatedIsland // 更新快取

            // 持久化到 DataStore
            context.plantDataStore.edit { preferences ->
                preferences[PreferencesKeys.islandUnlockedKey(islandId)] = isUnlocked
            }

            Log.d("PlantRepoImpl", "updateIslandUnlockedStatus: 快取與 DataStore 已更新。")
        } ?: Log.w("PlantRepoImpl", "updateIslandUnlockedStatus: 無法找到關卡 ${islandId} 進行更新。")
        Unit
    }

    // --- 活躍植物追蹤 ---

    // 透過 Flow 響應式地提供當前活躍植物 ID
    override fun getCurrentActivePlantId(): Flow<String?> = context.plantDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.CURRENT_ACTIVE_PLANT_ID]
        }.distinctUntilChanged()

    // 設定當前活躍植物 ID
    override suspend fun setCurrentActivePlantId(plantId: String?) = withContext(Dispatchers.IO) {
        Log.d("PlantRepoImpl", "setCurrentActivePlantId: 設定活躍植物 ID 為 ${plantId ?: "NULL"}")
        context.plantDataStore.edit { preferences ->
            if (plantId == null) {
                preferences.remove(PreferencesKeys.CURRENT_ACTIVE_PLANT_ID)
            } else {
                preferences[PreferencesKeys.CURRENT_ACTIVE_PLANT_ID] = plantId
            }
        }
        Unit
    }

    override suspend fun getCurrentActivePlantIdSuspend(): String? = withContext(Dispatchers.IO) {
        try {
            val preferences = context.plantDataStore.data.first()
            preferences[PreferencesKeys.CURRENT_ACTIVE_PLANT_ID]
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentActivePlantIdSuspend 失敗: ${e.message}", e)
            null
        }
    }

    override suspend fun ensureActivePlantExists() {
        val currentId = context.plantDataStore.data.first()[PreferencesKeys.CURRENT_ACTIVE_PLANT_ID]
        val DEFAULT_PLANT_ID = "1_1"

        if (currentId == null) {
            Log.i(TAG, "初始化：DataStore 首次啟動，設定活躍植物 ID 為 $DEFAULT_PLANT_ID")

            context.plantDataStore.edit { preferences ->
                preferences[PreferencesKeys.CURRENT_ACTIVE_PLANT_ID] = DEFAULT_PLANT_ID
            }

            // 確保預設的 plant_1_1 種子是被解鎖的 (Stage=1)
            val plant1_1_stage = context.plantDataStore.data.first()[PreferencesKeys.plantStageKey(DEFAULT_PLANT_ID)]
            if (plant1_1_stage == null || plant1_1_stage == 0) {

                // 設定 Plant 1_1 為 Stage 1
                updatePlantStatus(plantId = DEFAULT_PLANT_ID, newStage = 1, newEnergy = 0)
            }
        }
    }


    override suspend fun checkAndUnlockNextSeed(completedPlantId: String) = withContext(Dispatchers.IO) {
        Log.d(TAG, "checkAndUnlockNextSeed: 檢查是否解鎖下一顆關卡或植物，已完成植物 ID: ${completedPlantId}")

        val completedPlant = plantsCache[completedPlantId]
        if (completedPlant == null) {
            Log.w(TAG, "找不到已完成的植物 ID: ${completedPlantId}")
            return@withContext
        }

        val currentIslandId = completedPlant.islandId
        val currentPlantIndex = completedPlant.plantIndex

        // 1. 尋找下一個植物 ID，判斷是否為島嶼內最後一顆
        val nextPlantIndex = currentPlantIndex + 1
        val nextPlantId = "${currentIslandId}_${nextPlantIndex}"

        // 2. 檢查下一個植物是否存在於同一個島嶼
        val nextPlant = plantsCache[nextPlantId]

        if (nextPlant != null) {
            // 找到了同島嶼的下一顆植物 (例如 1_1 -> 1_2)
            // 設置為「可領取」，圖鑑紅點提示領取下一顆種子的流程。
            Log.i(TAG, "同島嶼植物完成，解鎖下一顆植物 ${nextPlantId}，設定為「可領取」。")
            setPlantAvailable(plantId = nextPlantId, isAvailable = true)

        } else {
            // 這是該島嶼的最後一顆植物 (例如 1_3)，檢查是否需要解鎖下一個島嶼
            val nextIslandId = currentIslandId + 1

            if (islandsCache.containsKey(nextIslandId)) {
                // 找到了下一個島嶼 (例如 Island 2)
                Log.i(TAG, "島嶼 ${currentIslandId} 完成，解鎖下一個島嶼: ${nextIslandId}")

                // a. 只解鎖下一個島嶼
                updateIslandUnlockedStatus(islandId = nextIslandId, isUnlocked = true)

                // b. 不再設置新島嶼的第一顆植物為「可領取」。
                // (新流程：由用戶在 LevelActivity 收到提示後，返回 MainActivity 選擇 Island 2 進入 LevelActivity，自動 claim 2-1)

            } else {
                Log.i(TAG, "所有關卡已完成！")
            }
        }
    }
    // 用於設定植物是否可領取
    private suspend fun setPlantAvailable(plantId: String, isAvailable: Boolean) = withContext(Dispatchers.IO) {
        // 1. 更新 DataStore
        context.plantDataStore.edit { preferences ->
            preferences[PreferencesKeys.plantAvailableKey(plantId)] = isAvailable
        }

        // 2. 更新快取中的 isAvailableToClaim
        plantsCache[plantId]?.let { currentPlant ->
            plantsCache[plantId] = currentPlant.copy(isAvailableToClaim = isAvailable)
        }
    }

    override suspend fun findNextActivePlantIdForIsland(islandId: Int): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "findNextActivePlantIdForIsland: 開始為島嶼 $islandId 尋找活躍植物 ID...")

        // 1. 取得該島嶼的所有植物 (從靜態快取 plantsCache 取得基本資料)
        val plantsForIsland = plantsCache.values
            .filter { it.islandId == islandId }
            .sortedBy { it.plantIndex }

        if (plantsForIsland.isEmpty()) {
            Log.w(TAG, "找不到島嶼 $islandId 的任何植物資料，使用預設值。")
            return@withContext "${islandId}_1"
        }

        // 2. 遍歷植物並檢查【最新的 DataStore 狀態】
        for (plantBase in plantsForIsland) {
            // 每次迭代都去 DataStore 獲取最新的狀態
            val status = getPlantStatus(plantBase.id)

            // a. 找到 Stage 1 或 Stage 2 (正在養成中)
            if (status.stage == 1 || status.stage == 2) {
                Log.d(TAG, "找到養成中的植物 (${plantBase.id}, Stage ${status.stage})，設為活躍。")
                return@withContext plantBase.id
            }

            // b. 找到 Stage 0 且 Is Available To Claim (圖鑑紅點提示領取)
            if (status.stage == 0 && status.isAvailableToClaim == true) {
                Log.d(TAG, "找到可領取的植物 (${plantBase.id})，設為活躍。")
                return@withContext plantBase.id
            }
        }

        // 3. 如果沒有找到活躍或可領取的植物，檢查是否是首次進入新的解鎖島嶼
        val defaultPlantId = plantsForIsland.first().id // 例如 "2_1"

        // 檢查該島嶼是否已解鎖 (從快取讀取)
        val islandUnlocked = islandsCache[islandId]?.isUnlocked ?: false
        val isCompleted = isIslandCompleted(islandId) // 檢查島嶼是否已完成
        val isFirstPlant = defaultPlantId.endsWith("_1")

        // 只有在【島嶼未完成】 且 【島嶼已解鎖】 且 【當前植物是該島嶼的第一顆】 且 【處於 Stage 0】 時，才設為可領取。
        if (islandUnlocked && isFirstPlant && !isCompleted) {
            // 發現新解鎖島嶼首次進入：將其狀態【暫時】設為可領取，然後返回其 ID
            // 這會觸發 LevelActivity 的 MainViewModel 執行 claimNewSeed (Stage 0 -> Stage 1)

            Log.i(TAG, "檢測到 **新解鎖** 島嶼 ${islandId}，${defaultPlantId} 是 Stage 0，將其標記為可領取並設為活躍。")
            setPlantAvailable(plantId = defaultPlantId, isAvailable = true)

            return@withContext defaultPlantId // 返回 "2_1"
        }


        // 4. 如果以上都不是 (例如 Island 1 完成後，所有植物都是 Stage 3)，回傳最後一個植物
        // 為了 UI 顯示，我們應該返回島嶼中最後一個植物的 ID (通常是 Stage 3 完成圖)
        val lastPlantId = plantsForIsland.last().id
        Log.d(TAG, "島嶼 $islandId 無養成中或可領取植物，回傳最後一個完成的植物 (${lastPlantId})。")
        return@withContext lastPlantId // 這裡改成回傳最後一個植物 ID (例如 "1_3")
    }

    // 檢查是否有任何植物處於「可領取」狀態
    override fun observeAnyPlantAvailable(): Flow<Boolean> = context.plantDataStore.data
        .map { preferences ->
            // 檢查所有植物 ID，看是否有任何一個 "Available" Key 為 true
            plantsCache.keys.any { plantId ->
                preferences[PreferencesKeys.plantAvailableKey(plantId)] ?: false
            }
        }.distinctUntilChanged()

    override suspend fun claimNewSeed(plantId: String) {
        Log.i(TAG, "claimNewSeed: 用戶領取新種子 ${plantId}")

        // 1. 獲取當前植物的狀態
        val currentStatus = getPlantStatus(plantId)

        // 2. 只有當植物未被種植 (Stage 0) 或狀態不存在時，才領取新種子
        if ( currentStatus.stage == 0) {

            // 設定 Stage = 1 (獲得種子/解鎖種植)
            updatePlantStatus(plantId = plantId, newStage = 1, newEnergy = 0)

            // 清除「可領取」旗標
            setPlantAvailable(plantId = plantId, isAvailable = false)

            // 設定為當前活躍植物
            setCurrentActivePlantId(plantId)

            Log.i(TAG, "種子 ${plantId} 已領取並設為當前活躍植物 (Stage 1)。")

        } else if (currentStatus.stage == 3) {
            // 如果植物已經是 Stage 3 (已完成)，我們不應該重置它，只需要確保它是活躍的即可
            Log.d(TAG, "植物 ${plantId} 已是 Stage 3，不進行領取操作，但確保其為活躍狀態。")
            setCurrentActivePlantId(plantId) // 確保它被設置為活躍

        } else {
            // 如果是 Stage 1 或 2 (正在種植中)，則保持狀態不變，但確保它被設為活躍
            Log.d(TAG, "植物 ${plantId} 處於 Stage ${currentStatus.stage}，保持狀態。")
            setCurrentActivePlantId(plantId)
        }
    }
    /**
     * 檢查指定島嶼上的所有植物是否都已達到 Stage 3 (即島嶼整體完成)。
     * @param islandId 要檢查的島嶼 ID。
     * @return 如果該島嶼所有植物都是 Stage 3，則返回 true。
     */
    override suspend fun isIslandCompleted(islandId: Int): Boolean = withContext(Dispatchers.IO) {
        // 1. 找出該島嶼上所有植物 ID
        val plantIdsForIsland = plantsCache.values
            .filter { it.islandId == islandId }
            .map { it.id }

        if (plantIdsForIsland.isEmpty()) {
            Log.w(TAG, "找不到島嶼 $islandId 的任何植物資料。")
            return@withContext false
        }

        // 2. 檢查每個植物的狀態是否為 Stage 3
        for (plantId in plantIdsForIsland) {
            // 使用非 Flow 的同步方法獲取當前狀態
            val status = getPlantStatus(plantId)

            if (status.stage != 3) {
                // 只要有一個植物不是 Stage 3，則島嶼未完成
                Log.d(TAG, "島嶼 $islandId 未完成，植物 ${plantId} 處於 Stage ${status.stage}")
                return@withContext false
            }
        }

        // 3. 全部都是 Stage 3
        Log.i(TAG, "島嶼 $islandId 的所有植物均已完成 (Stage 3)。")
        return@withContext true
    }
}

