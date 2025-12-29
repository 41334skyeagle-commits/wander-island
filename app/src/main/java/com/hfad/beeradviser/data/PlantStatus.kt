package com.hfad.beeradviser.data

/**
 * 代表單一植物在 DataStore 中儲存的動態狀態。
 * 這是從 PlantRepository.kt 讀取和寫入的狀態數據。
 *
 * @param stage 當前成長階段 (0:未解鎖/未種植, 1:幼苗, 2:成長中, 3:完成)
 * @param isUnlocked 種子是否已解鎖 (可用於 LevelActivity 開始種植)
 * @param currentEnergy 當前累積的能量值
 */
data class PlantStatus(
    val id: String? = null,
    val stage: Int = 0,
    val isUnlocked: Boolean = false, // 是否獲得種子 (Stage >= 1)
    val currentEnergy: Int = 0,
    val isAvailableToClaim: Boolean = false,
    val islandId: Int
)