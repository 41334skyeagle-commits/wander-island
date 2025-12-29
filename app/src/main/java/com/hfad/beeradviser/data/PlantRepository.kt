package com.hfad.beeradviser.data
import kotlinx.coroutines.flow.Flow
interface PlantRepository {

    // --- Plant 相關方法 (階段 Stage 取代 Obtained) ---
    suspend fun getAllPlants(): List<Plant>
    suspend fun getPlantById(plantId: String): Plant?

    suspend fun getPlantStatus(plantId: String): PlantStatus
    suspend fun updatePlantStatus(plantId: String, newStage: Int, newEnergy: Int)


    // 獲取植物的階段狀態 (Flow 讓 UI 響應式更新)
    fun getPlantStageFlow(plantId: String): Flow<Int>
    fun getPlantStatusFlow(plantId: String): Flow<PlantStatus>


    fun getAllIslandsFlow(): Flow<List<Island>>
    // 獲取所有關卡列表及其解鎖狀態
    suspend fun getAllIslands(): List<Island>

    // 更新關卡的解鎖狀態
    suspend fun updateIslandUnlockedStatus(islandId: Int, isUnlocked: Boolean)


    // --- 活躍植物追蹤 (Current Active Plant) ---
    // 獲取當前正在種植/計時的植物 ID (用於計時器功能)
    fun getCurrentActivePlantId(): Flow<String?>

    suspend fun getCurrentActivePlantIdSuspend(): String?

    // 設定當前正在種植/計時的植物 ID
    suspend fun setCurrentActivePlantId(plantId: String?)

    // 檢查是否有任何植物處於「可領取」狀態
    fun observeAnyPlantAvailable(): Flow<Boolean>

    // 用戶點擊領取種子時呼叫
    suspend fun claimNewSeed(plantId: String)


    // --- 初始化和載入 ---
    // 載入所有植物、關卡狀態和階段狀態到快取
    suspend fun loadAllDataIntoCache()

    suspend fun ensureActivePlantExists()

    suspend fun checkAndUnlockNextSeed(completedPlantId: String)

    /**
     * 檢查指定島嶼上的所有植物是否都已達到 Stage 3。
     * @param islandId 要檢查的島嶼 ID。
     * @return 如果該島嶼所有植物都是 Stage 3，則返回 true。
     */
    suspend fun isIslandCompleted(islandId: Int): Boolean

    suspend fun findNextActivePlantIdForIsland(islandId: Int): String
}