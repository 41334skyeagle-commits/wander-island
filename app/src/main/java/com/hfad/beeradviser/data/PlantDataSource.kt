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

// 產出一個包含所有植物基本資料的清單（List）。
fun loadPlants(): List<Plant> {
    // 找到 assets 資料夾裡面的 plants.json 並打開「水龍頭」（InputStream）準備流出資料
    val inputStream = context.assets.open("plants.json")
    // 這裡讀出來的是 位元 Bytes 但我要讀的是 文字 Character，InputStreamReader 這個類別就像一個轉接頭，負責把位元轉成文字
    val reader: Reader = InputStreamReader(inputStream)

    // 使用 GSON 解析
    val plantType = object : TypeToken<List<JsonPlant>>() {}.type
    val jsonPlants: List<JsonPlant> = Gson().fromJson(reader, plantType)

    /* JsonPlant (為了解析 JSON 存在的 它存檔名如 plant_1_1_s1) → Map (用它轉換) → Plant (Resource ID 如 21312308)
       透過 map 將 JSON 中的檔名轉換為遊戲邏輯所需的資源 ID。 */
    return jsonPlants.map { jsonPlant ->

        // 這裡在做轉換（Data Transformation）
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

        )// 產出最終的 Plant 物件
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