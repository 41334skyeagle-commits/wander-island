package com.hfad.beeradviser

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.hfad.beeradviser.data.PlantDataSource
import com.hfad.beeradviser.data.PlantRepository
import com.hfad.beeradviser.data.PlantRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


val Context.plantDataStore: DataStore<Preferences> by preferencesDataStore(name = "plant_preferences")


class PlantPokedexApplication : MultiDexApplication() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this) // <--- 確保在 onCreate 之前執行
    }
    private val plantDataSource by lazy { PlantDataSource(applicationContext) }

    val plantRepository: PlantRepository by lazy {
        PlantRepositoryImpl(plantDataSource, applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        // 在應用程式啟動時載入所有數據
        CoroutineScope(Dispatchers.IO).launch {
            plantRepository.loadAllDataIntoCache()
        }
    }
}

