package com.hfad.beeradviser.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import java.io.Reader

class PlantDataSource(private val context: Context) {

    // 一個專門用於 GSON 解析的 Data Class
    // 注意：這個類別不包含 stage，因為 stage 是動態狀態，不從 JSON 載入。
    private data class JsonPlant(
        val id: String,
        val name: String,
        val description: String,
        val stageOneImageFileName: String,
        val stageTwoImageFileName: String,
        val stageThreeImageFileName: String,
        val islandId: Int,
        val plantIndex: Int,
        val totalEnergyRequired: Int
    )

    fun loadPlants(): List<Plant> {
        // 從 assets/plants.json 讀取數據
        val inputStream = context.assets.open("plants.json")
        val reader: Reader = InputStreamReader(inputStream)

        // 使用 GSON 解析
        val plantType = object : TypeToken<List<JsonPlant>>() {}.type
        val jsonPlants: List<JsonPlant> = Gson().fromJson(reader, plantType)


        return jsonPlants.map { jsonPlant ->

            val stageOneRes = getDrawableId(jsonPlant.stageOneImageFileName)
            val stageTwoRes = getDrawableId(jsonPlant.stageTwoImageFileName)
            val stageThreeRes = getDrawableId(jsonPlant.stageThreeImageFileName)


            Plant(
                id = jsonPlant.id,
                name = jsonPlant.name,
                description = jsonPlant.description,
                stageOneImageRes = stageOneRes,
                stageTwoImageRes = stageTwoRes,
                stageThreeImageRes = stageThreeRes,
                energyRequired = jsonPlant.totalEnergyRequired,
                islandId = jsonPlant.islandId,
                plantIndex = jsonPlant.plantIndex,
                stage = 0, // 動態狀態，預設為 0
                currentEnergy = 0, // 動態狀態，預設為 0
                totalEnergyRequired = jsonPlant.totalEnergyRequired

            )
        }
    }
    private fun getDrawableId(fileName: String): Int {
        return context.resources.getIdentifier(
            fileName,
            "drawable", // 資源類型：從 drawable 資料夾中尋找
            context.packageName // 專案包名
        )
    }
}