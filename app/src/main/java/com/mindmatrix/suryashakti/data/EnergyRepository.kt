package com.mindmatrix.suryashakti.data

class EnergyRepository(private val dao: EnergyLogDao) {
    val recentLogs = dao.observeRecentLogs()

    suspend fun save(log: EnergyLogEntity) {
        dao.insert(log)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
