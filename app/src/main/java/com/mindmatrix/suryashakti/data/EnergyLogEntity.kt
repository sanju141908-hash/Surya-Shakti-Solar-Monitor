package com.mindmatrix.suryashakti.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "energy_logs")
data class EnergyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val dateLabel: String,
    val generationKwh: Double,
    val consumptionKwh: Double,
    val batteryPercent: Int,
    val ratePerKwh: Double,
    val weather: String
)
