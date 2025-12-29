package com.hfad.beeradviser.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hfad.beeradviser.data.PlantRepository

// 這個 Factory 負責創建 PlantPokedexViewModel，並將 Repository 依賴傳遞給它
class PlantPokedexViewModelFactory(private val plantRepository: PlantRepository) : ViewModelProvider.Factory {

    // 【修改】使用 @Suppress("UNCHECKED_CAST") 來處理 Kotlin DSL 版本的轉換警告
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlantPokedexViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlantPokedexViewModel(plantRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}