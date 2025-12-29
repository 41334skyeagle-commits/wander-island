package com.hfad.beeradviser.data

data class Plant(
    val id: String,
    val name: String,
    val description: String,

    val stageOneImageRes: Int,  // R.drawable.plant_1_1_s1
    val stageTwoImageRes: Int,  // R.drawable.plant_1_1_s2
    val stageThreeImageRes: Int, // R.drawable.plant_1_1_s3
    val energyRequired: Int,
    val isAvailableToClaim: Boolean = false,

    val islandId: Int,
    val plantIndex: Int,
    var stage: Int = 0,
    val currentEnergy: Int = 0,
    val totalEnergyRequired: Int
)