package com.hfad.beeradviser.data

data class Island(
    val id: Int, // 關卡/島嶼的 ID (例如 1, 2, 3...)
    val name: String, // 關卡名稱 (例如 "新手島", "火山島")
    var isUnlocked: Boolean = false // 該關卡是否已解鎖，預設為 false
)