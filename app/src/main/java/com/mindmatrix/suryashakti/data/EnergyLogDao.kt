package com.mindmatrix.suryashakti.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EnergyLogDao {
    @Query("SELECT * FROM energy_logs ORDER BY createdAtMillis DESC LIMIT 30")
    fun observeRecentLogs(): Flow<List<EnergyLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: EnergyLogEntity)

    @Query("DELETE FROM energy_logs")
    suspend fun clearAll()
}
